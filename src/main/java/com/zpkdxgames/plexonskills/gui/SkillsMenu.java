package com.zpkdxgames.plexonskills.gui;

import com.zpkdxgames.plexonskills.api.PlexonSkillsAPI;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.persistence.SkillsRepository;
import com.zpkdxgames.plexonskills.skill.SkillType;
import com.zpkdxgames.plexonskills.skill.XpCurve;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Session-aware premium GUI controller for player progression. */
public final class SkillsMenu implements Listener {
    private static final int SIZE = 54;
    private static final int BACK_SLOT = 45;
    private static final int CLOSE_SLOT = 53;
    private static final int[] SKILL_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24};
    private static final int[] STAGE_SLOTS = {10, 12, 14, 16, 29, 31};
    private static final int[] TOP_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};

    private final JavaPlugin plugin;
    private final PlexonSkillsAPI api;
    private final Supplier<RuntimeSettings> settings;
    private final SkillsRepository repository;
    private final Map<UUID, Long> generations = new HashMap<>();

    public SkillsMenu(JavaPlugin plugin, PlexonSkillsAPI api, Supplier<RuntimeSettings> settings, SkillsRepository repository) {
        this.plugin = plugin;
        this.api = api;
        this.settings = settings;
        this.repository = repository;
    }

    public void open(Player player) { openRoot(player); }

    private void openRoot(Player player) {
        if (!ready(player)) return;
        Inventory inv = menu(player, SkillsMenuHolder.Screen.ROOT, null,
            "<gradient:#FFF176:#FF8F00><bold>Skills</bold></gradient>");
        SkillType highest = highestSkill(player);
        int totalLevel = api.getTotalLevel(player.getUniqueId());
        long totalXp = totalXp(player);

        inv.setItem(4, SkillsUi.item(Material.NETHER_STAR,
            "<!italic><gradient:#FFF176:#FF8F00><bold>Player Profile</bold></gradient>",
            "<!italic><dark_gray>Your progression at a glance</dark_gray>",
            "",
            line("#FFD740", "✥", "Total Level", Integer.toString(totalLevel)),
            line("#66BB6A", "▰", "Total XP", SkillsUi.exact(totalXp)),
            line("#90CAF9", "◆", "Highest", highest.displayName()),
            line("#AEEA00", "✦", "Mastered", masteredCount(player) + " / " + enabled().size()),
            "",
            "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to open your profile.</gray>"));

        List<SkillType> skills = enabled();
        for (int i = 0; i < skills.size() && i < SKILL_SLOTS.length; i++) {
            inv.setItem(SKILL_SLOTS[i], skillCard(player, skills.get(i)));
        }

        inv.setItem(47, action(Material.WRITABLE_BOOK, "#90CAF9", "Statistics", "View exact loaded-profile values."));
        inv.setItem(48, action(Material.KNOWLEDGE_BOOK, "#B39DDB", "Help", "Review interactions and state language."));
        inv.setItem(49, action(Material.TOTEM_OF_UNDYING, "#FFD54F", "Leaderboards", "Open your highest-skill leaderboard."));
        inv.setItem(CLOSE_SLOT, closeItem());
        player.openInventory(inv);
    }

    private void openProfile(Player player) {
        if (!ready(player)) return;
        Inventory inv = menu(player, SkillsMenuHolder.Screen.PROFILE, null,
            "<gradient:#FFF176:#FF8F00><bold>Skill Profile</bold></gradient>");
        SkillType highest = highestSkill(player);
        inv.setItem(13, SkillsUi.item(Material.PLAYER_HEAD,
            "<!italic><gradient:#FFF176:#FF8F00><bold>" + player.getName() + "</bold></gradient>",
            "<!italic><dark_gray>PlexonSkills profile</dark_gray>", "",
            line("#FFD740", "✥", "Total Level", Integer.toString(api.getTotalLevel(player.getUniqueId()))),
            line("#66BB6A", "▰", "Total XP", SkillsUi.exact(totalXp(player))),
            line("#90CAF9", "◆", "Highest Skill", highest.displayName()),
            line("#AEEA00", "✦", "Mastered", masteredCount(player) + " / " + enabled().size())));
        inv.setItem(20, metric(Material.EXPERIENCE_BOTTLE, "Total Level", Integer.toString(api.getTotalLevel(player.getUniqueId())), "#FFD740"));
        inv.setItem(22, metric(Material.AMETHYST_SHARD, "Total XP", SkillsUi.exact(totalXp(player)), "#66BB6A"));
        inv.setItem(24, metric(highest.icon(), "Highest Skill", highest.displayName(), SkillPalette.forSkill(highest).colorA()));
        inv.setItem(31, metric(Material.NETHER_STAR, "Mastery", masteredCount(player) + " / " + enabled().size(), "#AEEA00"));
        nav(inv);
        player.openInventory(inv);
    }

    private void openDetail(Player player, SkillType skill) {
        if (!ready(player)) return;
        SkillPalette palette = SkillPalette.forSkill(skill);
        XpCurve curve = settings.get().curve();
        int level = api.getLevel(player.getUniqueId(), skill);
        SkillStage stage = SkillStage.forLevel(level, curve.maximumLevel());
        SkillStage next = stage.next();
        String nextText = level >= curve.maximumLevel()
            ? "Fully Mastered"
            : (stage == SkillStage.MASTERY ? "Mastery • Lv. " + curve.maximumLevel() : next.displayName() + " • Lv. " + next.unlockLevel(curve.maximumLevel()));

        Inventory inv = menu(player, SkillsMenuHolder.Screen.SKILL_DETAIL, skill, palette.gradient(skill.displayName()));
        inv.setItem(13, skillCard(player, skill));
        inv.setItem(20, metric(Material.EXPERIENCE_BOTTLE, "Exact XP", SkillsUi.exact(api.getTotalXp(player.getUniqueId(), skill)), palette.colorA()));
        inv.setItem(22, metric(Material.NETHER_STAR, "Current Stage", stage.displayName(), stage.colorA()));
        inv.setItem(24, metric(Material.COMPASS, "Next Chapter", nextText, next.colorA()));
        inv.setItem(29, action(Material.MAP, "#FFD54F", "Progression Roadmap", "View completed and future progression chapters."));
        inv.setItem(31, action(Material.BLAZE_POWDER, "#B39DDB", "Abilities", "Inspect this skill's active-ability state."));
        inv.setItem(33, action(Material.TOTEM_OF_UNDYING, "#FFD54F", "Leaderboard", "View the top ten and your rank."));
        nav(inv);
        player.openInventory(inv);
    }

    private void openProgression(Player player, SkillType skill) {
        if (!ready(player)) return;
        SkillPalette palette = SkillPalette.forSkill(skill);
        XpCurve curve = settings.get().curve();
        int level = api.getLevel(player.getUniqueId(), skill);
        SkillStage current = SkillStage.forLevel(level, curve.maximumLevel());
        Inventory inv = menu(player, SkillsMenuHolder.Screen.PROGRESSION, skill, palette.gradient(skill.displayName() + " Progression"));
        inv.setItem(4, skillCard(player, skill));

        SkillStage[] stages = SkillStage.values();
        for (int i = 0; i < stages.length; i++) {
            SkillStage stage = stages[i];
            int unlock = stage.unlockLevel(curve.maximumLevel());
            boolean mastered = level >= curve.maximumLevel() && stage == SkillStage.MASTERY;
            boolean active = current == stage && level < curve.maximumLevel();
            boolean completed = level >= unlock && !active;
            String state = mastered ? "✦ FULLY MASTERED" : active ? "◆ CURRENT CHAPTER" : completed ? "✔ COMPLETED" : "✕ LOCKED";
            String color = mastered ? "#AEEA00" : active ? "#FFD740" : completed ? "#66BB6A" : "#EF5350";
            Material material = mastered ? Material.NETHER_STAR : active ? Material.COMPASS : completed ? Material.LIME_DYE : Material.GRAY_DYE;
            inv.setItem(STAGE_SLOTS[i], SkillsUi.item(material,
                "<!italic>" + stage.gradient(stage.displayName()),
                "<!italic><dark_gray>Progression chapter</dark_gray>", "",
                line("#FFD740", "✥", "Unlock Level", Integer.toString(unlock)),
                "<!italic><gray>Status</gray> <" + color + "><bold>" + state + "</bold></" + color + ">"));
        }
        nav(inv);
        player.openInventory(inv);
    }

    private void openAbilities(Player player, SkillType skill) {
        if (!ready(player)) return;
        Inventory inv = menu(player, SkillsMenuHolder.Screen.ABILITIES, skill,
            "<gradient:#AB47BC:#EC407A><bold>" + skill.displayName() + " Abilities</bold></gradient>");
        inv.setItem(13, skillCard(player, skill));
        inv.setItem(22, SkillsUi.item(Material.BLAZE_POWDER,
            "<!italic><gradient:#AB47BC:#EC407A><bold>Ability Runtime Migration</bold></gradient>",
            "<!italic><dark_gray>Draft-branch state</dark_gray>", "",
            "<!italic><#FFD740>◆</#FFD740> <gray>The 1.0 candidate did not execute active abilities.</gray>",
            "<!italic><#90CAF9>◆</#90CAF9> <gray>2.0 lock/ready/active/cooldown runtime is the next implementation gate.</gray>",
            "<!italic><#66BB6A>◆</#66BB6A> <gray>No artificial READY state is shown before that runtime exists.</gray>"));
        nav(inv);
        player.openInventory(inv);
    }

    private void openStatistics(Player player) {
        if (!ready(player)) return;
        Inventory inv = menu(player, SkillsMenuHolder.Screen.STATISTICS, null,
            "<gradient:#90CAF9:#4DD0E1><bold>Skill Statistics</bold></gradient>");
        List<SkillType> skills = enabled();
        for (int i = 0; i < skills.size() && i < SKILL_SLOTS.length; i++) {
            SkillType skill = skills.get(i);
            SkillPalette palette = SkillPalette.forSkill(skill);
            inv.setItem(SKILL_SLOTS[i], SkillsUi.item(skill.icon(),
                SkillsUi.baseName(palette, skill.displayName(), "Lv. " + api.getLevel(player.getUniqueId(), skill)),
                "<!italic><dark_gray>Exact loaded-profile statistics</dark_gray>", "",
                line("#FFD740", "✥", "Level", Integer.toString(api.getLevel(player.getUniqueId(), skill))),
                line("#66BB6A", "▰", "Total XP", SkillsUi.exact(api.getTotalXp(player.getUniqueId(), skill)))));
        }
        nav(inv);
        player.openInventory(inv);
    }

    private void openHelp(Player player) {
        Inventory inv = menu(player, SkillsMenuHolder.Screen.HELP, null,
            "<gradient:#B39DDB:#E1BEE7><bold>Skills Help</bold></gradient>");
        inv.setItem(20, SkillsUi.item(Material.COMPASS,
            "<!italic><#FFD54F><bold>Interaction Language</bold></#FFD54F>",
            "<!italic><#FFD54F>Left Click</#FFD54F> <gray>primary action or progression.</gray>",
            "<!italic><#90CAF9>Right Click</#90CAF9> <gray>secondary/detail action where available.</gray>",
            "<!italic><gray>Back and Close remain in stable positions.</gray>"));
        inv.setItem(22, SkillsUi.item(Material.EXPERIENCE_BOTTLE,
            "<!italic><#66BB6A><bold>Progress States</bold></#66BB6A>",
            "<!italic><#66BB6A>✔ COMPLETED</#66BB6A> <gray>chapter reached.</gray>",
            "<!italic><#FFD740>◆ CURRENT</#FFD740> <gray>active progression chapter.</gray>",
            "<!italic><#EF5350>✕ LOCKED</#EF5350> <gray>requirement not reached.</gray>",
            "<!italic><#AEEA00>✦ FULLY MASTERED</#AEEA00> <gray>configured maximum reached.</gray>"));
        inv.setItem(24, SkillsUi.item(Material.SHIELD,
            "<!italic><#90CAF9><bold>Data Safety</bold></#90CAF9>",
            "<!italic><gray>Menus use already-loaded profiles.</gray>",
            "<!italic><gray>Leaderboard queries remain asynchronous.</gray>",
            "<!italic><gray>Stale asynchronous results are discarded.</gray>"));
        nav(inv);
        player.openInventory(inv);
    }

    private void openLeaderboard(Player player, SkillType skill) {
        if (!ready(player)) return;
        SkillPalette palette = SkillPalette.forSkill(skill);
        Inventory inv = menu(player, SkillsMenuHolder.Screen.LEADERBOARD, skill, palette.gradient(skill.displayName() + " Leaderboard"));
        SkillsMenuHolder holder = (SkillsMenuHolder) inv.getHolder();
        inv.setItem(22, SkillsUi.item(Material.CLOCK,
            "<!italic><#FFD54F><bold>Loading leaderboard…</bold></#FFD54F>",
            "<!italic><gray>Reading ranking data away from the main thread.</gray>"));
        nav(inv);
        player.openInventory(inv);

        repository.top(skill, 10).thenCombine(repository.rank(skill, player.getUniqueId()), LeaderboardResult::new)
            .whenComplete((result, error) -> Bukkit.getScheduler().runTask(plugin, () -> renderLeaderboard(player, holder, skill, result, error)));
    }

    private void renderLeaderboard(Player player, SkillsMenuHolder holder, SkillType skill, LeaderboardResult result, Throwable error) {
        if (!isOpen(player, holder)) return;
        Inventory inv = holder.getInventory();
        inv.setItem(22, null);
        if (error != null) {
            inv.setItem(22, SkillsUi.item(Material.BARRIER,
                "<!italic><#EF5350><bold>Leaderboard unavailable</bold></#EF5350>",
                "<!italic><gray>The asynchronous ranking query failed.</gray>",
                "<!italic><dark_gray>Use /skillsadmin diagnostics for server-side details.</dark_gray>"));
            return;
        }
        if (result.rows().isEmpty()) {
            inv.setItem(22, SkillsUi.item(Material.PAPER,
                "<!italic><gray><bold>No leaderboard entries yet</bold></gray>",
                "<!italic><gray>Progress in this skill to create a ranking entry.</gray>"));
        } else {
            for (int i = 0; i < result.rows().size() && i < TOP_SLOTS.length; i++) {
                SkillsRepository.LeaderboardEntry row = result.rows().get(i);
                String accent = i == 0 ? "#FFD740" : i == 1 ? "#CFD8DC" : i == 2 ? "#D7A86E" : "#90A4AE";
                inv.setItem(TOP_SLOTS[i], SkillsUi.item(Material.PLAYER_HEAD,
                    "<!italic><" + accent + "><bold>#" + (i + 1) + "</bold></" + accent + "> <white>" + row.playerName() + "</white>",
                    line("#FFD740", "✥", "Level", Integer.toString(row.level())),
                    line("#66BB6A", "▰", "Total XP", SkillsUi.exact(row.totalXp()))));
            }
        }
        inv.setItem(40, metric(Material.NAME_TAG, "Your Position", result.rank() <= 0 ? "Unranked" : "#" + result.rank(), "#FFD54F"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SkillsMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!holder.playerId().equals(player.getUniqueId()) || !isCurrent(holder)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;
        if (slot == CLOSE_SLOT) { player.closeInventory(); return; }
        if (slot == BACK_SLOT && holder.screen() != SkillsMenuHolder.Screen.ROOT) { back(player, holder); return; }

        switch (holder.screen()) {
            case ROOT -> clickRoot(player, slot);
            case SKILL_DETAIL -> clickDetail(player, holder.skill(), slot);
            default -> { }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SkillsMenuHolder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < SIZE)) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SkillsMenuHolder holder && isCurrent(holder)) {
            generations.remove(holder.playerId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { generations.remove(event.getPlayer().getUniqueId()); }

    private void clickRoot(Player player, int slot) {
        if (slot == 4) { openProfile(player); return; }
        if (slot == 47) { openStatistics(player); return; }
        if (slot == 48) { openHelp(player); return; }
        if (slot == 49) { openLeaderboard(player, highestSkill(player)); return; }
        SkillType skill = rootSkill(slot);
        if (skill != null) openDetail(player, skill);
    }

    private void clickDetail(Player player, SkillType skill, int slot) {
        if (skill == null) return;
        if (slot == 29) openProgression(player, skill);
        else if (slot == 31) openAbilities(player, skill);
        else if (slot == 33) openLeaderboard(player, skill);
    }

    private void back(Player player, SkillsMenuHolder holder) {
        switch (holder.screen()) {
            case PROGRESSION, ABILITIES, LEADERBOARD -> openDetail(player, holder.skill());
            default -> openRoot(player);
        }
    }

    private Inventory menu(Player player, SkillsMenuHolder.Screen screen, SkillType skill, String title) {
        long generation = generations.merge(player.getUniqueId(), 1L, Long::sum);
        SkillsMenuHolder holder = new SkillsMenuHolder(player.getUniqueId(), generation, screen, skill);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, SkillsUi.mm(title));
        holder.bind(inventory);
        return inventory;
    }

    private boolean ready(Player player) {
        if (api.isProfileReady(player.getUniqueId())) return true;
        player.sendMessage(SkillsUi.mm("<yellow>Your skill profile is still loading.</yellow>"));
        return false;
    }

    private boolean isCurrent(SkillsMenuHolder holder) {
        Long value = generations.get(holder.playerId());
        return value != null && value.longValue() == holder.generation();
    }

    private boolean isOpen(Player player, SkillsMenuHolder holder) {
        return player.isOnline() && isCurrent(holder) && player.getOpenInventory().getTopInventory().getHolder() == holder;
    }

    private List<SkillType> enabled() { return settings.get().skills().enabledSkills(); }

    private SkillType rootSkill(int slot) {
        List<SkillType> skills = enabled();
        for (int i = 0; i < skills.size() && i < SKILL_SLOTS.length; i++) if (SKILL_SLOTS[i] == slot) return skills.get(i);
        return null;
    }

    private ItemStack skillCard(Player player, SkillType skill) {
        XpCurve curve = settings.get().curve();
        long xp = api.getTotalXp(player.getUniqueId(), skill);
        int level = api.getLevel(player.getUniqueId(), skill);
        boolean mastered = level >= curve.maximumLevel();
        double progress = curve.progressWithinLevel(xp);
        SkillStage stage = SkillStage.forLevel(level, curve.maximumLevel());
        SkillPalette palette = SkillPalette.forSkill(skill);
        long required = curve.requiredForNext(level);
        long current = mastered ? 0L : Math.max(0L, xp - curve.cumulativeForLevel(level));
        String value = mastered
            ? "<#AEEA00><bold>FULLY MASTERED</bold></#AEEA00>"
            : "<" + SkillsUi.progressColor(progress) + ">" + SkillsUi.exact(current) + "</" + SkillsUi.progressColor(progress) + "><dark_gray>/</dark_gray><#B0BEC5>" + SkillsUi.exact(required) + "</#B0BEC5>";
        String status = mastered
            ? "<#AEEA00><bold>✦ FULLY MASTERED</bold></#AEEA00>"
            : "<" + stage.colorA() + "><bold>◆ " + stage.displayName().toUpperCase(Locale.ROOT) + "</bold></" + stage.colorA() + ">";

        return SkillsUi.item(skill.icon(),
            SkillsUi.baseName(palette, skill.displayName(), "Lv. " + level),
            "<!italic><dark_gray>" + stage.displayName() + " progression</dark_gray>", "",
            line("#FFD740", "✥", "Level", Integer.toString(level)),
            line("#90CAF9", "◆", "Stage", stage.displayName()),
            "<!italic><#FFD54F>⚡</#FFD54F> <gray>Progress</gray> " + value,
            SkillsUi.progressBar(progress, 12) + " <" + SkillsUi.progressColor(progress) + ">" + SkillsUi.percent(progress) + "</" + SkillsUi.progressColor(progress) + ">",
            "", "<!italic><gray>Status</gray> " + status,
            "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to inspect this skill.</gray>");
    }

    private ItemStack metric(Material material, String label, String value, String accent) {
        return SkillsUi.item(material,
            "<!italic><" + accent + "><bold>" + label + "</bold></" + accent + ">",
            "<!italic><gray>" + label + "</gray> <white>" + value + "</white>");
    }

    private ItemStack action(Material material, String accent, String name, String description) {
        return SkillsUi.item(material,
            "<!italic><" + accent + "><bold>" + name + "</bold></" + accent + ">",
            "<!italic><gray>" + description + "</gray>", "",
            "<!italic><" + accent + ">Left Click</" + accent + "> <gray>to open.</gray>");
    }

    private ItemStack closeItem() {
        return SkillsUi.item(Material.BARRIER, SkillsUi.closeName(), "<!italic><gray>Close the PlexonSkills menu.</gray>");
    }

    private void nav(Inventory inv) {
        inv.setItem(BACK_SLOT, SkillsUi.item(Material.ARROW, SkillsUi.backName(), "<!italic><gray>Return to the logical previous screen.</gray>"));
        inv.setItem(CLOSE_SLOT, closeItem());
    }

    private static String line(String accent, String icon, String label, String value) {
        return "<!italic><" + accent + ">" + icon + "</" + accent + "> <gray>" + label + "</gray> <white>" + value + "</white>";
    }

    private long totalXp(Player player) {
        long total = 0L;
        for (SkillType skill : enabled()) {
            long value = api.getTotalXp(player.getUniqueId(), skill);
            if (Long.MAX_VALUE - total < value) return Long.MAX_VALUE;
            total += value;
        }
        return total;
    }

    private SkillType highestSkill(Player player) {
        List<SkillType> skills = enabled();
        SkillType best = skills.isEmpty() ? SkillType.MINING : skills.getFirst();
        long bestXp = -1L;
        for (SkillType skill : skills) {
            long xp = api.getTotalXp(player.getUniqueId(), skill);
            if (xp > bestXp) { bestXp = xp; best = skill; }
        }
        return best;
    }

    private int masteredCount(Player player) {
        int max = settings.get().curve().maximumLevel();
        int count = 0;
        for (SkillType skill : enabled()) if (api.getLevel(player.getUniqueId(), skill) >= max) count++;
        return count;
    }

    private record LeaderboardResult(List<SkillsRepository.LeaderboardEntry> rows, int rank) { }
}
