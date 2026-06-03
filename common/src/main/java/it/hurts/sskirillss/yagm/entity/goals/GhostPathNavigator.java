package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;

public class GhostPathNavigator extends FlyingPathNavigation {

    private final GhostEntity ghost;

    public GhostPathNavigator(GhostEntity ghost, Level level) {
        super(ghost, level);
        this.ghost = ghost;
    }

    @Override
    public boolean moveTo(Entity entity, double speed) {
        ghost.getMoveControl().setWantedPosition(entity.getX(), entity.getY(), entity.getZ(), speed);
        return true;
    }

    @Override
    public boolean moveTo(double x, double y, double z, double speed) {
        ghost.getMoveControl().setWantedPosition(x, y, z, speed);
        return true;
    }
}
