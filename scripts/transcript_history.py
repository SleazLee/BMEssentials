"""Utilities for tracking transcript download history.

This module provides a :class:`HistoryTracker` helper that keeps a JSON
backed record of the videos that have already been processed.  The helper is
aware of a few failure modes that the YouTube data pipeline routinely hits:

* ``HTTP 418`` responses which indicate the video has no auto-generated
  transcript available.
* Members only videos where ``yt-dlp`` returns an error message requesting the
  viewer to join the channel.
* Shorts that are skipped because of their runtime.

In all of these cases we still want to treat the video as handled so future
runs do not continue retrying the same work.  The tracker therefore marks these
conditions as ``True`` in the history log instead of leaving them as
unprocessed failures.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
import json
import logging
from typing import Any, Dict, Iterable, MutableMapping, Optional

log = logging.getLogger(__name__)


NON_RETRYABLE_ERROR_STRINGS: tuple[str, ...] = (
    "http 418",
    "i'm a teapot",  # alternate message for 418 responses
    "members-only",
    "join this channel to get access to members-only content",
)


@dataclass
class HistoryTracker:
    """Persist the processing outcome for each video.

    Parameters
    ----------
    history_file:
        Location on disk where the JSON history mapping should be stored.
    storage:
        Optional pre-populated mapping.  Primarily useful for tests.
    """

    history_file: Path
    storage: MutableMapping[str, bool] | None = None
    _history: MutableMapping[str, bool] = field(init=False, repr=False)

    def __post_init__(self) -> None:
        self.history_file = Path(self.history_file)
        if self.storage is not None:
            self._history = self.storage
        else:
            self._history = self._load()

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------
    def processed(self, video_id: str) -> bool:
        """Return whether ``video_id`` has been processed."""

        return bool(self._history.get(video_id))

    def mark_success(self, video_id: str) -> None:
        """Record a successful transcript fetch for ``video_id``."""

        self._history[video_id] = True
        self._persist()

    def mark_skipped_short(self, video_id: str) -> None:
        """Record that the video was skipped because it is a short.

        Shorts do not expose transcripts, therefore they should be marked as
        processed to avoid retrying them on each run.
        """

        log.debug("Marking %s as processed because it is a short", video_id)
        self.mark_success(video_id)

    def mark_failure(self, video_id: str, error: Optional[BaseException]) -> bool:
        """Record a failure outcome for ``video_id``.

        ``True`` is returned when the failure should still be treated as a
        completed item (e.g. HTTP 418, members-only videos).  ``False`` is
        returned when the caller should retry in the future.
        """

        if self._is_non_retryable(error):
            log.debug(
                "Treating failure for %s as processed because it is non-retryable: %s",
                video_id,
                error,
            )
            self.mark_success(video_id)
            return True

        self._history[video_id] = False
        self._persist()
        return False

    def update_many(self, processed_ids: Iterable[str]) -> None:
        """Bulk mark ``processed_ids`` as successfully handled."""

        changed = False
        for video_id in processed_ids:
            if not self._history.get(video_id):
                self._history[video_id] = True
                changed = True
        if changed:
            self._persist()

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------
    def _load(self) -> MutableMapping[str, bool]:
        if not self.history_file.exists():
            return {}

        try:
            with self.history_file.open("r", encoding="utf-8") as fh:
                data: Dict[str, Any] = json.load(fh)
        except json.JSONDecodeError as exc:  # pragma: no cover - extremely rare
            log.warning(
                "History file %s is corrupt, starting fresh: %s",
                self.history_file,
                exc,
            )
            return {}

        return {str(k): bool(v) for k, v in data.items()}

    def _persist(self) -> None:
        if self.storage is not None:
            # Tests may inject a dict and expect no file IO.
            return

        self.history_file.parent.mkdir(parents=True, exist_ok=True)
        with self.history_file.open("w", encoding="utf-8") as fh:
            json.dump(self._history, fh, indent=2, sort_keys=True)

    @staticmethod
    def _is_non_retryable(error: Optional[BaseException]) -> bool:
        if error is None:
            return False

        # Direct attributes from HTTP style exceptions.
        status = getattr(error, "status", None) or getattr(error, "code", None)
        if isinstance(status, int) and status == 418:
            return True

        message = str(error).lower()
        if not message:
            return False

        for needle in NON_RETRYABLE_ERROR_STRINGS:
            if needle in message:
                return True

        return False


__all__ = ["HistoryTracker"]
