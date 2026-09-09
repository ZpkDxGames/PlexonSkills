package com.zpkdxgames.plexonskills.api;

import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.player.PlayerSkillsProfile;
import com.zpkdxgames.plexonskills.player.PlayerSkillsService;
import com.zpkdxgames.plexonskills.skill.SkillProgressionService;
import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class PlexonSkillsApiImpl implements PlexonSkillsAPI {
    private final PlayerSkillsService players; private final SkillProgressionService progression; private final Supplier<RuntimeSettings> settings;
    public PlexonSkillsApiImpl(PlayerSkillsService players,SkillProgressionService progression,Supplier<RuntimeSettings> settings){this.players=players;this.progression=progression;this.settings=settings;}
    @Override public int getLevel(UUID id,SkillType skill){PlayerSkillsProfile p=players.profile(id);return p==null?0:p.level(skill);}
    @Override public long getTotalXp(UUID id,SkillType skill){PlayerSkillsProfile p=players.profile(id);return p==null?0L:p.totalXp(skill);}
    @Override public int getTotalLevel(UUID id){PlayerSkillsProfile p=players.profile(id);if(p==null)return 0;int total=0;for(SkillType type:settings.get().skills().enabledSkills())total+=p.level(type);return total;}
    @Override public Optional<PlayerSkillView> profile(UUID id){
        PlayerSkillsProfile p=players.profile(id);if(p==null)return Optional.empty();
        var curve=settings.get().curve();var list=new ArrayList<SkillProgressView>();int total=0;
        for(SkillType type:settings.get().skills().enabledSkills()){int level=p.level(type);long xp=p.totalXp(type);total+=level;list.add(new SkillProgressView(type,xp,level,curve.requiredForNext(level),curve.progressWithinLevel(xp)));}
        return Optional.of(new PlayerSkillView(id,list,total,p.revision()));
    }
    @Override public boolean addXp(UUID id,SkillType skill,long amount,String source){assertMain();Player player=Bukkit.getPlayer(id);return player!=null&&progression.grant(player,skill,amount,source);}
    @Override public boolean setXp(UUID id,SkillType skill,long amount,String source){assertMain();Player player=Bukkit.getPlayer(id);return player!=null&&progression.setXp(player,skill,amount,source);}
    @Override public boolean isProfileReady(UUID id){return players.ready(id);}
    private static void assertMain(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("PlexonSkills API read/mutation requires primary thread; use a scheduled task for Bukkit-facing state");}
}
