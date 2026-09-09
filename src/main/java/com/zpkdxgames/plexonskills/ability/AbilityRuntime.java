package com.zpkdxgames.plexonskills.ability;

import com.zpkdxgames.plexonskills.api.events.PlexonSkillAbilityActivateEvent;
import com.zpkdxgames.plexonskills.api.events.PlexonSkillAbilityEndEvent;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.diagnostics.SkillsDiagnostics;
import com.zpkdxgames.plexonskills.skill.SkillType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Main-thread active-ability coordinator. One shared sweep task handles expiration for all players;
 * no repeating task is created per player or per ability.
 */
public final class AbilityRuntime implements Listener, AutoCloseable {
    public enum State { DISABLED, LOCKED, READY, ACTIVE, COOLDOWN }

    public record View(AbilityDefinition definition, State state, long remainingMillis) {
        public boolean canActivate() { return state == State.READY; }
    }

    public record ActivationResult(boolean activated, View view, String message) { }

    private final JavaPlugin plugin;
    private final Supplier<RuntimeSettings> settings;
    private final SkillsDiagnostics diagnostics;
    private final Map<UUID, long[]> activeUntil = new HashMap<>();
    private final Map<UUID, long[]> cooldownUntil = new HashMap<>();
    private BukkitTask sweepTask;

    public AbilityRuntime(JavaPlugin plugin, Supplier<RuntimeSettings> settings, SkillsDiagnostics diagnostics) {
        this.plugin = plugin;
        this.settings = settings;
        this.diagnostics = diagnostics;
    }

    public void start() {
        if (sweepTask != null) sweepTask.cancel();
        sweepTask = Bukkit.getScheduler().runTaskTimer(plugin, this::sweep, 20L, 20L);
    }

    public View view(UUID playerId, SkillType skill, int level) {
        AbilityDefinition definition = settings.get().abilities().definition(skill);
        if (!settings.get().abilities().enabled() || definition == null || !definition.enabled()) {
            return new View(definition, State.DISABLED, 0L);
        }
        if (level < definition.unlockLevel()) {
            return new View(definition, State.LOCKED, 0L);
        }
        long now = System.currentTimeMillis();
        long active = value(activeUntil, playerId, skill);
        if (active > now) return new View(definition, State.ACTIVE, active - now);
        long cooldown = value(cooldownUntil, playerId, skill);
        if (cooldown > now) return new View(definition, State.COOLDOWN, cooldown - now);
        return new View(definition, State.READY, 0L);
    }

    public ActivationResult activate(Player player, SkillType skill, int level) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Ability activation requires primary thread");
        View before = view(player.getUniqueId(), skill, level);
        if (!player.hasPermission("plexonskills.abilities")) {
            diagnostics.abilityRejected();
            return new ActivationResult(false, before, "You do not have permission to activate skill abilities.");
        }
        if (!before.canActivate()) {
            diagnostics.abilityRejected();
            return new ActivationResult(false, before, switch (before.state()) {
                case DISABLED -> "This ability is disabled by the server.";
                case LOCKED -> "This ability unlocks at level " + (before.definition() == null ? "?" : before.definition().unlockLevel()) + ".";
                case ACTIVE -> "This ability is already active.";
                case COOLDOWN -> "This ability is still cooling down.";
                case READY -> "Ability activation was rejected.";
            });
        }

        AbilityDefinition definition = before.definition();
        PlexonSkillAbilityActivateEvent event = new PlexonSkillAbilityActivateEvent(player, skill, definition.id());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            diagnostics.abilityRejected();
            return new ActivationResult(false, before, "Ability activation was cancelled by another plugin.");
        }

        long now = System.currentTimeMillis();
        set(activeUntil, player.getUniqueId(), skill, safeAdd(now, definition.durationMillis()));
        set(cooldownUntil, player.getUniqueId(), skill, safeAdd(now, definition.cooldownMillis()));
        diagnostics.abilityActivation();
        player.sendActionBar(Component.text(definition.displayName() + " active — " + formatSeconds(definition.durationMillis()) + "s"));
        return new ActivationResult(true, view(player.getUniqueId(), skill, level), "Activated " + definition.displayName() + ".");
    }

    /** Returns the configured XP multiplier only while the ability is authoritatively active. */
    public double xpMultiplier(UUID playerId, SkillType skill) {
        AbilityDefinition definition = settings.get().abilities().definition(skill);
        if (definition == null || !settings.get().abilities().enabled(skill)) return 1.0;
        return value(activeUntil, playerId, skill) > System.currentTimeMillis() ? definition.xpMultiplier() : 1.0;
    }

    public int activePlayers() { return activeUntil.size(); }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activeUntil.remove(event.getPlayer().getUniqueId());
        cooldownUntil.remove(event.getPlayer().getUniqueId());
    }

    private void sweep() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Ability sweep requires primary thread");
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, long[]>> iterator = activeUntil.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, long[]> entry = iterator.next();
            UUID playerId = entry.getKey();
            long[] active = entry.getValue();
            long[] cooldown = cooldownUntil.get(playerId);
            Player player = Bukkit.getPlayer(playerId);
            boolean anyActive = false;
            boolean anyCooldown = false;
            for (SkillType skill : SkillType.values()) {
                int index = skill.ordinal();
                if (active[index] > 0L && active[index] <= now) {
                    active[index] = 0L;
                    AbilityDefinition definition = settings.get().abilities().definition(skill);
                    if (definition != null && player != null && player.isOnline()) {
                        Bukkit.getPluginManager().callEvent(new PlexonSkillAbilityEndEvent(player, skill, definition.id()));
                        player.sendActionBar(Component.text(definition.displayName() + " ended."));
                    }
                    diagnostics.abilityEnded();
                }
                if (active[index] > now) anyActive = true;
                if (cooldown != null && cooldown[index] > now) anyCooldown = true;
            }
            if (!anyActive && !anyCooldown) {
                iterator.remove();
                cooldownUntil.remove(playerId);
            }
        }
    }

    private static long value(Map<UUID, long[]> map, UUID playerId, SkillType skill) {
        long[] values = map.get(playerId);
        return values == null ? 0L : values[skill.ordinal()];
    }

    private static void set(Map<UUID, long[]> map, UUID playerId, SkillType skill, long value) {
        map.computeIfAbsent(playerId, ignored -> new long[SkillType.values().length])[skill.ordinal()] = value;
    }

    private static long safeAdd(long base, long delta) {
        return delta > 0L && base > Long.MAX_VALUE - delta ? Long.MAX_VALUE : base + delta;
    }

    private static long formatSeconds(long millis) {
        return Math.max(1L, (millis + 999L) / 1_000L);
    }

    @Override
    public void close() {
        if (sweepTask != null) { sweepTask.cancel(); sweepTask = null; }
        activeUntil.clear();
        cooldownUntil.clear();
    }
}
