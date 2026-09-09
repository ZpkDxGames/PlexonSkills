package com.zpkdxgames.plexonskills.gui;

import com.zpkdxgames.plexonskills.ability.AbilityDefinition;
import com.zpkdxgames.plexonskills.ability.AbilityRuntime;
import com.zpkdxgames.plexonskills.api.PlexonSkillsAPI;
import com.zpkdxgames.plexonskills.skill.SkillType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

import java.util.Locale;

/** Renders and operates the dedicated active-ability screen without coupling ability logic to menu navigation. */
public final class AbilityMenuBridge implements Listener {
    private static final int ABILITY_SLOT = 22;
    private final PlexonSkillsAPI api;
    private final AbilityRuntime abilities;

    public AbilityMenuBridge(PlexonSkillsAPI api, AbilityRuntime abilities) {
        this.api = api;
        this.abilities = abilities;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkillsMenuHolder holder)) return;
        if (holder.screen() != SkillsMenuHolder.Screen.ABILITIES || !(event.getPlayer() instanceof Player player)) return;
        if (!holder.playerId().equals(player.getUniqueId())) return;
        render(player, holder, event.getInventory());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SkillsMenuHolder holder)) return;
        if (holder.screen() != SkillsMenuHolder.Screen.ABILITIES || event.getRawSlot() != ABILITY_SLOT) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !holder.playerId().equals(player.getUniqueId())) return;
        SkillType skill = holder.skill();
        if (skill == null || !api.isProfileReady(player.getUniqueId())) return;
        AbilityRuntime.ActivationResult result = abilities.activate(player, skill, api.getLevel(player.getUniqueId(), skill));
        player.sendMessage(Component.text(result.message()));
        render(player, holder, event.getView().getTopInventory());
    }

    private void render(Player player, SkillsMenuHolder holder, Inventory inventory) {
        SkillType skill = holder.skill();
        if (skill == null) return;
        AbilityRuntime.View view = abilities.view(player.getUniqueId(), skill, api.getLevel(player.getUniqueId(), skill));
        AbilityDefinition definition = view.definition();
        if (definition == null) {
            inventory.setItem(ABILITY_SLOT, SkillsUi.item(Material.BARRIER,
                "<!italic><#EF5350><bold>Ability unavailable</bold></#EF5350>",
                "<!italic><gray>No active ability is configured for this skill.</gray>"));
            return;
        }

        String color = switch (view.state()) {
            case DISABLED -> "#EF5350";
            case LOCKED -> "#FF7043";
            case READY -> "#76FF03";
            case ACTIVE -> "#AEEA00";
            case COOLDOWN -> "#FFD740";
        };
        Material material = switch (view.state()) {
            case DISABLED -> Material.BARRIER;
            case LOCKED -> Material.IRON_BARS;
            case READY -> Material.LIME_DYE;
            case ACTIVE -> Material.BLAZE_POWDER;
            case COOLDOWN -> Material.CLOCK;
        };
        String status = switch (view.state()) {
            case DISABLED -> "✘ DISABLED BY ADMIN";
            case LOCKED -> "✘ LOCKED";
            case READY -> "✔ READY";
            case ACTIVE -> "✦ ACTIVE";
            case COOLDOWN -> "◆ COOLDOWN";
        };
        String timing = switch (view.state()) {
            case ACTIVE -> "<!italic><#AEEA00>⚡</#AEEA00> <gray>Active For</gray> <white>" + seconds(view.remainingMillis()) + "s</white>";
            case COOLDOWN -> "<!italic><#FFD740>⌛</#FFD740> <gray>Ready In</gray> <white>" + seconds(view.remainingMillis()) + "s</white>";
            default -> "<!italic><dark_gray>State updates are server-authoritative.</dark_gray>";
        };
        String hint = view.state() == AbilityRuntime.State.READY
            ? "<!italic><#76FF03>Left Click</#76FF03> <gray>to activate this ability.</gray>"
            : "<!italic><gray>Click to refresh or attempt activation.</gray>";

        inventory.setItem(ABILITY_SLOT, SkillsUi.item(material,
            "<!italic><gradient:#AB47BC:#EC407A><bold>" + definition.displayName() + "</bold></gradient>",
            "<!italic><dark_gray>" + definition.description() + "</dark_gray>", "",
            row("#FFD740", "✥", "Unlock Level", Integer.toString(definition.unlockLevel())),
            row("#90CAF9", "◆", "Duration", seconds(definition.durationMillis()) + "s"),
            row("#B39DDB", "⌛", "Cooldown", seconds(definition.cooldownMillis()) + "s"),
            row("#66BB6A", "▰", "XP Boost", String.format(Locale.ROOT, "+%.0f%%", (definition.xpMultiplier() - 1.0) * 100.0)),
            "", "<!italic><gray>Status</gray> <" + color + "><bold>" + status + "</bold></" + color + ">",
            timing, "", hint));
    }

    private static String row(String color, String icon, String label, String value) {
        return "<!italic><" + color + ">" + icon + "</" + color + "> <gray>" + label + "</gray> <white>" + value + "</white>";
    }

    private static long seconds(long millis) { return Math.max(1L, (millis + 999L) / 1_000L); }
}
