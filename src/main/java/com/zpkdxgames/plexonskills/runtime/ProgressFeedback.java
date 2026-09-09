package com.zpkdxgames.plexonskills.runtime;

import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.player.PlayerSkillsProfile;
import com.zpkdxgames.plexonskills.skill.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class ProgressFeedback implements AutoCloseable {
    private final JavaPlugin plugin;
    private final Supplier<RuntimeSettings> settings;
    private final Map<Key, Long> pending = new HashMap<>();
    private BukkitTask task;

    public ProgressFeedback(JavaPlugin plugin, Supplier<RuntimeSettings> settings){this.plugin=plugin;this.settings=settings;}

    public void start(){
        stop();
        int ticks=settings.get().actionbarCoalesceTicks();
        task=Bukkit.getScheduler().runTaskTimer(plugin,this::flush,ticks,ticks);
    }
    public void add(Player player, SkillType skill, long amount){
        if(!settings.get().actionbar() || amount<=0)return;
        pending.merge(new Key(player.getUniqueId(),skill),amount,Long::sum);
    }
    public void levelUp(Player player, SkillType skill, PlayerSkillsProfile.Mutation mutation){
        if(!settings.get().levelUpTitle())return;
        Component title=Component.text(skill.displayName()+" Level Up");
        Component subtitle=Component.text(mutation.oldLevel()+" -> "+mutation.newLevel());
        player.showTitle(Title.title(title,subtitle,Title.Times.times(Duration.ofMillis(200),Duration.ofMillis(1400),Duration.ofMillis(300))));
    }
    public void flush(){
        if(pending.isEmpty())return;
        Map<Key,Long> batch=new HashMap<>(pending);pending.clear();
        for(var entry:batch.entrySet()){
            Player player=Bukkit.getPlayer(entry.getKey().playerId());
            if(player==null)continue;
            player.sendActionBar(Component.text(entry.getKey().skill().displayName()+" +"+entry.getValue()+" XP"));
        }
    }
    private void stop(){if(task!=null){task.cancel();task=null;}}
    @Override public void close(){stop();flush();pending.clear();}
    private record Key(UUID playerId,SkillType skill){}
}
