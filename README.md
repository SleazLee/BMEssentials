<pre>
 ____  __  __ ______
|  _ \|  \/  |  ____|
| |_) | \  / | |__
|  _ <| |\/| |  __|
| |_) | |  | | |____
|____/|_|  |_|______|
</pre>

# **BMEssentials**

## Overview
**BMEssentials** is a multi-purpose Spigot plugin designed for the Blockminer network. It bundles many utilities such as teleport systems, economy, rank ups and more into a single plugin.

- **Version**: `2.0`
- **API Version**: `1.21`
- **Author**: `SleazLee`
- **Soft Dependencies**: `VaultUnlocked, HuskHomes, PlaceholderAPI, LuckPerms, Lands, BlueMap`

## Major Features
- **Player Data & Economy** – MySQL backed data storage with PlaceholderAPI expansions and Vault compatible economy commands (`/pay`, `/bal`, `/eco`).
- **Rank Up System** – Configurable requirements for progressing through ranks.
- **TPShop** – Simple teleport shop command with tab completion.
- **Vote & Vot Systems** – Cross-server vote handling and in‑game weather/time votes.
- **Wild Teleport** – Safe random teleportation around the world.
- **Spawn Systems** – Healing springs, wishing well, obelisk and first join messages.
- **Common Commands** – `/playtime`, `/lag`, `/bmdiscord` and server restart utilities.
- **Virtual Containers** – Commands for opening crafting tables, anvils, ender chests and more.
- **Inventory Tools** – `/invsee` and `/seen` for managing player inventories.
- **VTell** – Network wide messaging using plugin channels.
- **BlueMap Integration** – `/map` and `/maps` to toggle map visibility.
- **Punishments** – Ban and mute commands with BungeeCord support.
- **Donations** – Handles donation packages across the network.
- **Trophy Room** – Collectible trophies with menu GUI and placeholders.
- **Command Queue** – Execute queued commands from a YAML file.
- **Help Books & Text** – In‑game books and help commands for players.
- **Purpur Enhancements** – QoL tweaks like crop trample prevention and mob spawner silk touch.
- **AFK System** – Detects AFK players and provides PlaceholderAPI placeholders.
- **Lands TP Fix** – Improves teleport handling when using HuskHomes and Lands.
- **AES Message Encryption** – Secure plugin messaging using an AES key.
- **Simple Portals** – WorldGuard flags that send players to the wild, healing springs or warps.
- **Player Shops** – Rentable shop regions with `/bms` management commands.
- **Combine System** – Allows unsafe enchantment books to be applied via anvils.

## Installation
1. Build the plugin with Maven or download the compiled jar.
2. Place the jar in your server's `plugins` folder.
3. Start or reload the server to generate the default configuration.
4. Edit `config.yml` to enable or disable the systems you want.

## Usage
Most features can be toggled in `config.yml`. Use `/bme` to reload the plugin after changing the configuration.


---
# ImageMaps Setup Guide

This plugin allows you to convert images into in‑game maps using the `/imagemap` command. Follow these rules when preparing images.

## Directory
- Place your images in the `plugins/BMEssentials/Images` folder. The directory is created automatically when the ImageMaps system is enabled in `config.yml`.

## Supported formats
- Java's `ImageIO` is used to load files, so common formats such as PNG, JPEG and BMP work. PNG is recommended for the best quality.
- The file name you supply in `/imagemap` must include the extension and match the case of the file on disk.

## Image size
- Minecraft maps are always 128x128 pixels. The plugin will scale your image to the dimensions provided with the command (defaults are `DefaultWidth` and `DefaultHeight` in `config.yml`).
- If the scaled image is larger than 128 pixels in either direction and is divisible by 128, `/imagemap` automatically divides it into multiple maps.
- For the most predictable output, prepare your image so its width and height are multiples of 128.
---

## Contributing
Pull requests and issues are welcome! Feel free to contribute new features or improvements.

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for full details.
