package it.hurts.sskirillss.yagm.client.overlays;

import it.hurts.sskirillss.yagm.api.compat.BaseAccessoryCompat;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackLoader;
import it.hurts.sskirillss.yagm.block.GraveStoneBlock;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.client.overlays.config.InventoryHudConfig;
import it.hurts.sskirillss.yagm.data.gravedata.GraveData;
import it.hurts.sskirillss.yagm.util.ArmorUtils;
import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
import it.hurts.sskirillss.yagm.util.TierUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class GraveInventoryHudRenderer {

    private static final NbtKeys KEYS = NbtKeys.INSTANCE;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());


    public static void onRenderHud(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.level == null || mc.player == null || !(mc.hitResult instanceof BlockHitResult hit)) {
            return;
        }

        GraveStoneBlockEntity grave = resolveTargetedGrave(mc, hit);
        if (grave == null) {
            return;
        }

        CompoundTag inventoryData = grave.getInventoryData();
        if (inventoryData == null || inventoryData.isEmpty()) {
            return;
        }

        render(graphics, mc.font, mc.level.registryAccess(), inventoryData, grave.getGraveData());
    }

    private static GraveStoneBlockEntity resolveTargetedGrave(Minecraft mc, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);

        if (!(state.getBlock() instanceof GraveStoneBlock)) {
            return null;
        }

        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            pos = pos.below();
        }

        if (mc.level.getBlockEntity(pos) instanceof GraveStoneBlockEntity grave) {
            return grave;
        }

        return null;
    }

    private static void render(GuiGraphics graphics, Font font, RegistryAccess registry, CompoundTag inventoryData, GraveData graveData) {
        InventoryHudConfig config = InventoryHudConfig.get();

        NonNullList<ItemStack> armor = ArmorUtils.parseArmor(registry, inventoryData);
        NonNullList<ItemStack> main = ArmorUtils.parseMainInventory(registry, inventoryData);
        NonNullList<ItemStack> offhand = ArmorUtils.parseOffHand(registry, inventoryData);

        List<ItemStack> cosmeticArmor = BaseAccessoryCompat.parseAccessories(registry, inventoryData, "CosmeticArmorReworked");
        List<ItemStack> backpacked = BackpackLoader.parseBackpacks(registry, inventoryData, "backpacked");
        List<ItemStack> accessories = BaseAccessoryCompat.parseAccessoriesOrElse(registry, inventoryData, "CosmeticArmorReworked");

        int cosmeticArmorRows = cosmeticArmor.isEmpty() ? 0 : (int) Math.ceil(cosmeticArmor.size() / (double) config.getAccessoriesPerRow());
        int backpackedRows = backpacked.isEmpty() ? 0 : (int) Math.ceil(backpacked.size() / (double) config.getAccessoriesPerRow());
        int accRows = accessories.isEmpty() ? 0 : (int) Math.ceil(accessories.size() / (double) config.getAccessoriesPerRow());
        int panelHeight = computePanelHeight(cosmeticArmorRows, backpackedRows, accRows, graveData, inventoryData);

        int panelX = graphics.guiWidth() - config.getPanelWidth() - config.getPad();

        int panelY = config.getPad();

        graphics.fillGradient(panelX, panelY, panelX + config.getPanelWidth(), panelY + panelHeight, config.getBackgroundColor(), config.getBackgroundColor());

        int contentX = panelX + config.getPad();

        int y = panelY + config.getPad();

        renderArmorColumn(graphics, font, armor, contentX - config.getSlot(), y);

        renderOffHand(graphics, font, offhand, contentX, y);

        renderInventoryGrid(graphics, font, main, contentX, y);

        y += 4 * config.getSlot();

        if (!cosmeticArmor.isEmpty()) {
            y += config.getSectionGap();
            y += 3;
            graphics.drawString(font, Component.translatable("hud.yagm.cosmetic_armor"), contentX, y, config.getLabelColor(), true);
            y += config.getLineHeight();
            y += renderAccessories(graphics, font, cosmeticArmor, contentX, y);
        }

        if (!backpacked.isEmpty()) {
            y += config.getSectionGap();
            y += 3;
            graphics.drawString(font, Component.translatable("hud.yagm.backpacked"), contentX, y, config.getLabelColor(), true);
            y += config.getLineHeight();
            y += renderAccessories(graphics, font, backpacked, contentX, y);
        }

        if (!accessories.isEmpty()) {
            y += config.getSectionGap();
            y += 3;
            graphics.drawString(font, Component.translatable("hud.yagm.accessories"), contentX, y, config.getLabelColor(), true);
            y += config.getLineHeight();
            y += renderAccessories(graphics, font, accessories, contentX, y);
        }

        y += config.getSectionGap();
        y += 3;
        renderPlayerData(graphics, font, graveData, inventoryData, contentX, y);
    }

    private static void renderArmorColumn(GuiGraphics graphics, Font font, NonNullList<ItemStack> armor, int x, int y) {
        InventoryHudConfig config = InventoryHudConfig.get();
        for (int row = 0; row < 4; row++) {
            renderSlot(graphics, font, armor.get(3 - row), x, y + row * config.getSlot());
        }
    }

    private static void renderOffHand(GuiGraphics graphics, Font font, NonNullList<ItemStack> offhand, int x, int y) {
        InventoryHudConfig config = InventoryHudConfig.get();
        if (!offhand.isEmpty() && !offhand.getFirst().isEmpty()) {
            renderSlot(graphics, font, offhand.getFirst(), x, y + 3 * config.getSlot() + config.getHotbarGap());
        }
    }

    private static void renderInventoryGrid(GuiGraphics graphics, Font font, NonNullList<ItemStack> main, int x, int y) {
        InventoryHudConfig config = InventoryHudConfig.get();
        int gridX = x + config.getSlot() + config.getArmorGap();

        for (int row = 0; row < 3; row++) {
            int rowY = y + row * config.getSlot();
            for (int col = 0; col < 9; col++) {
                renderSlot(graphics, font, main.get(9 + row * 9 + col), gridX + col * config.getSlot(), rowY);
            }
        }

        int hotbarY = y + 3 * config.getSlot() + config.getHotbarGap();
        for (int col = 0; col < 9; col++) {
            renderSlot(graphics, font, main.get(col), gridX + col * config.getSlot(), hotbarY);
        }
    }

    private static int renderAccessories(GuiGraphics graphics, Font font, List<ItemStack> accessories, int x, int y) {
        InventoryHudConfig config = InventoryHudConfig.get();
        for (int i = 0; i < accessories.size(); i++) {
            int row = i / config.getAccessoriesPerRow();
            int col = i % config.getAccessoriesPerRow();
            renderSlot(graphics, font, accessories.get(i), x + col * config.getSlot(), y + row * config.getSlot());
        }
        return (int) Math.ceil(accessories.size() / (double) config.getAccessoriesPerRow()) * config.getSlot();
    }

    private static void renderPlayerData(GuiGraphics graphics, Font font, GraveData graveData, CompoundTag inventoryData, int x, int y) {
        InventoryHudConfig config = InventoryHudConfig.get();
        if (graveData.getOwnerName() != null) {
            graphics.drawString(font, graveData.getOwnerName(), x, y, config.getNameColor(), true);
            y += config.getLineHeight();
        }

        String date = DATE_FMT.format(Instant.ofEpochMilli(graveData.getDeathTime()));
        graphics.drawString(font, date, x, y, config.getLabelColor(), true);
        y += config.getLineHeight();

        if (graveData.getDeathCause() != null) {
            graphics.drawString(font, graveData.getDeathCause().getString(), x, y, config.getCauseColor(), true);
            y = y + config.getLineHeight();
        }

        if (inventoryData.contains(KEYS.getTotalExperience())) {
            long xp = inventoryData.getLong(KEYS.getTotalExperience());
            if (xp > 0) {
                graphics.drawString(font, Component.translatable("hud.yagm.experience_lvl", xpToLevel(xp)), x, y, config.getLabelColor(), true);
            }
        }
    }

    private static void renderSlot(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
    }

    private static int computePanelHeight(int cosmeticArmorRows, int backpackedRows, int accRows, GraveData graveData, CompoundTag inventoryData) {
        InventoryHudConfig config = InventoryHudConfig.get();
        int height = config.getPad() + 3 * config.getSlot() + config.getHotbarGap() + config.getSlot();

        if (cosmeticArmorRows > 0) {
            height += config.getSectionGap() + 1;
            height += config.getLineHeight();
            height += cosmeticArmorRows * config.getSlot();
        }

        if (backpackedRows > 0) {
            height += config.getSectionGap() + 1;
            height += config.getLineHeight();
            height += backpackedRows * config.getSlot();
        }

        if (accRows > 0) {
            height += config.getSectionGap() + 1;
            height += config.getLineHeight();
            height += accRows * config.getSlot();
        }

        height += config.getSectionGap() + 1;
        height += countPlayerDataLines(graveData, inventoryData) * config.getLineHeight();
        height += config.getPad();

        return height;
    }

    private static int countPlayerDataLines(GraveData graveData, CompoundTag inventoryData) {
        int lines = 1;

        if (graveData.getOwnerName() != null) {
            lines++;
        }

        if (graveData.getDeathCause() != null) {
            lines++;
        }

        lines++;

        if (graveData.getVariantId() != null) {
            lines++;
        }

        if (inventoryData.contains(KEYS.getTotalExperience()) && inventoryData.getLong(KEYS.getTotalExperience()) > 0) {
            lines++;
        }

        return lines;
    }

    private static int xpToLevel(long totalXp) {
        int level = 0;
        while (totalXp >= TierUtils.getXpForLevel(level)) {
            totalXp -= TierUtils.getXpForLevel(level);
            level++;
        }
        return level;
    }
}
