package top.atdove.epithet.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.PacketDistributor;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.client.ClientTitleManager;
import top.atdove.epithet.network.AdminActionPayload;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.title.TitleRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Vanilla-style player-specific title management GUI.
 * Allows administrators to inspect owned titles, equip/unequip, revoke,
 * and grant new titles from the registry.
 */
public class PlayerTitlesManageScreen extends Screen {
    private final Screen lastScreen;
    private final UUID targetUuid;
    private final String targetName;
    private final PlayerSkin targetSkin;

    private EditBox searchBox;
    private TitleManageList titleList;
    private Button lockButton;
    private Button ownedTabButton;
    private Button availableTabButton;
    private Button switchModeButton;
    private Button backButton;

    private boolean showAvailable = false;
    private String filterText = "";

    public PlayerTitlesManageScreen(Screen lastScreen, ClientTitleManager.KnownPlayerEntry playerEntry) {
        this(lastScreen, playerEntry.uuid(), playerEntry.name(), playerEntry.skin());
    }

    public PlayerTitlesManageScreen(Screen lastScreen, UUID targetUuid, String targetName, PlayerSkin targetSkin) {
        super(Component.translatable("gui.epithet.manage.title"));
        this.lastScreen = lastScreen;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.targetSkin = targetSkin != null ? targetSkin : DefaultPlayerSkin.get(targetUuid);
    }

    @Override
    protected void init() {
        super.init();

        // Request fresh data from server
        PacketDistributor.sendToServer(new AdminActionPayload(this.targetUuid, "sync", ""));

        // Search Box and responsive layout bounds
        int searchWidth = 150;
        int listWidth = Math.min(360, this.width - 20);
        int leftAlign = this.width / 2 - listWidth / 2;
        int listRight = leftAlign + listWidth;
        int lockBtnW = 70;

        // Top Lock Button (bound to list right edge to prevent overflow)
        this.lockButton = Button.builder(Component.empty(), btn -> onToggleLock())
                .bounds(listRight - lockBtnW, 14, lockBtnW, 20).build();
        this.addRenderableWidget(this.lockButton);
        updateLockButton();

        this.searchBox = new EditBox(
                this.font,
                leftAlign,
                46,
                searchWidth,
                20,
                Component.translatable("gui.epithet.manage.search_hint")
        );
        this.searchBox.setHint(Component.translatable("gui.epithet.manage.search_hint"));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        // Tab Buttons
        int tabWidth = 98;
        int tabY = 46;
        int tabX = leftAlign + searchWidth + 6;

        this.ownedTabButton = Button.builder(Component.empty(), btn -> setViewMode(false))
                .bounds(tabX, tabY, tabWidth, 20).build();
        this.addRenderableWidget(this.ownedTabButton);

        this.availableTabButton = Button.builder(Component.empty(), btn -> setViewMode(true))
                .bounds(tabX + tabWidth + 4, tabY, tabWidth, 20).build();
        this.addRenderableWidget(this.availableTabButton);

        // Sunken Slot List
        int listTop = 72;
        int listBottom = this.height - 34;
        this.titleList = new TitleManageList(this.minecraft, this.width, listBottom - listTop, listTop, 38);
        this.addRenderableWidget(this.titleList);

        // Bottom Action Buttons
        int bottomBtnWidth = 140;
        int gap = 12;
        int bottomStartX = this.width / 2 - bottomBtnWidth - gap / 2;
        int bottomY = this.height - 26;

        this.switchModeButton = Button.builder(Component.empty(), btn -> setViewMode(!this.showAvailable))
                .bounds(bottomStartX, bottomY, bottomBtnWidth, 20).build();
        this.addRenderableWidget(this.switchModeButton);

        this.backButton = Button.builder(Component.translatable("gui.back"), btn -> onClose())
                .bounds(bottomStartX + bottomBtnWidth + gap, bottomY, bottomBtnWidth, 20).build();
        this.addRenderableWidget(this.backButton);

        updateTabButtons();
        updateBottomButtons();
        refreshList();
    }

    private void onSearchChanged(String text) {
        this.filterText = text != null ? text.trim().toLowerCase(Locale.ROOT) : "";
        refreshList();
    }

    public void setViewMode(boolean available) {
        this.showAvailable = available;
        updateTabButtons();
        updateBottomButtons();
        refreshList();
    }

    private void updateLockButton() {
        boolean locked = ClientTitleManager.isPlayerLocked(this.targetUuid);
        if (locked) {
            this.lockButton.setMessage(Component.translatable("gui.epithet.button.unlock").withStyle(ChatFormatting.RED));
            this.lockButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.button.unlock_tooltip")));
        } else {
            this.lockButton.setMessage(Component.translatable("gui.epithet.button.lock"));
            this.lockButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.button.lock_tooltip")));
        }
    }

    private void updateTabButtons() {
        Set<ResourceLocation> unlocked = ClientTitleManager.getUnlockedTitles(this.targetUuid);
        int ownedCount = unlocked.size();
        int totalCount = TitleRegistry.getInstance().getAllTitles().size();
        int availableCount = Math.max(0, totalCount - ownedCount);

        Component ownedText = Component.translatable("gui.epithet.manage.tab.owned", ownedCount);
        Component availableText = Component.translatable("gui.epithet.manage.tab.available", availableCount);

        if (!this.showAvailable) {
            this.ownedTabButton.setMessage(ownedText.copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            this.availableTabButton.setMessage(availableText.copy().withStyle(ChatFormatting.GRAY));
        } else {
            this.ownedTabButton.setMessage(ownedText.copy().withStyle(ChatFormatting.GRAY));
            this.availableTabButton.setMessage(availableText.copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        }
    }

    private void updateBottomButtons() {
        if (!this.showAvailable) {
            this.switchModeButton.setMessage(Component.translatable("gui.epithet.manage.button.add_title"));
            this.switchModeButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.manage.button.add_title_tooltip")));
        } else {
            this.switchModeButton.setMessage(Component.translatable("gui.epithet.manage.button.back_to_owned"));
            this.switchModeButton.setTooltip(null);
        }
    }

    private void onToggleLock() {
        PacketDistributor.sendToServer(new AdminActionPayload(
                this.targetUuid,
                "toggle_lock",
                ""
        ));
        ClientTitleManager.togglePlayerLock(this.targetUuid);
        updateLockButton();
    }

    public void onDataSynced(UUID uuid) {
        if (this.targetUuid.equals(uuid)) {
            updateLockButton();
            updateTabButtons();
            refreshList();
        }
    }

    public void refreshList() {
        if (this.titleList != null) {
            this.titleList.refreshEntries(this.showAvailable, this.filterText);
            updateTabButtons();
            updateBottomButtons();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int listWidth = Math.min(360, this.width - 20);
        int leftAlign = this.width / 2 - listWidth / 2;

        // Render Player Avatar Face
        int faceSize = 28;
        int faceX = leftAlign;
        int faceY = 10;
        PlayerFaceRenderer.draw(guiGraphics, this.targetSkin, faceX, faceY, faceSize);

        // Player Name
        int textX = faceX + faceSize + 8;
        int nameY = 12;
        Component nameComp = Component.literal(this.targetName).withStyle(ChatFormatting.BOLD);
        guiGraphics.drawString(this.font, nameComp, textX, nameY, 0xFFFFFF);

        // Lock Status & Active Title
        int statusY = nameY + 12;
        boolean locked = ClientTitleManager.isPlayerLocked(this.targetUuid);
        Component statusComp = locked
                ? Component.translatable("gui.epithet.manage.status.locked").withStyle(ChatFormatting.RED)
                : Component.translatable("gui.epithet.manage.status.unlocked").withStyle(ChatFormatting.GREEN);

        Optional<ResourceLocation> activeId = ClientTitleManager.getActiveTitleId(this.targetUuid);
        if (activeId.isPresent()) {
            Component activeTitleName = TitleRegistry.getInstance().getTitle(activeId.get())
                    .map(TitleDefinition::displayName)
                    .orElse(Component.literal(activeId.get().getPath()));
            statusComp = statusComp.copy().append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(activeTitleName);
        }
        guiGraphics.drawString(this.font, statusComp, textX, statusY, 0xAAAAAA);

        // Empty List Notice
        if (this.titleList != null && this.titleList.children().isEmpty()) {
            Component emptyMsg = !this.showAvailable
                    ? Component.translatable("gui.epithet.manage.empty_owned")
                    : Component.translatable("gui.epithet.manage.empty_available");
            guiGraphics.drawCenteredString(this.font, emptyMsg, this.width / 2, this.height / 2 - 4, 0x888888);
        }
    }

    public class TitleManageList extends ObjectSelectionList<TitleManageList.EntryBase> {
        public TitleManageList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        public void refreshEntries(boolean showAvailable, String filter) {
            this.clearEntries();
            Set<ResourceLocation> unlocked = ClientTitleManager.getUnlockedTitles(targetUuid);

            if (showAvailable) {
                // Show titles available in registry that player does not own yet
                List<TitleDefinition> all = new ArrayList<>(TitleRegistry.getInstance().getAllTitles().values());
                all.sort(Comparator.comparingInt(TitleDefinition::priority).reversed()
                        .thenComparing(def -> def.id().toString()));

                for (TitleDefinition def : all) {
                    if (!unlocked.contains(def.id())) {
                        if (matchesFilter(def, filter)) {
                            this.addEntry(new AvailableTitleEntry(def));
                        }
                    }
                }
            } else {
                // Show owned titles based on actual unlocked set (never loses orphaned titles)
                List<TitleDefinition> ownedList = new ArrayList<>();
                for (ResourceLocation id : unlocked) {
                    TitleDefinition def = TitleRegistry.getInstance().getTitle(id).orElseGet(() ->
                            new TitleDefinition(id, Component.literal(id.getPath()), Component.empty(), 0xFFFFFF, 0, "common", ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "default"), false)
                    );
                    ownedList.add(def);
                }
                ownedList.sort(Comparator.comparingInt(TitleDefinition::priority).reversed()
                        .thenComparing(def -> def.id().toString()));

                for (TitleDefinition def : ownedList) {
                    if (matchesFilter(def, filter)) {
                        this.addEntry(new OwnedTitleEntry(def));
                    }
                }
            }
        }

        private boolean matchesFilter(TitleDefinition def, String filter) {
            if (filter.isEmpty()) return true;
            return def.displayName().getString().toLowerCase(Locale.ROOT).contains(filter)
                    || def.id().toString().toLowerCase(Locale.ROOT).contains(filter)
                    || def.description().getString().toLowerCase(Locale.ROOT).contains(filter);
        }

        @Override
        public int getRowWidth() {
            return Math.min(360, this.width - 20);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() + 4;
        }

        public abstract static class EntryBase extends ObjectSelectionList.Entry<EntryBase> {}

        public class OwnedTitleEntry extends EntryBase {
            private final TitleDefinition titleDef;
            private final Button equipButton;
            private final Button takeButton;

            public OwnedTitleEntry(TitleDefinition titleDef) {
                this.titleDef = titleDef;
                this.equipButton = Button.builder(Component.empty(), btn -> onToggleEquip())
                        .bounds(0, 0, 56, 20).build();
                this.takeButton = Button.builder(Component.translatable("gui.epithet.manage.button.take"), btn -> onTake())
                        .bounds(0, 0, 46, 20).build();
                this.takeButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.manage.button.take_tooltip")));
                updateEquipButton();
            }

            private boolean isEquipped() {
                return ClientTitleManager.getActiveTitleId(targetUuid).filter(titleDef.id()::equals).isPresent();
            }

            private void updateEquipButton() {
                if (isEquipped()) {
                    this.equipButton.setMessage(Component.translatable("gui.epithet.manage.button.unequip"));
                    this.equipButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.manage.button.unequip_tooltip")));
                } else {
                    this.equipButton.setMessage(Component.translatable("gui.epithet.manage.button.set_equipped"));
                    this.equipButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.manage.button.set_equipped_tooltip")));
                }
            }

            private void onToggleEquip() {
                if (isEquipped()) {
                    PacketDistributor.sendToServer(new AdminActionPayload(targetUuid, "unequip", ""));
                    ClientTitleManager.setPlayerActiveTitle(targetUuid, Optional.empty());
                } else {
                    PacketDistributor.sendToServer(new AdminActionPayload(targetUuid, "equip", titleDef.id().toString()));
                    ClientTitleManager.setPlayerActiveTitle(targetUuid, Optional.of(titleDef.id()));
                }
                PlayerTitlesManageScreen.this.refreshList();
            }

            private void onTake() {
                PacketDistributor.sendToServer(new AdminActionPayload(targetUuid, "take", titleDef.id().toString()));
                ClientTitleManager.removePlayerTitle(targetUuid, titleDef.id());
                PlayerTitlesManageScreen.this.refreshList();
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                boolean equipped = isEquipped();
                updateEquipButton();

                // Equipped green highlight frame
                if (equipped) {
                    guiGraphics.fill(left - 2, top - 1, left + width + 2, top + height + 1, 0x3355FF55);
                    guiGraphics.renderOutline(left - 2, top - 1, width + 4, height + 2, 0x9955FF55);
                } else if (isMouseOver) {
                    guiGraphics.fill(left - 2, top - 1, left + width + 2, top + height + 1, 0x15FFFFFF);
                }

                // Title Name
                int textX = left + 4;
                int textY = top + 4;
                guiGraphics.drawString(font, titleDef.displayName(), textX, textY, 0xFFFFFF);

                // Rarity Tag
                String rawRarity = titleDef.rarity();
                String rarity = (rawRarity != null && !rawRarity.trim().isEmpty())
                        ? rawRarity.trim().toLowerCase(Locale.ROOT)
                        : "common";
                Component rarityComp = Component.translatableWithFallback("rarity.epithet." + rarity, rarity);
                int rarityX = textX + font.width(titleDef.displayName()) + 6;
                guiGraphics.drawString(font, rarityComp, rarityX, textY, titleDef.color() != 0 ? titleDef.color() : 0xFFAA00);

                // Equipped Status Tag
                if (equipped) {
                    Component equippedBadge = Component.translatable("gui.epithet.manage.status.equipped_badge")
                            .withStyle(ChatFormatting.GREEN);
                    int badgeX = rarityX + font.width(rarityComp) + 6;
                    guiGraphics.drawString(font, equippedBadge, badgeX, textY, 0x55FF55);
                }

                // Description
                int descY = textY + 12;
                guiGraphics.drawString(font, titleDef.description(), textX, descY, 0x888888);

                // Action Buttons
                int btnHeight = 20;
                int btnY = top + (height - btnHeight) / 2;
                int takeW = 46;
                int equipW = 56;

                this.takeButton.setX(left + width - takeW - 4);
                this.takeButton.setY(btnY);
                this.takeButton.render(guiGraphics, mouseX, mouseY, partialTick);

                this.equipButton.setX(left + width - takeW - 4 - equipW - 4);
                this.equipButton.setY(btnY);
                this.equipButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.equipButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (this.takeButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return titleDef.displayName();
            }
        }

        public class AvailableTitleEntry extends EntryBase {
            private final TitleDefinition titleDef;
            private final Button giveButton;

            public AvailableTitleEntry(TitleDefinition titleDef) {
                this.titleDef = titleDef;
                this.giveButton = Button.builder(
                        Component.translatable("gui.epithet.manage.button.give"),
                        btn -> onGive()
                ).bounds(0, 0, 52, 20).build();
                this.giveButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.manage.button.give_tooltip")));
            }

            private void onGive() {
                PacketDistributor.sendToServer(new AdminActionPayload(targetUuid, "give", titleDef.id().toString()));
                ClientTitleManager.unlockPlayerTitle(targetUuid, titleDef.id());
                PlayerTitlesManageScreen.this.refreshList();
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                if (isMouseOver) {
                    guiGraphics.fill(left - 2, top - 1, left + width + 2, top + height + 1, 0x15FFFFFF);
                }

                // Title Name
                int textX = left + 4;
                int textY = top + 4;
                guiGraphics.drawString(font, titleDef.displayName(), textX, textY, 0xFFFFFF);

                // Rarity Tag
                String rawRarity = titleDef.rarity();
                String rarity = (rawRarity != null && !rawRarity.trim().isEmpty())
                        ? rawRarity.trim().toLowerCase(Locale.ROOT)
                        : "common";
                Component rarityComp = Component.translatableWithFallback("rarity.epithet." + rarity, rarity);
                int rarityX = textX + font.width(titleDef.displayName()) + 6;
                guiGraphics.drawString(font, rarityComp, rarityX, textY, titleDef.color() != 0 ? titleDef.color() : 0xFFAA00);

                // Description
                int descY = textY + 12;
                guiGraphics.drawString(font, titleDef.description(), textX, descY, 0x888888);

                // Action Button (Give)
                int btnHeight = 20;
                int btnY = top + (height - btnHeight) / 2;
                int giveW = 52;

                this.giveButton.setX(left + width - giveW - 4);
                this.giveButton.setY(btnY);
                this.giveButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.giveButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return titleDef.displayName();
            }
        }
    }
}
