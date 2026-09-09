package com.zpkdxgames.plexonskills.api.events;

import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

/** Synchronous post-state event fired when an active ability ends or is ended administratively. */
public final class PlexonSkillAbilityEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player; private final SkillType skill; private final String abilityId;
    public PlexonSkillAbilityEndEvent(Player player, SkillType skill, String abilityId){this.player=Objects.requireNonNull(player);this.skill=Objects.requireNonNull(skill);this.abilityId=Objects.requireNonNull(abilityId);}
    public Player player(){return player;} public SkillType skill(){return skill;} public String abilityId(){return abilityId;}
    public Player getPlayer(){return player;} public SkillType getSkill(){return skill;} public String getAbilityId(){return abilityId;}
    @Override public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
