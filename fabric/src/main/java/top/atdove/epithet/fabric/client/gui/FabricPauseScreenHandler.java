package top.atdove.epithet.fabric.client.gui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.atdove.epithet.client.gui.TitleSelectionScreen;

public class FabricPauseScreenHandler {

    private static final ItemStack NAME_TAG_ICON = new ItemStack(Items.NAME_TAG);

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof PauseScreen pauseScreen) {
                // 安全防碰撞坐标: 对齐菜单首行右侧，y = height / 4 + 24
                int x = pauseScreen.width / 2 + 104 + 4;
                int y = pauseScreen.height / 4 + 24;
                int size = 20;

                Button titleButton = new Button(
                        x, y, size, size,
                        Component.empty(),
                        btn -> Minecraft.getInstance().setScreen(new TitleSelectionScreen(pauseScreen)),
                        supplier -> supplier.get()
                ) {
                    @Override
                    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                        guiGraphics.renderItem(NAME_TAG_ICON, getX() + 2, getY() + 2);
                    }
                };

                titleButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.pause_button_tooltip")));
                Screens.getButtons(screen).add(titleButton);
            }
        });
    }
}
