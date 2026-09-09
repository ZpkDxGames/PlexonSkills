package com.zpkdxgames.plexonskills.skill;

import com.zpkdxgames.plexonskills.ability.AbilityRuntime;
import com.zpkdxgames.plexonskills.api.events.PlexonSkillLevelUpEvent;
import com.zpkdxgames.plexonskills.api.events.PlexonSkillXpGainEvent;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.diagnostics.SkillsDiagnostics;
import com.zpkdxgames.plexonskills.migration.MigrationMode;
import com.zpkdxgames.plexonskills.player.PlayerSkillsProfile;
import com.zpkdxgames.plexonskills.player.PlayerSkillsService;
import com.zpkdxgames.plexonskills.runtime.ProgressFeedback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class SkillProgressionService {
    private final PlayerSkillsService players;
    private final Supplier<RuntimeSettings> settings;
    private final SkillsDiagnostics diagnostics;
    private final ProgressFeedback feedback;
    private final AbilityRuntime abilities;
    private final AtomicLong transactions=new AtomicLong();
    private final Map<UUID,long[]> shadowXp=new HashMap<>();

    public SkillProgressionService(PlayerSkillsService players,Supplier<RuntimeSettings> settings,SkillsDiagnostics diagnostics,ProgressFeedback feedback,AbilityRuntime abilities){
        this.players=players;this.settings=settings;this.diagnostics=diagnostics;this.feedback=feedback;this.abilities=abilities;
    }

    public boolean grant(Player player,SkillType skill,long amount,String source){
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("XP grants require primary thread");
        RuntimeSettings runtime=settings.get();
        if(amount<=0 || !runtime.skills().enabled(skill)){diagnostics.xpRejected();return false;}
        if(!runtime.allows(player.getWorld().getName(),player.getGameMode())){diagnostics.xpRejected();return false;}
        MigrationMode mode=runtime.migrationMode();
        if(mode==MigrationMode.DISABLED){diagnostics.xpRejected();return false;}
        PlayerSkillsProfile profile=players.profile(player.getUniqueId());
        if(profile==null || !players.ready(player.getUniqueId())){diagnostics.profileNotReady();return false;}

        long effectiveAmount=scale(amount,abilities.xpMultiplier(player.getUniqueId(),skill));
        if(mode==MigrationMode.SHADOW){
            long[] ledger=shadowXp.computeIfAbsent(player.getUniqueId(),ignored->new long[SkillType.values().length]);
            int idx=skill.ordinal(); ledger[idx]=saturatedAdd(ledger[idx],effectiveAmount); diagnostics.shadowContribution(); return true;
        }
        long current=profile.totalXp(skill);
        long projected=runtime.curve().clampXp(saturatedAdd(current,effectiveAmount));
        long tx=transactions.incrementAndGet();
        PlexonSkillXpGainEvent event=new PlexonSkillXpGainEvent(player,skill,projected-current,source,projected,profile.level(skill),tx);
        Bukkit.getPluginManager().callEvent(event);
        if(event.isCancelled()){diagnostics.xpRejected();return false;}
        PlayerSkillsProfile.Mutation mutation=players.addXp(player.getUniqueId(),skill,event.amount());
        if(mutation==null || !mutation.changed()){diagnostics.xpRejected();return false;}
        diagnostics.xpGrant();feedback.add(player,skill,mutation.newXp()-mutation.oldXp());
        if(mutation.leveledUp()){
            diagnostics.levelUp();
            Bukkit.getPluginManager().callEvent(new PlexonSkillLevelUpEvent(player,skill,mutation.oldLevel(),mutation.newLevel(),mutation.newXp(),source));
            feedback.levelUp(player,skill,mutation);
        }
        return true;
    }

    public boolean setXp(Player player,SkillType skill,long amount,String source){
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("XP mutation requires primary thread");
        RuntimeSettings runtime=settings.get();
        PlayerSkillsProfile profile=players.profile(player.getUniqueId()); if(profile==null||!players.ready(player.getUniqueId()))return false;
        PlayerSkillsProfile.Mutation mutation=players.setXp(player.getUniqueId(),skill,runtime.curve().clampXp(amount));
        if(mutation==null||!mutation.changed())return false;
        if(mutation.leveledUp())Bukkit.getPluginManager().callEvent(new PlexonSkillLevelUpEvent(player,skill,mutation.oldLevel(),mutation.newLevel(),mutation.newXp(),source));
        return true;
    }

    public long shadowXp(UUID playerId,SkillType skill){long[] values=shadowXp.get(playerId);return values==null?0L:values[skill.ordinal()];}
    public void clearShadow(){shadowXp.clear();}

    static long scale(long amount,double multiplier){
        if(amount<=0)return 0L;
        if(multiplier<=1.0)return amount;
        double scaled=amount*multiplier;
        if(!Double.isFinite(scaled)||scaled>=Long.MAX_VALUE)return Long.MAX_VALUE;
        return Math.max(amount,Math.round(scaled));
    }

    private static long saturatedAdd(long a,long b){if(b>0&&a>Long.MAX_VALUE-b)return Long.MAX_VALUE;return a+b;}
}
