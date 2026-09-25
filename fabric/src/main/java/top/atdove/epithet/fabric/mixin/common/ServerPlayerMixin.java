package top.atdove.epithet.fabric.mixin.common;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.atdove.epithet.fabric.duck.IPlayerTitleDataHolder;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void epithet$copyFromOldPlayer(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        IPlayerTitleDataHolder newHolder = (IPlayerTitleDataHolder) this;
        IPlayerTitleDataHolder oldHolder = (IPlayerTitleDataHolder) oldPlayer;
        newHolder.epithet$getTitleData().copyFrom(oldHolder.epithet$getTitleData());
    }
}
