package at.sleazlee.bmvelocity.AnnouncementSystem;

import at.sleazlee.bmvelocity.BMVelocity;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AnnouncementSystem {

    private final BMVelocity plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final Scheduler scheduler;
    private final Path dataPath;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final List<Announcement> announcements = new ArrayList<>();
    private Announcement lastAnnouncement = null;

    public AnnouncementSystem(BMVelocity plugin, ProxyServer server, Logger logger, Scheduler scheduler, Path dataPath) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.scheduler = scheduler;
        this.dataPath = dataPath;
    }

    public void initialize() {
        try {
            Files.createDirectories(dataPath);
        } catch (IOException e) {
            logger.error("Could not create plugin data directory!", e);
            return;
        }

        Path configPath = dataPath.resolve("announcements.yml");
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
        }
        loadAnnouncements(configPath);

        // Schedule broadcasting every 12 minutes
        scheduler.buildTask(plugin, this::broadcastRandomAnnouncement)
                .repeat(12, TimeUnit.MINUTES)
                .schedule();
    }

    private void loadAnnouncements(Path path) {
        try (var in = Files.newInputStream(path)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(in);

            if (data == null) {
                logger.warn("Announcements YAML is empty or invalid!");
                return;
            }

            Object rawList = data.get("announcements");
            if (rawList instanceof List<?>) {
                for (Object obj : (List<?>) rawList) {
                    if (obj instanceof Map<?, ?> map) {
                        String text = Objects.toString(map.get("text"), "");
                        int rarity = parseRarity(map.get("rarity"));
                        announcements.add(new Announcement(text, rarity));
                    }
                }
            }

            server.getConsoleCommandSource().sendMessage(miniMessage.deserialize("<gray>    Discovered <DARK_AQUA>" + announcements.size() + "</DARK_AQUA> announcements."));
        } catch (IOException e) {
            logger.error("Failed to load announcements.yml", e);
        }
    }

    private int parseRarity(Object raw) {
        if (raw == null) return 1;
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void createDefaultConfig(Path path) {
        String defaultYaml =
                "announcements:\n" +
                        "  - text: \"<green>This is a common announcement!\"\n" +
                        "    rarity: 5\n" +
                        "  - text: \"<yellow>This is a bit less common.\"\n" +
                        "    rarity: 2\n" +
                        "  - text: \"<red>You might never see me!\"\n" +
                        "    rarity: 1\n";
        try {
            Files.writeString(path, defaultYaml);
        } catch (IOException e) {
            logger.error("Failed to write default announcements.yml", e);
        }
    }

    private void broadcastRandomAnnouncement() {

        if (announcements.isEmpty()) {
            logger.warn("No announcements to broadcast!");
            return;
        }

    Announcement chosen = pickWeightedRandom(announcements, lastAnnouncement);
        if (chosen == null) return;

        lastAnnouncement = chosen;
        Component component = MiniMessage.miniMessage().deserialize(chosen.text());
        server.sendMessage(component);
    }

    private Announcement pickWeightedRandom(List<Announcement> list, Announcement lastUsed) {
        int total = list.stream().mapToInt(Announcement::rarity).sum();
        if (total <= 0) return null;

        int random = new Random().nextInt(total) + 1;
        int cumulative = 0;
        for (Announcement a : list) {
            cumulative += a.rarity();
            if (random <= cumulative) {
                // If we happen to pick the same as lastUsed, try again (if more than 1 announcement)
                if (lastUsed != null && a == lastUsed && list.size() > 1) {
                    return pickWeightedRandom(list, lastUsed);
                }
                return a;
            }
        }
        return null;
    }

    // A simple data class to store text + rarity
    private static class Announcement {
        private final String text;
        private final int rarity;

        public Announcement(String text, int rarity) {
            this.text = text;
            this.rarity = rarity;
        }

        public String text() {
            return text;
        }

        public int rarity() {
            return rarity;
        }
    }
}
