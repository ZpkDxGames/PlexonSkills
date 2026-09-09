package com.zpkdxgames.plexonskills.gui;

import com.zpkdxgames.plexonskills.api.PlexonSkillsAPI;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.persistence.SkillsRepository;
import com.zpkdxgames.plexonskills.skill.SkillType;
import com.zpkdxgames.plexonskills.skill.XpCurve;
import net.kyori.adventure.text.Component;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Session-aware premium menu controller. Inventory identity is holder/generation based rather than
 * raw-title matching, preventing unrelated inventories with the same title from being intercepted.
 */
public final class SkillsMenu implements Listener {
    private static final int SIZE = 54;
    private static final int BACK_SLOT = 45;
    private static final int CLOSE_SLOT = 53;
    private static final int[] SKILL_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24};
    private static final int[] ROADMAP_SLOTS = {10, 12, 14, 16, 29, 31};
    private static final int[] LEADERBOARD_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};

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

    public void open(Player player) {
        openRoot(player);
    }

    private void openRoot(Player player) {
        if (!ensureReady(player)) return;
        Inventory inventory = create(player, SkillsMenuHolder.Screen.ROOT, null,
            "<gradient:#FFF176:#FF8F00><bold>Skills</bold></gradient>");

        long totalXp = totalXp(player);
        int totalLevel = api.getTotalLevel(player.getUniqueId());
        SkillType highest = highestSkill(player);
        int mastered = masteredCount(player);

        inventory.setItem(4, SkillsUi.item(Material.NETHER_STAR,
            "<!italic><gradient:#FFF176:#FF8F00><bold>Player Profile</bold></gradient>",
            "<!italic><dark_gray>Your progression at a glance</dark_gray>",
            "",
            "<!italic><#FFD740>✥</#FFD740> <gray>Total Level</gray> <white>" + totalLevel + "</white>",
            "<!italic><#66BB6A>▰</#66BB6A> <gray>Total XP</gray> <white>" + SkillsUi.exact(totalXp) + "</white>",
            "<!italic><#90CAF9>◆</#90CAF9> <gray>Highest</gray> <white>" + highest.displayName() + "</white>",
            "<!italic><#AEEA00>✦</#AEEA00> <gray>Mastered</gray> <white>" + mastered + "</white>",
            "",
            "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to open your profile.</gray>"));

        List<SkillType> enabled = settings.get().skills().enabledSkills();
        for (int i = 0; i < enabled.size() && i < SKILL_SLOTS.length; i++) {
            inventory.setItem(SKILL_SLOTS[i], skillCard(player, enabled.get(i)));
        }

        inventory.setItem(47, SkillsUi.item(Material.WRITABLE_BOOK,
            "<!italic><gradient:#90CAF9:#4DD0E1><bold>Statistics</bold></gradient>",
            "<!italic><gray>Inspect exact XP and level values across every enabled skill.</gray>",
            "",
            "<!italic><#90CAF9>Left Click</#90CAF9> <gray>to view statistics.</gray>"));
        inventory.setItem(48, SkillsUi.item(Material.KNOWLEDGE_BOOK,
            "<!italic><gradient:#B39DDB:#E1BEE7><bold>Help</bold></gradient>",
            "<!italic><gray>Learn the shared interaction language and progression states.</gray>",
            "",
            "<!italic><#B39DDB>Left Click</#B39DDB> <gray>to open help.</gray>"));
        inventory.setItem(49, SkillsUi.item(Material.TOTEM_OF_UNDYING,
            "<!italic><gradient:#FFD54F:#FFF176><bold>Leaderboards</bold></gradient>",
            "<!italic><gray>Compare progression without blocking the main server thread.</gray>",
            "",
            "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to view your highest-skill board.</gray>"));
        inventory.setItem(CLOSE_SLOT, closeItem());
        player.openInventory(inventory);
    }

    private void openProfile(Player player) {
        if (!ensureReady(player)) return;
        Inventory inventory = create(player, SkillsMenuHolder.Screen.PROFILE, null,
            "<gradient:#FFF176:#FF8F00><bold>Skill Profile</bold></gradient>");
        SkillType highest = highestSkill(player);
        long totalXp = totalXp(player);
        int totalLevel = api.getTotalLevel(player.getUniqueId());
        int mastered = masteredCount(player);

        inventory.setItem(13, SkillsUi.item(Material.PLAYER_HEAD,
            "<!italic><gradient:#FFF176:#FF8F00><bold>" + player.getName() + "</bold></gradient>",
            "<!italic><dark_gray>PlexonSkills profile</dark_gray>",
            "",
            "<!italic><#FFD740>✥</#FFD740> <gray>Total Level</gray> <white>" + totalLevel + "</white>",
            "<!italic><#66BB6A>▰</#66BB6A> <gray>Total XP</gray> <white>" + SkillsUi.exact(totalXp) + "</white>",
            "<!italic><#90CAF9>◆</#90CAF9> <gray>Highest Skill</gray> <white>" + highest.displayName() + "</white>",
            "<!italic><#AEEA00>✦</#AEEA00> <gray>Mastered Skills</gray> <white>" + mastered + "</white>"));

        inventory.setItem(20, metric(Material.EXPERIENCE_BOTTLE, "Total Level", Integer.toString(totalLevel), "#FFD740"));
        inventory.setItem(22, metric(Material.AMETHYST_SHARD, "Total XP", SkillsUi.exact(totalXp), "#66BB6A"));
        inventory.setItem(24, metric(highest.icon(), "Highest Skill", highest.displayName(), SkillPalette.forSkill(highest).colorA()));
        inventory.setItem(31, metric(Material.NETHER_STAR, "Mastery", mastered + " / " + settings.get().skills().enabledSkills().size(), "#AEEA00"));
        addNavigation(inventory);
        player.openInventory(inventory);
    }

    private void openSkillDetail(Player player, SkillType skill) {
        if (!ensureReady(player)) return;
        SkillPalette palette = SkillPalette.forSkill(skill);
        Inventory inventory = create(player, SkillsMenuHolder.Screen.SKILL_DETAIL, skill,
            palette.gradient(skill.displayName()));
        inventory.setItem(13, skillCard(player, skill));

        long xp = api.getTotalXp(player.getUniqueId(), skill);
        int level = api.getLevel(player.getUniqueId(), skill);
        XpCurve curve = settings.get().curve();
        SkillStage stage = SkillStage.forLevel(level, curve.maximumLevel());
        SkillStage nextStage = stage.next();

        inventory.setItem(20, metric(Material.EXPERIENCE_BOTTLE, "Exact XP", SkillsUi.exact(xp), palette.colorA()));
        inventory.setItem(22, metric(Material.NETHER_STAR, "Current Stage", stage.displayName(), stage.colorA()));
        String nextValue = level >= curve.maximumLevel() ? "Fully Mastered" : nextStage.displayName() + " • Lv. " + nextStage.unlockLevel(curve.maximumLevel());
        inventory.setItem(24, metric(Material.COMPASS, "Next Chapter", nextValue, nextStage.colorA()));

        inventory.setItem(29, SkillsUi.item(Material.MAP,
            "<!italic>" + palette.gradient("Progression Roadmap"),
            "<!italic><gray>See completed, current and future progression chapters.</gray>",
            "",
            "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to view progression.</gray>"));
        inventory.setItem(31, SkillsUi.item(Material.BLAZE_POWDER,
            "<!italic><gradient:#AB47BC:#EC407A><bold>Abilities</bold></gradient>",
            "<!italic><gray>Inspect the ability state for this skill.</gray>",
            "<!italic><dark_gray>The 2.0 ability runtime is being migrated on this draft branch.</dark_gray>",
            "",
            "<!italic><#B39DDB>Left Click</#B39DDB> <gray>to inspect abilities.</gray>"));
        inventory.setItem(33, SkillsUi.item(Material.TOTEM_OF_UNDYING,
            "<!italic><gradient:#FFD54F:#FFF176><bold>Leaderboard</bold></gradient>",
            "<!italic><gray>View the top ten and your current rank.</gray>",
            "",
            "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to open this leaderboard.</gray>"));
        addNavigation(inventory);
        player.openInventory(inventory);
    }

    private void openProgression(Player player, SkillType skill) {
        if (!ensureReady(player)) return;
        SkillPalette palette = SkillPalette.forSkill(skill);
        Inventory inventory = create(player, SkillsMenuHolder.Screen.PROGRESSION, skill,
            palette.gradient(skill.displayName() + " Progression"));
        int level = api.getLevel(player.getUniqueId(), skill);
        int maximum = settings.get().curve().maximumLevel();
        SkillStage current = SkillStage.forLevel(level, maximum);
        inventory.setItem(4, skillCard(player, skill));

        SkillStage[] stages = SkillStage.values();
        for (int i = 0; i < stages.length && i < ROADMAP_SLOTS.length; i++) {
            SkillStage stage = stages[i];
            int unlock = stage.unlockLevel(maximum);
            boolean unlocked = level >= unlock;
            boolean active = current == stage && level < maximum;
            String state;
            String stateColor;
            Material material;
            if (level >= maximum && stage == SkillStage.MASTERY) {
                state = "✦ FULLY MASTERED";
                stateColor = "#AEEA00";
                material = Material.NETHER_STAR;
            } else if (active) {
                state = "◆ CURRENT CHAPTER";
                stateColor = "#FFD740";
                material = Material.COMPASS;
            } else if (unlocked) {
                state = "✔ COMPLETED";
                stateColor = "#66BB6A";
                material = Material.LIME_DYE;
            } else {
                state = "✕ LOCKED";
                stateColor = "#EF5350";
                material = Material.GRAY_DYE;
            }
            inventory.setItem(ROADMAP_SLOTS[i], SkillsUi.item(material,
                "<!italic>" + stage.gradient(stage.displayName()),
                "<!italic><dark_gray>Progression chapter</dark_gray>",
                "",
                "<!italic><#FFD740>✥</#FFD740> <gray>Unlock Level</gray> <white>" + unlock + "</white>",
                "<!italic><gray>Status</gray> <" + stateColor + "><bold>" + state + "</bold></" + stateColor + ">"));
        }
        addNavigation(inventory);
        player.openInventory(inventory);
    }

    private void openAbilities(Player player, SkillType skill) {
        if (!ensureReady(player)) return;
        SkillPalette palette = SkillPalette.forSkill(skill);
        Inventory inventory = create(player, SkillsMenuHolder.Screen.ABILITIES, skill,
            "<gradient:#AB47BC:#EC407A><bold>" + skill.displayName() + " Abilities</bold></gradient>");
        inventory.setItem(13, skillCard(player, skill));
        inventory.setItem(22, SkillsUi.item(Material.BLAZE_POWDER,
            "<!italic><gradient:#AB47BC:#EC407A><bold>Ability Runtime Migration</bold></gradient>",
            "<!italic><dark_gray>Draft-branch status</dark_gray>",
            "",
            "<!italic><#FFD740>◆</#FFD740> <gray>The 1.0 candidate shipped abilities disabled.</gray>",
            "<!italic><#90CAF9>◆</#90CAF9> <gray>The 2.0 runtime will add real lock/ready/active/cooldown states.</gray>",
            "<!italic><#66BB6A>◆</#66BB6A> <gray>This screen already uses the safe 2.0 menu/session architecture.</gray>",
            "",
            "<!italic><gray>No fake ability state is shown before the runtime exists.</gray>"));
        addNavigation(inventory);
        player.openInventory(inventory);
    }

    private void openStatistics(Player player) {
        if (!ensureReady(player)) return;
        Inventory inventory = create(player, SkillsMenuHolder.Screen.STATISTICS, null,
            "<gradient:#90CAF9:#4DD0E1><bold>Skill Statistics</bold></gradient>");
        List<SkillType> enabled = settings.get().skills().enabledSkills();
        for (int i = 0; i < enabled.size() && i < SKILL_SLOTS.length; i++) {
            SkillType skill = enabled.get(i);
            SkillPalette palette = SkillPalette.forSkill(skill);
            long xp = api.getTotalXp(player.getUniqueId(), skill);
            int level = api.getLevel(player.getUniqueId(), skill);
            inventory.setItem(SKILL_SLOTS[i], SkillsUi.item(skill.icon(),
                SkillsUi.baseName(palette, skill.displayName(), "Lv. " + level),
                "<!italic><dark_gray>Exact loaded-profile statistics</dark_gray>",
                "",
                "<!italic><#FFD740>✥</#FFD740> <gray>Level</gray> <white>" + level + "</white>",
                "<!italic><#66BB6A>▰</#66BB6A> <gray>Total XP</gray> <white>" + SkillsUi.exact(xp) + "</white>"));
        }
        addNavigation(inventory);
        player.openInventory(inventory);
    }

    private void openHelp(Player player) {
        Inventory inventory = create(player, SkillsMenuHolder.Screen.HELP, null,
            "<gradient:#B39DDB:#E1BEE7><bold>Skills Help</bold></gradient>");
        inventory.setItem(20, SkillsUi.item(Material.COMPASS,
            "<!italic><#FFD54F><bold>Interaction Language</bold></#FFD54F>",
            "<!italic><#FFD54F>Left Click</#FFD54F> <gray>Primary action / progression.</gray>",
            "<!italic><#90CAF9>Right Click</#90CAF9> <gray>Details or secondary information.</gray>",
            "<!italic><gray>Back and Close always remain in stable bottom-row positions.</gray>"));
        inventory.setItem(22, SkillsUi.item(Material.EXPERIENCE_BOTTLE,
            "<!italic><#66BB6A><bold>Progress States</bold></#66BB6A>",
            "<!italic><#66BB6A>✔ COMPLETED</#66BB6A> <gray>chapter reached.</gray>",
            "<!italic><#FFD740>◆ CURRENT</#FFD740> <gray>your active progression chapter.</gray>",
            "<!italic><#EF5350>✕ LOCKED</#EF5350> <gray>future requirement not reached.</gray>",
            "<!italic><#AEEA00>✦ FULLY MASTERED</#AEEA00> <gray>terminal configured level reached.</gray>"));
        inventory.setItem(24, SkillsUi.item(Material.SHIELD,
            "<!italic><#90CAF9><bold>Data Safety</bold></#90CAF9>",
            "<!italic><gray>Menus read already-loaded profiles.</gray>",
            "<!italic><gray>Leaderboard reads run asynchronously.</gray>",
            "<!italic><gray>Stale async results are discarded when you leave the screen.</gray>"));
        addNavigation(inventory);
        player.openInventory(inventory);
    }

    private void openLeaderboard(Player player, SkillType skill) {
        if (!ensureReady(player)) return;
        SkillPalette palette = SkillPalette.forSkill(skill);
        Inventory inventory = create(player, SkillsMenuHolder.Screen.LEADERBOARD, skill,
            palette.gradient(skill.displayName() + " Leaderboard"));
        SkillsMenuHolder holder = (SkillsMenuHolder) inventory.getHolder();
        inventory.setItem(22, SkillsUi.item(Material.CLOCK,
            "<!italic><#FFD54F><bold>Loading leaderboard…</bold></#FFD54F>",
            "<!italic><gray>The database query is running off the main thread.</gray>"));
        addNavigation(inventory);
        player.openInventory(inventory);

        repository.top(skill, 10).thenCombine(repository.rank(skill, player.getUniqueId()), LeaderboardResult::new)
            .whenComplete((result, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (!isOpen(player, holder)) return;
                Inventory current = holder.getInventory();
                current.clear(22);
                if (error != null) {
                    current.setItem(22, SkillsUi.item(Material.BARRIER,
                        "<!italic><#EF5350><bold>Leaderboard unavailable</bold></#EF5350>",
                        "<!italic><gray>The asynchronous query failed.</gray>",
                        "<!italic><dark_gray>Use /skillsadmin diagnostics for server-side details.</dark_gray>"));
                    return;
                }
                List<SkillsRepository.LeaderboardEntry> rows = result.rows();
                if (rows.isEmpty()) {
                    current.setItem(22, SkillsUi.item(Material.PAPER,
                        "<!italic><gray><bold>No leaderboard entries yet</bold></gray>",
                        "<!italic><gray>Progress in this skill to create the first ranking entry.</gray>"));
                } else {
                    for (int i = 0; i < rows.size() && i < LEADERBOARD_SLOTS.length; i++) {
                        SkillsRepository.LeaderboardEntry row = rows.get(i);
                        String medal = i == 0 ? "#FFD740" : i == 1 ? "#CFD8DC" : i == 2 ? "#D7A86E" : "#90A4AE";
                        current.setItem(LEADERBOARD_SLOTS[i], SkillsUi.item(Material.PLAYER_HEAD,
                            "<!italic><" + medal + "><bold>#" + (i + 1) + "</bold></" + medal + "> <white>" + row.playerName() + "</white>",
                            "<!italic><#FFD740>✥</#FFD740> <gray>Level</gray> <white>" + row.level() + "</white>",
                            "<!italic><#66BB6A>▰</#66BB6A> <gray>Total XP</gray> <white>" + SkillsUi.exact(row.totalXp()) + "</white>"));
                    }
                }
                String ownRank = result.rank() <= 0 ? "Unranked" : "#" + result.rank();
                current.setItem(40, SkillsUi.item(Material.NAME_TAG,
                    "<!italic><gradient:#FFF176:#FF8F00><bold>Your Position</bold></gradient>",
                    "<!italic><gray>Current rank</gray> <white>" + ownRank + "</white>",
                    "<!italic><gray>Skill</gray> <white>" + skill.displayName() + "</white>"));
            }));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SkillsMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!holder.playerId().equals(player.getUniqueId()) || !isCurrent(holder)) return;
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) return;

        if (rawSlot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (rawSlot == BACK_SLOT && holder.screen() != SkillsMenuHolder.Screen.ROOT) {
            if (holder.screen() == SkillsMenuHolder.Screen.PROGRESSION
                || holder.screen() == SkillsMenuHolder.Screen.ABILITIES
                || holder.screen() == SkillsMenuHolder.Screen.LEADERBOARD) {
                openSkillDetail(player, holder.skill());
            } else {
                openRoot(player);
            }
            return;
        }

        switch (holder.screen()) {
            case ROOT -> handleRootClick(player, rawSlot);
            case SKILL_DETAIL -> handleDetailClick(player, holder.skill(), rawSlot);
            default -> { }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SkillsMenuHolder)) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkillsMenuHolder holder)) return;
        if (isCurrent(holder)) generations.remove(holder.playerId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        generations.remove(event.getPlayer().getUniqueId());
    }

    private void handleRootClick(Player player, int slot) {
        if (slot == 4) { openProfile(player); return; }
        if (slot == 47) { openStatistics(player); return; }
        if (slot == 48) { openHelp(player); return; }
        if (slot == 49) { openLeaderboard(player, highestSkill(player)); return; }
        SkillType skill = skillAtRootSlot(slot);
        if (skill != null) openSkillDetail(player, skill);
    }

    private void handleDetailClick(Player player, SkillType skill, int slot) {
        if (skill == null) return;
        if (slot == 29) openProgression(player, skill);
        else if (slot == 31) openAbilities(player, skill);
        else if (slot == 33) openLeaderboard(player, skill);
    }

    private Inventory create(Player player, SkillsMenuHolder.Screen screen, SkillType skill, String titleMarkup) {
        long generation = generations.merge(player.getUniqueId(), 1L, Long::sum);
        SkillsMenuHolder holder = new SkillsMenuHolder(player.getUniqueId(), generation, screen, skill);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, SkillsUi.mm(titleMarkup));
        holder.bind(inventory);
        return inventory;
    }

    private boolean ensureReady(Player player) {
        if (api.isProfileReady(player.getUniqueId())) return true;
        player.sendMessage(SkillsUi.mm("<yellow>Your skill profile is still loading.</yellow>"));
        return false;
    }

    private boolean isCurrent(SkillsMenuHolder holder) {
        Long current = generations.get(holder.playerId());
        return current != null && current == holder.generation();
    }

    private boolean isOpen(Player player, SkillsMenuHolder holder) {
        return player.isOnline()
            && isCurrent(holder)
            && player.getOpenInventory().getTopInventory().getHolder() == holder;
    }

    private SkillType skillAtRootSlot(int slot) {
        List<SkillType> enabled = settings.get().skills().enabledSkills();
        for (int i = 0; i < enabled.size() && i < SKILL_SLOTS.length; i++) {
            if (SKILL_SLOTS[i] == slot) return enabled.get(i);
        }
        return null;
    }

    private org.bukkit.inventory.ItemStack skillCard(Player player, SkillType skill) {
        RuntimeSettings runtime = settings.get();
        XpCurve curve = runtime.curve();
        long xp = api.getTotalXp(player.getUniqueId(), skill);
        int level = api.getLevel(player.getUniqueId(), skill);
        boolean mastered = level >= curve.maximumLevel();
        double progress = curve.progressWithinLevel(xp);
        SkillStage stage = SkillStage.forLevel(level, curve.maximumLevel());
        SkillPalette palette = SkillPalette.forSkill(skill);
        long required = curve.requiredForNext(level);
        long levelStart = curve.cumulativeForLevel(level);
        long current = mastered ? required : Math.max(0L, xp - levelStart);
        String progressValue = mastered
            ? "<#AEEA00><bold>FULLY MASTERED</bold></#AEEA00>"
            : "<" + SkillsUi.progressColor(progress) + ">" + SkillsUi.exact(current) + "</" + SkillsUi.progressColor(progress) + "><dark_gray>/</dark_gray><#B0BEC5>" + SkillsUi.exact(required) + "</#B0BEC5>";
        String status = mastered
            ? "<#AEEA00><bold>✦ FULLY MASTERED</bold></#AEEA00>"
            : "<" + stage.colorA() + "><bold>◆ " + stage.displayName().toUpperCase(java.util.Locale.ROOT) + "</bold></" + stage.colorA() + ">";

        return SkillsUi.item(skill.icon(),
            SkillsUi.baseName(palette, skill.displayName(), "Lv. " + level),
            "<!italic><dark_gray>" + stage.displayName() + " progression</dark_gray>",
            "",
            "<!italic><#FFD740>✥</#FFD740> <gray>Level</gray> <white>" + level + "</white>",
            "<!italic><#90CAF9>◆</#90CAF9> <gray>Stage</gray> <white>" + stage.displayName() + "</white>",
            "<!italic><#FFD54F>⚡</#FFD54F> <gray>Progress</gray> " + progressValue,
            SkillsUi.progressBar(progress, 12) + " <" + SkillsUi.progressColor(progress) + ">" + SkillsUi.percent(progress) + "</" + SkillsUi.progressColor(progress) + ">",
            "",
            "<!italic><gray>Status</gray> " + status,
            "<!italic><#FFD54F>Left Click</#FFD54F> <gray>to inspect this skill.</gray>"));
    }

    private org.bukkit.inventory.ItemStack metric(Material material, String label, String value, String accent) {
        return SkillsUi.item(material,
            "<!italic><" + accent + "><bold>" + label + "</bold></" + accent + ">",
            "<!italic><gray>" + label + "</gray> <white>" + value + "</white>");
    }

    private org.bukkit.inventory.ItemStack closeItem() {
        return SkillsUi.item(Material.BARRIER, SkillsUi.closeName(),
            "<!italic><gray>Close the PlexonSkills menu.</gray>");
    }

    private void addNavigation(Inventory inventory) {
        inventory.setItem(BACK_SLOT, SkillsUi.item(Material.ARROW, SkillsUi.backName(),
            "<!italic><gray>Return to the logical previous screen.</gray>"));
        inventory.setItem(CLOSE_SLOT, closeItem());
    }

    private long totalXp(Player player) {
        long total = 0L;
        for (SkillType skill : settings.get().skills().enabledSkills()) {
            long value = api.getTotalXp(player.getUniqueId(), skill);
            if (Long.MAX_VALUE - total < value) return Long.MAX_VALUE;
            total += value;
        }
        return total;
    }

    private SkillType highestSkill(Player player) {
        List<SkillType> enabled = settings.get().skills().enabledSkills();
        SkillType best = enabled.isEmpty() ? SkillType.MINING : enabled.getFirst();
        long bestXp = -1L;
        for (SkillType skill : enabled) {
            long xp = api.getTotalXp(player.getUniqueId(), skill);
            if (xp > bestXp) {
                bestXp = xp;
                best = skill;
            }
        }
        return best;
    }

    private int masteredCount(Player player) {
        int maximum = settings.get().curve().maximumLevel();
        int mastered = 0;
        for (SkillType skill : settings.get().skills().enabledSkills()) {
            if (api.getLevel(player.getUniqueId(), skill) >= maximum) mastered++;
        }
        return mastered;
    }

    private record LeaderboardResult(List<SkillsRepository.LeaderboardEntry> rows, int rank) { }
}
