package ie.rotomic.hytale.mineinvein.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.*;
import com.hypixel.hytale.server.core.asset.type.item.config.container.ItemDropContainer;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import ie.rotomic.hytale.mineinvein.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;

public class BreakBlockSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private final HytaleLogger logger;
    private static final int MAX_BLOCKS = 64;
    private static final int[][] NEIGHBOR_OFFSETS = generateNeighborOffsets();

    public BreakBlockSystem() {
        super(BreakBlockEvent.class);
        logger = Entry.getInstance().getLogger();
    }

    @Override
    public void handle(int index, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store,
                       @NotNull CommandBuffer<EntityStore> commandBuffer, @NotNull BreakBlockEvent event) {
        Ref<EntityStore> entityStoreRef = archetypeChunk.getReferenceTo(index);

        Player player = store.getComponent(entityStoreRef, Player.getComponentType());
        if (player == null) return;

        // Get movement states to check if crouching
        MovementStatesComponent movementStatesComponent = store.getComponent(entityStoreRef, MovementStatesComponent.getComponentType());
        if (movementStatesComponent == null) return;

        MovementStates movementStates = movementStatesComponent.getMovementStates();
        if (!movementStates.crouching) return; // Only vein mine if crouching

        World world = player.getWorld();
        if (world == null) return;

        BlockType targetType = event.getBlockType();
        if (!isOre(targetType)) return;

//        player.sendMessage(Message.raw("YOU MINED: %s".formatted(targetType.getId())));

        Vector3i startPos = event.getTargetBlock();
        List<Vector3i> blocksToBreak = floodFillVein(world, startPos, targetType);

        Inventory inventory = player.getInventory();
        ItemContainer hotbar = inventory.getHotbar();
        ItemStack heldItem = inventory.getItemInHand();

        if(heldItem == null) return;
        if(!isPickaxe(heldItem.getItemId())) return;

        // Trim blocks to break based on available durability
        double availableDurability = heldItem.getDurability();
        if (availableDurability < blocksToBreak.size()) {
            blocksToBreak = blocksToBreak.subList(0, (int) availableDurability);
        }

        // Skip if no durability left or item is broken
        if (blocksToBreak.isEmpty() || heldItem.isBroken()) return;

        if (!blocksToBreak.isEmpty()) {
//            player.sendMessage(Message.raw("Vein mining " + blocksToBreak.size() + " additional blocks!"));
            hotbar.setItemStackForSlot(inventory.getActiveSlot(Inventory.HOTBAR_SECTION_ID),
                    heldItem.withIncreasedDurability(-blocksToBreak.size()));


            List<Vector3i> finalBlocksToBreak = blocksToBreak;
            commandBuffer.run(s -> {
                // Get the drop list from the block type
                if (targetType.getGathering().getBreaking().getDropListId() != null) {

                    ItemDropList dropList = ItemDropList.getAssetMap().getAsset(targetType.getGathering().getBreaking().getDropListId());
                    assert dropList != null;
                    ItemDropContainer container = dropList.getContainer();

                    if (container != null) {
                        List<ItemDrop> drops = new ObjectArrayList<>();
                        container.populateDrops(drops, () -> 1.0, dropList.getId());

                        int blocksProcessed = 0;
                        for (Vector3i pos : finalBlocksToBreak) {
                            world.setBlock(pos.x, pos.y, pos.z, BlockType.EMPTY_KEY);

                            // Skip drops for the first block since it was already mined by the player
                            if (blocksProcessed > 0) {
                                // Drop all items from the drop list
                                for (ItemDrop drop : drops) {
                                    if (drop != null && drop.getItemId() != null) {
                                        ItemStack itemStack = new ItemStack(
                                                drop.getItemId(),
                                                1,
                                                drop.getMetadata()
                                        );
                                        ItemUtils.dropItem(entityStoreRef, itemStack, s);
                                    }
                                }
                            }
                            blocksProcessed++;
                        }
                    }
                } else {
                    logger.at(Level.WARNING).log("No list ID for target type " + targetType.getId());
                }
            });
        }
    }

    private static int[][] generateNeighborOffsets() {
        List<int[]> offsets = new ArrayList<>(26);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    offsets.add(new int[]{x, y, z});
                }
            }
        }
        return offsets.toArray(new int[0][]);
    }

    private List<Vector3i> floodFillVein(World world, Vector3i start, BlockType targetType) {
        List<Vector3i> result = new ArrayList<>(MAX_BLOCKS);
        Queue<Vector3i> queue = new ArrayDeque<>(MAX_BLOCKS);
        Set<Long> visited = new HashSet<>(MAX_BLOCKS * 2); // Pre-size to reduce rehashing

        String targetId = targetType.getId();
        int startX = start.x;
        int startY = start.y;
        int startZ = start.z;

        // Add starting neighbors - check and add in one pass
        for (int[] offset : NEIGHBOR_OFFSETS) {
            int nx = startX + offset[0];
            int ny = startY + offset[1];
            int nz = startZ + offset[2];
            long hash = positionHash(nx, ny, nz);

            if (visited.add(hash)) {
                BlockType blockType = world.getBlockType(nx, ny, nz);

                if (blockType != null && targetId.equals(blockType.getId())) {
                    Vector3i neighbor = new Vector3i(nx, ny, nz);
                    queue.add(neighbor);
                    result.add(neighbor);
                }
            }
        }

        // BFS flood fill
        while (!queue.isEmpty() && result.size() < MAX_BLOCKS) {
            Vector3i current = queue.poll();
            int cx = current.x;
            int cy = current.y;
            int cz = current.z;

            for (int[] offset : NEIGHBOR_OFFSETS) {
                int nx = cx + offset[0];
                int ny = cy + offset[1];
                int nz = cz + offset[2];
                long hash = positionHash(nx, ny, nz);

                if (visited.add(hash)) { // Combine check and add
                    BlockType blockType = world.getBlockType(nx, ny, nz);

                    if (blockType != null && targetId.equals(blockType.getId())) {
                        Vector3i neighbor = new Vector3i(nx, ny, nz);
                        queue.add(neighbor);
                        result.add(neighbor);

                        if (result.size() >= MAX_BLOCKS) return result;
                    }
                }
            }
        }

        return result;
    }

    // Overloaded for primitives - avoids Vector3i allocation during hashing
    private long positionHash(int x, int y, int z) {
        // Use proper bit masking and shifting for safe hashing
        // Supports coordinates from -1,048,576 to 1,048,575 (21 bits each)
        long hash = (x & 0x1FFFFFL);
        hash |= ((long)(y & 0x1FFFFFL) << 21);
        hash |= ((long)(z & 0x1FFFFFL) << 42);
        return hash;
    }

    public boolean isOre(BlockType blockType) {
        return blockType.getId().toLowerCase().contains("ore");
    }

    public boolean isPickaxe(String id) {
        return id.toLowerCase().contains("pickaxe");
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}