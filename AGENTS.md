# AGENTS.md - Epithet Mod Architecture & Module Specification

This document provides automated coding agents with an overview of the Epithet mod codebase, detailing the package topology, module responsibilities, and system data flows.

---

## 1. Project Metadata

- **Mod Identifier**: `epithet`
- **Root Java Package**: `top.atdove.epithet`
- **Platform**: Minecraft 1.21.1 (NeoForge 21.1.248)
- **Toolchain**: Java 21, Gradle ModDevGradle

---

## 2. Package Topology

```text
top.atdove.epithet
├── Epithet.java
├── attachment
│   ├── ModAttachments.java
│   └── PlayerTitleData.java
├── client
│   ├── ClientTitleManager.java
│   ├── gui
│   │   ├── PauseScreenHandler.java
│   │   ├── PlayerTitlesManageScreen.java
│   │   ├── TitleSelectionScreen.java
│   │   └── VanillaPlayerSelectorScreen.java
│   └── renderer
│       └── TitleRenderHandler.java
├── command
│   └── TitleCommands.java
├── config
│   └── EpithetConfig.java
├── data
│   ├── DataGenerators.java
│   ├── LanguageDataGenTool.java
│   ├── ModLanguageProvider.java
│   ├── TitleReloadListener.java
│   └── TitleSavedData.java
├── event
│   └── TitleEventHandler.java
├── network
│   ├── AdminActionPayload.java
│   ├── EquipTitlePayload.java
│   ├── OpenTitleGuiPayload.java
│   ├── PlayerTitleSyncPayload.java
│   ├── TitleNetworkHandler.java
│   └── TitleRegistrySyncPayload.java
├── test
│   ├── DummyManager.java
│   └── EpithetGameTests.java
├── title
│   ├── TitleDefinition.java
│   └── TitleRegistry.java
└── util
    └── ColorHelper.java
```

---

## 3. Module Responsibilities

### Core & Entry Point
- **`Epithet.java`**: Mod lifecycle coordinator. Registers configuration specifications, data attachment types, payload handlers, and event listeners.

### State & Attachments (`attachment/`)
- **`ModAttachments.java`**: Registers the `PLAYER_TITLE_DATA` attachment type on the NeoForge attachment registry, configured with `copyOnDeath()` for persistence.
- **`PlayerTitleData.java`**: Data holder attached to player entities storing:
  - `activeTitle`: The currently equipped title ID (`Optional<ResourceLocation>`).
  - `unlockedTitles`: The set of unlocked title IDs (`Set<ResourceLocation>`).
  - `locked`: Administrative lock flag preventing the player from modifying their equipped title.

### Client-Side Subsystems (`client/`)
- **`ClientTitleManager.java`**: Maintains client-side state of active and unlocked titles, handles optimistic local updates, and provides querying methods for the GUI.
- **`client.gui`**:
  - `PauseScreenHandler`: Injects a shortcut button into `PauseScreen` to open the title GUI.
  - `TitleSelectionScreen`: Vanilla-styled list interface where players inspect and toggle equipped titles.
  - `VanillaPlayerSelectorScreen`: Admin selection screen for choosing target players.
  - `PlayerTitlesManageScreen`: Admin workbench for viewing, granting, revoking, equipping, and locking titles of a selected player.
- **`client.renderer.TitleRenderHandler`**: Subscribes to `RenderNameTagEvent` to render formatted title prefixes above player and dummy entity nametags.

### Commands (`command/`)
- **`TitleCommands.java`**: Implements the Brigadier command tree under `/epithet`:
  - `/epithet gui`: Opens the selection interface.
  - `/epithet list`: Displays owned titles in chat.
  - `/epithet admin <create|give|take|set|lock|unlock>`: Admin title manipulation.
  - `/epithet test dummy <spawn|remove|pose|action|look>`: Test dummy controls.

### Configuration (`config/`)
- **`EpithetConfig.java`**: Defines configuration specifications using `ModConfigSpec`:
  - `Client`: Configures bracket symbols (default `「` and `」`), bracket wrapping toggle, and chat hover tooltips.
  - `Common`: Configures server-side beginner default title parameters.

### Data Management & Generation (`data/`)
- **`TitleReloadListener.java`**: Subscribes to resource reload events to parse datapack title definitions from `data/<namespace>/titles/*.json`.
- **`TitleSavedData.java`**: Implements `SavedData` to serialize dynamically created in-game titles to `world/data/epithet_titles.dat`.
- **`DataGenerators.java`**: Binds DataGen providers on `GatherDataEvent`.
- **`LanguageDataGenTool.java` & `ModLanguageProvider.java`**: Programmatically generates localization assets for `zh_cn`, `zh_tw`, `en_us`, and `en_gb`.

### Event Handlers (`event/`)
- **`TitleEventHandler.java`**: Central event listener subscribing to:
  - `PlayerEvent.NameFormat`: Injects formatted titles into player chat prefixes.
  - `PlayerEvent.TabListNameFormat`: Synchronizes formatted titles into Tab list display names.
  - `PlayerEvent.PlayerLoggedInEvent`: Synchronizes registry data, syncs title attachments, and checks default or advancement titles.
  - `PlayerEvent.PlayerRespawnEvent`: Resyncs title data across player respawn.
  - `PlayerEvent.StartTracking`: Synchronizes active titles to tracking players.
  - `AdvancementEvent.AdvancementEarnEvent`: Evaluates and grants titles mapped to earned advancements.

### Networking (`network/`)
- **`TitleNetworkHandler.java`**: Registers `CustomPacketPayload` types and associates packet handlers.
- **Payload Types**:
  - `EquipTitlePayload` (C2S): Player request to equip or unequip a title.
  - `AdminActionPayload` (C2S): Admin request to grant, revoke, force-equip, or toggle title locks.
  - `OpenTitleGuiPayload` (S2C): Server command to trigger GUI opening on the client.
  - `PlayerTitleSyncPayload` (S2C): Synchronizes title attachment state to clients.
  - `TitleRegistrySyncPayload` (S2C): Broadcasts the full set of registered titles from server to clients.

### Testing & Verification (`test/`)
- **`DummyManager.java`**: Spawns and manages tagged `ArmorStand` entities for visual rendering verification.
- **`EpithetGameTests.java`**: Automated test definitions utilizing Minecraft's native GameTest framework.

### Title Domain Model (`title/`)
- **`TitleDefinition.java`**: Immutable record storing title metadata: ID, display name component, description component, color, priority, rarity, icon, default unlocked flag, and optional advancement ID.
- **`TitleRegistry.java`**: Central in-memory registry combining datapack titles and dynamic world titles, handling hot reloads and cache invalidation.

### Utilities (`util/`)
- **`ColorHelper.java`**: Color parsing engine supporting hexadecimal formats (`#RRGGBB`, `0xRRGGBB`), formatting codes (`§`, `&`), and named colors.

---

## 4. System Data Flow

1. **Title Definition & Registration**:
   - Static titles are loaded from datapacks (`TitleReloadListener`) on server start or `/reload`.
   - Dynamic titles are loaded from `world/data/epithet_titles.dat` (`TitleSavedData`).
   - Both sources are merged into `TitleRegistry`.
2. **Synchronization**:
   - Full registry is broadcast to connecting players via `TitleRegistrySyncPayload`.
   - Player-specific title state is synchronized via `PlayerTitleSyncPayload`.
3. **Application & Rendering**:
   - Player display names and Tab entries are dynamically updated in `TitleEventHandler`.
   - Overhead nametags are formatted and displayed via `TitleRenderHandler`.
   - GUIs read local state from `ClientTitleManager` and dispatch updates via network payloads.

---

## 5. Development Tasks

- **Compile**: `./gradlew compileJava`
- **Data Generation**: `./gradlew generateLang`
- **Client Testing**: `./gradlew runClient`
- **Server Testing**: `./gradlew runServer`
- **Automated Tests**: `./gradlew runGameTestServer`
