package com.zpkdxgames.plexonskills;

import com.zpkdxgames.plexoncore.api.PlexonCoreAPI;
import com.zpkdxgames.plexoncore.module.ModuleRegistry;
import com.zpkdxgames.plexoncore.persistence.SqliteService;
import com.zpkdxgames.plexonskills.ability.AbilityRuntime;
import com.zpkdxgames.plexonskills.api.PlexonSkillsAPI;
import com.zpkdxgames.plexonskills.api.PlexonSkillsApiImpl;
import com.zpkdxgames.plexonskills.command.SkillsAdminCommand;
import com.zpkdxgames.plexonskills.command.SkillsCommand;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.diagnostics.SkillsDiagnostics;
import com.zpkdxgames.plexonskills.gui.AbilityMenuBridge;
import com.zpkdxgames.plexonskills.gui.MilestoneMenuBridge;
import com.zpkdxgames.plexonskills.gui.SkillsMenu;
import com.zpkdxgames.plexonskills.migration.McMmoMigrationService;
import com.zpkdxgames.plexonskills.persistence.SkillsRepository;
import com.zpkdxgames.plexonskills.placeholder.SkillsPlaceholderExpansion;
import com.zpkdxgames.plexonskills.player.PlayerSkillsService;
import com.zpkdxgames.plexonskills.runtime.CoreBlockSkillsRuntime;
import com.zpkdxgames.plexonskills.runtime.GameplayListener;
import com.zpkdxgames.plexonskills.runtime.ProgressFeedback;
import com.zpkdxgames.plexonskills.skill.SkillProgressionService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class PlexonSkillsPlugin extends JavaPlugin {
    private final AtomicReference<RuntimeSettings> runtime = new AtomicReference<>();
    private final AtomicLong epoch = new AtomicLong();
    private final SkillsDiagnostics diagnostics = new SkillsDiagnostics();
    private PlexonCoreAPI core;
    private SkillsRepository repository;
    private PlayerSkillsService players;
    private SkillProgressionService progression;
    private ProgressFeedback feedback;
    private AbilityRuntime abilities;
    private CoreBlockSkillsRuntime blockRuntime;
    private PlexonSkillsApiImpl api;
    private McMmoMigrationService migration;
    private BukkitTask flushTask;

    @Override
    public void onEnable() {
        try {
            saveBundledResources();
            resolveCore();
            registerModule(ModuleRegistry.ModuleState.STARTING, "Initializing PlexonSkills");
            RuntimeSettings initial = RuntimeSettings.load(this, epoch.incrementAndGet());
            runtime.set(initial);

            SqliteService.SqliteDatabase database = core.persistence().open(getDataFolder().toPath().resolve("skills.db"));
            repository = new SkillsRepository(database);
            repository.initialize().get(10, TimeUnit.SECONDS);

            players = new PlayerSkillsService(this, repository, runtime::get, diagnostics);
            feedback = new ProgressFeedback(this, runtime::get);
            abilities = new AbilityRuntime(this, runtime::get, diagnostics);
            progression = new SkillProgressionService(players, runtime::get, diagnostics, feedback, abilities);
            api = new PlexonSkillsApiImpl(players, progression, runtime::get);
            migration = new McMmoMigrationService(this, core, repository);
            blockRuntime = new CoreBlockSkillsRuntime(core, runtime::get, progression, diagnostics);

            Bukkit.getServicesManager().register(PlexonSkillsAPI.class, api, this, ServicePriority.Normal);
            feedback.start();
            abilities.start();
            blockRuntime.rebuild();

            GameplayListener gameplay = new GameplayListener(runtime::get, progression, players, diagnostics);
            SkillsMenu menu = new SkillsMenu(this, api, runtime::get, repository);
            AbilityMenuBridge abilityMenu = new AbilityMenuBridge(api, abilities);
            MilestoneMenuBridge milestoneMenu = new MilestoneMenuBridge(api, runtime::get);
            Bukkit.getPluginManager().registerEvents(gameplay, this);
            Bukkit.getPluginManager().registerEvents(abilities, this);
            Bukkit.getPluginManager().registerEvents(menu, this);
            Bukkit.getPluginManager().registerEvents(abilityMenu, this);
            Bukkit.getPluginManager().registerEvents(milestoneMenu, this);

            SkillsCommand skillsCommand = new SkillsCommand(this, api, runtime::get, repository, menu);
            getCommand("skills").setExecutor(skillsCommand);
            getCommand("skills").setTabCompleter(skillsCommand);
            SkillsAdminCommand adminCommand = new SkillsAdminCommand(this, migration);
            getCommand("skillsadmin").setExecutor(adminCommand);
            getCommand("skillsadmin").setTabCompleter(adminCommand);

            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                new SkillsPlaceholderExpansion(api, runtime::get, abilities, getPluginMeta().getVersion()).register();
            }

            scheduleFlush();
            for (var player : Bukkit.getOnlinePlayers()) players.load(player);
            core.modules().updateState("skills", ModuleRegistry.ModuleState.READY,
                "Core API " + core.version().apiVersion() + ", mode=" + runtime.get().migrationMode());
            getLogger().info("PlexonSkills " + getPluginMeta().getVersion() + " enabled on Core API " + core.version().apiVersion() + " in " + runtime.get().migrationMode() + " mode.");
        } catch (Throwable failure) {
            getLogger().log(java.util.logging.Level.SEVERE, "PlexonSkills failed to initialize safely", failure);
            if (core != null) core.modules().updateState("skills", ModuleRegistry.ModuleState.FAILED, failure.getClass().getSimpleName() + ": " + failure.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (flushTask != null) { flushTask.cancel(); flushTask = null; }
        if (blockRuntime != null) blockRuntime.close();
        if (abilities != null) abilities.close();
        if (feedback != null) feedback.close();
        if (players != null && runtime.get() != null) players.shutdown(Duration.ofSeconds(runtime.get().shutdownTimeoutSeconds()));
        if (repository != null) repository.close();
        Bukkit.getServicesManager().unregisterAll(this);
        if (core != null) {
            core.modules().updateState("skills", ModuleRegistry.ModuleState.DISABLED, "Plugin disabled");
            core.modules().unregister("skills");
        }
    }

    public boolean reloadRuntime() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("reloadRuntime must run on primary thread");
        RuntimeSettings previous = runtime.get();
        try {
            RuntimeSettings candidate = RuntimeSettings.load(this, epoch.incrementAndGet());
            runtime.set(candidate);
            blockRuntime.rebuild();
            feedback.start();
            scheduleFlush();
            core.modules().updateState("skills", ModuleRegistry.ModuleState.READY, "Reloaded epoch " + candidate.epoch() + ", mode=" + candidate.migrationMode());
            return true;
        } catch (Throwable error) {
            runtime.set(previous);
            getLogger().warning("Runtime reload rejected: " + error.getMessage());
            return false;
        }
    }

    public void sendDiagnostics(CommandSender sender) {
        var d = diagnostics.snapshot();
        var eventMetrics = core.events().metrics();
        var origin = core.blockOrigins().stats();
        sender.sendMessage(Component.text("PlexonSkills diagnostics"));
        sender.sendMessage(Component.text("Core: plugin=" + core.version().pluginVersion() + " api=" + core.version().apiVersion() + " module=" + core.modules().find("skills").map(x -> x.state().name()).orElse("missing")));
        sender.sendMessage(Component.text("Runtime: epoch=" + runtime.get().epoch() + " mode=" + runtime.get().migrationMode() + " block-routes=" + runtime.get().skills().subscribedMaterials().size()));
        sender.sendMessage(Component.text("Profiles: loaded=" + players.loadedCount() + " dirty=" + players.dirtyCount()));
        sender.sendMessage(Component.text("XP: grants=" + d.xpGrants() + " rejected=" + d.xpRejected() + " shadow=" + d.shadowContributions() + " levelups=" + d.levelUps()));
        sender.sendMessage(Component.text("Abilities: active-players=" + abilities.activePlayers() + " activated=" + d.abilityActivations() + " rejected=" + d.abilityRejected() + " ended=" + d.abilityEnded()));
        sender.sendMessage(Component.text("Milestones: reached=" + d.milestonesReached() + " templates=" + runtime.get().milestones().milestones(com.zpkdxgames.plexonskills.skill.SkillType.MINING).size()));
        sender.sendMessage(Component.text("Anti-exploit: origin-rejected=" + d.originRejected() + " profile-not-ready=" + d.profileNotReady()));
        sender.sendMessage(Component.text("Events: block=" + d.blockFacts() + " combat=" + d.combatFacts() + " fishing=" + d.fishingFacts() + " acrobatics=" + d.acrobaticsFacts()));
        sender.sendMessage(Component.text("Persistence: queue=" + repository.queuedWrites() + " health=" + repository.health().state() + " failures=" + d.persistenceFailures() + " flush-batches=" + d.flushBatches()));
        sender.sendMessage(Component.text("Core events: " + eventMetrics));
        sender.sendMessage(Component.text("Core origin: " + origin));
        var m = migration.status();
        sender.sendMessage(Component.text("Migration: " + m.state() + " source=" + m.source() + " exists=" + m.exists()));
    }

    public void backup(CommandSender sender) {
        Path destination = getDataFolder().toPath().resolve("backups").resolve("skills-" + System.currentTimeMillis() + ".db");
        core.scheduler().supplyIo(() -> {
            try { return repository.backup(destination); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        }).whenComplete((path, error) -> Bukkit.getScheduler().runTask(this, () -> {
            if (error != null) sender.sendMessage(Component.text("Backup failed: " + error.getMessage()));
            else sender.sendMessage(Component.text("Backup created: " + path));
        }));
    }

    public PlexonSkillsAPI api() { return api; }
    public RuntimeSettings runtime() { return runtime.get(); }
    public SkillsRepository repository() { return repository; }
    public PlexonCoreAPI core() { return core; }

    private void resolveCore() {
        RegisteredServiceProvider<PlexonCoreAPI> registration = Bukkit.getServicesManager().getRegistration(PlexonCoreAPI.class);
        if (registration == null) throw new IllegalStateException("PlexonCore API service is unavailable");
        core = registration.getProvider();
        if (!core.supportsApi(2, 0)) throw new IllegalStateException("PlexonSkills requires Core API 2.0; running " + core.version().apiVersion());
    }

    private void registerModule(ModuleRegistry.ModuleState state, String detail) {
        ModuleRegistry.RegistrationResult result = core.modules().register(new ModuleRegistry.ModuleDescriptor(
            "skills", "PlexonSkills", getName(), getPluginMeta().getVersion(), this,
            ModuleRegistry.ModuleVersionRange.parse(">=2.0 <3.0"),
            Set.of("skills", "progression", "active-abilities", "milestones", "block-break-consumer", "sqlite-persistence", "placeholderapi"),
            state, detail, Instant.now()));
        if (!result.success()) throw new IllegalStateException("Core module registration failed: " + result.message());
    }

    private void scheduleFlush() {
        if (flushTask != null) flushTask.cancel();
        long interval = runtime.get().flushIntervalTicks();
        flushTask = Bukkit.getScheduler().runTaskTimer(this, players::flushDirty, interval, interval);
    }

    private void saveBundledResources() {
        saveDefaultConfig();
        saveIfMissing("skills.yml");
        saveIfMissing("abilities.yml");
        saveIfMissing("rewards.yml");
        saveIfMissing("messages.yml");
    }

    private void saveIfMissing(String name) {
        if (!new java.io.File(getDataFolder(), name).exists()) saveResource(name, false);
    }
}
