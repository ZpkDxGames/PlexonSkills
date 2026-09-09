package com.zpkdxgames.plexonskills.runtime;

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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class GameplayListener implements Listener {
    private static final Set<Material> SWORDS=materialsEnding("_SWORD");
    private static final Set<Material> AXES=materialsEnding("_AXE");
    private final Supplier<RuntimeSettings> settings;
    private final SkillProgressionService progression;
    private final PlayerSkillsService players;
    private final SkillsDiagnostics diagnostics;
    private final Map<LocationKey,LastBrewer> brewers=new HashMap<>();

    public GameplayListener(Supplier<RuntimeSettings> settings,SkillProgressionService progression,PlayerSkillsService players,SkillsDiagnostics diagnostics){this.settings=settings;this.progression=progression;this.players=players;this.diagnostics=diagnostics;}

    @EventHandler public void onJoin(PlayerJoinEvent event){players.load(event.getPlayer());}
    @EventHandler public void onQuit(PlayerQuitEvent event){players.unload(event.getPlayer());}

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onCombat(EntityDamageByEntityEvent event){
        if(event.getFinalDamage()<=0.0)return; diagnostics.combatFact();
        if(event.getEntity() instanceof ArmorStand)return;
        RuntimeSettings runtime=settings.get();
        if(event.getEntity() instanceof Player && !runtime.allowPvp())return;
        Attribution attribution=attribute(event.getDamager()); if(attribution==null)return;
        SkillDefinition def=runtime.skills().definition(attribution.skill()); if(def==null||!def.enabled())return;
        long xp=Math.max(1L,Math.round(event.getFinalDamage()*Math.max(0.0,def.damageXp())));
        progression.grant(attribution.player(),attribution.skill(),xp,"COMBAT_DAMAGE");
    }

    private Attribution attribute(Entity damager){
        if(damager instanceof Tameable tameable && tameable.getOwner() instanceof Player owner)return new Attribution(owner,SkillType.TAMING);
        if(damager instanceof AbstractArrow arrow){ProjectileSource source=arrow.getShooter();if(source instanceof Player player)return new Attribution(player,SkillType.ARCHERY);return null;}
        if(damager instanceof Projectile projectile){ProjectileSource source=projectile.getShooter();if(source instanceof Player player && projectile instanceof AbstractArrow)return new Attribution(player,SkillType.ARCHERY);return null;}
        if(!(damager instanceof Player player))return null;
        Material held=player.getInventory().getItemInMainHand().getType();
        if(SWORDS.contains(held))return new Attribution(player,SkillType.SWORDS);
        if(AXES.contains(held))return new Attribution(player,SkillType.AXES);
        if(held.isAir())return new Attribution(player,SkillType.UNARMED);
        return null;
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onFishing(PlayerFishEvent event){
        if(event.getState()!=PlayerFishEvent.State.CAUGHT_FISH)return; diagnostics.fishingFact();
        SkillDefinition def=settings.get().skills().definition(SkillType.FISHING);
        if(def!=null&&def.enabled())progression.grant(event.getPlayer(),SkillType.FISHING,Math.max(1L,def.defaultXp()),"FISH_CAUGHT");
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onFall(EntityDamageEvent event){
        if(event.getCause()!=EntityDamageEvent.DamageCause.FALL||event.getFinalDamage()<=0.0||!(event.getEntity() instanceof Player player))return;
        diagnostics.acrobaticsFact(); SkillDefinition def=settings.get().skills().definition(SkillType.ACROBATICS);
        if(def!=null&&def.enabled())progression.grant(player,SkillType.ACROBATICS,Math.max(1L,Math.round(event.getFinalDamage()*Math.max(0.0,def.damageXp()))),"FALL_SURVIVED");
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event){
        if(!(event.getWhoClicked() instanceof Player player))return;
        if(event.getInventory() instanceof AnvilInventory anvil && event.getRawSlot()==2){
            ItemStack input=anvil.getItem(0); ItemStack result=event.getCurrentItem();
            if(input!=null&&result!=null&&input.getType()==result.getType()&&input.getItemMeta() instanceof Damageable before&&result.getItemMeta() instanceof Damageable after){
                int repaired=Math.max(0,before.getDamage()-after.getDamage());
                if(repaired>0){SkillDefinition def=settings.get().skills().definition(SkillType.REPAIR);if(def!=null&&def.enabled())progression.grant(player,SkillType.REPAIR,Math.max(def.defaultXp(),repaired/10L),"ANVIL_REPAIR");}
            }
        }
        if(event.getInventory().getHolder() instanceof BrewingStand stand){
            brewers.put(LocationKey.of(stand.getLocation()),new LastBrewer(player.getUniqueId(),System.nanoTime()));
        }
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBrew(BrewEvent event){
        LocationKey key=LocationKey.of(event.getBlock().getLocation()); LastBrewer last=brewers.get(key); if(last==null)return;
        if(System.nanoTime()-last.nanoTime()>300_000_000_000L){brewers.remove(key);return;}
        Player player=Bukkit.getPlayer(last.playerId()); if(player==null)return;
        SkillDefinition def=settings.get().skills().definition(SkillType.ALCHEMY);if(def!=null&&def.enabled())progression.grant(player,SkillType.ALCHEMY,Math.max(1L,def.defaultXp()),"BREW_COMPLETE");
    }

    private static Set<Material> materialsEnding(String suffix){EnumSet<Material> set=EnumSet.noneOf(Material.class);for(Material m:Material.values())if(m.name().endsWith(suffix))set.add(m);return Set.copyOf(set);}
    private record Attribution(Player player,SkillType skill){}
    private record LastBrewer(UUID playerId,long nanoTime){}
    private record LocationKey(UUID world,int x,int y,int z){static LocationKey of(Location l){return new LocationKey(l.getWorld().getUID(),l.getBlockX(),l.getBlockY(),l.getBlockZ());}}
}
