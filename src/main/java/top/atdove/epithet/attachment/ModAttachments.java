package top.atdove.epithet.attachment;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import top.atdove.epithet.Epithet;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Epithet.MOD_ID);

    public static final Supplier<AttachmentType<PlayerTitleData>> PLAYER_TITLE_DATA =
        ATTACHMENT_TYPES.register("player_title_data", () -> AttachmentType.builder(PlayerTitleData::new)
            .serialize(PlayerTitleData.CODEC)
            .copyOnDeath()
            .build());
}
