package com.zpkdxgames.plexonskills.gui;

/**
 * Presentation stages for premium skill progression. Stage boundaries scale with the configured
 * maximum level so the UI never pretends level 100 is terminal when a server uses a larger cap.
 */
public enum SkillStage {
    NOVICE("Novice", "#8D8D8D", "#D6D6D6", 0.00),
    ADEPT("Adept", "#43A047", "#A5D6A7", 0.10),
    EXPERT("Expert", "#039BE5", "#80DEEA", 0.25),
    MASTER("Master", "#7E57C2", "#B39DDB", 0.50),
    ASCENDANT("Ascendant", "#FF8F00", "#FFD54F", 0.75),
    MASTERY("Mastery", "#FF6F00", "#FFF176", 0.90);

    private final String displayName;
    private final String colorA;
    private final String colorB;
    private final double threshold;

    SkillStage(String displayName, String colorA, String colorB, double threshold) {
        this.displayName = displayName;
        this.colorA = colorA;
        this.colorB = colorB;
        this.threshold = threshold;
    }

    public String displayName() { return displayName; }
    public String colorA() { return colorA; }
    public String colorB() { return colorB; }
    public double threshold() { return threshold; }

    public String gradient(String text) {
        return "<gradient:" + colorA + ":" + colorB + "><bold>" + text + "</bold></gradient>";
    }

    public int unlockLevel(int maximumLevel) {
        int max = Math.max(1, maximumLevel);
        if (this == NOVICE) return 1;
        return Math.max(1, Math.min(max, 1 + (int) Math.floor((max - 1) * threshold)));
    }

    public static SkillStage forLevel(int level, int maximumLevel) {
        int max = Math.max(1, maximumLevel);
        int safeLevel = Math.max(1, Math.min(level, max));
        if (safeLevel >= max) return MASTERY;
        double progress = max <= 1 ? 1.0 : (double) (safeLevel - 1) / (double) (max - 1);
        SkillStage selected = NOVICE;
        for (SkillStage stage : values()) {
            if (progress >= stage.threshold) selected = stage;
            else break;
        }
        return selected;
    }

    public SkillStage next() {
        int index = ordinal() + 1;
        return index >= values().length ? this : values()[index];
    }
}
