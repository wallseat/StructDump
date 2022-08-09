package io.github.wallseat.structdump.selection;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.math.vector.Vector3d;

public class Area {
    private final ResourceKey worldKey;
    private Vector3d firstPos;
    private Vector3d secondPos;

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

        return new Area(
                new Vector3d(minX, minY, minZ),
                new Vector3d(maxX, maxY, maxZ),
                this.worldKey
        );
    }

    public boolean isFullFilled() {
        return firstPos != null && secondPos != null;
    }

    public ResourceKey getWorldKey() {
        return worldKey;
    }

    public Vector3d size() throws Exception {
        if (!isFullFilled())
            throw new Exception("Area is not full filled, can't get size!");

        return firstPos.sub(secondPos).abs();
    }

    public Vector3d getFirstPos() {
        return firstPos;
    }

    public void setFirstPos(Vector3d firstPos) {
        this.firstPos = firstPos;
    }

    public Vector3d getSecondPos() {
        return secondPos;
    }

    public void setSecondPos(Vector3d secondPos) {
        this.secondPos = secondPos;
    }

    @Override
    public String toString() {
        return "Area<from: (x:"
                + this.firstPos.floorX()
                + ", y: "
                + this.firstPos.floorY()
                + ", z: "
                + this.firstPos.floorZ()
                + "), to: (x: "
                + this.secondPos.floorX()
                + ", y: "
                + this.secondPos.floorY()
                + ", z: "
                + this.secondPos.floorZ() + ")>";
    }
}
