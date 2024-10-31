package at.sleazlee.bmessentials.TextUtils;

import java.util.HashMap;
import java.util.Map;

public class replaceLegacyColors {
    public static String replaceLegacyColors(String input) {
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

        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                char code = chars[i + 1];
                String tag = legacyToMiniMessage.get(Character.toLowerCase(code));
                if (tag != null) {
                    result.append('<').append(tag).append('>');
                    i++; // Skip the next character as it is part of the code
                    continue;
                }
            }
            result.append(chars[i]);
        }
        return result.toString();
    }
}
