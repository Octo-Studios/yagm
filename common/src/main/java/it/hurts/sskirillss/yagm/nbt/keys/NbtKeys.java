package it.hurts.sskirillss.yagm.nbt.keys;

import it.hurts.sskirillss.yagm.YAGMCommon;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NbtKeys {

    public static final NbtKeys INSTANCE = new NbtKeys();

    // GraveData
    private final String ownerUuid = "OwnerUUID";
    private final String ownerName = "OwnerName";
    private final String graveId = "GraveId";
    private final String deathTime = "DeathTime";
    private final String deathCause = "DeathCause";
    private final String testament = "Testament";
    private final String graveLevel = "GraveLevel";
    private final String variantId = "VariantId";
    private final String sync = "Sync";

    // Inventory
    private final String inventoryData = "InventoryData";
    private final String mainInventory = "MainInventory";
    private final String armorInventory = "ArmorInventory";
    private final String offhandInventory = "OffhandInventory";
    private final String totalExperience = "TotalExperience";
    private final String accessories = "Accessories";
    private final String backpacks = "Backpacks";
    private final String droppedItems = "DroppedItems";
    private final String interrupted = "Interrupted";

    // Ghost entity
    private final String mood = "Mood";
    private final String tame = "Tame";
    private final String behaviorMode = "BehaviorMode";
    private final String feedCount = "FeedCount";
    private final String homePos = "HomePos";
    private final String breedLoveTicks = "BreedLoveTicks";
    private final String breedCooldownTicks = "BreedCooldownTicks";
    private final String ghostAge = "GhostAge";

    // GraveDataManager
    private final String graves = "Graves";

    // FallingGrave entity / GraveSaveManager wrapper
    private final String graveData = "GraveData";
    private final String facing = "Facing";
    private final String rotation = "Rotation";
    private final String rotationSpeed = "RotationSpeed";
    private final String lifetime = "Lifetime";
    private final String voidRecovery = "VoidRecovery";

    // Death position (InventoryUtils.savePlayerInventory)
    private final String deathPosX = "PosX";
    private final String deathPosY = "PosY";
    private final String deathPosZ = "PosZ";
    private final String dimension = "Dimension";

    // Legacy item-data format (GraveStoneBlockEntity.getItemData / loadGraveData)
    private final String playerId = "PlayerUuid";
    private final String playerName = "PlayerName";
    private final String id = "Id";

    // Other things
    public final String dataName = YAGMCommon.MODID + "_grave_saves";
    public final String saveTime = "SavedAt";
    public final String players = "Players";
    public final String player = "Player";
    public final String saves = "Saves";

    public String interrupted() {
        return interrupted;
    }
}


