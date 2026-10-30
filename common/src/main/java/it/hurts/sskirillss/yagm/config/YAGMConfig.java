package it.hurts.sskirillss.yagm.config;

import it.hurts.octostudios.octolib.module.config.annotation.Prop;
import it.hurts.octostudios.octolib.module.config.impl.OctoConfig;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class YAGMConfig implements OctoConfig {
    @Prop(comment = "The percentage of the player's experience that is stored in the gravestone. The rest is lost on death. Allowed values: 0 - 100")
    private double xpRetentionPercent = 100D;

    @Prop(comment = "If enabled, the gravestone stores only the experience that vanilla would drop on death (7 points per level, up to 100). The rest is lost, just like in vanilla. xpRetentionPercent is applied to the resulting amount")
    private boolean storeOnlyVanillaXp = false;

    @Prop(comment = """
            Items that the mod ignores completely: they are never put into a gravestone and are left to the vanilla death handling or to the mod that owns them.

            Each entry can be one of the following:
            - Item ID - matches exactly one item (e.g. minecraft:diamond)
            - Regex - matches every item whose ID fits the pattern (e.g. sift:.*)
            - Item tag with the # prefix - matches every item in the tag (e.g. #c:tools)""")
    private List<String> itemBlacklist = new ArrayList<>();

    @Prop(comment = "If enabled, only the owner of a gravestone is able to open or break it")
    private boolean ownerOnlyAccess = false;

    @Prop(comment = "The time in seconds after death during which only the owner can access the gravestone. After this time everyone can open it. Set to 0 to keep it owner-only forever. Requires ownerOnlyAccess to be enabled")
    private int ownerOnlyDurationSeconds = 0;

    @Prop(comment = "The maximum distance in blocks from the intended position in which the mod searches for a free cell to place a gravestone")
    private int supportSearchRadius = 16;

    @Prop(comment = """
            Should a support block be created under the gravestone when there is nothing solid to place it on.

            Allowed values:
            - true - a block of the dimension's soil or stone is created under the gravestone
            - false - gravestones are only placed on existing solid blocks. When there is none nearby (for example when dying in the void), the contents are dropped instead""")
    private boolean createSupportBlock = true;

    @Prop(comment = "Should the gravestone fly out of the player's death position and fall to the ground, or should it be placed immediately at the death position")
    private boolean fallingGraveEnabled = true;

    @Prop(comment = "The minimum distance in blocks that a falling gravestone flies away from the death position")
    private double fallingMinDistance = 3D;

    @Prop(comment = "The maximum distance in blocks that a falling gravestone flies away from the death position")
    private double fallingMaxDistance = 6D;

    @Prop(comment = """
            Defines how the tier of a gravestone is chosen.

            Allowed values:
            - AUTO - calculated from the player's inventory and experience
            - FIXED - always equals fixedTier
            - RANDOM - random value between randomTierMin and randomTierMax""")
    private TierMode tierMode = TierMode.AUTO;

    @Prop(comment = "The tier of all gravestones when tierMode is FIXED. Allowed values: 1 - 4")
    private int fixedTier = 1;

    @Prop(comment = "The lowest tier that can be chosen when tierMode is RANDOM. Allowed values: 1 - 4")
    private int randomTierMin = 1;

    @Prop(comment = "The highest tier that can be chosen when tierMode is RANDOM. Allowed values: 1 - 4")
    private int randomTierMax = 4;

    @Prop(comment = """
            The score a player has to reach to get a gravestone of a higher tier. Used when tierMode is AUTO.

            Entries by position:
            - 1st - score required for the 2nd tier
            - 2nd - score required for the 3rd tier
            - 3rd - score required for the 4th tier""")
    private List<Double> tierThresholds = new ArrayList<>(List.of(20D, 50D, 80D));

    @Prop(comment = "The multiplier of the player's experience level in the gravestone tier score. Used when tierMode is AUTO")
    private double experienceLevelWeight = 1D;

    @Prop(comment = """
            Defines pairs where the value is the score that a single item of the kind adds to the gravestone tier score. The first matching entry is used. Used when tierMode is AUTO.

            The key can be one of the following:
            - Item ID - matches exactly one item (e.g. minecraft:diamond)
            - Item tag with the # prefix - matches every item in the tag (e.g. #c:gems)""")
    private Map<String, Double> valuableItems = new LinkedHashMap<>() {{
        put("#c:ingots", 1.5D);
        put("#c:gems", 2.4D);
        put("#c:storage_blocks", 4.8D);
        put("#c:ores", 1.2D);
        put("#c:raw_materials", 1.2D);
        put("#c:rods", 1.8D);
        put("#c:alloys", 1.8D);
        put("#c:circuits", 2.4D);
        put("#c:dusts", 0.3D);
        put("#c:foods/golden", 4.8D);
        put("#c:tools", 0.6D);
        put("#c:armors", 0.6D);
        put("#c:blocks", 0.1D);
        put("#c:music_discs", 2.4D);
    }};

    @Prop(comment = "Should gravestones placed close to each other form cemeteries. Cemeteries are required for ghosts and fog to appear")
    private boolean cemeteriesEnabled = true;

    @Prop(comment = "The maximum distance in blocks between gravestones to be counted as parts of the same cemetery")
    private int cemeteryRadius = 48;

    @Prop(comment = "The minimum number of gravestones close to each other required to form a cemetery")
    private int cemeteryMinGraves = 10;

    @Prop(comment = "Should ghosts spawn at cemeteries at night")
    private boolean ghostSpawnEnabled = true;

    @Prop(comment = "The maximum number of untamed ghosts around a single cemetery")
    private int maxGhostsPerCemetery = 5;

    @Prop(comment = "The time in seconds between ghost spawns at the same cemetery")
    private int ghostSpawnCooldownSeconds = 60;

    @Prop(comment = "Should the Restore Key work. If disabled, it can not be used to restore a gravestone")
    private boolean restoreKeyEnabled = true;

    @Prop(comment = """
            The list of mod compatibilities that are enabled. Remove an entry to disable the compatibility with that mod.

            Allowed values:
            - accessories - Accessories
            - curios - Curios API
            - trinkets - Trinkets (Fabric only)
            - cosmeticarmorreworked - Cosmetic Armor Reworked
            - backpacked - Backpacked
            - travelersbackpack - Traveler's Backpack
            - twilightforest - The Twilight Forest""")
    private List<String> enabledCompats = new ArrayList<>(List.of("accessories", "curios", "trinkets", "cosmeticarmorreworked", "backpacked", "travelersbackpack", "twilightforest"));
}
