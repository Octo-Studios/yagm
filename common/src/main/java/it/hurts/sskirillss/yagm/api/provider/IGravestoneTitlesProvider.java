package it.hurts.sskirillss.yagm.api.provider;

import it.hurts.sskirillss.yagm.client.titles.renderer.GravestoneTitles;
import net.minecraft.core.Direction;

public interface IGravestoneTitlesProvider {

    GravestoneTitles getGravestoneTitles();

    GravestoneTitles setGravestoneTitles(GravestoneTitles titles);

    boolean shouldRenderTitles();

    Direction getTitleFacing();

    float getTitleBaseScale();

    float getTitleStartY();
}