package com.zpkdxgames.plexonskills.gui;

import com.zpkdxgames.plexonskills.PlexonSkillsPlugin;
import com.zpkdxgames.plexonskills.ability.AbilityRuntime;
import com.zpkdxgames.plexonskills.api.PlexonSkillsAPI;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.skill.SkillType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Premium administrator GUI. All progression mutations route through the public authoritative API. */
public final class AdminSkillsMenu implements Listener {
    private static final int SIZE = 54;
    private static final int BACK = 45;
    private static final int CLOSE = 53;
    private static final int[] SKILL_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24};
    private static final int[] PLAYER_SLOTS = {
        9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,
        27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44
    };

    private final PlexonSkillsPlugin plugin;
    private final PlexonSkillsAPI api;
    private final Supplier<RuntimeSettings> settings;
    private final AbilityRuntime abilities;
    private final Map<UUID, Long> generations = new HashMap<>();

    public AdminSkillsMenu(PlexonSkillsPlugin plugin, PlexonSkillsAPI api, Supplier<RuntimeSettings> settings, AbilityRuntime abilities) {
        this.plugin = plugin;
        this.api = api;
        this.settings = settings;
        this.abilities = abilities;
    }

    public void open(Player admin) {
        if (!admin.hasPermission("plexonskills.admin.gui")) { deny(admin); return; }
        Inventory inv = create(admin, AdminSkillsMenuHolder.Screen.ROOT, null, null, Map.of(),
            "<gradient:#FFD54F:#FF8F00><bold>PlexonSkills Admin</bold></gradient>");
        inv.setItem(10, action(Material.COMPARATOR, "Skill Configuration", "Enable or disable individual skills.", "#90CAF9"));
        inv.setItem(12, action(Material.PLAYER_HEAD, "Player Inspector", "Inspect online profiles and progression.", "#80CBC4"));
        inv.setItem(14, action(Material.EXPERIENCE_BOTTLE, "XP / Level Management", "Select a player and manage one skill safely.", "#AEEA00"));
        inv.setItem(16, action(Material.BLAZE_POWDER, "Ability Configuration", "Enable or disable per-skill abilities.", "#CE93D8"));
        inv.setItem(28, action(Material.NETHER_STAR, "Milestone Configuration", "Manage the derived milestone system.", "#FFD740"));
        inv.setItem(30, action(Material.AMETHYST_SHARD, "Multipliers", "Review configured passive and ability multipliers.", "#B39DDB"));
        inv.setItem(32, action(Material.CLOCK, "Leaderboard / Cache", "Leaderboard reads remain asynchronous and bounded.", "#90CAF9"));
        inv.setItem(34, action(Material.REDSTONE_TORCH, "Diagnostics", "View runtime, persistence and coordinator health.", "#FF8A65"));
        inv.setItem(49, SkillsUi.item(Material.RECOVERY_COMPASS,
            "<!italic><#66BB6A><bold>Reload Runtime</bold></#66BB6A>",
            "<!italic><gray>Atomically reload validated configuration.</gray>",
            "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to reload.</gray>"));
        inv.setItem(CLOSE, closeItem());
        admin.openInventory(inv);
    }

    private void openPlayers(Player admin) {
        Map<Integer, UUID> mapping = new LinkedHashMap<>();
        int index = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (index >= PLAYER_SLOTS.length) break;
            mapping.put(PLAYER_SLOTS[index++], player.getUniqueId());
        }
        Inventory inv = create(admin, AdminSkillsMenuHolder.Screen.PLAYER_LIST, null, null, mapping,
            "<gradient:#80CBC4:#42A5F5><bold>Select Player</bold></gradient>");
        for (Map.Entry<Integer, UUID> entry : mapping.entrySet()) {
            Player target = Bukkit.getPlayer(entry.getValue());
            if (target == null) continue;
            boolean ready = api.isProfileReady(target.getUniqueId());
            inv.setItem(entry.getKey(), SkillsUi.item(Material.PLAYER_HEAD,
                "<!italic><#80CBC4><bold>" + target.getName() + "</bold></#80CBC4>",
                "<!italic><gray>Profile</gray> " + (ready ? "<#66BB6A>READY</#66BB6A>" : "<#FFD54F>LOADING</#FFD54F>"),
                ready ? "<!italic><gray>Total Level</gray> <white>" + api.getTotalLevel(target.getUniqueId()) + "</white>" : "",
                "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to inspect.</gray>"));
        }
        if (mapping.isEmpty()) inv.setItem(22, SkillsUi.item(Material.BARRIER, "<!italic><red>No online players</red>", "<!italic><gray>Player inspection currently targets online profiles.</gray>"));
        addNavigation(inv);
        admin.openInventory(inv);
    }

    private void openPlayer(Player admin, Player target) {
        if (!api.isProfileReady(target.getUniqueId())) { admin.sendMessage(Component.text("That profile is still loading.")); return; }
        Inventory inv = create(admin, AdminSkillsMenuHolder.Screen.PLAYER, target.getUniqueId(), null, Map.of(),
            "<gradient:#80CBC4:#42A5F5><bold>Inspect " + target.getName() + "</bold></gradient>");
        long totalXp = 0L;
        SkillType highest = settings.get().skills().enabledSkills().isEmpty() ? SkillType.MINING : settings.get().skills().enabledSkills().getFirst();
        long highXp = Long.MIN_VALUE;
        for (SkillType skill : settings.get().skills().enabledSkills()) {
            long xp = Math.max(0L, api.getTotalXp(target.getUniqueId(), skill));
            totalXp = saturatedAdd(totalXp, xp);
            if (xp > highXp) { highXp = xp; highest = skill; }
        }
        inv.setItem(4, SkillsUi.item(Material.PLAYER_HEAD,
            "<!italic><gradient:#80CBC4:#42A5F5><bold>" + target.getName() + "</bold></gradient>",
            "<!italic><gray>Total Level</gray> <white>" + api.getTotalLevel(target.getUniqueId()) + "</white>",
            "<!italic><gray>Total XP</gray> <white>" + SkillsUi.exact(totalXp) + "</white>",
            "<!italic><gray>Highest Skill</gray> <white>" + highest.displayName() + "</white>"));
        List<SkillType> skills = settings.get().skills().enabledSkills();
        for (int i = 0; i < skills.size() && i < SKILL_SLOTS.length; i++) {
            SkillType skill = skills.get(i);
            int level = api.getLevel(target.getUniqueId(), skill);
            long xp = api.getTotalXp(target.getUniqueId(), skill);
            AbilityRuntime.View view = abilities.view(target.getUniqueId(), skill, level);
            inv.setItem(SKILL_SLOTS[i], SkillsUi.item(skill.icon(),
                "<!italic>" + SkillPalette.forSkill(skill).gradient(skill.displayName()) + " <gray>Lv. " + level + "</gray>",
                "<!italic><gray>XP</gray> <white>" + SkillsUi.exact(xp) + "</white>",
                "<!italic><gray>Stage</gray> <white>" + SkillStage.forLevel(level, settings.get().curve().maximumLevel()).displayName() + "</white>",
                "<!italic><gray>Ability</gray> <white>" + view.state().name() + "</white>",
                "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to manage.</gray>"));
        }
        inv.setItem(48, SkillsUi.item(Material.TNT,
            "<!italic><#EF5350><bold>Reset All Skills</bold></#EF5350>",
            "<!italic><gray>Requires confirmation.</gray>"));
        addNavigation(inv);
        admin.openInventory(inv);
    }

    private void openSkill(Player admin, Player target, SkillType skill) {
        int level = api.getLevel(target.getUniqueId(), skill);
        long xp = api.getTotalXp(target.getUniqueId(), skill);
        AbilityRuntime.View ability = abilities.view(target.getUniqueId(), skill, level);
        Inventory inv = create(admin, AdminSkillsMenuHolder.Screen.SKILL, target.getUniqueId(), skill, Map.of(),
            SkillPalette.forSkill(skill).gradient(target.getName() + " • " + skill.displayName()));
        inv.setItem(4, SkillsUi.item(skill.icon(),
            "<!italic>" + SkillPalette.forSkill(skill).gradient(skill.displayName()),
            "<!italic><gray>Level</gray> <white>" + level + "</white>",
            "<!italic><gray>XP</gray> <white>" + SkillsUi.exact(xp) + "</white>",
            "<!italic><gray>Ability</gray> <white>" + ability.state().name() + "</white>"));
        inv.setItem(20, action(Material.LIME_DYE, "+100 XP", "Authoritative XP addition.", "#66BB6A"));
        inv.setItem(21, action(Material.EMERALD, "+1,000 XP", "Authoritative XP addition.", "#66BB6A"));
        inv.setItem(23, action(Material.RED_DYE, "-100 XP", "Clamped at zero.", "#EF5350"));
        inv.setItem(24, action(Material.REDSTONE, "-1,000 XP", "Clamped at zero.", "#EF5350"));
        inv.setItem(29, action(Material.EXPERIENCE_BOTTLE, "+1 Level", "Sets XP to the next level boundary.", "#AEEA00"));
        inv.setItem(30, action(Material.GLASS_BOTTLE, "-1 Level", "Sets XP to the previous level boundary.", "#FFD54F"));
        inv.setItem(32, action(Material.BARRIER, "End Active Ability", "Ends only an active instance; cooldown remains.", "#FF8A65"));
        inv.setItem(33, action(Material.MILK_BUCKET, "Clear Ability Cooldown", "Clears only this skill cooldown.", "#90CAF9"));
        inv.setItem(40, SkillsUi.item(Material.TNT,
            "<!italic><#EF5350><bold>Reset This Skill</bold></#EF5350>",
            "<!italic><gray>Requires confirmation before XP is cleared.</gray>"));
        addNavigation(inv);
        admin.openInventory(inv);
    }

    private void openConfig(Player admin, AdminSkillsMenuHolder.Screen screen) {
        String title = switch (screen) {
            case SKILL_CONFIG -> "<gradient:#90CAF9:#42A5F5><bold>Skill Configuration</bold></gradient>";
            case ABILITY_CONFIG -> "<gradient:#CE93D8:#AB47BC><bold>Ability Configuration</bold></gradient>";
            case MILESTONE_CONFIG -> "<gradient:#FFD740:#FF8F00><bold>Milestone Configuration</bold></gradient>";
            case MULTIPLIERS -> "<gradient:#B39DDB:#7E57C2><bold>Progression Multipliers</bold></gradient>";
            default -> throw new IllegalArgumentException("Not a config screen: " + screen);
        };
        Inventory inv = create(admin, screen, null, null, Map.of(), title);
        if (screen == AdminSkillsMenuHolder.Screen.MILESTONE_CONFIG) {
            boolean enabled = settings.get().milestones().enabled();
            inv.setItem(22, SkillsUi.item(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "<!italic><#FFD740><bold>Milestone System</bold></#FFD740>",
                "<!italic><gray>Status</gray> " + state(enabled),
                "<!italic><gray>Source</gray> <white>CONFIGURED</white>",
                "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to toggle persistently.</gray>"));
        } else {
            for (int i = 0; i < SkillType.values().length && i < SKILL_SLOTS.length; i++) {
                SkillType skill = SkillType.values()[i];
                boolean enabled = settings.get().skills().enabled(skill);
                String status;
                String detail;
                Material material = skill.icon();
                if (screen == AdminSkillsMenuHolder.Screen.SKILL_CONFIG) {
                    status = state(enabled);
                    detail = "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to toggle persistently.</gray>";
                } else if (screen == AdminSkillsMenuHolder.Screen.ABILITY_CONFIG) {
                    var definition = settings.get().abilities().definition(skill);
                    boolean abilityEnabled = settings.get().abilities().enabled(skill);
                    status = state(abilityEnabled);
                    detail = definition == null ? "<!italic><red>INVALID / missing definition</red>" : "<!italic><gray>Unlock</gray> <white>Lv. " + definition.unlockLevel() + "</white> <dark_gray>•</dark_gray> <gray>Duration</gray> <white>" + definition.durationSeconds() + "s</white>\n<!italic><#FFD54F>Left Click</#FFD54F> <gray>to toggle persistently.</gray>";
                } else {
                    double passive = settings.get().milestones().passiveMultiplier(skill, settings.get().curve().maximumLevel()) - 1.0;
                    var ability = settings.get().abilities().definition(skill);
                    status = "<#B39DDB>CONFIGURED</#B39DDB>";
                    detail = "<!italic><gray>Max passive</gray> <white>" + String.format(java.util.Locale.ROOT, "%.1f%%", passive * 100.0) + "</white>" + (ability == null ? "" : "\n<!italic><gray>Ability XP</gray> <white>x" + String.format(java.util.Locale.ROOT, "%.2f", ability.xpMultiplier()) + "</white>");
                }
                inv.setItem(SKILL_SLOTS[i], SkillsUi.item(material,
                    "<!italic>" + SkillPalette.forSkill(skill).gradient(skill.displayName()),
                    "<!italic><gray>Status</gray> " + status,
                    detail));
            }
        }
        addNavigation(inv);
        admin.openInventory(inv);
    }

    private void openDiagnostics(Player admin) {
        Inventory inv = create(admin, AdminSkillsMenuHolder.Screen.DIAGNOSTICS, null, null, Map.of(),
            "<gradient:#FF8A65:#EF5350><bold>Skills Diagnostics</bold></gradient>");
        var health = plugin.repository().health();
        inv.setItem(13, SkillsUi.item(Material.REDSTONE_TORCH,
            "<!italic><#FF8A65><bold>Runtime Health</bold></#FF8A65>",
            "<!italic><gray>Core API</gray> <white>" + plugin.core().version().apiVersion() + "</white>",
            "<!italic><gray>Registered Skills</gray> <white>" + settings.get().skills().enabledSkills().size() + "</white>",
            "<!italic><gray>Ability Entries</gray> <white>" + abilities.activePlayers() + "</white>",
            "<!italic><gray>DB Health</gray> <white>" + health.state() + "</white>",
            "<!italic><gray>Queued Writes</gray> <white>" + plugin.repository().queuedWrites() + "</white>"));
        inv.setItem(31, SkillsUi.item(Material.PAPER,
            "<!italic><#90CAF9><bold>Detailed Diagnostics</bold></#90CAF9>",
            "<!italic><gray>Click to print the full diagnostic snapshot to chat.</gray>"));
        addNavigation(inv);
        admin.openInventory(inv);
    }

    private void openConfirmation(Player admin, Player target, SkillType skill, boolean all) {
        AdminSkillsMenuHolder.Screen screen = all ? AdminSkillsMenuHolder.Screen.CONFIRM_ALL_RESET : AdminSkillsMenuHolder.Screen.CONFIRM_SKILL_RESET;
        Inventory inv = create(admin, screen, target.getUniqueId(), skill, Map.of(), "<red><bold>Confirm Destructive Reset</bold></red>");
        inv.setItem(22, SkillsUi.item(Material.TNT,
            "<!italic><red><bold>CONFIRM RESET</bold></red>",
            "<!italic><gray>Player</gray> <white>" + target.getName() + "</white>",
            all ? "<!italic><gray>Scope</gray> <red>ALL SKILLS</red>" : "<!italic><gray>Scope</gray> <red>" + skill.displayName() + "</red>",
            "",
            "<!italic><red>This cannot be undone without a data backup.</red>",
            "<!italic><#FFD54F>Left Click again</#FFD54F> <gray>to confirm.</gray>"));
        addNavigation(inv);
        admin.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AdminSkillsMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player admin)) return;
        if (!valid(admin, holder)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
        if (slot == CLOSE) { admin.closeInventory(); return; }
        if (slot == BACK) { back(admin, holder); return; }

        switch (holder.screen()) {
            case ROOT -> handleRoot(admin, slot);
            case PLAYER_LIST -> {
                UUID id = holder.playerAt(slot);
                Player target = id == null ? null : Bukkit.getPlayer(id);
                if (target != null) openPlayer(admin, target);
            }
            case PLAYER -> handlePlayer(admin, holder, slot);
            case SKILL -> handleSkill(admin, holder, slot);
            case CONFIRM_SKILL_RESET -> { if (slot == 22) confirmReset(admin, holder, false); }
            case CONFIRM_ALL_RESET -> { if (slot == 22) confirmReset(admin, holder, true); }
            case SKILL_CONFIG -> toggleSkill(admin, slot);
            case ABILITY_CONFIG -> toggleAbility(admin, slot);
            case MILESTONE_CONFIG -> { if (slot == 22) toggleMilestones(admin); }
            case MULTIPLIERS -> { }
            case DIAGNOSTICS -> { if (slot == 31 && admin.hasPermission("plexonskills.admin.diagnostics")) plugin.sendDiagnostics(admin); }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AdminSkillsMenuHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { generations.remove(event.getPlayer().getUniqueId()); }

    private void handleRoot(Player admin, int slot) {
        switch (slot) {
            case 10 -> openConfig(admin, AdminSkillsMenuHolder.Screen.SKILL_CONFIG);
            case 12,14 -> openPlayers(admin);
            case 16 -> openConfig(admin, AdminSkillsMenuHolder.Screen.ABILITY_CONFIG);
            case 28 -> openConfig(admin, AdminSkillsMenuHolder.Screen.MILESTONE_CONFIG);
            case 30 -> openConfig(admin, AdminSkillsMenuHolder.Screen.MULTIPLIERS);
            case 32 -> admin.sendMessage(Component.text("Leaderboard reads are asynchronous; cache diagnostics are handled by the runtime diagnostics surface."));
            case 34 -> openDiagnostics(admin);
            case 49 -> {
                if (!admin.hasPermission("plexonskills.admin.reload")) { deny(admin); return; }
                boolean ok = plugin.reloadRuntime();
                admin.sendMessage(Component.text(ok ? "PlexonSkills runtime reloaded." : "Reload rejected; previous runtime remains active."));
                open(admin);
            }
            default -> { }
        }
    }

    private void handlePlayer(Player admin, AdminSkillsMenuHolder holder, int slot) {
        Player target = Bukkit.getPlayer(holder.targetId());
        if (target == null) { admin.sendMessage(Component.text("Player is no longer online.")); openPlayers(admin); return; }
        int index = indexOf(SKILL_SLOTS, slot);
        if (index >= 0) {
            List<SkillType> skills = settings.get().skills().enabledSkills();
            if (index < skills.size()) openSkill(admin, target, skills.get(index));
            return;
        }
        if (slot == 48) {
            if (!admin.hasPermission("plexonskills.admin.reset")) { deny(admin); return; }
            openConfirmation(admin, target, null, true);
        }
    }

    private void handleSkill(Player admin, AdminSkillsMenuHolder holder, int slot) {
        Player target = Bukkit.getPlayer(holder.targetId());
        SkillType skill = holder.skill();
        if (target == null || skill == null) { openPlayers(admin); return; }
        long xp = api.getTotalXp(target.getUniqueId(), skill);
        int level = api.getLevel(target.getUniqueId(), skill);
        switch (slot) {
            case 20 -> addXp(admin, target, skill, 100L);
            case 21 -> addXp(admin, target, skill, 1_000L);
            case 23 -> setXp(admin, target, skill, Math.max(0L, xp - 100L));
            case 24 -> setXp(admin, target, skill, Math.max(0L, xp - 1_000L));
            case 29 -> setLevel(admin, target, skill, Math.min(settings.get().curve().maximumLevel(), level + 1));
            case 30 -> setLevel(admin, target, skill, Math.max(1, level - 1));
            case 32 -> {
                if (!admin.hasPermission("plexonskills.admin.ability")) { deny(admin); return; }
                admin.sendMessage(Component.text(abilities.endActive(target, skill) ? "Active ability ended." : "No active ability to end."));
                openSkill(admin, target, skill);
            }
            case 33 -> {
                if (!admin.hasPermission("plexonskills.admin.ability")) { deny(admin); return; }
                abilities.clearCooldown(target.getUniqueId(), skill);
                admin.sendMessage(Component.text("Ability cooldown cleared."));
                openSkill(admin, target, skill);
            }
            case 40 -> {
                if (!admin.hasPermission("plexonskills.admin.reset")) { deny(admin); return; }
                openConfirmation(admin, target, skill, false);
            }
            default -> { }
        }
    }

    private void confirmReset(Player admin, AdminSkillsMenuHolder holder, boolean all) {
        if (!admin.hasPermission("plexonskills.admin.reset")) { deny(admin); return; }
        Player target = Bukkit.getPlayer(holder.targetId());
        if (target == null) { openPlayers(admin); return; }
        if (all) {
            for (SkillType skill : SkillType.values()) api.setXp(target.getUniqueId(), skill, 0L, "ADMIN_GUI_RESET_ALL");
            admin.sendMessage(Component.text("All skills reset for " + target.getName() + "."));
            openPlayer(admin, target);
        } else {
            api.setXp(target.getUniqueId(), holder.skill(), 0L, "ADMIN_GUI_RESET");
            admin.sendMessage(Component.text(holder.skill().displayName() + " reset for " + target.getName() + "."));
            openSkill(admin, target, holder.skill());
        }
    }

    private void addXp(Player admin, Player target, SkillType skill, long amount) {
        if (!admin.hasPermission("plexonskills.admin.xp")) { deny(admin); return; }
        boolean changed = api.addXp(target.getUniqueId(), skill, amount, "ADMIN_GUI");
        admin.sendMessage(Component.text(changed ? "Added " + amount + " XP." : "No change/profile unavailable."));
        openSkill(admin, target, skill);
    }

    private void setXp(Player admin, Player target, SkillType skill, long amount) {
        if (!admin.hasPermission("plexonskills.admin.xp")) { deny(admin); return; }
        boolean changed = api.setXp(target.getUniqueId(), skill, amount, "ADMIN_GUI");
        admin.sendMessage(Component.text(changed ? "XP updated." : "No change/profile unavailable."));
        openSkill(admin, target, skill);
    }

    private void setLevel(Player admin, Player target, SkillType skill, int level) {
        if (!admin.hasPermission("plexonskills.admin.xp")) { deny(admin); return; }
        boolean changed = api.setXp(target.getUniqueId(), skill, settings.get().curve().cumulativeForLevel(level), "ADMIN_GUI_LEVEL");
        admin.sendMessage(Component.text(changed ? "Level updated to " + level + "." : "No change/profile unavailable."));
        openSkill(admin, target, skill);
    }

    private void toggleSkill(Player admin, int slot) {
        if (!admin.hasPermission("plexonskills.admin.config")) { deny(admin); return; }
        int index = indexOf(SKILL_SLOTS, slot);
        if (index < 0 || index >= SkillType.values().length) return;
        SkillType skill = SkillType.values()[index];
        boolean next = !settings.get().skills().enabled(skill);
        admin.closeInventory();
        plugin.updateBooleanConfig("skills.yml", "skills." + skill.name() + ".enabled", next, admin,
            () -> openConfig(admin, AdminSkillsMenuHolder.Screen.SKILL_CONFIG));
    }

    private void toggleAbility(Player admin, int slot) {
        if (!admin.hasPermission("plexonskills.admin.config")) { deny(admin); return; }
        int index = indexOf(SKILL_SLOTS, slot);
        if (index < 0 || index >= SkillType.values().length) return;
        SkillType skill = SkillType.values()[index];
        boolean next = !settings.get().abilities().enabled(skill);
        admin.closeInventory();
        plugin.updateBooleanConfig("abilities.yml", "abilities.skills." + skill.name() + ".enabled", next, admin,
            () -> openConfig(admin, AdminSkillsMenuHolder.Screen.ABILITY_CONFIG));
    }

    private void toggleMilestones(Player admin) {
        if (!admin.hasPermission("plexonskills.admin.config")) { deny(admin); return; }
        boolean next = !settings.get().milestones().enabled();
        admin.closeInventory();
        plugin.updateBooleanConfig("rewards.yml", "milestones.enabled", next, admin,
            () -> openConfig(admin, AdminSkillsMenuHolder.Screen.MILESTONE_CONFIG));
    }

    private void back(Player admin, AdminSkillsMenuHolder holder) {
        switch (holder.screen()) {
            case ROOT -> open(admin);
            case PLAYER_LIST, SKILL_CONFIG, ABILITY_CONFIG, MILESTONE_CONFIG, MULTIPLIERS, DIAGNOSTICS -> open(admin);
            case PLAYER -> openPlayers(admin);
            case SKILL -> {
                Player target = Bukkit.getPlayer(holder.targetId());
                if (target == null) openPlayers(admin); else openPlayer(admin, target);
            }
            case CONFIRM_SKILL_RESET -> {
                Player target = Bukkit.getPlayer(holder.targetId());
                if (target == null) openPlayers(admin); else openSkill(admin, target, holder.skill());
            }
            case CONFIRM_ALL_RESET -> {
                Player target = Bukkit.getPlayer(holder.targetId());
                if (target == null) openPlayers(admin); else openPlayer(admin, target);
            }
        }
    }

    private Inventory create(Player admin, AdminSkillsMenuHolder.Screen screen, UUID targetId, SkillType skill, Map<Integer, UUID> playerSlots, String title) {
        long generation = generations.merge(admin.getUniqueId(), 1L, Long::sum);
        AdminSkillsMenuHolder holder = new AdminSkillsMenuHolder(admin.getUniqueId(), generation, screen, targetId, skill, playerSlots);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, SkillsUi.mm(title));
        holder.bind(inventory);
        return inventory;
    }

    private boolean valid(Player admin, AdminSkillsMenuHolder holder) {
        return holder.adminId().equals(admin.getUniqueId()) && generations.getOrDefault(admin.getUniqueId(), -1L) == holder.generation();
    }

    private static org.bukkit.inventory.ItemStack action(Material material, String name, String description, String accent) {
        return SkillsUi.item(material,
            "<!italic><" + accent + "><bold>" + name + "</bold></" + accent + ">",
            "<!italic><gray>" + description + "</gray>",
            "",
            "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to open.</gray>");
    }

    private static String state(boolean enabled) { return enabled ? "<#66BB6A><bold>CONFIGURED / ENABLED</bold></#66BB6A>" : "<#78909C><bold>DISABLED</bold></#78909C>"; }
    private static org.bukkit.inventory.ItemStack closeItem() { return SkillsUi.item(Material.BARRIER, SkillsUi.closeName(), "<!italic><gray>Close the admin menu.</gray>"); }
    private static void addNavigation(Inventory inv) { inv.setItem(BACK, SkillsUi.item(Material.ARROW, SkillsUi.backName(), "<!italic><gray>Return to the previous admin screen.</gray>")); inv.setItem(CLOSE, closeItem()); }
    private static int indexOf(int[] values, int target) { for (int i = 0; i < values.length; i++) if (values[i] == target) return i; return -1; }
    private static long saturatedAdd(long a, long b) { return b > 0L && a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
    private static void deny(Player player) { player.sendMessage(Component.text("You do not have permission for that PlexonSkills administration action.")); }
}
