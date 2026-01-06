package it.hurts.sskirillss.yagm.api.item_valuator.providers;

/**
 * Determines a level based on a value.
 * Allows customization of level thresholds.
 *
 * @param <T> Level type (e.g., GraveStoneLevels)
 */
public interface ILevelDeterminer<T> {

    /**
     * Determines the level by value
     */
    T determine(double value);

    /**
     * Returns all available levels
     */
    T[] getAllLevels();

    /**
     * Returns the threshold for the given level
     */
    double getThreshold(T level);

    /**
     * Returns the default level
     */
    T getDefault();
}