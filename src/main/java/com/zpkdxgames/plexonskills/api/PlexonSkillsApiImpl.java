package com.zpkdxgames.plexonskills.api;

import com.zpkdxgames.plexonskills.ability.AbilityRuntime;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.player.PlayerSkillsProfile;
import com.zpkdxgames.plexonskills.player.PlayerSkillsService;
import com.zpkdxgames.plexonskills.player.ProfileStatus;
import com.zpkdxgames.plexonskills.skill.SkillProgressionService;
import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class PlexonSkillsApiImpl implements PlexonSkillsAPI {
    private final PlayerSkillsService players;
    private final SkillProgressionService progression;
    private final Supplier<RuntimeSettings> settings;
    private final AbilityRuntime abilities;

    public PlexonSkillsApiImpl(PlayerSkillsService players, SkillProgressionService progression, Supplier<RuntimeSettings> settings, AbilityRuntime abilities) {
        this.players=players; this.progression=progression; this.settings=settings; this.abilities=abilities;
    }

    @Override public int getLevel(UUID id,SkillType skill){PlayerSkillsProfile p=players.profile(id);return p==null?0:p.level(skill);}
    @Override public long getTotalXp(UUID id,SkillType skill){PlayerSkillsProfile p=players.profile(id);return p==null?0L:p.totalXp(skill);}
    @Override public int getTotalLevel(UUID id){PlayerSkillsProfile p=players.profile(id);if(p==null)return 0;int total=0;for(SkillType type:settings.get().skills().enabledSkills())total+=p.level(type);return total;}

    @Override public Optional<PlayerSkillView> profile(UUID id){
        PlayerSkillsProfile p=players.profile(id);if(p==null)return Optional.empty();
        RuntimeSettings runtime=settings.get();var curve=runtime.curve();var list=new ArrayList<SkillProgressView>();int total=0;
        for(SkillType type:runtime.skills().enabledSkills()){int level=p.level(type);long xp=p.totalXp(type);total+=level;list.add(new SkillProgressView(type,xp,level,curve.requiredForNext(level),curve.progressWithinLevel(xp)));}
        return Optional.of(new PlayerSkillView(id,list,total,p.revision()));
    }

    @Override public boolean addXp(UUID id,SkillType skill,long amount,String source){assertMain();Player player=Bukkit.getPlayer(id);return player!=null&&progression.grant(player,skill,amount,source);}
    @Override public boolean setXp(UUID id,SkillType skill,long amount,String source){assertMain();Player player=Bukkit.getPlayer(id);return player!=null&&progression.setXp(player,skill,amount,source);}
    @Override public boolean isProfileReady(UUID id){return players.ready(id);}

    @Override public Optional<AbilityView> ability(UUID playerId, SkillType skill) {
        PlayerSkillsProfile profile=players.profile(playerId);
        if(profile==null || profile.status()!=ProfileStatus.READY) return Optional.empty();
        AbilityRuntime.View view=abilities.view(playerId,skill,profile.level(skill));
        if(view.definition()==null) return Optional.empty();
        var definition=view.definition();
        return Optional.of(new AbilityView(
            definition.id(), definition.displayName(), definition.description(), definition.skill(),
            AbilityState.valueOf(view.state().name()), definition.unlockLevel(), definition.durationMillis(),
            definition.cooldownMillis(), view.remainingMillis(), view.canActivate()));
    }

    @Override public List<MilestoneView> milestones(SkillType skill) {
        return settings.get().milestones().milestones(skill).stream()
            .map(m -> new MilestoneView(m.id(),m.displayName(),m.description(),m.skill(),m.level(),m.passiveXpBonus()))
            .toList();
    }

    @Override public boolean isMastered(UUID playerId, SkillType skill) {
        PlayerSkillsProfile profile=players.profile(playerId);
        return profile!=null && profile.level(skill)>=settings.get().curve().maximumLevel();
    }

    @Override public Optional<SkillStatisticsView> statistics(UUID playerId) {
        PlayerSkillsProfile profile=players.profile(playerId);
        if(profile==null) return Optional.empty();
        RuntimeSettings runtime=settings.get();
        List<SkillType> enabled=runtime.skills().enabledSkills();
        long totalXp=0L; int totalLevel=0; int mastered=0;
        SkillType highest=null; int highestLevel=0; long highestXp=0L;
        for(SkillType type:enabled){
            int level=profile.level(type); long xp=profile.totalXp(type);
            totalLevel+=level; totalXp=saturatedAdd(totalXp,xp);
            if(level>=runtime.curve().maximumLevel()) mastered++;
            if(highest==null || level>highestLevel || (level==highestLevel && xp>highestXp) || (level==highestLevel && xp==highestXp && type.name().compareTo(highest.name())<0)) {
                highest=type; highestLevel=level; highestXp=xp;
            }
        }
        return Optional.of(new SkillStatisticsView(playerId,totalLevel,totalXp,highest,highestLevel,mastered,enabled.size(),profile.revision()));
    }

    private static long saturatedAdd(long a,long b){return b>0L&&a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b;}
    private static void assertMain(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("PlexonSkills API mutation requires the server primary thread");}
}
