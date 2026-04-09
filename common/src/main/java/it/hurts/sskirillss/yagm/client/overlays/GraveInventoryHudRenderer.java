package it.hurts.sskirillss.yagm.client.overlays;

import it.hurts.sskirillss.yagm.api.compat.BaseAccessoryCompat;
import it.hurts.sskirillss.yagm.block.GraveStoneBlock;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.data.gravedata.GraveData;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import it.hurts.sskirillss.yagm.util.NbtKeys;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
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

    private static final int SLOT = 18;
    private static final int PAD = 8;
    private static final int ARMOR_GAP = 4;
    private static final int HOTBAR_GAP = 2;
    private static final int SECTION_GAP = 6;
    private static final int LINE_H = 10;

    private static final int PANEL_WIDTH = PAD + SLOT + ARMOR_GAP + 9 * SLOT + PAD;
    private static final int CONTENT_W = PANEL_WIDTH - PAD * 2;
    private static final int ACC_PER_ROW = CONTENT_W / SLOT;

    private static final int COLOR_BG = 0x00000000;
    private static final int COLOR_SEPARATOR = 0x40FFFFFF;
    private static final int COLOR_NAME = 0xFFFFFFFF;
    private static final int COLOR_LABEL = 0xFFAAAAAA;
    private static final int COLOR_CAUSE = 0xFFFF7070;

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
        NonNullList<ItemStack> armor = InventoryUtils.parseArmor(registry, inventoryData);
        NonNullList<ItemStack> main = InventoryUtils.parseMainInventory(registry, inventoryData);
        List<ItemStack> accessories = BaseAccessoryCompat.parseAccessories(registry, inventoryData);

        int accRows = accessories.isEmpty() ? 0 : (int) Math.ceil(accessories.size() / (double) ACC_PER_ROW);
        int panelHeight = computePanelHeight(accRows, graveData, inventoryData);

        int panelX = graphics.guiWidth() - PANEL_WIDTH - PAD;
        int panelY = PAD;

        graphics.fillGradient(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, COLOR_BG, COLOR_BG);

        int contentX = panelX + PAD;
        int y = panelY + PAD;

        renderArmorColumn(graphics, font, armor, contentX, y);
        renderInventoryGrid(graphics, font, main, contentX + SLOT + ARMOR_GAP, y);
        y += 3 * SLOT + HOTBAR_GAP + SLOT;

        if (!accessories.isEmpty()) {
            y += SECTION_GAP;
            y += 3;
            graphics.drawString(font, "Accessories:", contentX, y, COLOR_LABEL, true);
            y += LINE_H;
            y += renderAccessories(graphics, font, accessories, contentX, y);
        }

        y += SECTION_GAP;
        y += 3;
        renderPlayerData(graphics, font, graveData, inventoryData, contentX, y);
    }

    private static void renderArmorColumn(GuiGraphics graphics, Font font, NonNullList<ItemStack> armor, int x, int y) {
        for (int row = 0; row < 4; row++) {
            renderSlot(graphics, font, armor.get(3 - row), x, y + row * SLOT);
        }
    }

    private static void renderInventoryGrid(GuiGraphics graphics, Font font, NonNullList<ItemStack> main, int x, int y) {
        for (int row = 0; row < 3; row++) {
            int rowY = y + row * SLOT;
            for (int col = 0; col < 9; col++) {
                renderSlot(graphics, font, main.get(9 + row * 9 + col), x + col * SLOT, rowY);
            }
        }

        int hotbarY = y + 3 * SLOT + HOTBAR_GAP;
        for (int col = 0; col < 9; col++) {
            renderSlot(graphics, font, main.get(col), x + col * SLOT, hotbarY);
        }
    }

    private static int renderAccessories(GuiGraphics graphics, Font font, List<ItemStack> accessories, int x, int y) {
        for (int i = 0; i < accessories.size(); i++) {
            int row = i / ACC_PER_ROW;
            int col = i % ACC_PER_ROW;
            renderSlot(graphics, font, accessories.get(i), x + col * SLOT, y + row * SLOT);
        }

        return (int) Math.ceil(accessories.size() / (double) ACC_PER_ROW) * SLOT;
    }

    private static void renderPlayerData(GuiGraphics graphics, Font font, GraveData graveData, CompoundTag inventoryData, int x, int y) {
        if (graveData.getOwnerName() != null) {
            graphics.drawString(font, graveData.getOwnerName(), x, y, COLOR_NAME, true);
            y += LINE_H;
        }

        String date = DATE_FMT.format(Instant.ofEpochMilli(graveData.getDeathTime()));
        graphics.drawString(font, date, x, y, COLOR_LABEL, true);
        y += LINE_H;

        if (graveData.getDeathCause() != null) {
            graphics.drawString(font, graveData.getDeathCause().getString(), x, y, COLOR_CAUSE, true);
            y = y + LINE_H;
        }

        String levelLabel = "Level " + toRoman(graveData.getGraveLevel().ordinal() + 1);
        graphics.drawString(font, levelLabel, x, y, COLOR_LABEL, true);
        y = y + LINE_H;

        if (graveData.getVariantId() != null) {
            String path = graveData.getVariantId().getPath();
            String variant = Character.toUpperCase(path.charAt(0)) + path.substring(1);
            graphics.drawString(font, variant, x, y, COLOR_LABEL, true);
            y += LINE_H;
        }

        if (inventoryData.contains(KEYS.getTotalExperience())) {
            int xp = inventoryData.getInt(KEYS.getTotalExperience());
            if (xp > 0) {
                graphics.drawString(font, xp + " XP", x, y, COLOR_LABEL, true);
            }
        }
    }

    private static void renderSlot(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
    }

    private static void drawSeparator(GuiGraphics graphics, int panelX, int y) {
        graphics.fill(panelX + 4, y, panelX + PANEL_WIDTH - 4, y + 1, COLOR_SEPARATOR);
    }

    private static int computePanelHeight(int accRows, GraveData graveData, CompoundTag inventoryData) {
        int height = PAD + 3 * SLOT + HOTBAR_GAP + SLOT;

        if (accRows > 0) {
            height += SECTION_GAP + 1;
            height += LINE_H;
            height += accRows * SLOT;
        }

        height += SECTION_GAP + 1;
        height += countPlayerDataLines(graveData, inventoryData) * LINE_H;
        height += PAD;

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

        if (inventoryData.contains(KEYS.getTotalExperience()) && inventoryData.getInt(KEYS.getTotalExperience()) > 0) {
            lines++;
        }

        return lines;
    }

    private static String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> String.valueOf(number);
        };
    }
}