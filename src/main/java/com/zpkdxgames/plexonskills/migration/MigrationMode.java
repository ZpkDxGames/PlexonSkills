package com.zpkdxgames.plexonskills.migration;

import java.util.Locale;

public enum MigrationMode {
    DISABLED, SHADOW, PRIMARY;

    public static MigrationMode parse(String value) {
        try { return valueOf((value == null ? "SHADOW" : value).trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Unknown migration mode: " + value); }
    }
}
