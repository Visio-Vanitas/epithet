package top.atdove.epithet.client.renderer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.client.ClientDummyManager;
import top.atdove.epithet.client.ClientTitleManager;
import top.atdove.epithet.config.EpithetConfig;
import top.atdove.epithet.test.DummyManager;

/**
 * Overhead NameTag rendering pipeline for Title System.
 * Fully inherits all vanilla render pipeline characteristics:
 * - Sneaking translucent channel (isDiscrete)
 * - Invisibility effects (isInvisibleTo) completely hidden
 * - Block occlusion depth test
 * - 64 blocks (4096) distance culling
 * - Scoreboard team (PlayerTeam) visibility rules
 */
@EventBusSubscriber(modid = Epithet.MOD_ID, value = Dist.CLIENT)
public class TitleRenderHandler {

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        Entity entity = event.getEntity();
        Component title = null;
        if (entity instanceof Player player) {
            title = ClientTitleManager.getActiveTitle(player);
        } else if (entity.getTags().contains(DummyManager.DUMMY_TAG) || ClientDummyManager.isDummy(entity)) {
            title = ClientDummyManager.getDummyTitle(entity);
        }

        if (title != null && !title.getString().isEmpty()) {
            Component wrapped = EpithetConfig.formatTitle(title);
            Component content = event.getContent();
            if (content != null) {
                String contentStr = content.getString();
                String wrappedStr = wrapped.getString();
                if (!wrappedStr.isEmpty() && (contentStr.startsWith(wrappedStr + " ") || contentStr.startsWith(wrappedStr))) {
                    return;
                }
                event.setContent(Component.empty().append(wrapped).append(" ").append(content));
            }
        }
    }
}
