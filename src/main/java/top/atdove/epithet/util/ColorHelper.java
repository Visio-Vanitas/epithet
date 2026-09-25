package top.atdove.epithet.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.Locale;

/**
 * Universal color parser and codec supporting:
 * 1) Hexadecimal RGB codes: "#FFAA00", "0xFFAA00", "FFAA00"
 * 2) Minecraft default § / & color codes: "§6", "§a", "&c", "§e"
 * 3) Vanilla formatting color names: "gold", "red", "aqua", "green", "light_purple", etc.
 */
public class ColorHelper {

    public static final Codec<Integer> FLEXIBLE_COLOR_CODEC = Codec.either(
        Codec.INT,
        Codec.STRING
    ).xmap(
        either -> either.map(
            num -> num,
            ColorHelper::parseColor
        ),
        Either::left
    );

    /**
     * Parses an arbitrary color input string in hex, section-sign, or color name formats.
     */
    public static int parseColor(String input) {
        if (input == null || input.trim().isEmpty()) {
            return 0xFFFFFF;
        }
        String s = input.trim().toLowerCase(Locale.ROOT);

        // Format 2: § or & color codes (e.g., "§a", "&6", "§c")
        if (s.startsWith("§") || s.startsWith("&")) {
            if (s.length() >= 2) {
                char code = s.charAt(1);
                ChatFormatting format = ChatFormatting.getByCode(code);
                if (format != null && format.getColor() != null) {
                    return format.getColor();
                }
            }
        }

        // Format 3: Official Minecraft color names (e.g. "gold", "aqua", "light_purple", "red")
        ChatFormatting namedFormat = ChatFormatting.getByName(s);
        if (namedFormat != null && namedFormat.getColor() != null) {
            return namedFormat.getColor();
        }

        // Also check with hyphens/underscores normalized
        String normalizedName = s.replace("-", "_").replace(" ", "_");
        ChatFormatting normalizedFormat = ChatFormatting.getByName(normalizedName);
        if (normalizedFormat != null && normalizedFormat.getColor() != null) {
            return normalizedFormat.getColor();
        }

        // Format 1: Hexadecimal RGB code
        try {
            if (s.startsWith("#")) {
                return (int) Long.parseLong(s.substring(1), 16);
            } else if (s.startsWith("0x")) {
                return (int) Long.parseLong(s.substring(2), 16);
            } else if (s.matches("^[0-9a-f]{6}$")) {
                return (int) Long.parseLong(s, 16);
            } else {
                return (int) Long.parseLong(s);
            }
        } catch (NumberFormatException ignored) {
        }

        // Fallback default white
        return 0xFFFFFF;
    }

    /**
     * Converts a raw RGB integer into a TextColor for Minecraft components.
     */
    public static TextColor toTextColor(int rgb) {
        return TextColor.fromRgb(rgb);
    }
}
