package me.yuhan8954.flashback.snapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SnapshotDelta {

    private SnapshotDelta() {}

    public static ReplaySnapshotDelta create(ReplaySnapshot previous, ReplaySnapshot current) {
        boolean dimensionChanged =
            previous.getDimensionId() != current.getDimensionId()
                || previous.getSeed() != current.getSeed();

        Map<Long, ReplayChunkSnapshot> previousChunks =
            indexChunks(previous.getChunks());
        Map<Long, ReplayChunkSnapshot> currentChunks =
            indexChunks(current.getChunks());

        List<ReplayChunkSnapshot> changedChunks = new ArrayList<>();
        if (dimensionChanged) {
            changedChunks.addAll(current.getChunks());
        } else {
            for (ReplayChunkSnapshot chunk : current.getChunks()) {
                ReplayChunkSnapshot previousChunk = previousChunks.get(
                    chunkKey(chunk.getChunkX(), chunk.getChunkZ()));
                if (previousChunk == null || !sameChunk(previousChunk, chunk)) {
                    changedChunks.add(chunk);
                }
            }
        }

        List<ReplayChunkPosition> removedChunks = new ArrayList<>();
        for (ReplayChunkSnapshot chunk : previous.getChunks()) {
            long key = chunkKey(chunk.getChunkX(), chunk.getChunkZ());
            if (dimensionChanged || !currentChunks.containsKey(key)) {
                removedChunks.add(new ReplayChunkPosition(
                    chunk.getChunkX(),
                    chunk.getChunkZ()));
            }
        }

        Map<Long, ReplayTileEntitySnapshot> previousTileEntities =
            indexTileEntities(previous.getTileEntities());
        Map<Long, ReplayTileEntitySnapshot> currentTileEntities =
            indexTileEntities(current.getTileEntities());

        List<ReplayTileEntitySnapshot> changedTileEntities = new ArrayList<>();
        if (dimensionChanged) {
            changedTileEntities.addAll(current.getTileEntities());
        } else {
            for (ReplayTileEntitySnapshot tileEntity : current.getTileEntities()) {
                ReplayTileEntitySnapshot previousTileEntity =
                    previousTileEntities.get(tileEntityKey(
                        tileEntity.getX(),
                        tileEntity.getY(),
                        tileEntity.getZ()));
                if (previousTileEntity == null
                    || !previousTileEntity.getNbt().equals(tileEntity.getNbt())) {
                    changedTileEntities.add(tileEntity);
                }
            }
        }

        List<ReplayBlockPosition> removedTileEntities = new ArrayList<>();
        for (ReplayTileEntitySnapshot tileEntity : previous.getTileEntities()) {
            long key = tileEntityKey(
                tileEntity.getX(),
                tileEntity.getY(),
                tileEntity.getZ());
            if (dimensionChanged || !currentTileEntities.containsKey(key)) {
                removedTileEntities.add(new ReplayBlockPosition(
                    tileEntity.getX(),
                    tileEntity.getY(),
                    tileEntity.getZ()));
            }
        }

        Map<Integer, ReplayEntitySnapshot> previousEntities =
            indexEntities(previous.getEntities());
        Map<Integer, ReplayEntitySnapshot> currentEntities =
            indexEntities(current.getEntities());

        List<ReplayEntitySnapshot> changedEntities = new ArrayList<>();
        if (dimensionChanged) {
            changedEntities.addAll(current.getEntities());
        } else {
            for (ReplayEntitySnapshot entity : current.getEntities()) {
                ReplayEntitySnapshot previousEntity =
                    previousEntities.get(entity.getEntityId());
                if (previousEntity == null || !sameEntity(previousEntity, entity)) {
                    changedEntities.add(entity);
                }
            }
        }

        int removedEntityCount = 0;
        for (ReplayEntitySnapshot entity : previous.getEntities()) {
            if (dimensionChanged || !currentEntities.containsKey(entity.getEntityId())) {
                removedEntityCount++;
            }
        }

        int[] removedEntityIds = new int[removedEntityCount];
        int removedEntityIndex = 0;
        for (ReplayEntitySnapshot entity : previous.getEntities()) {
            if (dimensionChanged || !currentEntities.containsKey(entity.getEntityId())) {
                removedEntityIds[removedEntityIndex++] = entity.getEntityId();
            }
        }

        ReplaySnapshot changedSnapshot = new ReplaySnapshot(
            current.getDimensionId(),
            current.getSeed(),
            current.getWorldTime(),
            current.getTotalWorldTime(),
            current.getRaining(),
            current.getThundering(),
            current.getRainStrength(),
            current.getThunderStrength(),
            current.getPlayer(),
            changedChunks,
            changedTileEntities,
            changedEntities);

        return new ReplaySnapshotDelta(
            changedSnapshot,
            removedChunks,
            removedTileEntities,
            removedEntityIds);
    }

    public static ReplaySnapshot apply(
        ReplaySnapshot snapshot,
        ReplaySnapshotDelta delta
    ) {
        Map<Long, ReplayChunkSnapshot> chunks =
            new LinkedHashMap<>(expectedCapacity(snapshot.getChunks().size()));
        for (ReplayChunkSnapshot chunk : snapshot.getChunks()) {
            chunks.put(chunkKey(chunk.getChunkX(), chunk.getChunkZ()), chunk);
        }
        for (ReplayChunkPosition position : delta.getRemovedChunks()) {
            chunks.remove(chunkKey(position.getChunkX(), position.getChunkZ()));
        }
        for (ReplayChunkSnapshot chunk : delta.getSnapshot().getChunks()) {
            chunks.put(chunkKey(chunk.getChunkX(), chunk.getChunkZ()), chunk);
        }

        Map<Long, ReplayTileEntitySnapshot> tileEntities =
            new LinkedHashMap<>(expectedCapacity(snapshot.getTileEntities().size()));
        for (ReplayTileEntitySnapshot tileEntity : snapshot.getTileEntities()) {
            tileEntities.put(
                tileEntityKey(tileEntity.getX(), tileEntity.getY(), tileEntity.getZ()),
                tileEntity);
        }
        for (ReplayBlockPosition position : delta.getRemovedTileEntities()) {
            tileEntities.remove(tileEntityKey(
                position.getX(),
                position.getY(),
                position.getZ()));
        }
        for (ReplayTileEntitySnapshot tileEntity : delta.getSnapshot().getTileEntities()) {
            tileEntities.put(
                tileEntityKey(tileEntity.getX(), tileEntity.getY(), tileEntity.getZ()),
                tileEntity);
        }

        Map<Integer, ReplayEntitySnapshot> entities =
            new LinkedHashMap<>(expectedCapacity(snapshot.getEntities().size()));
        for (ReplayEntitySnapshot entity : snapshot.getEntities()) {
            entities.put(entity.getEntityId(), entity);
        }
        for (int entityId : delta.getRemovedEntityIds()) {
            entities.remove(entityId);
        }
        for (ReplayEntitySnapshot entity : delta.getSnapshot().getEntities()) {
            entities.put(entity.getEntityId(), entity);
        }

        ReplaySnapshot state = delta.getSnapshot();
        return new ReplaySnapshot(
            state.getDimensionId(),
            state.getSeed(),
            state.getWorldTime(),
            state.getTotalWorldTime(),
            state.getRaining(),
            state.getThundering(),
            state.getRainStrength(),
            state.getThunderStrength(),
            state.getPlayer(),
            new ArrayList<>(chunks.values()),
            new ArrayList<>(tileEntities.values()),
            new ArrayList<>(entities.values()));
    }

    private static Map<Long, ReplayChunkSnapshot> indexChunks(
        List<ReplayChunkSnapshot> chunks
    ) {
        Map<Long, ReplayChunkSnapshot> result =
            new HashMap<>(expectedCapacity(chunks.size()));
        for (ReplayChunkSnapshot chunk : chunks) {
            result.put(chunkKey(chunk.getChunkX(), chunk.getChunkZ()), chunk);
        }
        return result;
    }

    private static Map<Long, ReplayTileEntitySnapshot> indexTileEntities(
        List<ReplayTileEntitySnapshot> tileEntities
    ) {
        Map<Long, ReplayTileEntitySnapshot> result =
            new HashMap<>(expectedCapacity(tileEntities.size()));
        for (ReplayTileEntitySnapshot tileEntity : tileEntities) {
            result.put(
                tileEntityKey(tileEntity.getX(), tileEntity.getY(), tileEntity.getZ()),
                tileEntity);
        }
        return result;
    }

    private static Map<Integer, ReplayEntitySnapshot> indexEntities(
        List<ReplayEntitySnapshot> entities
    ) {
        Map<Integer, ReplayEntitySnapshot> result =
            new HashMap<>(expectedCapacity(entities.size()));
        for (ReplayEntitySnapshot entity : entities) {
            result.put(entity.getEntityId(), entity);
        }
        return result;
    }

    private static boolean sameChunk(
        ReplayChunkSnapshot first,
        ReplayChunkSnapshot second
    ) {
        return Arrays.equals(first.getBlockIds(), second.getBlockIds())
            && Arrays.equals(first.getMetadata(), second.getMetadata())
            && Arrays.equals(first.getBiomes(), second.getBiomes());
    }

    private static boolean sameEntity(
        ReplayEntitySnapshot first,
        ReplayEntitySnapshot second
    ) {
        return equalsNullable(first.getEntityType(), second.getEntityType())
            && first.getEntityClass().equals(second.getEntityClass())
            && equalsNullable(first.getPlayerProfileId(), second.getPlayerProfileId())
            && equalsNullable(first.getPlayerProfileName(), second.getPlayerProfileName())
            && first.getServerPosX() == second.getServerPosX()
            && first.getServerPosY() == second.getServerPosY()
            && first.getServerPosZ() == second.getServerPosZ()
            && Double.compare(first.getX(), second.getX()) == 0
            && Double.compare(first.getY(), second.getY()) == 0
            && Double.compare(first.getZ(), second.getZ()) == 0
            && Float.compare(first.getYaw(), second.getYaw()) == 0
            && Float.compare(first.getPitch(), second.getPitch()) == 0
            && Double.compare(first.getMotionX(), second.getMotionX()) == 0
            && Double.compare(first.getMotionY(), second.getMotionY()) == 0
            && Double.compare(first.getMotionZ(), second.getMotionZ()) == 0
            && first.getNbt().equals(second.getNbt());
    }

    private static boolean equalsNullable(Object first, Object second) {
        return first == second || (first != null && first.equals(second));
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ ((long) chunkZ & 0xffffffffL);
    }

    private static long tileEntityKey(int x, int y, int z) {
        return ((long) x & 0x3ffffffL) << 38
            ^ ((long) z & 0x3ffffffL) << 12
            ^ ((long) y & 0xfffL);
    }

    private static int expectedCapacity(int size) {
        if (size < 3) {
            return size + 1;
        }
        if (size < 1 << 30) {
            return (int) (size / 0.75f) + 1;
        }
        return Integer.MAX_VALUE;
    }
}
