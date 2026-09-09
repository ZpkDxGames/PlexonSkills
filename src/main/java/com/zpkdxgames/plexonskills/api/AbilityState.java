package com.zpkdxgames.plexonskills.api;

/** Stable public ability state exposed independently from the internal runtime implementation. */
public enum AbilityState {
    DISABLED,
    LOCKED,
    READY,
    ACTIVE,
    COOLDOWN
}
