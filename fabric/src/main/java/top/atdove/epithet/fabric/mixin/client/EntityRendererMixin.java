package top.atdove.epithet.fabric.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.atdove.epithet.client.ClientDummyManager;
import top.atdove.epithet.client.ClientTitleManager;
import top.atdove.epithet.config.EpithetConfig;
import top.atdove.epithet.test.DummyManager;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @ModifyVariable(
            method = "renderNameTag",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Component epithet$injectTitleToNameTag(
            Component originalContent,
            T entity,
            Component content,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float partialTick
    ) {
        if (entity instanceof Player player) {
            Component title = ClientTitleManager.getActiveTitle(player);
            if (title != null && !title.getString().isEmpty()) {
                Component wrapped = EpithetConfig.formatTitle(title);
                return Component.empty().append(wrapped).append(" ").append(originalContent);
            }
        } else if (entity.getTags().contains(DummyManager.DUMMY_TAG) || ClientDummyManager.isDummy(entity)) {
            entity.addTag(DummyManager.DUMMY_TAG);
            Component title = ClientDummyManager.getDummyTitle(entity);
            if (title != null && !title.getString().isEmpty()) {
                Component wrapped = EpithetConfig.formatTitle(title);
                return Component.empty().append(wrapped).append(" ").append(originalContent);
            }
        }
        return originalContent;
    }
}
