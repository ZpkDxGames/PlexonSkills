package com.zpkdxgames.plexonskills.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class SkillsUi {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final NumberFormat INTEGER = NumberFormat.getIntegerInstance(Locale.US);

    private SkillsUi() {}

    static Component mm(String markup) {
        return MINI.deserialize(markup);
    }

    static ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm(name));
        List<Component> lines = new ArrayList<>(lore.length);
        for (String line : lore) lines.add(mm(line));
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }

    static String exact(long value) {
        synchronized (INTEGER) {
            return INTEGER.format(value);
        }
    }

    static String percent(double progress) {
        double safe = Math.max(0.0, Math.min(1.0, progress));
        return String.format(Locale.ROOT, "%.1f%%", safe * 100.0);
    }

    static String progressColor(double progress) {
        double safe = Math.max(0.0, Math.min(1.0, progress));
        if (safe >= 0.999) return "#76FF03";
        if (safe >= 0.50) return "#FFD740";
        return "#FF5252";
    }

    static String progressBar(double progress, int width) {
        int safeWidth = Math.max(4, Math.min(20, width));
        double safeProgress = Math.max(0.0, Math.min(1.0, progress));
        int filled = (int) Math.round(safeProgress * safeWidth);
        filled = Math.max(0, Math.min(safeWidth, filled));
        int empty = safeWidth - filled;
        StringBuilder result = new StringBuilder("<!italic>");
        if (filled > 0) {
            result.append("<gradient:#66BB6A:#AEEA00>")
                .append("■".repeat(filled))
                .append("</gradient>");
        }
        if (empty > 0) {
            result.append("<#263238>")
                .append("■".repeat(empty))
                .append("</#263238>");
        }
        return result.toString();
    }

    static String baseName(SkillPalette palette, String displayName, String secondary) {
        return "<!italic><" + palette.colorA() + ">" + palette.icon() + "</" + palette.colorA() + "> "
            + palette.gradient(displayName)
            + (secondary == null || secondary.isBlank() ? "" : " <dark_gray>•</dark_gray> <gray>" + secondary + "</gray>");
    }

    static String backName() {
        return "<!italic><#FFD54F><bold>← Back</bold></#FFD54F>";
    }

    static String closeName() {
        return "<!italic><#EF5350><bold>✕ Close</bold></#EF5350>";
    }
}
