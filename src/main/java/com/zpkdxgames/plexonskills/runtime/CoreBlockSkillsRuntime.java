package com.zpkdxgames.plexonskills.runtime;

import com.zpkdxgames.plexoncore.api.PlexonCoreAPI;
import com.zpkdxgames.plexoncore.context.BlockOrigin;
import com.zpkdxgames.plexoncore.context.CoreBlockBreakContext;
import com.zpkdxgames.plexoncore.event.CoreBlockSubscription;
import com.zpkdxgames.plexonskills.config.AntiExploitPolicy;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.diagnostics.SkillsDiagnostics;
import com.zpkdxgames.plexonskills.skill.SkillDefinition;
import com.zpkdxgames.plexonskills.skill.SkillProgressionService;
import com.zpkdxgames.plexonskills.skill.SkillRegistry;
import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public final class CoreBlockSkillsRuntime implements AutoCloseable {
    private final PlexonCoreAPI core;
    private final Supplier<RuntimeSettings> settings;
    private final SkillProgressionService progression;
    private final SkillsDiagnostics diagnostics;
    private AutoCloseable subscription;

    public CoreBlockSkillsRuntime(PlexonCoreAPI core, Supplier<RuntimeSettings> settings, SkillProgressionService progression, SkillsDiagnostics diagnostics) {
        this.core = core;
        this.settings = settings;
        this.progression = progression;
        this.diagnostics = diagnostics;
    }

    public void rebuild() {
        closeSubscription();
        RuntimeSettings runtime = settings.get();
        if (runtime.skills().subscribedMaterials().isEmpty()) return;
        // Subscribe to all relevant origins and apply policy per routed skill. Farming intentionally
        // differs from natural-block skills and must not be pre-filtered as NATURAL by Core.
        CoreBlockSubscription spec = CoreBlockSubscription.builder()
            .materials(runtime.skills().subscribedMaterials())
            .requiresNaturalOrigin(false)
            .build();
        subscription = core.events().subscribeBlockBreak("plexonskills", spec, this::handle);
    }

    private void handle(CoreBlockBreakContext context) {
        diagnostics.blockFact();
        RuntimeSettings runtime = settings.get();
        SkillRegistry.BlockRoute route = runtime.skills().blockRoute(context.material());
        if (route == null) return;
        SkillDefinition definition = runtime.skills().definition(route.skill());
        if (definition == null || !definition.enabled()) return;

        if (route.skill() == SkillType.FARMING) {
            boolean mature = !definition.requireMatureCrop() || isMature(context);
            if (!mature || !farmingOriginAllowed(context.origin(), runtime.antiExploit().farming())) {
                diagnostics.originRejected();
                return;
            }
        } else if (definition.naturalOriginRequired() && !naturalOriginAllowed(context.origin(), runtime.rejectUnknownOrigin())) {
            diagnostics.originRejected();
            return;
        }

        Player player = Bukkit.getPlayer(context.playerId());
        if (player == null || !player.isOnline()) return;
        progression.grant(player, route.skill(), route.xp(), "CORE_BLOCK_BREAK:" + context.eventId());
    }

    static boolean naturalOriginAllowed(BlockOrigin origin, boolean rejectUnknown) {
        return switch (origin) {
            case NATURAL -> true;
            case PLAYER_PLACED -> false;
            case UNKNOWN -> !rejectUnknown;
        };
    }

    static boolean farmingOriginAllowed(BlockOrigin origin, AntiExploitPolicy.FarmingPolicy policy) {
        return switch (origin) {
            case NATURAL -> true;
            case PLAYER_PLACED -> policy.playerPlantedPolicy() == AntiExploitPolicy.PlantedCropPolicy.MATURE_ONLY;
            case UNKNOWN -> policy.allowUnknownOrigin();
        };
    }

    private boolean isMature(CoreBlockBreakContext context) {
        World world = Bukkit.getWorld(context.worldId());
        if (world == null) return false;
        Block block = world.getBlockAt(context.x(), context.y(), context.z());
        if (!(block.getBlockData() instanceof Ageable ageable)) return true;
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private void closeSubscription() {
        if (subscription != null) {
            try { subscription.close(); }
            catch (Exception ignored) { }
            subscription = null;
        }
    }

    @Override public void close() { closeSubscription(); }
}
