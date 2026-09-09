package com.zpkdxgames.plexonskills.gui;

import com.zpkdxgames.plexonskills.api.PlexonSkillsAPI;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.skill.SkillType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class SkillsMenu implements Listener {
    private static final String TITLE="PlexonSkills"; private final PlexonSkillsAPI api; private final Supplier<RuntimeSettings> settings;
    public SkillsMenu(PlexonSkillsAPI api,Supplier<RuntimeSettings> settings){this.api=api;this.settings=settings;}
    public void open(Player player){
        if(!api.isProfileReady(player.getUniqueId())){player.sendMessage(Component.text("Your skill profile is still loading."));return;}
        Inventory inv=Bukkit.createInventory(null,36,TITLE);int slot=0;
        for(SkillType type:settings.get().skills().enabledSkills()){
            ItemStack item=new ItemStack(type.icon());ItemMeta meta=item.getItemMeta();meta.displayName(Component.text(type.displayName()));
            long xp=api.getTotalXp(player.getUniqueId(),type);int level=api.getLevel(player.getUniqueId(),type);double progress=settings.get().curve().progressWithinLevel(xp)*100.0;
            List<Component> lore=new ArrayList<>();lore.add(Component.text("Level "+level));lore.add(Component.text("XP "+xp));lore.add(Component.text(String.format(java.util.Locale.ROOT,"Progress %.1f%%",progress)));meta.lore(lore);item.setItemMeta(meta);inv.setItem(slot++,item);
        }
        player.openInventory(inv);
    }
    @EventHandler public void onClick(InventoryClickEvent event){if(!event.getView().getTitle().equals(TITLE))return;event.setCancelled(true);}
}
