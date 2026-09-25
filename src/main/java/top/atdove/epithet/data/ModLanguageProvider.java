package top.atdove.epithet.data;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.common.data.LanguageProvider;

import top.atdove.epithet.Epithet;

import java.util.Map;

/**
 * Standard NeoForge DataGen LanguageProvider for Epithet Mod.
 */
public class ModLanguageProvider extends LanguageProvider {

    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, Epithet.MOD_ID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        Map<String, String> translations;
        switch (locale) {
            case "zh_tw" -> translations = LanguageDataGenTool.buildZhTw();
            case "en_gb" -> translations = LanguageDataGenTool.buildEnGb();
            case "en_us" -> translations = LanguageDataGenTool.buildEnUs();
            default -> translations = LanguageDataGenTool.buildZhCn();
        }

        for (Map.Entry<String, String> entry : translations.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }
}
