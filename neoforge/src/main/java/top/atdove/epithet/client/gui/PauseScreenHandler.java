package top.atdove.epithet.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import top.atdove.epithet.Epithet;

/**
 * Injects a vanilla-style compact Title System button into the Pause (Game Menu) Screen.
 * Uses vanilla Items.NAME_TAG rendering to maintain native aesthetic consistency without emojis.
 */
@EventBusSubscriber(modid = Epithet.MOD_ID, value = Dist.CLIENT)
public class PauseScreenHandler {

    private static final ItemStack NAME_TAG_ICON = new ItemStack(Items.NAME_TAG);

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof PauseScreen pauseScreen) {
            // Aligned with the top row of the Pause Menu (Return to Game row at y = height / 4 + 24)
            // Perfectly avoids collision with Redeem Gift Codes (which injects at height / 4 + 57)
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
                    // Render native Minecraft NameTag item icon inside the button
                    guiGraphics.renderItem(NAME_TAG_ICON, getX() + 2, getY() + 2);
                }
            };

            titleButton.setTooltip(Tooltip.create(Component.translatable("gui.epithet.pause_button_tooltip")));
            event.addListener(titleButton);
        }
    }
}
