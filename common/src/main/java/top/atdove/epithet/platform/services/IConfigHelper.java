package top.atdove.epithet.platform.services;

import net.minecraft.resources.ResourceLocation;

public interface IConfigHelper {
    // Client configuration properties
    boolean isWrapWithBrackets();
    String getPrefixBracket();
    String getSuffixBracket();
    boolean isChatHoverTooltipEnabled();

    // Common/Server configuration properties
    boolean isDefaultTitleEnabled();
    boolean isAutoEquipDefaultTitle();
    ResourceLocation getDefaultTitleId();
    String getDefaultTitleDisplayName();
    String getDefaultTitleDescription();
    String getDefaultTitleColor();
}
