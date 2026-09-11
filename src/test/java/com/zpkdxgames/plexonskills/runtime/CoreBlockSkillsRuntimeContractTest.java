package com.zpkdxgames.plexonskills.runtime;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreBlockSkillsRuntimeContractTest {
    @Test
    void blockSubscriptionRequestsOneSharedOriginResolution() {
        var subscription = CoreBlockSkillsRuntime.blockSubscription(Set.of(Material.STONE, Material.WHEAT));

        assertTrue(subscription.requiresNaturalOrigin());
        assertEquals(Set.of(Material.STONE, Material.WHEAT), subscription.materials());
    }
}
