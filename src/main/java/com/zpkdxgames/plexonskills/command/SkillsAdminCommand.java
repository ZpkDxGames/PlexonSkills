package com.zpkdxgames.plexonskills.command;

import com.zpkdxgames.plexonskills.PlexonSkillsPlugin;
import com.zpkdxgames.plexonskills.migration.McMmoMigrationService;
import com.zpkdxgames.plexonskills.skill.SkillType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class SkillsAdminCommand implements CommandExecutor, TabCompleter {
    private final PlexonSkillsPlugin plugin; private final McMmoMigrationService migration;
    public SkillsAdminCommand(PlexonSkillsPlugin plugin,McMmoMigrationService migration){this.plugin=plugin;this.migration=migration;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(args.length==0){sender.sendMessage("/skillsadmin <reload|diagnostics|setxp|setlevel|addxp|reset|backup|migrate>");return true;}
        String sub=args[0].toLowerCase(Locale.ROOT);
        return switch(sub){
            case "reload"->{if(!sender.hasPermission("plexonskills.admin.reload")){deny(sender);yield true;}boolean ok=plugin.reloadRuntime();sender.sendMessage(Component.text(ok?"PlexonSkills runtime reloaded atomically.":"Reload rejected; previous runtime remains active."));yield true;}
            case "diagnostics"->{if(!sender.hasPermission("plexonskills.admin.diagnostics")){deny(sender);yield true;}plugin.sendDiagnostics(sender);yield true;}
            case "addxp","setxp","setlevel"->{if(!sender.hasPermission("plexonskills.admin.modify")){deny(sender);yield true;}yield modify(sender,sub,args);}
            case "reset"->{if(!sender.hasPermission("plexonskills.admin.modify")){deny(sender);yield true;}yield reset(sender,args);}
            case "backup"->{if(!sender.hasPermission("plexonskills.admin.modify")){deny(sender);yield true;}plugin.backup(sender);yield true;}
            case "migrate"->{if(!sender.hasPermission("plexonskills.admin.migrate")){deny(sender);yield true;}yield migrate(sender,args);}
            default->{sender.sendMessage("Unknown subcommand.");yield true;}
        };
    }
    private boolean modify(CommandSender sender,String op,String[] args){if(args.length<4){sender.sendMessage("Usage: /skillsadmin "+op+" <player> <skill> <value>");return true;}Player target=Bukkit.getPlayerExact(args[1]);if(target==null){sender.sendMessage("Player must be online.");return true;}SkillType type=SkillType.parse(args[2]).orElse(null);if(type==null){sender.sendMessage("Unknown skill.");return true;}long value;try{value=Long.parseLong(args[3]);}catch(NumberFormatException ex){sender.sendMessage("Value must be an integer.");return true;}boolean changed;if(op.equals("addxp"))changed=plugin.api().addXp(target.getUniqueId(),type,value,"ADMIN");else if(op.equals("setlevel")){int level=(int)Math.max(1,Math.min(plugin.runtime().curve().maximumLevel(),value));changed=plugin.api().setXp(target.getUniqueId(),type,plugin.runtime().curve().cumulativeForLevel(level),"ADMIN");}else changed=plugin.api().setXp(target.getUniqueId(),type,value,"ADMIN");sender.sendMessage(changed?"Updated.":"No change/profile unavailable.");return true;}
    private boolean reset(CommandSender sender,String[] args){if(args.length<2){sender.sendMessage("Usage: /skillsadmin reset <player> [skill]");return true;}Player target=Bukkit.getPlayerExact(args[1]);if(target==null){sender.sendMessage("Player must be online.");return true;}if(args.length>2){SkillType type=SkillType.parse(args[2]).orElse(null);if(type==null){sender.sendMessage("Unknown skill.");return true;}plugin.api().setXp(target.getUniqueId(),type,0,"ADMIN_RESET");}else for(SkillType type:SkillType.values())plugin.api().setXp(target.getUniqueId(),type,0,"ADMIN_RESET");sender.sendMessage("Reset complete.");return true;}
    private boolean migrate(CommandSender sender,String[] args){String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"status";switch(action){case "status"->{var s=migration.status();sender.sendMessage("mcMMO migration: "+s.state()+", source="+s.source()+", exists="+s.exists());}case "scan"->migration.scan().whenComplete((text,error)->Bukkit.getScheduler().runTask(plugin,()->sender.sendMessage(error==null?text:"Scan failed: "+error.getMessage())));case "plan"->sender.sendMessage(migration.plan());case "execute"->sender.sendMessage(migration.execute());default->sender.sendMessage("Usage: /skillsadmin migrate <status|scan|plan|execute>");}return true;}
    private static void deny(CommandSender sender){sender.sendMessage("No permission.");}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){if(args.length==1)return match(List.of("reload","diagnostics","addxp","setxp","setlevel","reset","backup","migrate"),args[0]);if(args.length==2&&args[0].equalsIgnoreCase("migrate"))return match(List.of("status","scan","plan","execute"),args[1]);if(args.length==3&&List.of("addxp","setxp","setlevel").contains(args[0].toLowerCase(Locale.ROOT)))return match(Arrays.stream(SkillType.values()).map(SkillType::id).toList(),args[2]);return List.of();}
    private static List<String> match(List<String> values,String prefix){String p=prefix.toLowerCase(Locale.ROOT);return values.stream().filter(v->v.startsWith(p)).toList();}
}
