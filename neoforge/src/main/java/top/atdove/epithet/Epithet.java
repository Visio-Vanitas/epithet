package top.atdove.epithet;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

import org.slf4j.Logger;
import top.atdove.epithet.attachment.ModAttachments;
import top.atdove.epithet.command.TitleCommands;
import top.atdove.epithet.config.EpithetConfig;
import top.atdove.epithet.data.TitleReloadListener;
import top.atdove.epithet.event.TitleEventHandler;
import top.atdove.epithet.network.TitleNetworkHandler;

@Mod(Epithet.MOD_ID)
public class Epithet {
    public static final String MOD_ID = "epithet";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Epithet(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Epithet Mod (NeoForge 1.21.1)");

        modContainer.registerConfig(ModConfig.Type.CLIENT, EpithetConfig.CLIENT_SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, EpithetConfig.COMMON_SPEC);

        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(TitleNetworkHandler::registerPayloadHandlers);

        NeoForge.EVENT_BUS.register(TitleEventHandler.class);
        NeoForge.EVENT_BUS.addListener(TitleCommands::register);
        NeoForge.EVENT_BUS.addListener(TitleReloadListener::onAddReloadListener);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Epithet common setup completed");
    }
}
