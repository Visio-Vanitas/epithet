package top.atdove.epithet.client;

import net.minecraft.client.Minecraft;

import top.atdove.epithet.client.gui.TitleSelectionScreen;

public class ClientHooks {
    public static void openTitleSelectionScreen() {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new TitleSelectionScreen(null));
        });
    }
}
