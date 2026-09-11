package com.zpkdxgames.plexonskills.gui;

import com.zpkdxgames.plexonskills.skill.SkillType;

/** Domain-specific visual accents. These follow the shared Plexon hierarchy without copying one palette everywhere. */
public record SkillPalette(String icon, String colorA, String colorB) {
    public String gradient(String text) {
        return "<gradient:" + colorA + ":" + colorB + "><bold>" + text + "</bold></gradient>";
    }

    public static SkillPalette forSkill(SkillType skill) {
        return switch (skill) {
            case MINING -> new SkillPalette("⛏", "#00BCD4", "#80DEEA");
            case WOODCUTTING -> new SkillPalette("♣", "#66BB6A", "#FFD54F");
            case EXCAVATION -> new SkillPalette("◆", "#D7A86E", "#FFCC80");
            case FARMING -> new SkillPalette("✿", "#8BC34A", "#FFD740");
            case FISHING -> new SkillPalette("≈", "#29B6F6", "#80DEEA");
            case SWORDS -> new SkillPalette("⚔", "#EF5350", "#ECEFF1");
            case AXES -> new SkillPalette("⚒", "#FF8A65", "#B0BEC5");
            case ARCHERY -> new SkillPalette("➹", "#43A047", "#26C6DA");
            case UNARMED -> new SkillPalette("✦", "#FFB300", "#EF5350");
            case TAMING -> new SkillPalette("♞", "#FFD54F", "#66BB6A");
            case ACROBATICS -> new SkillPalette("➤", "#81D4FA", "#ECEFF1");
            case REPAIR -> new SkillPalette("⚙", "#90A4AE", "#4DD0E1");
            case ALCHEMY -> new SkillPalette("✧", "#AB47BC", "#EC407A");
        };
    }
}
