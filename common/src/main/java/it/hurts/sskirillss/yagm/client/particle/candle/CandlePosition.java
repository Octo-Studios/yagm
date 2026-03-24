package it.hurts.sskirillss.yagm.client.particle.candle;

import net.minecraft.core.Direction;


public record CandlePosition(double pixelX, double pixelY, double pixelZ, double yOffset, boolean isSoulFire) {

    public double getBlockX() {
        return pixelX / 16.0;
    }

    public double getBlockY() {
        return (pixelY + yOffset) / 16.0;
    }

    public double getBlockZ() {
        return pixelZ / 16.0;
    }


    public CandlePosition rotateForFacing(Direction facing) {
        double centerX = 8.0;
        double centerZ = 8.0;

        double relX = pixelX - centerX;
        double relZ = pixelZ - centerZ;

        double rotatedX, rotatedZ;

        switch (facing) {
            case SOUTH -> {
                rotatedX = -relX;
                rotatedZ = -relZ;
            }
            case WEST -> {
                rotatedX = relZ;
                rotatedZ = -relX;
            }
            case EAST -> {
                rotatedX = -relZ;
                rotatedZ = relX;
            }
            default -> {
                rotatedX = relX;
                rotatedZ = relZ;
            }
        }

        return new CandlePosition(rotatedX + centerX, pixelY, rotatedZ + centerZ, yOffset, isSoulFire
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double pixelX = 8.0;
        private double pixelY = 0.0;
        private double pixelZ = 8.0;
        private double yOffset = 0.0;
        private boolean isSoulFire = false;


        public Builder x(double pixelX) {
            this.pixelX = pixelX;
            return this;
        }

        public Builder y(double pixelY) {
            this.pixelY = pixelY;
            return this;
        }


        public Builder z(double pixelZ) {
            this.pixelZ = pixelZ;
            return this;
        }


        public Builder at(double pixelX, double pixelY, double pixelZ) {
            this.pixelX = pixelX;
            this.pixelY = pixelY;
            this.pixelZ = pixelZ;
            return this;
        }


        public Builder offsetY(double pixels) {
            this.yOffset = pixels;
            return this;
        }


        public Builder soulFire(boolean isSoul) {
            this.isSoulFire = isSoul;
            return this;
        }


        public Builder soulFire() {
            return soulFire(true);
        }

        public CandlePosition build() {
            return new CandlePosition(pixelX, pixelY, pixelZ, yOffset, isSoulFire);
        }
    }
}