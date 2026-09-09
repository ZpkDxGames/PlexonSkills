package com.zpkdxgames.plexonskills.migration;

import com.zpkdxgames.plexoncore.api.PlexonCoreAPI;
import com.zpkdxgames.plexonskills.persistence.SkillsRepository;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Safe migration coordinator. It deliberately does not guess mcMMO's database schema.
 * Source discovery can be performed without mutation; execution remains blocked until an
 * administrator-provided/user-owned schema mapping is implemented and validated on staging data.
 */
public final class McMmoMigrationService {
    private final JavaPlugin plugin; private final PlexonCoreAPI core; private final SkillsRepository repository;
    public McMmoMigrationService(JavaPlugin plugin,PlexonCoreAPI core,SkillsRepository repository){this.plugin=plugin;this.core=core;this.repository=repository;}
    public MigrationStatus status(){Path source=sourcePath();return new MigrationStatus(source,source!=null&&Files.exists(source),"SCHEMA_MAPPING_REQUIRED");}
    public CompletableFuture<String> scan(){
        Path source=sourcePath(); if(source==null)return CompletableFuture.completedFuture("No migration.mcmmo.source configured.");
        return core.scheduler().supplyIo(()->{if(!Files.exists(source))return "Configured mcMMO source does not exist: "+source;try{return "Source detected: "+source+" ("+Files.size(source)+" bytes). Read-only schema mapping is still required before plan/execute.";}catch(Exception e){return "Unable to inspect source: "+e.getMessage();}});
    }
    public String plan(){return "Migration plan is blocked until the administrator's actual mcMMO source schema is mapped from user-owned staging data. No schema is guessed.";}
    public String execute(){return "Migration execute is blocked: no validated mcMMO schema/source evidence was supplied to this build. Source data is never modified.";}
    private Path sourcePath(){YamlConfiguration yaml=YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(),"config.yml"));String raw=yaml.getString("migration.mcmmo.source","").trim();if(raw.isEmpty())return null;Path path=Path.of(raw);return path.isAbsolute()?path:plugin.getDataFolder().toPath().resolve(path).normalize();}
    public record MigrationStatus(Path source,boolean exists,String state){}
}
