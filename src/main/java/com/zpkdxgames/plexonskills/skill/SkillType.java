package com.zpkdxgames.plexonskills.skill;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum SkillType {
    MINING(Material.DIAMOND_PICKAXE),
    WOODCUTTING(Material.IRON_AXE),
    EXCAVATION(Material.IRON_SHOVEL),
    FARMING(Material.WHEAT),
    FISHING(Material.FISHING_ROD),
    SWORDS(Material.IRON_SWORD),
    AXES(Material.DIAMOND_AXE),
    ARCHERY(Material.BOW),
    UNARMED(Material.LEATHER),
    TAMING(Material.BONE),
    ACROBATICS(Material.FEATHER),
    REPAIR(Material.ANVIL),
    ALCHEMY(Material.BREWING_STAND);

    private final Material icon;

    SkillType(Material icon) { this.icon = icon; }

    public Material icon() { return icon; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String displayName() {
        String lower = id().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public static Optional<SkillType> parse(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try { return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT))); }
        catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }

    public static Set<SkillType> blockSkills() { return EnumSet.of(MINING, WOODCUTTING, EXCAVATION, FARMING); }
    public static Set<SkillType> combatSkills() { return EnumSet.of(SWORDS, AXES, ARCHERY, UNARMED, TAMING); }
}
