package it.hurts.sskirillss.yagm.api.compat.provider;

public interface IAccessoryHandler extends IAccessoryCollector, IAccessorySerializer, IAccessoryEquipper, IAccessoryRestorer {


    boolean isModLoaded();


    String getModName();
}
