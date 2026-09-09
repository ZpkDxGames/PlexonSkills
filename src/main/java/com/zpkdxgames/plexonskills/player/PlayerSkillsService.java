package com.zpkdxgames.plexonskills.player;

import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.diagnostics.SkillsDiagnostics;
import com.zpkdxgames.plexonskills.persistence.SkillsRepository;
import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class PlayerSkillsService {
    private final JavaPlugin plugin;
    private final SkillsRepository repository;
    private final Supplier<RuntimeSettings> settings;
    private final SkillsDiagnostics diagnostics;
    private final Map<UUID, PlayerSkillsProfile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> generations = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private volatile CompletableFuture<Void> lastFlush = CompletableFuture.completedFuture(null);

    public PlayerSkillsService(JavaPlugin plugin, SkillsRepository repository, Supplier<RuntimeSettings> settings, SkillsDiagnostics diagnostics) {
        this.plugin=plugin; this.repository=repository; this.settings=settings; this.diagnostics=diagnostics;
    }

    public void load(Player player) {
        assertMainThread();
        UUID id = player.getUniqueId();
        int generation = generations.computeIfAbsent(id, ignored -> new AtomicInteger()).incrementAndGet();
        names.put(id, player.getName());
        profiles.put(id, new PlayerSkillsProfile(id, ProfileStatus.LOADING));
        RuntimeSettings snapshot = settings.get();
        repository.load(id, player.getName(), snapshot.curve()).whenComplete((loaded, error) ->
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (generations.getOrDefault(id, new AtomicInteger()).get() != generation) return;
                Player online = Bukkit.getPlayer(id);
                if (online == null || !online.isOnline()) return;
                if (error != null) {
                    PlayerSkillsProfile degraded = profiles.computeIfAbsent(id, key -> new PlayerSkillsProfile(key, ProfileStatus.DEGRADED));
                    degraded.status(ProfileStatus.DEGRADED);
                    plugin.getLogger().severe("Failed to load skill profile for " + id + ": " + rootMessage(error));
                    return;
                }
                PlayerSkillsProfile profile = new PlayerSkillsProfile(id, ProfileStatus.READY);
                loaded.totalXp().forEach((skill, xp) -> profile.load(skill, xp, snapshot.curve().levelForXp(xp), snapshot.curve()));
                profiles.put(id, profile);
            })
        );
    }

    public void unload(Player player) {
        assertMainThread();
        UUID id = player.getUniqueId();
        generations.computeIfAbsent(id, ignored -> new AtomicInteger()).incrementAndGet();
        PlayerSkillsProfile profile = profiles.remove(id);
        dirty.remove(id);
        names.remove(id);
        if (profile != null && profile.status() == ProfileStatus.READY) {
            repository.save(java.util.List.of(new SkillsRepository.NamedSnapshot(player.getName(), profile.snapshot())))
                .exceptionally(error -> { diagnostics.persistenceFailure(); plugin.getLogger().severe("Failed logout skill flush for " + id + ": " + rootMessage(error)); return null; });
        }
    }

    public PlayerSkillsProfile profile(UUID id) { return profiles.get(id); }
    public boolean ready(UUID id) { PlayerSkillsProfile p=profiles.get(id); return p!=null && p.status()==ProfileStatus.READY; }
    public int loadedCount() { return profiles.size(); }
    public long dirtyCount() { return dirty.size(); }

    public PlayerSkillsProfile.Mutation addXp(UUID id, SkillType skill, long amount) {
        assertMainThread();
        PlayerSkillsProfile profile=profiles.get(id);
        if (profile==null || profile.status()!=ProfileStatus.READY) return null;
        PlayerSkillsProfile.Mutation mutation=profile.addXp(skill,amount,settings.get().curve());
        if(mutation.changed()) dirty.add(id);
        return mutation;
    }

    public PlayerSkillsProfile.Mutation setXp(UUID id, SkillType skill, long amount) {
        assertMainThread();
        PlayerSkillsProfile profile=profiles.get(id);
        if(profile==null || profile.status()!=ProfileStatus.READY) return null;
        PlayerSkillsProfile.Mutation mutation=profile.setXp(skill,amount,settings.get().curve());
        if(mutation.changed()) dirty.add(id);
        return mutation;
    }

    public void markDirty(UUID id){ if(profiles.containsKey(id)) dirty.add(id); }

    public void flushDirty() {
        assertMainThread();
        if(dirty.isEmpty()) return;
        Collection<SkillsRepository.NamedSnapshot> batch = snapshotDirty(false);
        if(batch.isEmpty()) return;
        diagnostics.flushBatch();
        CompletableFuture<Void> future = repository.save(batch);
        lastFlush = future;
        future.whenComplete((ignored,error)->{
            if(error!=null){
                diagnostics.persistenceFailure();
                for(SkillsRepository.NamedSnapshot s:batch) dirty.add(s.snapshot().playerId());
                plugin.getLogger().severe("Skill flush failed: "+rootMessage(error));
            }
        });
    }

    public void shutdown(Duration timeout) {
        assertMainThread();
        Collection<SkillsRepository.NamedSnapshot> all = snapshotDirty(true);
        CompletableFuture<Void> flush = all.isEmpty() ? lastFlush : repository.save(all);
        try { flush.get(Math.max(1,timeout.toSeconds()), TimeUnit.SECONDS); }
        catch (Exception ex) { diagnostics.persistenceFailure(); plugin.getLogger().severe("Timed out/failure flushing skill profiles on shutdown: "+rootMessage(ex)); }
        profiles.clear(); dirty.clear(); names.clear();
    }

    private Collection<SkillsRepository.NamedSnapshot> snapshotDirty(boolean allReady) {
        Collection<UUID> ids = allReady ? new ArrayList<>(profiles.keySet()) : new ArrayList<>(dirty);
        ArrayList<SkillsRepository.NamedSnapshot> batch=new ArrayList<>();
        for(UUID id:ids){
            PlayerSkillsProfile p=profiles.get(id);
            if(p==null || p.status()!=ProfileStatus.READY) continue;
            batch.add(new SkillsRepository.NamedSnapshot(names.getOrDefault(id,"unknown"),p.snapshot()));
            dirty.remove(id);
        }
        return batch;
    }

    private static String rootMessage(Throwable error){Throwable root=error;while(root.getCause()!=null)root=root.getCause();return String.valueOf(root.getMessage());}
    private static void assertMainThread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("PlexonSkills profile mutation requires the server primary thread");}
}
