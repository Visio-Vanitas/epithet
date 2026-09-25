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
import net.minecraft.network.chat.Component;

import net.neoforged.neoforge.network.PacketDistributor;

import top.atdove.epithet.client.ClientTitleManager;
import top.atdove.epithet.network.AdminActionPayload;

import java.util.List;
import java.util.Locale;

/**
 * Vanilla-style player selector GUI for administrators and players to inspect
 * player faces, active titles, and toggle lock status.
 */
public class VanillaPlayerSelectorScreen extends Screen {
    private final Screen lastScreen;
    private EditBox searchBox;
    private PlayerSelectionList playerList;
    private String filterText = "";

    public VanillaPlayerSelectorScreen(Screen lastScreen) {
        super(Component.translatable("gui.epithet.player_selector.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        super.init();

        int searchWidth = 200;
        this.searchBox = new EditBox(
                this.font,
                this.width / 2 - searchWidth / 2,
                24,
                searchWidth,
                20,
                Component.translatable("gui.epithet.player_selector.search")
        );
        this.searchBox.setHint(Component.translatable("gui.epithet.player_selector.search_hint"));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        this.playerList = new PlayerSelectionList(this.minecraft, this.width, this.height - 84, 48, 36);
        this.addRenderableWidget(this.playerList);

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.back"),
                btn -> onClose()
        ).bounds(this.width / 2 - 75, this.height - 26, 150, 20).build());

        refreshPlayers();
    }

    private void onSearchChanged(String text) {
        this.filterText = text != null ? text.trim().toLowerCase(Locale.ROOT) : "";
        refreshPlayers();
    }

    public void refreshPlayers() {
        if (this.playerList != null) {
            this.playerList.refreshEntries(this.filterText);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
    }

    public class PlayerSelectionList extends ObjectSelectionList<PlayerSelectionList.PlayerEntry> {
        public PlayerSelectionList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        public void refreshEntries(String filter) {
            this.clearEntries();
            List<ClientTitleManager.KnownPlayerEntry> known = ClientTitleManager.getKnownPlayers();
            for (ClientTitleManager.KnownPlayerEntry entry : known) {
                if (filter.isEmpty() || entry.name().toLowerCase(Locale.ROOT).contains(filter)) {
                    this.addEntry(new PlayerEntry(entry));
                }
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

        public class PlayerEntry extends ObjectSelectionList.Entry<PlayerEntry> {
            private final ClientTitleManager.KnownPlayerEntry playerEntry;
            private final Button lockButton;
            private final Button manageButton;

            public PlayerEntry(ClientTitleManager.KnownPlayerEntry playerEntry) {
                this.playerEntry = playerEntry;
                this.manageButton = Button.builder(
                        Component.translatable("gui.epithet.button.manage"),
                        btn -> onOpenManage()
                ).bounds(0, 0, 56, 20).build();
                this.manageButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.button.manage_tooltip")));

                this.lockButton = Button.builder(
                        Component.empty(),
                        btn -> onToggleLock()
                ).bounds(0, 0, 48, 20).build();
                updateLockButton();
            }

            private void onOpenManage() {
                VanillaPlayerSelectorScreen.this.minecraft.setScreen(
                        new PlayerTitlesManageScreen(VanillaPlayerSelectorScreen.this, playerEntry)
                );
            }

            private void updateLockButton() {
                boolean locked = playerEntry.isLocked();
                if (locked) {
                    this.lockButton.setMessage(Component.translatable("gui.epithet.button.unlock").withStyle(ChatFormatting.RED));
                    this.lockButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.button.unlock_tooltip")));
                } else {
                    this.lockButton.setMessage(Component.translatable("gui.epithet.button.lock"));
                    this.lockButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.button.lock_tooltip")));
                }
            }

            private void onToggleLock() {
                PacketDistributor.sendToServer(new AdminActionPayload(
                        playerEntry.uuid(),
                        "toggle_lock",
                        ""
                ));
                ClientTitleManager.togglePlayerLock(playerEntry.uuid());
                VanillaPlayerSelectorScreen.this.refreshPlayers();
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                if (isMouseOver) {
                    guiGraphics.fill(left - 2, top - 1, left + width + 2, top + height + 1, 0x15FFFFFF);
                }

                // Render Player Face using PlayerFaceRenderer.draw (8x8 player skin face texture)
                int faceSize = 24;
                int faceX = left + 4;
                int faceY = top + (height - faceSize) / 2;
                PlayerFaceRenderer.draw(guiGraphics, playerEntry.skin(), faceX, faceY, faceSize);

                // Player name
                int nameX = faceX + faceSize + 6;
                int nameY = top + 5;
                Component nameComp = Component.literal(playerEntry.name());
                if (playerEntry.isLocked()) {
                    nameComp = Component.translatable("gui.epithet.status.locked_tag").append(" ").append(nameComp).withStyle(ChatFormatting.RED);
                }
                guiGraphics.drawString(font, nameComp, nameX, nameY, 0xFFFFFF);

                // Active title or "No Title"
                int titleY = nameY + 11;
                Component titleComp = playerEntry.formattedTitle();
                if (titleComp == null || titleComp.getString().isEmpty()) {
                    titleComp = Component.translatable("gui.epithet.no_title").withStyle(ChatFormatting.DARK_GRAY);
                }
                guiGraphics.drawString(font, titleComp, nameX, titleY, 0xAAAAAA);

                // Action buttons (Manage titles + Lock / Unlock toggle)
                int btnHeight = 20;
                int btnY = top + (height - btnHeight) / 2;
                int lockW = 48;
                int manageW = 56;

                this.lockButton.setX(left + width - lockW - 2);
                this.lockButton.setY(btnY);
                this.lockButton.render(guiGraphics, mouseX, mouseY, partialTick);

                this.manageButton.setX(left + width - lockW - 2 - manageW - 4);
                this.manageButton.setY(btnY);
                this.manageButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.manageButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (this.lockButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return Component.literal(playerEntry.name());
            }
        }
    }
}
