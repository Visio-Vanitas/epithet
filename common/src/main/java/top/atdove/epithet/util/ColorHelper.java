package top.atdove.epithet.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal color parser and gradient engine supporting:
 * 1) Hexadecimal RGB codes: "#FFAA00", "0xFFAA00", "FFAA00"
 * 2) Minecraft default § / & color codes: "§6", "§a", "&c", "§e"
 * 3) Vanilla formatting color names: "gold", "red", "aqua", "green", "light_purple", etc.
 * 4) Static Gradient tags: <gradient:#FF416C:#FF4B2B>Text</gradient> or multi-color <gradient:gold:red:#FF00FF>Text</gradient>
 * 5) Animated Gradient tags: <animated-gradient:#FF416C:#FF4B2B:speed=5>Text</animated-gradient>
 * 6) Static & Animated Rainbow tags: <rainbow>Text</rainbow>, <animated-rainbow:speed=6>Text</animated-rainbow>
 */
public class ColorHelper {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:([^>]+)>(.*?)</gradient>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANIMATED_GRADIENT_PATTERN = Pattern.compile("<animated-gradient:([^>]+)>(.*?)</animated-gradient>", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAINBOW_PATTERN = Pattern.compile("<rainbow>(.*?)</rainbow>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANIMATED_RAINBOW_PATTERN = Pattern.compile("<animated-rainbow(?::speed=(\\d+))?>(.*?)</animated-rainbow>", Pattern.CASE_INSENSITIVE);

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

    /**
     * Checks if a string contains any gradient or rainbow markup tags.
     */
    public static boolean hasGradientTags(String text) {
        if (text == null || text.isEmpty()) return false;
        return text.contains("<gradient:") || text.contains("<animated-gradient:")
                || text.contains("<rainbow>") || text.contains("<animated-rainbow");
    }

    /**
     * Parses gradient or animated gradient markup into a styled Minecraft Component.
     */
    public static Component parseGradientComponent(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        if (!hasGradientTags(input)) {
            return Component.translatableWithFallback(input, input);
        }

        MutableComponent root = Component.empty();
        int lastIndex = 0;

        // Comprehensive pattern matching all gradient/rainbow tags in order
        Pattern combined = Pattern.compile(
            "<(?<type>gradient|animated-gradient|rainbow|animated-rainbow)(?::(?<params>[^>]+))?>(?<content>.*?)</\\k<type>>",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = combined.matcher(input);

        while (matcher.find()) {
            // Append non-gradient text leading up to this match
            if (matcher.start() > lastIndex) {
                String plain = input.substring(lastIndex, matcher.start());
                root.append(Component.literal(plain));
            }

            String type = matcher.group("type").toLowerCase(Locale.ROOT);
            String params = matcher.group("params");
            String content = matcher.group("content");

            switch (type) {
                case "gradient" -> root.append(buildStaticGradient(content, parseColorList(params)));
                case "animated-gradient" -> root.append(buildAnimatedGradient(content, parseColorList(params), parseSpeed(params)));
                case "rainbow" -> root.append(buildStaticRainbow(content));
                case "animated-rainbow" -> root.append(buildAnimatedRainbow(content, parseSpeed(params)));
                default -> root.append(Component.literal(content));
            }

            lastIndex = matcher.end();
        }

        if (lastIndex < input.length()) {
            root.append(Component.literal(input.substring(lastIndex)));
        }

        return root;
    }

    private static List<Integer> parseColorList(String params) {
        List<Integer> colors = new ArrayList<>();
        if (params == null || params.isEmpty()) {
            colors.add(0xFFFFFF);
            colors.add(0xAAAAAA);
            return colors;
        }

        String[] parts = params.split(":");
        for (String part : parts) {
            String p = part.trim();
            if (p.toLowerCase(Locale.ROOT).startsWith("speed=")) {
                continue;
            }
            if (!p.isEmpty()) {
                colors.add(parseColor(p));
            }
        }
        if (colors.isEmpty()) {
            colors.add(0xFFFFFF);
        }
        if (colors.size() == 1) {
            colors.add(colors.get(0));
        }
        return colors;
    }

    private static int parseSpeed(String params) {
        if (params != null) {
            Matcher m = Pattern.compile("speed=(\\d+)", Pattern.CASE_INSENSITIVE).matcher(params);
            if (m.find()) {
                try {
                    return Math.max(1, Math.min(20, Integer.parseInt(m.group(1))));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 5; // Default wave speed
    }

    /**
     * Builds static multi-color interpolated linear gradient.
     */
    public static Component buildStaticGradient(String text, List<Integer> colors) {
        if (text == null || text.isEmpty()) return Component.empty();
        MutableComponent comp = Component.empty();
        int len = text.length();

        for (int i = 0; i < len; i++) {
            float progress = len > 1 ? (float) i / (len - 1) : 0.0f;
            int rgb = interpolateMulti(colors, progress);
            comp.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        return comp;
    }

    /**
     * Builds dynamic time-phased animated gradient component (per-frame wave).
     */
    public static Component buildAnimatedGradient(String text, List<Integer> colors, int speed) {
        if (text == null || text.isEmpty()) return Component.empty();
        MutableComponent comp = Component.empty();
        int len = text.length();

        long now = System.currentTimeMillis();
        long period = Math.max(500L, 5000L / speed);
        float phase = (float) (now % period) / period;

        for (int i = 0; i < len; i++) {
            float progress = (phase + (float) i / Math.max(1, len)) % 1.0f;
            int rgb = interpolateMulti(colors, progress);
            comp.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        return comp;
    }

    /**
     * Builds static rainbow spectrum component.
     */
    public static Component buildStaticRainbow(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        MutableComponent comp = Component.empty();
        int len = text.length();

        for (int i = 0; i < len; i++) {
            float hue = len > 1 ? (float) i / len : 0.0f;
            int rgb = hsvToRgb(hue, 0.85f, 1.0f);
            comp.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        return comp;
    }

    /**
     * Builds dynamic rolling rainbow spectrum component.
     */
    public static Component buildAnimatedRainbow(String text, int speed) {
        if (text == null || text.isEmpty()) return Component.empty();
        MutableComponent comp = Component.empty();
        int len = text.length();

        long now = System.currentTimeMillis();
        long period = Math.max(500L, 6000L / speed);
        float phase = (float) (now % period) / period;

        for (int i = 0; i < len; i++) {
            float hue = (phase + (float) i / Math.max(1, len)) % 1.0f;
            int rgb = hsvToRgb(hue, 0.85f, 1.0f);
            comp.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        return comp;
    }

    /**
     * Multi-color smooth linear interpolation for progress in [0.0, 1.0].
     */
    public static int interpolateMulti(List<Integer> colors, float progress) {
        if (colors == null || colors.isEmpty()) return 0xFFFFFF;
        if (colors.size() == 1) return colors.get(0);

        float p = Math.max(0.0f, Math.min(1.0f, progress));
        int segments = colors.size() - 1;
        float segmentSize = 1.0f / segments;
        int index = Math.min(segments - 1, (int) (p / segmentSize));
        float segmentProgress = (p - (index * segmentSize)) / segmentSize;

        return interpolate(colors.get(index), colors.get(index + 1), segmentProgress);
    }

    /**
     * 2-color linear interpolation.
     */
    public static int interpolate(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Converts HSV color to RGB integer.
     */
    public static int hsvToRgb(float h, float s, float v) {
        int r = 0, g = 0, b = 0;
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        switch (i % 6) {
            case 0 -> { r = Math.round(v * 255); g = Math.round(t * 255); b = Math.round(p * 255); }
            case 1 -> { r = Math.round(q * 255); g = Math.round(v * 255); b = Math.round(p * 255); }
            case 2 -> { r = Math.round(p * 255); g = Math.round(v * 255); b = Math.round(t * 255); }
            case 3 -> { r = Math.round(p * 255); g = Math.round(q * 255); b = Math.round(v * 255); }
            case 4 -> { r = Math.round(t * 255); g = Math.round(p * 255); b = Math.round(v * 255); }
            case 5 -> { r = Math.round(v * 255); g = Math.round(p * 255); b = Math.round(q * 255); }
        }
        return (r << 16) | (g << 8) | b;
    }
}
