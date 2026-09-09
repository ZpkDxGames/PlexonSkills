package com.zpkdxgames.plexonskills.gui;

import com.zpkdxgames.plexonskills.ability.AbilityRuntime;
import com.zpkdxgames.plexonskills.api.PlexonSkillsAPI;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.milestone.MilestoneDefinition;
import com.zpkdxgames.plexonskills.skill.SkillPassive;
import com.zpkdxgames.plexonskills.skill.SkillType;
import com.zpkdxgames.plexonskills.skill.XpCurve;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Adds skill-specific passive identity and richer derived statistics to existing premium screens. */
public final class PassiveMenuBridge implements Listener {
    private static final int[] SKILL_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24};

    private final PlexonSkillsAPI api;
    private final Supplier<RuntimeSettings> settings;
    private final AbilityRuntime abilities;

    public PassiveMenuBridge(PlexonSkillsAPI api, Supplier<RuntimeSettings> settings, AbilityRuntime abilities) {
        this.api = api;
        this.settings = settings;
        this.abilities = abilities;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkillsMenuHolder holder)) return;
        if (!(event.getPlayer() instanceof Player player) || !holder.playerId().equals(player.getUniqueId())) return;
        if (!api.isProfileReady(player.getUniqueId())) return;
        switch (holder.screen()) {
            case ROOT -> renderRootCards(player, event.getInventory());
            case SKILL_DETAIL -> renderDetail(player, holder.skill(), event.getInventory());
            case STATISTICS -> renderStatistics(player, event.getInventory());
            default -> { }
        }
    }

    private void renderRootCards(Player player, Inventory inventory) {
        List<SkillType> skills = settings.get().skills().enabledSkills();
        for (int i = 0; i < skills.size() && i < SKILL_SLOTS.length; i++) inventory.setItem(SKILL_SLOTS[i], card(player, skills.get(i), true));
    }

    private void renderDetail(Player player, SkillType skill, Inventory inventory) {
        if (skill == null) return;
        RuntimeSettings runtime = settings.get();
        int level = api.getLevel(player.getUniqueId(), skill);
        SkillPassive passive = SkillPassive.forSkill(skill);
        double passiveBonus = passivePercent(runtime, skill, level);
        String nextMilestone = runtime.milestones().next(skill, level)
            .map(m -> m.displayName() + " • Lv. " + m.level())
            .orElse(level >= runtime.curve().maximumLevel() ? "FULLY MASTERED" : "NONE");
        String highest = runtime.milestones().highestReached(skill, level).map(MilestoneDefinition::displayName).orElse("None yet");
        double mastery = mastery(level, runtime.curve().maximumLevel());

        inventory.setItem(13, card(player, skill, false));
        inventory.setItem(25, SkillsUi.item(Material.ENCHANTED_BOOK,
            "<!italic><gradient:#80CBC4:#66BB6A><bold>" + passive.displayName() + "</bold></gradient>",
            "<!italic><dark_gray>" + passive.description() + "</dark_gray>", "",
            row("#66BB6A", "▰", "Current Passive", String.format(Locale.ROOT, "+%.1f%% XP", passiveBonus)),
            row("#90CAF9", "◆", "Source", "Derived milestones")));
        inventory.setItem(26, SkillsUi.item(Material.COMPASS,
            "<!italic><#FFD740><bold>Next Milestone</bold></#FFD740>",
            "<!italic><gray>" + nextMilestone + "</gray>"));
        inventory.setItem(35, SkillsUi.item(Material.WRITABLE_BOOK,
            "<!italic><#90CAF9><bold>Key Statistics</bold></#90CAF9>",
            row("#FFD740", "✥", "Level", Integer.toString(level)),
            row("#66BB6A", "▰", "Lifetime XP", SkillsUi.exact(api.getTotalXp(player.getUniqueId(), skill))),
            row("#B39DDB", "◆", "Highest Milestone", highest),
            row("#AEEA00", "✦", "Mastery", String.format(Locale.ROOT, "%.1f%%", mastery))));
    }

    private void renderStatistics(Player player, Inventory inventory) {
        List<SkillType> skills = settings.get().skills().enabledSkills();
        for (int i = 0; i < skills.size() && i < SKILL_SLOTS.length; i++) inventory.setItem(SKILL_SLOTS[i], card(player, skills.get(i), false));
    }

    private org.bukkit.inventory.ItemStack card(Player player, SkillType skill, boolean clickable) {
        RuntimeSettings runtime = settings.get();
        XpCurve curve = runtime.curve();
        int level = api.getLevel(player.getUniqueId(), skill);
        long xp = api.getTotalXp(player.getUniqueId(), skill);
        double within = curve.progressWithinLevel(xp);
        double mastery = mastery(level, curve.maximumLevel());
        SkillStage stage = SkillStage.forLevel(level, curve.maximumLevel());
        SkillPalette palette = SkillPalette.forSkill(skill);
        SkillPassive passive = SkillPassive.forSkill(skill);
        AbilityRuntime.View ability = abilities.view(player.getUniqueId(), skill, level);
        String next = runtime.milestones().next(skill, level).map(m -> m.displayName() + " @ " + m.level()).orElse("FULLY MASTERED");
        double passiveBonus = passivePercent(runtime, skill, level);
        String finalLine = clickable ? "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to inspect this skill.</gray>" : "<!italic><dark_gray>Loaded-profile statistics</dark_gray>";
        return SkillsUi.item(skill.icon(),
            SkillsUi.baseName(palette, skill.displayName(), "Lv. " + level),
            "<!italic><dark_gray>" + stage.displayName() + " progression</dark_gray>", "",
            row("#FFD740", "✥", "Level", Integer.toString(level)),
            row("#90CAF9", "◆", "Stage", stage.displayName()),
            row("#66BB6A", "▰", "Lifetime XP", SkillsUi.exact(xp)),
            "<!italic><#FFD54F>⚡</#FFD54F> <gray>Level Progress</gray> <white>" + SkillsUi.percent(within) + "</white>",
            SkillsUi.progressBar(within, 10),
            row("#AEEA00", "✦", "Mastery", String.format(Locale.ROOT, "%.1f%%", mastery)),
            row("#B39DDB", "◇", "Passive", passive.displayName() + " +" + String.format(Locale.ROOT, "%.1f%%", passiveBonus)),
            row("#CE93D8", "◈", "Ability", ability.state().name()),
            row("#FFD740", "→", "Next Milestone", next),
            "", finalLine);
    }

    private static double passivePercent(RuntimeSettings runtime, SkillType skill, int level) {
        return Math.max(0.0, (runtime.milestones().passiveMultiplier(skill, level) - 1.0) * 100.0);
    }

    private static double mastery(int level, int maximum) {
        return Math.max(0.0, Math.min(100.0, (double) level * 100.0 / Math.max(1, maximum)));
    }

    private static String row(String color, String icon, String label, String value) {
        return "<!italic><" + color + ">" + icon + "</" + color + "> <gray>" + label + "</gray> <white>" + value + "</white>";
    }
}
