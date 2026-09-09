package com.zpkdxgames.plexonskills.skill;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkillRegistry {
    private final Map<SkillType, SkillDefinition> definitions;
    private final BlockRoute[] blockRoutes;
    private final Set<Material> subscribedMaterials;

    private SkillRegistry(Map<SkillType, SkillDefinition> definitions, BlockRoute[] blockRoutes, Set<Material> subscribedMaterials) {
        this.definitions = Collections.unmodifiableMap(new EnumMap<>(definitions));
        this.blockRoutes = blockRoutes;
        this.subscribedMaterials = Collections.unmodifiableSet(EnumSet.copyOf(subscribedMaterials));
    }

    public static SkillRegistry load(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("skills");
        if (root == null) throw new IllegalArgumentException("skills.yml is missing 'skills'");
        EnumMap<SkillType, SkillDefinition> definitions = new EnumMap<>(SkillType.class);
        BlockRoute[] routes = new BlockRoute[Material.values().length];
        EnumSet<Material> subscribed = EnumSet.noneOf(Material.class);

        for (SkillType type : SkillType.values()) {
            ConfigurationSection section = root.getConfigurationSection(type.name());
            boolean enabled = section != null && section.getBoolean("enabled", true);
            boolean natural = section != null && section.getBoolean("natural-origin", type != SkillType.FARMING);
            boolean mature = section != null && section.getBoolean("require-mature", type == SkillType.FARMING);
            long defaultXp = section == null ? 0L : Math.max(0L, section.getLong("default-xp", 0L));
            double damageXp = section == null ? 0.0 : Math.max(0.0, section.getDouble("damage-xp", 0.0));
            EnumMap<Material, Long> materialXp = new EnumMap<>(Material.class);
            if (section != null) {
                ConfigurationSection mats = section.getConfigurationSection("materials");
                if (mats != null) {
                    for (String key : mats.getKeys(false)) {
                        Material material = Material.matchMaterial(key);
                        if (material == null) throw new IllegalArgumentException("Unknown material in " + type + ": " + key);
                        materialXp.put(material, Math.max(0L, mats.getLong(key)));
                    }
                }
                for (String tagName : section.getStringList("material-tags")) {
                    if (tagName.equalsIgnoreCase("LOGS")) {
                        for (Material material : Material.values()) if (Tag.LOGS.isTagged(material)) materialXp.putIfAbsent(material, defaultXp);
                    } else {
                        throw new IllegalArgumentException("Unsupported material tag in 1.0.0: " + tagName);
                    }
                }
            }
            SkillDefinition definition = new SkillDefinition(type, enabled, natural, mature, defaultXp, damageXp, materialXp);
            definitions.put(type, definition);
            if (enabled && SkillType.blockSkills().contains(type)) {
                for (Map.Entry<Material, Long> entry : materialXp.entrySet()) {
                    Material material = entry.getKey();
                    if (routes[material.ordinal()] != null) throw new IllegalArgumentException("Block material routed to multiple skills: " + material);
                    routes[material.ordinal()] = new BlockRoute(type, entry.getValue());
                    subscribed.add(material);
                }
            }
        }
        if (subscribed.isEmpty()) {
            // EnumSet.copyOf(empty) throws; constructor expects an enum set.
            return new SkillRegistry(definitions, routes, EnumSet.noneOf(Material.class));
        }
        return new SkillRegistry(definitions, routes, subscribed);
    }

    public SkillDefinition definition(SkillType type) { return definitions.get(type); }
    public Map<SkillType, SkillDefinition> definitions() { return definitions; }
    public boolean enabled(SkillType type) { SkillDefinition d = definitions.get(type); return d != null && d.enabled(); }
    public BlockRoute blockRoute(Material material) { return material == null ? null : blockRoutes[material.ordinal()]; }
    public Set<Material> subscribedMaterials() { return subscribedMaterials; }
    public List<SkillType> enabledSkills() {
        List<SkillType> result = new ArrayList<>();
        for (SkillType type : SkillType.values()) if (enabled(type)) result.add(type);
        return List.copyOf(result);
    }

    public record BlockRoute(SkillType skill, long xp) {}
}
