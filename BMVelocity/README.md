# BMVelocity

Velocity plugin used with BMEssentials for the Blockminer network.

## Vote System Overview
- Acts as the source of truth for vote streaks, lifetime vote counts, and altar token/vote point rewards.
- Stores pre-determined rewards for offline players in SQLite and delivers them one-at-a-time when the player next connects.
- Sends MiniMessage-formatted streak announcements (streak ≥ 4) as votes arrive.
- Communicates rewards to BMEssentials via encrypted plugin messages; the Spigot side simply executes the provided reward without recalculating anything.
- Token odds smoothly interpolate from early-game values (~55/30/15) toward late-game targets (~35/40/25) over a 30-day capped streak.
