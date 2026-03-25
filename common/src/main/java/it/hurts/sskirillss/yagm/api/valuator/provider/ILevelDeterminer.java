package it.hurts.sskirillss.yagm.api.valuator.provider;


public interface ILevelDeterminer<T> {


    T determine(double value);


    T[] getAllLevels();


    double getThreshold(T level);


    T getDefault();
}
