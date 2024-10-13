import yaml
import re

# Define mappings for color codes and formatting using color names
COLOR_CODES = {
    '&0': ' ',  # Blank space
    '&1': '<dark_blue>',
    '&2': '<dark_green>',
    '&3': '<dark_aqua>',
    '&4': '<red>',
    '&5': '<dark_purple>',
    '&6': '<gold>',
    '&7': ' ',  # Blank space
    '&8': '<dark_gray>',
    '&9': '<blue>',
    '&a': '<green>',
    '&b': '<aqua>',
    '&c': '<red>',
    '&d': '<light_purple>',
    '&e': '<yellow>',
    '&f': '<white>',
    # Formatting
    '&l': '<bold>',
    '&m': '<strikethrough>',
    '&n': '<underlined>',
    '&o': '<italic>',
    '&r': '</>',
    # Reset formatting (could be extended if needed)
    # Obfuscated (&k) and other unsupported codes can be handled if necessary
}

# Regular expression patterns
color_code_pattern = re.compile(r'(&[0-9a-fk-or])', re.IGNORECASE)
and_pattern = re.compile(r'<and>')
run_command_pattern = re.compile(r'\$RUN_COMMAND\$')
newline_pattern = re.compile(r'<newline>')


def convert_color_codes(text):
    """
    Replace legacy & codes with MiniMessage tags based on color names.
    """

    def replace_color(match):
        code = match.group(1).lower()
        return COLOR_CODES.get(code, '')

    return color_code_pattern.sub(replace_color, text)


def handle_and_construct(text):
    """
    Remove <and> constructs as MiniMessage supports nested tags inherently.
    """
    return and_pattern.sub('', text)


def handle_run_command(text):
    """
    Replace $RUN_COMMAND$ placeholders with MiniMessage clickable commands.
    Assumes the format: $RUN_COMMAND$Display Text; /command
    Example:
    $RUN_COMMAND$Go Back; /help337
    """
    # This pattern matches $RUN_COMMAND$ followed by display text, a semicolon, and the command
    pattern = re.compile(r'\$RUN_COMMAND\$([^;]+);([^<]+)<and>')

    def replace_run_command(match):
        display_text = match.group(1).strip()
        command = match.group(2).strip()
        return f'<click:run_command:"{command}">{display_text}</click>'

    return pattern.sub(replace_run_command, text)


def handle_newline(text):
    """
    Replace <newline> with actual line breaks.
    """
    return newline_pattern.sub('\n', text)


def clean_text(text):
    """
    Clean &0 and &7 used as blank spaces.
    Since we've already replaced &0 and &7 with spaces, we can further clean redundant spaces.
    """
    # Replace multiple spaces with a single space
    return re.sub(r' {2,}', ' ', text)


def convert_text(text):
    """
    Perform all conversions on the text.
    """
    text = convert_color_codes(text)
    text = handle_and_construct(text)
    text = handle_run_command(text)
    text = handle_newline(text)
    text = clean_text(text)
    return text


def convert_menus(old_menus):
    """
    Convert the entire menus configuration.
    """
    new_menus = {'Books': {}}

    for book_name, pages in old_menus.get('Books', {}).items():
        new_pages = []
        for page in pages:
            converted_page = convert_text(page)
            new_pages.append(converted_page)
        new_menus['Books'][book_name] = new_pages

    # Copy over other configurations if necessary, e.g., Systems.Menus.Cooldown
    if 'systems' in old_menus and 'Menus' in old_menus['systems']:
        new_menus['systems'] = {'Menus': old_menus['systems']['Menus']}

    return new_menus


def main():
    # Load the old menus.yml
    with open('menus_old.yml', 'r', encoding='utf-8') as file:
        old_menus = yaml.safe_load(file)

    # Convert the menus
    new_menus = convert_menus(old_menus)

    # Save the new menus.yml
    with open('menus_new.yml', 'w', encoding='utf-8') as file:
        yaml.dump(new_menus, file, allow_unicode=True)

    print("Conversion complete. The new menus.yml has been saved as 'menus_new.yml'.")


if __name__ == "__main__":
    main()
