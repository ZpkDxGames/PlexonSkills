package com.zpkdxgames.plexonskills.gui;

import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;

/** Strong identity for administrator-bound PlexonSkills inventory sessions. */
final class AdminSkillsMenuHolder implements InventoryHolder {
    enum Screen {
        ROOT,
        PLAYER_LIST,
        PLAYER,
        SKILL,
        CONFIRM_SKILL_RESET,
        CONFIRM_ALL_RESET,
        SKILL_CONFIG,
        ABILITY_CONFIG,
        MILESTONE_CONFIG,
        MULTIPLIERS,
        DIAGNOSTICS
    }

    private final UUID adminId;
    private final long generation;
    private final Screen screen;
    private final UUID targetId;
    private final SkillType skill;
    private final Map<Integer, UUID> playerSlots;
    private Inventory inventory;

    AdminSkillsMenuHolder(UUID adminId, long generation, Screen screen, UUID targetId, SkillType skill, Map<Integer, UUID> playerSlots) {
        this.adminId = adminId;
        this.generation = generation;
        this.screen = screen;
        this.targetId = targetId;
        this.skill = skill;
        this.playerSlots = playerSlots == null ? Map.of() : Map.copyOf(playerSlots);
    }

    UUID adminId() { return adminId; }
    long generation() { return generation; }
    Screen screen() { return screen; }
    UUID targetId() { return targetId; }
    SkillType skill() { return skill; }
    UUID playerAt(int slot) { return playerSlots.get(slot); }

    void bind(Inventory inventory) {
        if (this.inventory != null) throw new IllegalStateException("Admin menu holder already bound");
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        if (inventory == null) throw new IllegalStateException("Admin menu holder is not bound yet");
        return inventory;
    }
}
