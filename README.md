<div align="center">

# Epithet

*A modern, native-feel player title system mod for Minecraft 1.21.1 (NeoForge).*

[English](README.md) · [简体中文](README_CN.md)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg?style=flat-square&logo=minecraft)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.248-orange.svg?style=flat-square)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-blue.svg?style=flat-square&logo=openjdk)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

</div>

---

## 🌟 Highlights

Unlike traditional implementations that modify player prefixes via Scoreboard Teams, **Epithet** directly hooks into entity rendering and text formatting pipelines.

- **🎯 Overhead Visual Parity**: Strictly inherits vanilla sneaking translucency (`isDiscrete`), complete invisibility concealing (`isInvisibleTo`), depth test occlusion (`seeThrough`), 64-block distance culling, and team visibility rules.
- **💬 Chat & Tab Integration**: Injects formatted prefixes `<「Title」 Player> Message` with achievement-like hover tooltips in chat, perfectly synchronized with Tab player lists.
- **🖥️ Native Vanilla UI**: Features a collision-free pause menu shortcut (`PauseScreen`), a player title selection interface, and an admin CRUD management workbench.
- **📦 Data-Driven & Advancement Unlocks**: Fully configurable via standard Datapacks (`data/<namespace>/titles/*.json`) with `/reload` hot reloading, plus automatic title unlocking mapped to vanilla advancements.
- **🔒 Server-Authoritative Lock**: Administrators can lock a player's title slot, preventing client-side unequip or changes.
- **🎨 Gradients & Animated Waves**: Full support for Hex colors (`#FFAA00`), formatting codes (`§6`), static multi-color linear gradients (`<gradient:...>`), and 60/144 FPS smooth animated wave gradients (`<animated-gradient:...>`, `<animated-rainbow>`) with configurable bracket wrappers.

---

## 📚 Documentation

Comprehensive technical specifications and reference manuals are available in the [docs/](docs/) directory:

| Document | Description |
| :--- | :--- |
| 📖 **[Features & Command Reference](docs/features.md)** | Full command list, test dummy tool controls, GUI layouts, and configuration specs. |
| 📖 **[Title Configuration & Datapack Guide](docs/title_configuration_guide.md)** | JSON schema, advancement binding examples, dynamic in-game creation, and troubleshooting. |

---

## 🚀 Quick Start

### For Players
- Press <kbd>ESC</kbd> to open the Pause Menu and click the **Name Tag** icon on the top right, or execute `/epithet gui`.
- Execute `/epithet list` in chat to inspect your unlocked titles and active status.

### For Administrators (OP Level 2+)
```bash
# Grant / revoke titles
/epithet admin give <player> <title_id>
/epithet admin take <player> <title_id>

# Force equip / lock title slot
/epithet admin set <player> <title_id>
/epithet admin lock <player>
/epithet admin unlock <player>
```
*See [docs/features.md](docs/features.md) for full syntax and dummy testing commands.*

---

## 🛠️ Building & Development

```bash
# Compile Java sources
./gradlew compileJava

# Apply code formatting (Spotless)
./gradlew spotlessApply

# Regenerate all 4 locale files (zh_cn, zh_tw, en_us, en_gb)
./gradlew generateLang

# Run client in development environment
./gradlew runClient
```

---

## 📄 License

This project is open-source software licensed under the [MIT License](LICENSE).
