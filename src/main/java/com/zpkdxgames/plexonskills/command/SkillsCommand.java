package com.zpkdxgames.plexonskills.command;

import com.zpkdxgames.plexonskills.api.PlexonSkillsAPI;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.gui.SkillsMenu;
import com.zpkdxgames.plexonskills.persistence.SkillsRepository;
import com.zpkdxgames.plexonskills.skill.SkillType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class SkillsCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin; private final PlexonSkillsAPI api; private final Supplier<RuntimeSettings> settings; private final SkillsRepository repository; private final SkillsMenu menu;
    public SkillsCommand(JavaPlugin plugin,PlexonSkillsAPI api,Supplier<RuntimeSettings> settings,SkillsRepository repository,SkillsMenu menu){this.plugin=plugin;this.api=api;this.settings=settings;this.repository=repository;this.menu=menu;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player player)){sender.sendMessage("PlexonSkills player command only; use /skillsadmin diagnostics from console.");return true;}
        if(args.length==0){menu.open(player);return true;}
        String sub=args[0].toLowerCase(Locale.ROOT);
        if(sub.equals("stats")){Player target=args.length>1?Bukkit.getPlayerExact(args[1]):player;if(target==null){player.sendMessage(Component.text("Player must be online."));return true;}sendStats(player,target);return true;}
        if(sub.equals("top")){SkillType type=args.length>1?SkillType.parse(args[1]).orElse(SkillType.MINING):SkillType.MINING;repository.top(type,10).whenComplete((rows,error)->Bukkit.getScheduler().runTask(plugin,()->{if(error!=null){player.sendMessage(Component.text("Leaderboard unavailable: "+error.getMessage()));return;}player.sendMessage(Component.text(type.displayName()+" Top 10"));int i=1;for(var row:rows)player.sendMessage(Component.text((i++)+". "+row.playerName()+" — L"+row.level()+" / "+row.totalXp()+" XP"));}));return true;}
        if(sub.equals("rank")){SkillType type=args.length>1?SkillType.parse(args[1]).orElse(SkillType.MINING):SkillType.MINING;repository.rank(type,player.getUniqueId()).whenComplete((rank,error)->Bukkit.getScheduler().runTask(plugin,()->player.sendMessage(Component.text(error==null?type.displayName()+" rank: #"+rank:"Rank unavailable."))));return true;}
        if(sub.equals("abilities")){player.sendMessage(Component.text("Active abilities are framework-ready but disabled by default in the 1.0.0 candidate until production balancing is accepted."));return true;}
        var parsed=SkillType.parse(args[0]);if(parsed.isPresent()){sendSkill(player,player,parsed.get());return true;}
        player.sendMessage(Component.text("Usage: /skills [skill|stats|top|rank|abilities]"));return true;
    }
    private void sendStats(Player viewer,Player target){if(!api.isProfileReady(target.getUniqueId())){viewer.sendMessage(Component.text("Profile is not ready."));return;}viewer.sendMessage(Component.text(target.getName()+" — total level "+api.getTotalLevel(target.getUniqueId())));for(SkillType type:settings.get().skills().enabledSkills())viewer.sendMessage(Component.text(type.displayName()+": L"+api.getLevel(target.getUniqueId(),type)+" ("+api.getTotalXp(target.getUniqueId(),type)+" XP)"));}
    private void sendSkill(Player viewer,Player target,SkillType type){if(!api.isProfileReady(target.getUniqueId())){viewer.sendMessage(Component.text("Profile is not ready."));return;}long xp=api.getTotalXp(target.getUniqueId(),type);int level=api.getLevel(target.getUniqueId(),type);viewer.sendMessage(Component.text(type.displayName()+" — Level "+level+", "+xp+" XP, "+String.format(Locale.ROOT,"%.1f%%",settings.get().curve().progressWithinLevel(xp)*100.0)));}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){if(args.length==1){List<String> values=new ArrayList<>(List.of("stats","top","rank","abilities"));for(SkillType type:SkillType.values())values.add(type.id());return match(values,args[0]);}if(args.length==2&&(args[0].equalsIgnoreCase("top")||args[0].equalsIgnoreCase("rank")))return match(Arrays.stream(SkillType.values()).map(SkillType::id).toList(),args[1]);return List.of();}
    private static List<String> match(List<String> values,String prefix){String p=prefix.toLowerCase(Locale.ROOT);return values.stream().filter(v->v.startsWith(p)).sorted().toList();}
}
