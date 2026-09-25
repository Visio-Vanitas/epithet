package top.atdove.epithet.config;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.common.ModConfigSpec;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.util.ColorHelper;

/**
 * Configuration specification for Epithet Mod.
 * Supports configurable bracket wrappers, toggleable hover tooltips in chat,
 * and customizable built-in default titles for new players.
 */
public class EpithetConfig {

    public static class Client {
        public final ModConfigSpec.ConfigValue<String> prefixBracket;
        public final ModConfigSpec.ConfigValue<String> suffixBracket;
        public final ModConfigSpec.BooleanValue wrapWithBrackets;
        public final ModConfigSpec.BooleanValue enableChatHoverTooltip;

        public Client(ModConfigSpec.Builder builder) {
            builder.push("rendering");

            wrapWithBrackets = builder
                    .comment("Whether to automatically wrap title display names with brackets if they don't have them")
                    .define("wrapWithBrackets", true);

            prefixBracket = builder
                    .comment("Left/opening bracket symbol used when rendering titles (default: 「)")
                    .define("prefixBracket", "「");

            suffixBracket = builder
                    .comment("Right/closing bracket symbol used when rendering titles (default: 」)")
                    .define("suffixBracket", "」");

            enableChatHoverTooltip = builder
                    .comment("Whether hovering over a title in chat displays its detailed description tooltip (like achievements)")
                    .define("enableChatHoverTooltip", true);

            builder.pop();
        }
    }

    public static class Common {
        public final ModConfigSpec.BooleanValue enableDefaultTitle;
        public final ModConfigSpec.BooleanValue autoEquipDefaultTitle;
        public final ModConfigSpec.ConfigValue<String> defaultTitleId;
        public final ModConfigSpec.ConfigValue<String> defaultTitleDisplayName;
        public final ModConfigSpec.ConfigValue<String> defaultTitleDescription;
        public final ModConfigSpec.ConfigValue<String> defaultTitleColor;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("default_title");

            enableDefaultTitle = builder
                    .comment("Whether to grant a built-in default title to every new player joining the server")
                    .define("enableDefaultTitle", true);

            autoEquipDefaultTitle = builder
                    .comment("Whether to automatically equip the default title for new players if they have no active title")
                    .define("autoEquipDefaultTitle", true);

            defaultTitleId = builder
                    .comment("ResourceLocation ID for the built-in default title")
                    .define("defaultTitleId", "epithet:beginner");

            defaultTitleDisplayName = builder
                    .comment("Display name for the built-in default title")
                    .define("defaultTitleDisplayName", "初出茅庐");

            defaultTitleDescription = builder
                    .comment("Description for the built-in default title")
                    .define("defaultTitleDescription", "初入方块世界的懵懂冒险家。");

            defaultTitleColor = builder
                    .comment("Color for the built-in default title (hex code, § code, or color name)")
                    .define("defaultTitleColor", "green");

            builder.pop();
        }
    }

    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();

        ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
        COMMON = new Common(commonBuilder);
        COMMON_SPEC = commonBuilder.build();
    }

    public static TitleDefinition getBuiltinDefaultTitle() {
        ResourceLocation id = ResourceLocation.tryParse(COMMON.defaultTitleId.get());
        if (id == null) {
            id = ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "beginner");
        }
        int color = ColorHelper.parseColor(COMMON.defaultTitleColor.get());
        return new TitleDefinition(
            id,
            Component.literal(COMMON.defaultTitleDisplayName.get()),
            Component.literal(COMMON.defaultTitleDescription.get()),
            color,
            10,
            "common",
            ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "default"),
            true
        );
    }

    /**
     * Formats a title component by wrapping it with configured brackets if not already wrapped.
     */
    public static Component formatTitle(Component titleComponent) {
        if (titleComponent == null || titleComponent.getString().isEmpty()) {
            return Component.empty();
        }
        if (!CLIENT.wrapWithBrackets.get()) {
            return titleComponent;
        }

        String left = CLIENT.prefixBracket.get() != null ? CLIENT.prefixBracket.get() : "「";
        String right = CLIENT.suffixBracket.get() != null ? CLIENT.suffixBracket.get() : "」";

        String raw = titleComponent.getString().trim();

        // Dynamic brackets check: if already enclosed by configured brackets, do not double-wrap
        if (!left.isEmpty() && !right.isEmpty()) {
            if (raw.startsWith(left) && raw.endsWith(right)) {
                return titleComponent;
            }
        } else if (!left.isEmpty() && raw.startsWith(left)) {
            return titleComponent;
        } else if (!right.isEmpty() && raw.endsWith(right)) {
            return titleComponent;
        }

        // Common static brackets check: if already enclosed, do not double-wrap
        if ((raw.startsWith("「") && raw.endsWith("」"))
                || (raw.startsWith("[") && raw.endsWith("]"))
                || (raw.startsWith("【") && raw.endsWith("】"))
                || (raw.startsWith("《") && raw.endsWith("》"))
                || (raw.startsWith("(") && raw.endsWith(")"))
                || (raw.startsWith("{") && raw.endsWith("}"))
                || (raw.startsWith("<") && raw.endsWith(">"))
                || (raw.startsWith("『") && raw.endsWith("』"))
                || (raw.startsWith("〈") && raw.endsWith("〉"))
                || (raw.startsWith("〔") && raw.endsWith("〕"))
                || (raw.startsWith("〖") && raw.endsWith("〗"))
                || (raw.startsWith("〘") && raw.endsWith("〙"))
                || (raw.startsWith("〚") && raw.endsWith("〛"))) {
            return titleComponent;
        }

        Style style = titleComponent.getStyle();
        return Component.literal(left).withStyle(style)
                .append(titleComponent)
                .append(Component.literal(right).withStyle(style));
    }

    /**
     * Formats a title for chat, adding rich achievement-like hover tooltips if enabled.
     */
    public static Component formatChatTitle(TitleDefinition titleDef) {
        if (titleDef == null) {
            return Component.empty();
        }
        Component baseTitle = formatTitle(titleDef.getFormattedDisplayName());
        if (!CLIENT.enableChatHoverTooltip.get()) {
            return baseTitle;
        }

        // Build rich hover tooltip like vanilla achievements
        MutableComponent tooltip = Component.empty();
        tooltip.append(titleDef.getFormattedDisplayName());

        if (titleDef.rarity() != null && !titleDef.rarity().isEmpty()) {
            tooltip.append("\n")
                   .append(Component.translatableWithFallback("rarity.epithet." + titleDef.rarity(), titleDef.rarity()).withStyle(ChatFormatting.DARK_AQUA));
        }

        if (titleDef.description() != null && !titleDef.description().getString().isEmpty()) {
            tooltip.append("\n\n")
                   .append(titleDef.description().copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        return baseTitle.copy().withStyle(style ->
            style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))
        );
    }
}
