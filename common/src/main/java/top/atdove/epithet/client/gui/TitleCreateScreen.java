package top.atdove.epithet.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.PacketDistributor;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.config.EpithetConfig;
import top.atdove.epithet.network.AdminCreateTitlePayload;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.util.ColorHelper;

import java.util.Locale;

/**
 * Native Minecraft vanilla-style GUI for administrators to create custom titles interactively.
 * Features live rendering preview of static/animated gradients.
 */
public class TitleCreateScreen extends Screen {
    private final Screen lastScreen;

    private EditBox idBox;
    private EditBox nameBox;
    private EditBox descBox;
    private EditBox colorBox;
    private EditBox rarityBox;

    public TitleCreateScreen(Screen lastScreen) {
        super(Component.translatable("gui.epithet.create.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 40;
        int boxWidth = 220;
        int boxHeight = 18;

        // 1. ID EditBox
        this.idBox = new EditBox(this.font, centerX - boxWidth / 2, startY, boxWidth, boxHeight, Component.empty());
        this.idBox.setValue("custom:my_title");
        this.idBox.setTooltip(Tooltip.create(Component.translatable("gui.epithet.create.id_tooltip")));
        this.addRenderableWidget(this.idBox);

        // 2. Display Name EditBox (Supports text, gradient, animated gradient)
        this.nameBox = new EditBox(this.font, centerX - boxWidth / 2, startY + 32, boxWidth, boxHeight, Component.empty());
        this.nameBox.setValue("<animated-gradient:#FF416C:#FF4B2B:speed=6>烈火流光</animated-gradient>");
        this.nameBox.setMaxLength(256);
        this.nameBox.setTooltip(Tooltip.create(Component.translatable("gui.epithet.create.name_tooltip")));
        this.addRenderableWidget(this.nameBox);

        // 3. Description EditBox
        this.descBox = new EditBox(this.font, centerX - boxWidth / 2, startY + 64, boxWidth, boxHeight, Component.empty());
        this.descBox.setValue("在浩瀚天地间游历的冒险者。");
        this.descBox.setMaxLength(256);
        this.addRenderableWidget(this.descBox);

        // 4. Color & Rarity side-by-side
        int halfWidth = (boxWidth - 8) / 2;
        this.colorBox = new EditBox(this.font, centerX - boxWidth / 2, startY + 96, halfWidth, boxHeight, Component.empty());
        this.colorBox.setValue("#FFAA00");
        this.colorBox.setTooltip(Tooltip.create(Component.translatable("gui.epithet.create.color_tooltip")));
        this.addRenderableWidget(this.colorBox);

        this.rarityBox = new EditBox(this.font, centerX - boxWidth / 2 + halfWidth + 8, startY + 96, halfWidth, boxHeight, Component.empty());
        this.rarityBox.setValue("epic");
        this.rarityBox.setTooltip(Tooltip.create(Component.translatable("gui.epithet.create.rarity_tooltip")));
        this.addRenderableWidget(this.rarityBox);

        // Action Buttons
        int buttonY = this.height - 32;
        int btnWidth = 90;
        int btnGap = 8;
        int totalBtnWidth = btnWidth * 3 + btnGap * 2;
        int btnStartX = centerX - totalBtnWidth / 2;

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.epithet.create.button.save_and_equip"),
                btn -> onSave(true)
        ).bounds(btnStartX, buttonY, btnWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.epithet.create.button.save_only"),
                btn -> onSave(false)
        ).bounds(btnStartX + btnWidth + btnGap, buttonY, btnWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                btn -> onClose()
        ).bounds(btnStartX + (btnWidth + btnGap) * 2, buttonY, btnWidth, 20).build());
    }

    private void onSave(boolean autoEquip) {
        String idStr = this.idBox.getValue().trim();
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        if (id == null) {
            id = ResourceLocation.fromNamespaceAndPath("custom", idStr.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_"));
        }

        String nameStr = this.nameBox.getValue().trim();
        Component nameComp = ColorHelper.hasGradientTags(nameStr)
                ? Component.literal(nameStr)
                : Component.literal(nameStr.isEmpty() ? id.getPath() : nameStr);

        Component descComp = this.descBox.getValue().trim().isEmpty()
                ? Component.empty()
                : Component.literal(this.descBox.getValue().trim());

        int color = ColorHelper.parseColor(this.colorBox.getValue());
        String rarity = this.rarityBox.getValue().trim().isEmpty() ? "common" : this.rarityBox.getValue().trim().toLowerCase(Locale.ROOT);

        TitleDefinition newTitle = new TitleDefinition(
                id,
                nameComp,
                descComp,
                color,
                0,
                rarity,
                ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "default"),
                false
        );

        // Send payload to server
        PacketDistributor.sendToServer(new AdminCreateTitlePayload(newTitle, autoEquip));

        onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = 40;
        int boxWidth = 220;

        // Title Header
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 12, 0xFFFFFF);

        // Field Labels
        guiGraphics.drawString(this.font, Component.translatable("gui.epithet.create.label.id"), centerX - boxWidth / 2, startY - 11, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.translatable("gui.epithet.create.label.name"), centerX - boxWidth / 2, startY + 21, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.translatable("gui.epithet.create.label.desc"), centerX - boxWidth / 2, startY + 53, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.translatable("gui.epithet.create.label.color"), centerX - boxWidth / 2, startY + 85, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.translatable("gui.epithet.create.label.rarity"), centerX - boxWidth / 2 + (boxWidth - 8) / 2 + 8, startY + 85, 0xAAAAAA);

        // Live Preview Box (Renders real-time gradient and animated wave preview)
        int previewY = startY + 130;
        guiGraphics.fill(centerX - boxWidth / 2, previewY, centerX + boxWidth / 2, previewY + 28, 0x44000000);
        guiGraphics.renderOutline(centerX - boxWidth / 2, previewY, boxWidth, 28, 0x66FFFFFF);

        String currentName = this.nameBox.getValue().trim();
        Component previewComp;
        if (ColorHelper.hasGradientTags(currentName)) {
            previewComp = ColorHelper.parseGradientComponent(currentName);
        } else {
            previewComp = Component.literal(currentName.isEmpty() ? "预览文本 Preview" : currentName);
            int parsedColor = ColorHelper.parseColor(this.colorBox.getValue());
            if (parsedColor != 0xFFFFFF) {
                previewComp = previewComp.copy().withStyle(style -> style.withColor(parsedColor));
            }
        }
        Component fullPreview = EpithetConfig.formatTitle(previewComp);

        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.epithet.create.preview_label"), centerX, previewY - 10, 0xFFAA00);
        guiGraphics.drawCenteredString(this.font, fullPreview, centerX, previewY + 10, 0xFFFFFF);
    }
}
