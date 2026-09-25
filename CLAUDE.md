# Epithet - Codebase Architecture & Module Guide

This document outlines the package structure, module responsibilities, and system design of the Epithet mod for Minecraft 1.21.1 (NeoForge 21.1.248).

---

## 1. Project Overview

- **Mod Identifier**: `epithet`
- **Root Java Package**: `top.atdove.epithet`
- **Target Platform**: Minecraft 1.21.1
- **Mod Loader**: NeoForge 21.1.248
- **Language Level**: Java 21

---

## 2. Directory & Package Structure

```text
src/main/java/top/atdove/epithet/
├── Epithet.java                  # Main mod entry point and lifecycle listener
├── attachment/                   # Player state persistence using NeoForge Data Attachments
├── client/                       # Client-side implementations (GUI & rendering)
│   ├── gui/                      # Screen and widget implementations
│   └── renderer/                 # Entity nameplate rendering integration
├── command/                      # Server command dispatchers and argument parsing
├── config/                       # Client and common configuration specifications
├── data/                         # Data loading, persistence, and data generation
├── event/                        # Forge/NeoForge event bus subscribers
├── network/                      # Network payloads and packet dispatch handlers
├── test/                         # Test fixtures and GameTest automation
├── title/                        # Core domain models and registry
└── util/                         # Common utilities and helper classes
```

---

## 3. Module Responsibilities

### `top.atdove.epithet`
- **`Epithet.java`**: Mod lifecycle coordinator. Registers configuration specifications (`ModConfig.Type.CLIENT` and `ModConfig.Type.COMMON`), registers attachment types, initializes network payload handlers, and hooks event bus subscribers.

### `attachment/`
Manages persistent player title data using NeoForge's Data Attachment system (`IAttachmentHolder`).
- **`ModAttachments.java`**: Registers the `PLAYER_TITLE_DATA` attachment type configured with `copyOnDeath()` to preserve title state across player respawns.
- **`PlayerTitleData.java`**: Stores the active title ID (`Optional<ResourceLocation>`), the set of unlocked title IDs (`Set<ResourceLocation>`), and the administrative lock state (`boolean`).

### `client/`
Encapsulates all client-only logic, including user interfaces, local state tracking, and overhead text rendering.
- **`ClientTitleManager.java`**: Manages the local player's unlocked and equipped titles, provides caching for GUI components, and tracks title locks.
- **`gui/`**:
  - **`PauseScreenHandler.java`**: Injects a shortcut button into the vanilla `PauseScreen` to open the title selection interface.
  - **`TitleSelectionScreen.java`**: Vanilla-styled selection list displaying a player's unlocked titles with equip/unequip controls.
  - **`VanillaPlayerSelectorScreen.java`**: Administrative interface for browsing online players.
  - **`PlayerTitlesManageScreen.java`**: Administrative interface for viewing, granting, revoking, equipping, and locking titles for a target player.
- **`renderer/`**:
  - **`TitleRenderHandler.java`**: Subscribes to `RenderNameTagEvent` to render formatted title prefixes above entity head nametags.

### `command/`
Handles in-game commands registered via `RegisterCommandsEvent`.
- **`TitleCommands.java`**: Defines the `/epithet` command tree:
  - `/epithet gui`: Opens the title selection interface.
  - `/epithet list`: Outputs currently owned titles and active status to chat.
  - `/epithet admin <create|give|take|set|lock|unlock>`: Administrative management operations.
  - `/epithet test dummy <spawn|remove|pose|action|look>`: Test dummy controls for visual verification.

### `config/`
Manages configuration files using `ModConfigSpec`.
- **`EpithetConfig.java`**:
  - `Client`: Customizes bracket wrapping symbols (`「`, `」`), toggles automatic wrapping, and controls chat hover tooltips.
  - `Common`: Configures the optional built-in beginner title (ID, display name, description, color, auto-equip status).

### `data/`
Handles external data loading, world data persistence, and build-time asset generation.
- **`TitleReloadListener.java`**: Loads JSON title definitions from `data/<namespace>/titles/*.json` during datapack loading and `/reload`.
- **`TitleSavedData.java`**: Extends `SavedData` to persist dynamic titles created via in-game commands to `world/data/epithet_titles.dat`.
- **`DataGenerators.java`**: Subscribes to `GatherDataEvent` to run automated data providers.
- **`LanguageDataGenTool.java` & `ModLanguageProvider.java`**: Generates translation files for supported locales (`zh_cn`, `zh_tw`, `en_us`, `en_gb`).

### `event/`
Subscribes to server and game lifecycle events.
- **`TitleEventHandler.java`**:
  - Hooks `PlayerEvent.NameFormat` to prepend titles to player names in chat.
  - Hooks `PlayerEvent.TabListNameFormat` to synchronize player names in the Tab player list.
  - Hooks `PlayerEvent.PlayerLoggedInEvent` and `PlayerRespawnEvent` to sync registry and player title data.
  - Hooks `AdvancementEvent.AdvancementEarnEvent` to unlock titles mapped to specific advancements.
  - Hooks `PlayerEvent.StartTracking` to sync active titles to surrounding tracking players.

### `network/`
Implements custom network communication using NeoForge's `CustomPacketPayload`.
- **`TitleNetworkHandler.java`**: Registers network payloads to the `PayloadRegistrar`.
- **Payloads**:
  - `EquipTitlePayload` (C2S): Requests title equip/unequip.
  - `AdminActionPayload` (C2S): Dispatches admin operations (grant, revoke, force equip, lock toggle).
  - `OpenTitleGuiPayload` (S2C): Instructs the client to display the title GUI.
  - `PlayerTitleSyncPayload` (S2C): Synchronizes title data for self and tracked players.
  - `TitleRegistrySyncPayload` (S2C): Synchronizes the full title registry from server to client.

### `test/`
Provides testing tools and automated suites.
- **`DummyManager.java`**: Spawns and manages tagged `ArmorStand` entities for testing overhead title rendering under various states (sneaking, invisible, posing).
- **`EpithetGameTests.java`**: Headless automated test cases leveraging Minecraft's GameTest framework.

### `title/`
Contains the core domain representations of titles.
- **`TitleDefinition.java`**: Record defining a title (ID, display name component, description component, color, priority, rarity, icon, default unlocked flag, advancement ID).
- **`TitleRegistry.java`**: Thread-safe registry that merges datapack-defined titles with dynamic world titles and handles registry synchronization.

### `util/`
- **`ColorHelper.java`**: Utility for parsing hex color codes (`#RRGGBB`, `0xRRGGBB`), formatting codes (`§`, `&`), and named Minecraft formatting colors.

---

## 4. Common Build Tasks

- `./gradlew compileJava`: Compiles Java source files.
- `./gradlew generateLang`: Generates locale files via DataGen.
- `./gradlew runClient`: Launches the development client environment.
- `./gradlew runServer`: Launches the development dedicated server environment.
- `./gradlew runGameTestServer`: Executes automated GameTests.
