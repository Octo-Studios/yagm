package it.hurts.sskirillss.yagm.util;

import net.minecraft.server.level.ServerPlayer;

public class TierUtils {

    public static long getXpForLevel(long level) {
        if (level < 0) return 0;
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 15) return 37 + (level - 15) * 5;
        return 7 + level * 2;
    }

    public static long getTotalXpFromLevelAndProgress(long level, double progress) {
        return getTotalXpToReachLevel(level) + (long) (progress * getXpForLevel(level));
    }

    private static long getTotalXpToReachLevel(long level) {
        if (level <= 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (5 * level * level - 81 * level + 720) / 2;
        return (9 * level * level - 325 * level + 4440) / 2;
    }

    public static void clearExperience(ServerPlayer player) {
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        player.totalExperience = 0;
    }

    static void addExperience(ServerPlayer player, long xpToAdd) {
        long current = getTotalXpFromLevelAndProgress(player.experienceLevel, player.experienceProgress);

        long total = Long.MAX_VALUE - current < xpToAdd ? Long.MAX_VALUE : current + xpToAdd;

        applyTotalExperience(player, total);
    }

    private static void applyTotalExperience(ServerPlayer player, long totalXp) {
        XpState state = decodeTotalExperience(totalXp);
        player.experienceLevel = state.level();
        player.experienceProgress = state.progress();
        player.totalExperience = (int) Math.min(Integer.MAX_VALUE, totalXp);
    }

    private static XpState decodeTotalExperience(long totalXp) {
        if (totalXp <= 0) {
            return new XpState(0, 0.0f);
        }

        long low = 0;
        long high = 1;

        while (high < Integer.MAX_VALUE && getTotalXpToReachLevel(high) <= totalXp) {
            long next = high << 1;
            high = next <= 0 || next > Integer.MAX_VALUE ? Integer.MAX_VALUE : next;
        }

        while (low < high) {
            long mid = (low + high + 1) >>> 1;
            if (getTotalXpToReachLevel(mid) <= totalXp) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }

        long level = Math.min(low, Integer.MAX_VALUE);

        long base = getTotalXpToReachLevel(level);

        long toNext = Math.max(1L, getXpForLevel(level));

        float progress = (float) Math.clamp((totalXp - base) / (double) toNext, 0.0, Math.nextDown(1.0f));

        return new XpState((int) level, progress);
    }

    private record XpState(int level, float progress) { }
}

