package top.atdove.epithet.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.PacketDistributor;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.client.ClientTitleManager;
import top.atdove.epithet.network.EquipTitlePayload;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.title.TitleRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Vanilla-style Title Selection GUI.
 */
public class TitleSelectionScreen extends Screen {
    private final Screen lastScreen;
    private TitleSelectionList list;

    public TitleSelectionScreen(Screen lastScreen) {
        super(Component.translatable("gui.epithet.title_selection.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        super.init();

        this.list = new TitleSelectionList(this.minecraft, this.width, this.height - 64, 32, 38);
        this.addRenderableWidget(this.list);

        int buttonY = this.height - 26;
        boolean isAdmin = this.minecraft != null && this.minecraft.player != null && this.minecraft.player.hasPermissions(2);

        if (isAdmin) {
            int buttonWidth = 90;
            int gap = 6;
            int totalWidth = buttonWidth * 3 + gap * 2;
            int startX = (this.width - totalWidth) / 2;

            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.epithet.button.create_title"),
                    btn -> this.minecraft.setScreen(new TitleCreateScreen(this))
            ).bounds(startX, buttonY, buttonWidth, 20).build());

            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.epithet.button.player_selector"),
                    btn -> this.minecraft.setScreen(new VanillaPlayerSelectorScreen(this))
            ).bounds(startX + buttonWidth + gap, buttonY, buttonWidth, 20).build());

            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.done"),
                    btn -> onClose()
            ).bounds(startX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20).build());
        } else {
            // Regular players only see the centered "Done" button
            int buttonWidth = 150;
            int startX = (this.width - buttonWidth) / 2;

            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.done"),
                    btn -> onClose()
            ).bounds(startX, buttonY, buttonWidth, 20).build());
        }

        refreshList();
    }

    public void refreshList() {
        if (this.list != null) {
            this.list.refreshEntries();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Header Title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        // Locked Banner warning if self locked
        if (ClientTitleManager.isSelfLocked()) {
            Component lockedBanner = Component.translatable("gui.epithet.title.locked_banner")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            guiGraphics.drawCenteredString(this.font, lockedBanner, this.width / 2, 21, 0xFF5555);
        }

        // Empty titles banner
        if (this.list != null && this.list.children().isEmpty()) {
            Component emptyMsg = Component.translatable("gui.epithet.empty_titles");
            guiGraphics.drawCenteredString(this.font, emptyMsg, this.width / 2, this.height / 2 - 10, 0xAAAAAA);
        }
    }

    public class TitleSelectionList extends ObjectSelectionList<TitleSelectionList.TitleEntry> {
        public TitleSelectionList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        public void refreshEntries() {
            this.clearEntries();
            Set<ResourceLocation> unlocked = new HashSet<>();
            if (this.minecraft.player != null) {
                unlocked.addAll(ClientTitleManager.getUnlockedTitles(this.minecraft.player.getUUID()));
            }
            ClientTitleManager.getLocalActiveTitle().ifPresent(unlocked::add);

            // Also check any registered titles that are marked as unlocked locally
            for (TitleDefinition d : TitleRegistry.getInstance().getAllTitles().values()) {
                if (ClientTitleManager.isUnlocked(d.id())) {
                    unlocked.add(d.id());
                }
            }

            List<TitleDefinition> list = new ArrayList<>();
            for (ResourceLocation id : unlocked) {
                TitleDefinition def = TitleRegistry.getInstance().getTitle(id).orElseGet(() ->
                        new TitleDefinition(id, Component.literal(id.getPath()), Component.empty(), 0xFFFFFF, 0, "common", ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "default"), false)
                );
                list.add(def);
            }
            list.sort(Comparator.comparingInt(TitleDefinition::priority).reversed()
                    .thenComparing(def -> def.id().toString()));

            for (TitleDefinition def : list) {
                this.addEntry(new TitleEntry(def));
            }
        }

        @Override
        public int getRowWidth() {
            return Math.min(360, this.width - 20);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() + 4;
        }

        public class TitleEntry extends ObjectSelectionList.Entry<TitleEntry> {
            private final TitleDefinition titleDef;
            private final Button actionButton;

            public TitleEntry(TitleDefinition titleDef) {
                this.titleDef = titleDef;
                this.actionButton = Button.builder(Component.empty(), btn -> onActionClicked())
                        .bounds(0, 0, 60, 20)
                        .build();
                updateButtonState();
            }

            private void updateButtonState() {
                boolean isEquipped = ClientTitleManager.getLocalActiveTitle().filter(titleDef.id()::equals).isPresent();
                boolean isUnlocked = ClientTitleManager.isUnlocked(titleDef.id());
                boolean isLocked = ClientTitleManager.isSelfLocked();

                if (isLocked) {
                    this.actionButton.active = false;
                    this.actionButton.setMessage(Component.translatable("gui.epithet.status.locked_tag").append(" ").append(
                            isEquipped ? Component.translatable("gui.epithet.button.unequip") : Component.translatable("gui.epithet.button.equip")
                    ));
                    this.actionButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.title.locked_tooltip")));
                } else if (isEquipped) {
                    this.actionButton.active = true;
                    this.actionButton.setMessage(Component.translatable("gui.epithet.button.unequip"));
                    this.actionButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.button.unequip_tooltip")));
                } else if (isUnlocked) {
                    this.actionButton.active = true;
                    this.actionButton.setMessage(Component.translatable("gui.epithet.button.equip"));
                    this.actionButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.button.equip_tooltip")));
                } else {
                    this.actionButton.active = false;
                    this.actionButton.setMessage(Component.translatable("gui.epithet.button.locked"));
                    this.actionButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.title.not_unlocked_tooltip")));
                }
            }

            private void onActionClicked() {
                boolean isEquipped = ClientTitleManager.getLocalActiveTitle().filter(titleDef.id()::equals).isPresent();
                Optional<ResourceLocation> newTitle = isEquipped ? Optional.empty() : Optional.of(titleDef.id());

                // Send equip payload to server
                PacketDistributor.sendToServer(new EquipTitlePayload(newTitle));

                // Optimistically update client state
                ClientTitleManager.setLocalActiveTitle(newTitle);

                // Immediately refresh client player's DisplayName cache
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.refreshDisplayName();
                }

                TitleSelectionScreen.this.refreshList();
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                updateButtonState();

                boolean isEquipped = ClientTitleManager.getLocalActiveTitle().filter(titleDef.id()::equals).isPresent();

                // Equipped green highlight box
                if (isEquipped) {
                    guiGraphics.fill(left - 2, top - 1, left + width + 2, top + height + 1, 0x3355FF55);
                    guiGraphics.renderOutline(left - 2, top - 1, width + 4, height + 2, 0x9955FF55);
                } else if (isMouseOver) {
                    guiGraphics.fill(left - 2, top - 1, left + width + 2, top + height + 1, 0x15FFFFFF);
                }

                // Render Title Display Name
                int textX = left + 4;
                int textY = top + 4;
                guiGraphics.drawString(font, titleDef.displayName(), textX, textY, 0xFFFFFF);

                // Render Rarity Badge with safe fallback
                String rarity = titleDef.rarity();
                if (rarity != null && !rarity.isEmpty() && !"none".equalsIgnoreCase(rarity) && !"default".equalsIgnoreCase(rarity)) {
                    String fallbackName = rarity.substring(0, 1).toUpperCase(Locale.ROOT) + rarity.substring(1).toLowerCase(Locale.ROOT);
                    Component rarityComp = Component.translatableWithFallback("rarity.epithet." + rarity.toLowerCase(Locale.ROOT), fallbackName);
                    int rarityX = textX + font.width(titleDef.displayName()) + 6;
                    guiGraphics.drawString(font, rarityComp, rarityX, textY, titleDef.color() != 0 ? titleDef.color() : 0xFFAA00);
                }

                // Render Description
                int descY = textY + 12;
                Component desc = titleDef.description();
                guiGraphics.drawString(font, desc, textX, descY, 0x888888);

                // Render Action Button on right side
                this.actionButton.setX(left + width - 64);
                this.actionButton.setY(top + (height - 20) / 2);
                this.actionButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.actionButton.mouseClicked(mouseX, mouseY, button)) {
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
