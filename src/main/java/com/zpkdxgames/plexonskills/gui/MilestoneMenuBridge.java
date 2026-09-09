package com.zpkdxgames.plexonskills.gui;

import com.zpkdxgames.plexonskills.api.PlexonSkillsAPI;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.milestone.MilestoneDefinition;
import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Replaces generic chapter placeholders with configured milestone rewards on the progression screen. */
public final class MilestoneMenuBridge implements Listener {
    private static final int[] SLOTS = {10, 12, 14, 16, 29, 31};
    private final PlexonSkillsAPI api;
    private final Supplier<RuntimeSettings> settings;

    public MilestoneMenuBridge(PlexonSkillsAPI api, Supplier<RuntimeSettings> settings) {
        this.api = api;
        this.settings = settings;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkillsMenuHolder holder)) return;
        if (holder.screen() != SkillsMenuHolder.Screen.PROGRESSION || !(event.getPlayer() instanceof Player player)) return;
        SkillType skill = holder.skill();
        if (skill == null || !holder.playerId().equals(player.getUniqueId()) || !api.isProfileReady(player.getUniqueId())) return;
        render(player, skill, event.getInventory());
    }

    private void render(Player player, SkillType skill, Inventory inventory) {
        RuntimeSettings runtime = settings.get();
        int level = api.getLevel(player.getUniqueId(), skill);
        List<MilestoneDefinition> milestones = runtime.milestones().milestones(skill);
        for (int slot : SLOTS) inventory.setItem(slot, null);

        boolean nextMarked = false;
        int index = 0;
        for (MilestoneDefinition milestone : milestones) {
            if (index >= SLOTS.length - 1) break;
            boolean completed = level >= milestone.level();
            boolean next = !completed && !nextMarked;
            if (next) nextMarked = true;
            String state = completed ? "✔ COMPLETED" : next ? "◆ NEXT MILESTONE" : "✕ LOCKED";
            String color = completed ? "#66BB6A" : next ? "#FFD740" : "#EF5350";
            Material material = completed ? Material.LIME_DYE : next ? Material.COMPASS : Material.GRAY_DYE;
            inventory.setItem(SLOTS[index++], SkillsUi.item(material,
                "<!italic><gradient:#FFF176:#FF8F00><bold>" + milestone.displayName() + "</bold></gradient>",
                "<!italic><dark_gray>" + milestone.description() + "</dark_gray>", "",
                row("#FFD740", "✥", "Required Level", Integer.toString(milestone.level())),
                row("#66BB6A", "▰", "Permanent XP", String.format(Locale.ROOT, "+%.1f%%", milestone.passiveXpBonus() * 100.0)),
                "", "<!italic><gray>Status</gray> <" + color + "><bold>" + state + "</bold></" + color + ">"));
        }

        boolean mastered = level >= runtime.curve().maximumLevel();
        inventory.setItem(SLOTS[SLOTS.length - 1], SkillsUi.item(mastered ? Material.NETHER_STAR : Material.NETHERITE_INGOT,
            "<!italic><gradient:#8BC34A:#DCEDC8><bold>FULL MASTERY</bold></gradient>",
            "<!italic><dark_gray>Terminal configured progression state</dark_gray>", "",
            row("#FFD740", "✥", "Required Level", Integer.toString(runtime.curve().maximumLevel())),
            "<!italic><gray>Status</gray> " + (mastered ? "<#AEEA00><bold>✦ FULLY MASTERED</bold></#AEEA00>" : "<#EF5350><bold>✕ LOCKED</bold></#EF5350>")));
    }

    private static String row(String color, String icon, String label, String value) {
        return "<!italic><" + color + ">" + icon + "</" + color + "> <gray>" + label + "</gray> <white>" + value + "</white>";
    }
}
