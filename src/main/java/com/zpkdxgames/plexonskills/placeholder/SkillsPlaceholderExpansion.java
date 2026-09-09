package com.zpkdxgames.plexonskills.placeholder;

import com.zpkdxgames.plexonskills.api.PlexonSkillsAPI;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.skill.SkillType;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Locale;
import java.util.function.Supplier;

public final class SkillsPlaceholderExpansion extends PlaceholderExpansion {
    private final PlexonSkillsAPI api; private final Supplier<RuntimeSettings> settings; private final String version;
    public SkillsPlaceholderExpansion(PlexonSkillsAPI api,Supplier<RuntimeSettings> settings,String version){this.api=api;this.settings=settings;this.version=version;}
    @Override public String getIdentifier(){return "plexonskills";} @Override public String getAuthor(){return "Tonim (ZpkDxGames)";} @Override public String getVersion(){return version;} @Override public boolean persist(){return true;}
    @Override public String onRequest(OfflinePlayer player,String params){
        if(player==null||params==null)return ""; var id=player.getUniqueId(); if(!api.isProfileReady(id))return "0";
        String key=params.toLowerCase(Locale.ROOT); if(key.equals("total_level"))return Integer.toString(api.getTotalLevel(id));
        for(SkillType type:SkillType.values()){String prefix=type.id()+"_";if(!key.startsWith(prefix))continue;String suffix=key.substring(prefix.length());
            return switch(suffix){
                case "level"->Integer.toString(api.getLevel(id,type));
                case "xp"->Long.toString(api.getTotalXp(id,type));
                case "xp_next"->{int level=api.getLevel(id,type);yield Long.toString(settings.get().curve().requiredForNext(level));}
                case "progress"->{long xp=api.getTotalXp(id,type);yield String.format(Locale.ROOT,"%.1f",settings.get().curve().progressWithinLevel(xp)*100.0);}
                case "rank"->"0";
                default->"";
            };
        }
        return "";
    }
}
