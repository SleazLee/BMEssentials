package at.sleazlee.bmessentials.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for converting legacy Minecraft color codes to MiniMessage tags.
 * <p>
 * This class supports both {@code &} and {@code §} symbols for legacy color codes.
 * </p>
 */
public class replaceLegacyColors {

    /**
     * Replaces legacy Minecraft color codes (e.g., {@code &7} or {@code §7}) with MiniMessage tags.
     * <p>
     * For example, the input {@code "&7Hello §bWorld"} will be converted to
     * {@code "<gray>Hello <aqua>World"}.
     * </p>
     *
     * @param input the input string containing legacy color codes.
     * @return the string with MiniMessage tags replacing legacy color codes.
     */
    public static String replaceLegacyColors(String input) {
        // Map of legacy color codes to their corresponding MiniMessage tags
        Map<Character, String> legacyToMiniMessage = new HashMap<>();
        legacyToMiniMessage.put('0', "black");
        legacyToMiniMessage.put('1', "dark_blue");
        legacyToMiniMessage.put('2', "dark_green");
        legacyToMiniMessage.put('3', "dark_aqua");
        legacyToMiniMessage.put('4', "dark_red");
        legacyToMiniMessage.put('5', "dark_purple");
        legacyToMiniMessage.put('6', "gold");
        legacyToMiniMessage.put('7', "gray");
        legacyToMiniMessage.put('8', "dark_gray");
        legacyToMiniMessage.put('9', "blue");
        legacyToMiniMessage.put('a', "green");
        legacyToMiniMessage.put('b', "aqua");
        legacyToMiniMessage.put('c', "red");
        legacyToMiniMessage.put('d', "light_purple");
        legacyToMiniMessage.put('e', "yellow");
        legacyToMiniMessage.put('f', "white");
        legacyToMiniMessage.put('k', "obfuscated");
        legacyToMiniMessage.put('l', "bold");
        legacyToMiniMessage.put('m', "strikethrough");
        legacyToMiniMessage.put('n', "underlined");
        legacyToMiniMessage.put('o', "italic");
        legacyToMiniMessage.put('r', "reset");

        // StringBuilder for constructing the result
        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();

        // Iterate through the input string
        for (int i = 0; i < chars.length; i++) {
            // Check for valid legacy color code prefixes (& or §)
            if ((chars[i] == '&' || chars[i] == '§') && i + 1 < chars.length) {
                char code = chars[i + 1];
                // Retrieve the corresponding MiniMessage tag
                String tag = legacyToMiniMessage.get(Character.toLowerCase(code));
                if (tag != null) {
                    // Append the MiniMessage tag to the result
                    result.append('<').append(tag).append('>');
                    i++; // Skip the next character as it is part of the code
                    continue;
                }
            }
            // Append regular characters to the result
            result.append(chars[i]);
        }
        return result.toString();
    }
}
