package com.zpkdxgames.plexonskills.runtime;

import com.zpkdxgames.plexonskills.migration.MigrationMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MigrationModeTest {
    @Test void parsesModesCaseInsensitively() {
        assertEquals(MigrationMode.SHADOW, MigrationMode.parse("shadow"));
        assertEquals(MigrationMode.PRIMARY, MigrationMode.parse("PRIMARY"));
        assertThrows(IllegalArgumentException.class, () -> MigrationMode.parse("both"));
    }
}
