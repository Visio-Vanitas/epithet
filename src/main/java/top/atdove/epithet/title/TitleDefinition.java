package top.atdove.epithet.title;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.util.ColorHelper;

import java.util.Optional;

public record TitleDefinition(
    ResourceLocation id,
    Component displayName,
    Component description,
    int color,
    int priority,
    String rarity,
    ResourceLocation icon,
    boolean defaultUnlocked,
    Optional<ResourceLocation> advancement
) {
    public TitleDefinition(
        ResourceLocation id,
        Component displayName,
        Component description,
        int color,
        int priority,
        String rarity,
        ResourceLocation icon,
        boolean defaultUnlocked
    ) {
        this(id, displayName, description, color, priority, rarity, icon, defaultUnlocked, Optional.empty());
    }

    /**
     * Flexible Codec that seamlessly accepts:
     * 1) Standard Component JSON object (e.g. {"text": "Dragon Slayer", "color": "gold"} or {"translate": "title.x"})
     * 2) Plain string: automatically resolves via translatableWithFallback(str, str),
     *    supporting both translation keys (for i18n) and literal strings (for unified cross-language display).
     */
    public static final Codec<Component> FLEXIBLE_COMPONENT_CODEC = Codec.either(
        Codec.STRING,
        ComponentSerialization.CODEC
    ).xmap(
        either -> either.map(
            str -> Component.translatableWithFallback(str, str),
            comp -> comp
        ),
        Either::right
    );

    public static final Codec<TitleDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("id").forGetter(TitleDefinition::id),
        FLEXIBLE_COMPONENT_CODEC.fieldOf("displayName").forGetter(TitleDefinition::displayName),
        FLEXIBLE_COMPONENT_CODEC.optionalFieldOf("description", CommonComponents.EMPTY).forGetter(TitleDefinition::description),
        ColorHelper.FLEXIBLE_COLOR_CODEC.optionalFieldOf("color", 0xFFFFFF).forGetter(TitleDefinition::color),
        Codec.INT.optionalFieldOf("priority", 0).forGetter(TitleDefinition::priority),
        Codec.STRING.optionalFieldOf("rarity", "common").forGetter(TitleDefinition::rarity),
        ResourceLocation.CODEC.optionalFieldOf("icon", ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "default")).forGetter(TitleDefinition::icon),
        Codec.BOOL.optionalFieldOf("defaultUnlocked", false).forGetter(TitleDefinition::defaultUnlocked),
        ResourceLocation.CODEC.optionalFieldOf("advancement").forGetter(TitleDefinition::advancement)
    ).apply(instance, TitleDefinition::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TitleDefinition> STREAM_CODEC = StreamCodec.of(
        TitleDefinition::encode,
        TitleDefinition::decode
    );

    public static void encode(RegistryFriendlyByteBuf buf, TitleDefinition value) {
        ResourceLocation.STREAM_CODEC.encode(buf, value.id);
        ComponentSerialization.STREAM_CODEC.encode(buf, value.displayName);
        ComponentSerialization.STREAM_CODEC.encode(buf, value.description);
        ByteBufCodecs.VAR_INT.encode(buf, value.color);
        ByteBufCodecs.VAR_INT.encode(buf, value.priority);
        ByteBufCodecs.STRING_UTF8.encode(buf, value.rarity);
        ResourceLocation.STREAM_CODEC.encode(buf, value.icon);
        ByteBufCodecs.BOOL.encode(buf, value.defaultUnlocked);
        ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, value.advancement);
    }

    public static TitleDefinition decode(RegistryFriendlyByteBuf buf) {
        ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
        Component displayName = ComponentSerialization.STREAM_CODEC.decode(buf);
        Component description = ComponentSerialization.STREAM_CODEC.decode(buf);
        int color = ByteBufCodecs.VAR_INT.decode(buf);
        int priority = ByteBufCodecs.VAR_INT.decode(buf);
        String rarity = ByteBufCodecs.STRING_UTF8.decode(buf);
        ResourceLocation icon = ResourceLocation.STREAM_CODEC.decode(buf);
        boolean defaultUnlocked = ByteBufCodecs.BOOL.decode(buf);
        Optional<ResourceLocation> advancement = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf);
        return new TitleDefinition(id, displayName, description, color, priority, rarity, icon, defaultUnlocked, advancement);
    }

    /**
     * Resolves the display name with fallback color styling applied if the component itself
     * has not explicitly set a color style.
     */
    public Component getFormattedDisplayName() {
        if (this.color != 0xFFFFFF && this.displayName.getStyle().getColor() == null) {
            return this.displayName.copy().withStyle(style -> style.withColor(this.color));
        }
        return this.displayName;
    }
}
