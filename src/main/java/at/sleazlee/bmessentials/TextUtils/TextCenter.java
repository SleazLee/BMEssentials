package at.sleazlee.bmessentials.TextUtils;

public class TextCenter {

    /**
     * Centers the given message within the default chat width.
     *
     * @param message the message to be centered
     * @return the centered message
     */
    public static String center(String message) {
        int defaultTotalSpaces = 79; // Adjust this value based on your needs
        return center(message, defaultTotalSpaces, null);
    }

    /**
     * Centers the given message within the default chat width, with optional strikeColorName.
     *
     * @param message         the message to be centered
     * @param strikeColorName the color name for the strikethrough spaces
     * @return the centered message
     */
    public static String center(String message, String strikeColorName) {
        int defaultTotalSpaces = 79; // Adjust this value based on your needs
        return center(message, defaultTotalSpaces, strikeColorName);
    }

    /**
     * Centers the given message within the specified number of spaces.
     *
     * @param message     the message to be centered
     * @param totalSpaces the total number of spaces to center within
     * @return the centered message
     */
    public static String center(String message, int totalSpaces) {
        return center(message, totalSpaces, null);
    }

    /**
     * Centers the given message within the specified number of spaces, with optional strikeColorName.
     *
     * @param message         the message to be centered
     * @param totalSpaces     the total number of spaces to center within
     * @param strikeColorName the color name for the strikethrough spaces
     * @return the centered message
     */
    public static String center(String message, int totalSpaces, String strikeColorName) {
        if (message == null || message.equals("")) return "";

        // Check if strikeColorName is provided
        boolean useStrike = strikeColorName != null && !strikeColorName.isEmpty();
        if (useStrike) {
            message = " " + message + " ";
        }

        // Calculate the total width in pixels of the given number of spaces
        int spaceWidth = DefaultFontInfo.SPACE.getLength() + 1;
        int totalWidth = totalSpaces * spaceWidth;

        int messagePxSize = 0;
        boolean isBold = false;
        int index = 0;

        while (index < message.length()) {
            char c = message.charAt(index);

            // Handle MiniMessage tags
            if (c == '<') {
                int closingIndex = message.indexOf('>', index);
                if (closingIndex != -1) {
                    String tag = message.substring(index + 1, closingIndex);

                    // Adjust bold state based on tags
                    if (tag.equalsIgnoreCase("bold")) {
                        isBold = true;
                    } else if (tag.equalsIgnoreCase("/bold") || tag.equalsIgnoreCase("reset")) {
                        isBold = false;
                    } // Other tags are ignored for width calculation

                    // Skip over the tag
                    index = closingIndex + 1;
                    continue;
                } else {
                    // No closing '>', treat as normal character
                    index++;
                    continue;
                }
            }

            // Regular character
            DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
            messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
            messagePxSize++;
            index++;
        }

        // Calculate the amount of space needed to center the message within the total width
        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = (totalWidth / 2) - halvedMessageSize;
        StringBuilder sb = new StringBuilder();

        if (useStrike) {
            // Divide compensation between left and right
            int leftCompensate = toCompensate - 1;
            int rightCompensate = toCompensate - 1;

            // Build left padding with strikethrough
            sb.append("<color:").append(strikeColorName).append("><st>");
            int compensated = 0;
            while (compensated < leftCompensate) {
                sb.append(" ");
                compensated += spaceWidth;
            }
            sb.append("</st></color>");

            // Append the message
            sb.append(message);

            // Build right padding with strikethrough
            StringBuilder sbRight = new StringBuilder();
            sbRight.append("<color:").append(strikeColorName).append("><st>");
            compensated = 0;
            while (compensated < rightCompensate) {
                sbRight.append(" ");
                compensated += spaceWidth;
            }
            sbRight.append("</st></color>");

            // Append the right padding
            sb.append(sbRight.toString());
        } else {
            // Compensate only on the left side
            int compensated = 0;
            while (compensated < toCompensate) {
                sb.append(" ");
                compensated += spaceWidth;
            }
            // Append the message
            sb.append(message);
        }

        // Return the centered message with MiniMessage formatting preserved
        return sb.toString();
    }

    /**
     * Generates a full-line strikethrough in the specified color.
     *
     * @param colorName the color name for the strikethrough line
     * @return the full-line strikethrough string
     */
    public static String fullLineStrike(String colorName) {
        int defaultTotalSpaces = 79; // Adjust this value as needed
        int spaceWidth = DefaultFontInfo.SPACE.getLength() + 1;
        int totalWidth = defaultTotalSpaces * spaceWidth;

        // Build the strikethrough line
        StringBuilder sb = new StringBuilder();
        sb.append("<color:").append(colorName).append("><st>");

        int compensated = 0;
        while (compensated < totalWidth) {
            sb.append(" ");
            compensated += spaceWidth;
        }

        sb.append("</st></color>");

        return sb.toString();
    }

    /**
     * Enum representing the default width of Minecraft characters.
     */
    public enum DefaultFontInfo {
        // Character definitions with their corresponding widths
        A('A', 5),
        a('a', 5),
        B('B', 5),
        b('b', 5),
        C('C', 5),
        c('c', 5),
        D('D', 5),
        d('d', 5),
        E('E', 5),
        e('e', 5),
        F('F', 5),
        f('f', 4),
        G('G', 5),
        g('g', 5),
        H('H', 5),
        h('h', 5),
        I('I', 3),
        i('i', 1),
        J('J', 5),
        j('j', 5),
        K('K', 5),
        k('k', 4),
        L('L', 5),
        l('l', 1),
        M('M', 5),
        m('m', 5),
        N('N', 5),
        n('n', 5),
        O('O', 5),
        o('o', 5),
        P('P', 5),
        p('p', 5),
        Q('Q', 5),
        q('q', 5),
        R('R', 5),
        r('r', 5),
        S('S', 5),
        s('s', 5),
        T('T', 5),
        t('t', 4),
        U('U', 5),
        u('u', 5),
        V('V', 5),
        v('v', 5),
        W('W', 5),
        w('w', 5),
        X('X', 5),
        x('x', 5),
        Y('Y', 5),
        y('y', 5),
        Z('Z', 5),
        z('z', 5),
        NUM_1('1', 5),
        NUM_2('2', 5),
        NUM_3('3', 5),
        NUM_4('4', 5),
        NUM_5('5', 5),
        NUM_6('6', 5),
        NUM_7('7', 5),
        NUM_8('8', 5),
        NUM_9('9', 5),
        NUM_0('0', 5),
        EXCLAMATION_POINT('!', 1),
        AT_SYMBOL('@', 6),
        NUM_SIGN('#', 5),
        DOLLAR_SIGN('$', 5),
        PERCENT('%', 5),
        UP_ARROW('^', 5),
        AMPERSAND('&', 5),
        ASTERISK('*', 5),
        LEFT_PARENTHESIS('(', 4),
        RIGHT_PARENTHESIS(')', 4),
        MINUS('-', 5),
        UNDERSCORE('_', 5),
        PLUS_SIGN('+', 5),
        EQUALS_SIGN('=', 5),
        LEFT_CURL_BRACE('{', 4),
        RIGHT_CURL_BRACE('}', 4),
        LEFT_BRACKET('[', 3),
        RIGHT_BRACKET(']', 3),
        COLON(':', 1),
        SEMI_COLON(';', 1),
        DOUBLE_QUOTE('"', 3),
        SINGLE_QUOTE('\'', 1),
        LEFT_ARROW('<', 4),
        RIGHT_ARROW('>', 4),
        QUESTION_MARK('?', 5),
        SLASH('/', 5),
        BACK_SLASH('\\', 5),
        LINE('|', 1),
        TILDE('~', 5),
        TICK('`', 2),
        PERIOD('.', 1),
        COMMA(',', 1),
        SPACE(' ', 3),
        DEFAULT('a', 4);

        private char character; // The character
        private int length;     // The width of the character in pixels

        /**
         * Constructs a DefaultFontInfo for a character with a specified length.
         *
         * @param character the character
         * @param length    the width of the character in pixels
         */
        DefaultFontInfo(char character, int length) {
            this.character = character;
            this.length = length;
        }

        /**
         * Gets the character.
         *
         * @return the character
         */
        public char getCharacter() {
            return this.character;
        }

        /**
         * Gets the length of the character in pixels.
         *
         * @return the length in pixels
         */
        public int getLength() {
            return this.length;
        }

        /**
         * Gets the length of the character when bold.
         *
         * @return the bold length in pixels
         */
        public int getBoldLength() {
            if (this == DefaultFontInfo.SPACE) return this.getLength();
            return this.length + 1;
        }

        /**
         * Retrieves the DefaultFontInfo for a given character.
         *
         * @param c the character
         * @return the corresponding DefaultFontInfo
         */
        public static DefaultFontInfo getDefaultFontInfo(char c) {
            for (DefaultFontInfo dFI : DefaultFontInfo.values()) {
                if (dFI.getCharacter() == c) return dFI;
            }
            return DefaultFontInfo.DEFAULT;
        }
    }
}
