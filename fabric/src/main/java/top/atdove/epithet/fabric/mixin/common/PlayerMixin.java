package top.atdove.epithet.fabric.mixin.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.atdove.epithet.Epithet;
import top.atdove.epithet.attachment.PlayerTitleData;
import top.atdove.epithet.fabric.duck.IPlayerTitleDataHolder;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements IPlayerTitleDataHolder {

    @Unique
    private PlayerTitleData epithet$titleData = new PlayerTitleData();

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public PlayerTitleData epithet$getTitleData() {
        return this.epithet$titleData;
    }

    @Override
    public void epithet$setTitleData(PlayerTitleData data) {
        this.epithet$titleData = data != null ? data : new PlayerTitleData();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void epithet$writeTitleData(CompoundTag compound, CallbackInfo ci) {
        PlayerTitleData.CODEC.encodeStart(NbtOps.INSTANCE, this.epithet$titleData)
                .resultOrPartial(err -> Epithet.LOGGER.error("Failed to encode title data: {}", err))
                .ifPresent(tag -> compound.put("epithet_data", tag));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void epithet$readTitleData(CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("epithet_data", Tag.TAG_COMPOUND)) {
            PlayerTitleData.CODEC.parse(NbtOps.INSTANCE, compound.getCompound("epithet_data"))
                    .resultOrPartial(err -> Epithet.LOGGER.error("Failed to decode title data: {}", err))
                    .ifPresent(data -> this.epithet$titleData = data);
        }
    }
}
