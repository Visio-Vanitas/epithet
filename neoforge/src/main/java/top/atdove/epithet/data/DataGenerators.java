package top.atdove.epithet.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import top.atdove.epithet.Epithet;

/**
 * Standard NeoForge GatherDataEvent subscriber for registering DataGen providers.
 */
@EventBusSubscriber(modid = Epithet.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();

        // Register client-side Language Providers for all 4 supported locales
        generator.addProvider(event.includeClient(), new ModLanguageProvider(packOutput, "zh_cn"));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(packOutput, "zh_tw"));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(packOutput, "en_us"));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(packOutput, "en_gb"));
    }
}
