package at.sleazlee.bmvelocity.VTell;

import at.sleazlee.bmvelocity.BMVelocity;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class VTellListener {
    private final BMVelocity plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public VTellListener(BMVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // only our vtell channel
        if (!event.getIdentifier().getId().equals("bmessentials:vtell")) {
            return;
        }
        // drop anything coming from a client directly
        if (event.getSource() instanceof com.velocitypowered.api.proxy.Player) {
            return;
        }

        // 1) decrypt
        byte[] encrypted = event.getData();
        byte[] decrypted;
        try {
            decrypted = plugin.getAes().decrypt(encrypted);
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to decrypt vtell message, dropping it.", e);
            return;
        }

        // 2) interpret as UTF‑8 string (no DataInputStream.readUTF())
        String message = new String(decrypted, StandardCharsets.UTF_8);

        // 3) convert legacy color codes → MiniMessage tags
        message = replaceLegacyColors(message);

        // 4) split on your newline token and send each line
        String[] lines = message.split("<newline>");
        plugin.getServer().getAllPlayers().forEach(player -> {
            for (String line : lines) {
                player.sendMessage(miniMessage.deserialize(line));
            }
        });
    }

    /**
     * Replaces legacy Minecraft color codes (&7 or §7) with MiniMessage tags.
     */
    public static String replaceLegacyColors(String input) {
        Map<Character, String> legacyToMini = new HashMap<>();
        legacyToMini.put('0', "black");      legacyToMini.put('1', "dark_blue");
        legacyToMini.put('2', "dark_green"); legacyToMini.put('3', "dark_aqua");
        legacyToMini.put('4', "dark_red");   legacyToMini.put('5', "dark_purple");
        legacyToMini.put('6', "gold");       legacyToMini.put('7', "gray");
        legacyToMini.put('8', "dark_gray");  legacyToMini.put('9', "blue");
        legacyToMini.put('a', "green");      legacyToMini.put('b', "aqua");
        legacyToMini.put('c', "red");        legacyToMini.put('d', "light_purple");
        legacyToMini.put('e', "yellow");     legacyToMini.put('f', "white");
        legacyToMini.put('k', "obfuscated"); legacyToMini.put('l', "bold");
        legacyToMini.put('m', "strikethrough"); legacyToMini.put('n', "underlined");
        legacyToMini.put('o', "italic");     legacyToMini.put('r', "reset");

        StringBuilder sb = new StringBuilder();
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if ((c == '&' || c == '§') && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[i + 1]);
                String tag = legacyToMini.get(code);
                if (tag != null) {
                    sb.append('<').append(tag).append('>');
                    i++; // skip the code char
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
