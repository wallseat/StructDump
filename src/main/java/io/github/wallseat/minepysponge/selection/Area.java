package io.github.wallseat.minepysponge.selection;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.world.WorldType;
import org.spongepowered.math.vector.Vector3d;

public class Area {
    private final ResourceKey worldKey;
    private Vector3d firstPos;
    private Vector3d secondPos;
    private boolean frozen;

    public Area(Vector3d firstPos, Vector3d secondPos, ResourceKey worldKey) {
        this.worldKey = worldKey;
        this.firstPos = firstPos;
        this.secondPos = secondPos;
    }


    public static Area fromFirstPos(Vector3d firstPos, ResourceKey worldKey) {
        return new Area(firstPos, null, worldKey);
    }

    public static Area fromSecondPos(Vector3d secondPos, ResourceKey worldKey) {
        return new Area(null, secondPos, worldKey);
    }

    public Area normalized() throws Exception {
        if (!isFullFilled()) throw new Exception("Area is not full filled to be normalized!");

        int minX, minY, minZ, maxX, maxY, maxZ;
        minX = Math.min(firstPos.floorX(), secondPos.floorX());
        minY = Math.min(firstPos.floorY(), secondPos.floorY());
        minZ = Math.min(firstPos.floorZ(), secondPos.floorZ());

        maxX = Math.max(firstPos.floorX(), secondPos.floorX());
        maxY = Math.max(firstPos.floorY(), secondPos.floorY());
        maxZ = Math.max(firstPos.floorZ(), secondPos.floorZ());

        return new Area(new Vector3d(minX, minY, minZ), new Vector3d(maxX, maxY, maxZ), this.worldKey).freeze();
    }

    public boolean isFullFilled() {
        return firstPos != null && secondPos != null;
    }

    public void setFirstPos(Vector3d firstPos) throws Exception {
        if (this.frozen) throw new Exception("Attempt to change position in frozen area!");
        this.firstPos = firstPos;
    }

    public void setSecondPos(Vector3d secondPos) throws Exception {
        if (this.frozen) throw new Exception("Attempt to change position in frozen area!");
        this.secondPos = secondPos;
    }

    public ResourceKey getWorldKey() {
        return worldKey;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public Area freeze() {
        this.frozen = true;
        return this;
    }

    public Vector3d size() throws Exception {
        if (!isFullFilled()) throw new Exception("Area is not full filled, can't get size!");
        return firstPos.sub(secondPos).abs();
    }

    public Vector3d getFirstPos() {
        return firstPos;
    }

    public Vector3d getSecondPos() {
        return  secondPos;
    }

}
