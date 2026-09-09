package com.zpkdxgames.plexonskills.runtime;

import com.zpkdxgames.plexonskills.config.AntiExploitPolicy;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.diagnostics.SkillsDiagnostics;
import com.zpkdxgames.plexonskills.player.PlayerSkillsService;
import com.zpkdxgames.plexonskills.skill.SkillDefinition;
import com.zpkdxgames.plexonskills.skill.SkillProgressionService;
import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.projectiles.ProjectileSource;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class GameplayListener implements Listener {
    private static final int MAX_MOB_ORIGINS = 8_192;
    private static final int MAX_COMBAT_KEYS = 4_096;
    private static final int MAX_BREWERS = 2_048;
    private static final Set<Material> SWORDS = materialsEnding("_SWORD");
    private static final Set<Material> AXES = materialsEnding("_AXE");

    private final Supplier<RuntimeSettings> settings;
    private final SkillProgressionService progression;
    private final PlayerSkillsService players;
    private final SkillsDiagnostics diagnostics;
    private final LinkedHashMap<UUID, AntiExploitPolicy.MobOrigin> mobOrigins = new LinkedHashMap<>();
    private final LinkedHashMap<CombatKey, Long> combatHits = new LinkedHashMap<>();
    private final LinkedHashMap<LocationKey, LastBrewer> brewers = new LinkedHashMap<>();
    private final Map<UUID, Long> fishingEvents = new HashMap<>();
    private final Map<UUID, Long> acrobaticsEvents = new HashMap<>();
    private final Map<UUID, Long> repairEvents = new HashMap<>();

    public GameplayListener(Supplier<RuntimeSettings> settings, SkillProgressionService progression, PlayerSkillsService players, SkillsDiagnostics diagnostics) {
        this.settings = settings;
        this.progression = progression;
        this.players = players;
        this.diagnostics = diagnostics;
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) { players.load(event.getPlayer()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        players.unload(event.getPlayer());
        fishingEvents.remove(id);
        acrobaticsEvents.remove(id);
        repairEvents.remove(id);
        combatHits.entrySet().removeIf(entry -> entry.getKey().attacker().equals(id));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        AntiExploitPolicy.MobOrigin origin = classifySpawn(event.getSpawnReason().name());
        UUID id = event.getEntity().getUniqueId();
        if (origin == AntiExploitPolicy.MobOrigin.NATURAL) mobOrigins.remove(id);
        else boundedPut(mobOrigins, id, origin, MAX_MOB_ORIGINS);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        mobOrigins.remove(id);
        combatHits.entrySet().removeIf(entry -> entry.getKey().target().equals(id));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0.0) return;
        diagnostics.combatFact();
        Entity target = event.getEntity();
        if (target instanceof ArmorStand) return;
        RuntimeSettings runtime = settings.get();
        if (runtime.antiExploit().combat().excludeNpcs() && isNpc(target)) { diagnostics.xpRejected(); return; }

        Attribution attribution = attribute(event.getDamager());
        if (attribution == null) return;
        if (target.getUniqueId().equals(attribution.player().getUniqueId())) { diagnostics.xpRejected(); return; }
        if (target instanceof Player && !runtime.allowPvp()) { diagnostics.xpRejected(); return; }
        if (target instanceof Tameable tameable && tameable.getOwner() != null && attribution.player().getUniqueId().equals(tameable.getOwner().getUniqueId())) {
            diagnostics.xpRejected();
            return;
        }
        if (!allowCombatHit(attribution.player().getUniqueId(), target.getUniqueId(), runtime.antiExploit().combat().sameTargetMinimumIntervalMillis())) {
            diagnostics.xpRejected();
            return;
        }

        double originMultiplier = target instanceof Player ? 1.0 : runtime.antiExploit().combat().multiplier(mobOrigins.getOrDefault(target.getUniqueId(), AntiExploitPolicy.MobOrigin.NATURAL));
        if (originMultiplier <= 0.0) { diagnostics.xpRejected(); return; }
        SkillDefinition def = runtime.skills().definition(attribution.skill());
        if (def == null || !def.enabled()) return;
        long xp = Math.max(1L, Math.round(event.getFinalDamage() * Math.max(0.0, def.damageXp()) * originMultiplier));
        progression.grant(attribution.player(), attribution.skill(), xp, "COMBAT_DAMAGE");
    }

    private Attribution attribute(Entity damager) {
        if (damager instanceof Tameable tameable && tameable.getOwner() instanceof Player owner) return new Attribution(owner, SkillType.TAMING);
        if (damager instanceof AbstractArrow arrow) {
            ProjectileSource source = arrow.getShooter();
            if (source instanceof Player player) return new Attribution(player, SkillType.ARCHERY);
            return null;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player && projectile instanceof AbstractArrow) return new Attribution(player, SkillType.ARCHERY);
            return null;
        }
        if (!(damager instanceof Player player)) return null;
        Material held = player.getInventory().getItemInMainHand().getType();
        if (SWORDS.contains(held)) return new Attribution(player, SkillType.SWORDS);
        if (AXES.contains(held)) return new Attribution(player, SkillType.AXES);
        if (held.isAir()) return new Attribution(player, SkillType.UNARMED);
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        diagnostics.fishingFact();
        if (!allowPlayerInterval(fishingEvents, event.getPlayer().getUniqueId(), settings.get().antiExploit().fishingMinimumIntervalMillis())) {
            diagnostics.xpRejected();
            return;
        }
        SkillDefinition def = settings.get().skills().definition(SkillType.FISHING);
        if (def != null && def.enabled()) progression.grant(event.getPlayer(), SkillType.FISHING, Math.max(1L, def.defaultXp()), "FISH_CAUGHT");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || event.getFinalDamage() <= 0.0 || !(event.getEntity() instanceof Player player)) return;
        diagnostics.acrobaticsFact();
        if (!allowPlayerInterval(acrobaticsEvents, player.getUniqueId(), settings.get().antiExploit().acrobaticsMinimumIntervalMillis())) {
            diagnostics.xpRejected();
            return;
        }
        SkillDefinition def = settings.get().skills().definition(SkillType.ACROBATICS);
        if (def != null && def.enabled()) progression.grant(player, SkillType.ACROBATICS, Math.max(1L, Math.round(event.getFinalDamage() * Math.max(0.0, def.damageXp()))), "FALL_SURVIVED");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory() instanceof AnvilInventory anvil && event.getRawSlot() == 2) {
            if (!allowPlayerInterval(repairEvents, player.getUniqueId(), settings.get().antiExploit().repairMinimumIntervalMillis())) return;
            ItemStack input = anvil.getItem(0);
            ItemStack result = event.getCurrentItem();
            if (input != null && result != null && input.getType() == result.getType() && input.getItemMeta() instanceof Damageable before && result.getItemMeta() instanceof Damageable after) {
                int repaired = Math.max(0, before.getDamage() - after.getDamage());
                if (repaired > 0) {
                    SkillDefinition def = settings.get().skills().definition(SkillType.REPAIR);
                    if (def != null && def.enabled()) progression.grant(player, SkillType.REPAIR, Math.max(def.defaultXp(), repaired / 10L), "ANVIL_REPAIR");
                }
            }
        }
        if (event.getInventory().getHolder() instanceof BrewingStand stand) {
            boundedPut(brewers, LocationKey.of(stand.getLocation()), new LastBrewer(player.getUniqueId(), System.nanoTime()), MAX_BREWERS);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        LocationKey key = LocationKey.of(event.getBlock().getLocation());
        LastBrewer last = brewers.get(key);
        if (last == null) return;
        long windowNanos = settings.get().antiExploit().alchemyAttributionWindowMillis() * 1_000_000L;
        if (System.nanoTime() - last.nanoTime() > windowNanos) { brewers.remove(key); return; }
        Player player = Bukkit.getPlayer(last.playerId());
        if (player == null) return;
        SkillDefinition def = settings.get().skills().definition(SkillType.ALCHEMY);
        if (def != null && def.enabled()) progression.grant(player, SkillType.ALCHEMY, Math.max(1L, def.defaultXp()), "BREW_COMPLETE");
    }

    static AntiExploitPolicy.MobOrigin classifySpawn(String reason) {
        String normalized = reason == null ? "" : reason.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SPAWNER" -> AntiExploitPolicy.MobOrigin.SPAWNER;
            case "BREEDING" -> AntiExploitPolicy.MobOrigin.BREEDING;
            case "CUSTOM", "COMMAND", "SPAWNER_EGG" -> AntiExploitPolicy.MobOrigin.CUSTOM;
            default -> AntiExploitPolicy.MobOrigin.NATURAL;
        };
    }

    private boolean allowCombatHit(UUID attacker, UUID target, long minimumMillis) {
        if (minimumMillis <= 0L) return true;
        long now = System.nanoTime();
        CombatKey key = new CombatKey(attacker, target);
        long previous = combatHits.getOrDefault(key, Long.MIN_VALUE);
        if (previous != Long.MIN_VALUE && now - previous < minimumMillis * 1_000_000L) return false;
        boundedPut(combatHits, key, now, MAX_COMBAT_KEYS);
        return true;
    }

    static boolean intervalElapsed(long previousNanos, long nowNanos, long minimumMillis) {
        return minimumMillis <= 0L || previousNanos == Long.MIN_VALUE || nowNanos - previousNanos >= minimumMillis * 1_000_000L;
    }

    private static boolean allowPlayerInterval(Map<UUID, Long> map, UUID playerId, long minimumMillis) {
        if (minimumMillis <= 0L) return true;
        long now = System.nanoTime();
        long previous = map.getOrDefault(playerId, Long.MIN_VALUE);
        if (!intervalElapsed(previous, now, minimumMillis)) return false;
        map.put(playerId, now);
        return true;
    }

    private static boolean isNpc(Entity entity) {
        if (entity.hasMetadata("NPC")) return true;
        for (String tag : entity.getScoreboardTags()) {
            String normalized = tag.toLowerCase(Locale.ROOT);
            if (normalized.equals("npc") || normalized.startsWith("citizens_npc")) return true;
        }
        return false;
    }

    private static <K, V> void boundedPut(LinkedHashMap<K, V> map, K key, V value, int maximum) {
        map.remove(key);
        map.put(key, value);
        while (map.size() > maximum) map.remove(map.keySet().iterator().next());
    }

    private static Set<Material> materialsEnding(String suffix) {
        EnumSet<Material> set = EnumSet.noneOf(Material.class);
        for (Material material : Material.values()) if (material.name().endsWith(suffix)) set.add(material);
        return Set.copyOf(set);
    }

    private record Attribution(Player player, SkillType skill) { }
    private record CombatKey(UUID attacker, UUID target) { }
    private record LastBrewer(UUID playerId, long nanoTime) { }
    private record LocationKey(UUID world, int x, int y, int z) {
        static LocationKey of(Location location) { return new LocationKey(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ()); }
    }
}
