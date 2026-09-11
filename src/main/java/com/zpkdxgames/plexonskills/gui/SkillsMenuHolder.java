package com.zpkdxgames.plexonskills.gui;

import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** Strong inventory identity for player-bound PlexonSkills menu sessions. */
final class SkillsMenuHolder implements InventoryHolder {
    enum Screen {
        ROOT,
        PROFILE,
        SKILL_DETAIL,
        PROGRESSION,
        ABILITIES,
        LEADERBOARD,
        STATISTICS,
        HELP
    }

    private final UUID playerId;
    private final long generation;
    private final Screen screen;
    private final SkillType skill;
    private Inventory inventory;

    SkillsMenuHolder(UUID playerId, long generation, Screen screen, SkillType skill) {
        this.playerId = playerId;
        this.generation = generation;
        this.screen = screen;
        this.skill = skill;
    }

    UUID playerId() { return playerId; }
    long generation() { return generation; }
    Screen screen() { return screen; }
    SkillType skill() { return skill; }

    void bind(Inventory inventory) {
        if (this.inventory != null) throw new IllegalStateException("Menu holder is already bound");
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        if (inventory == null) throw new IllegalStateException("Menu holder is not bound yet");
        return inventory;
    }
}
