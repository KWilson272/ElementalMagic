package me.kwilson272.elementalmagic.api.revertible;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class TempBlock implements Revertible {

    private static final BlockFace[] CARTESIAN_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

    private static final Map<Block, BlockState> ORIGINAL_STATES = new HashMap<>();
    private static final Map<Block, Deque<TempBlock>> INSTANCES = new HashMap<>();

    private long duration;
    private long revertTime;
    private List<Consumer<TempBlock>> revertTasks;

    private Block block;
    private BlockData data;
    private Ability ability;
    private boolean isUsable;
    private boolean isCollidable;
    private double damage;
    private long burnDuration;
    private boolean isReverted;

    private TempBlock(Block block, TempBlockBuilder builder) {
        this.duration = builder.duration;
        this.revertTime = duration > 0 ? System.currentTimeMillis() + duration : duration;
        this.revertTasks = builder.revertTasks;

        this.block = block;
        this.data = builder.data;
        this.ability = builder.ability;
        this.isUsable = builder.isUsable;
        this.isCollidable = builder.isCollidable;
        this.damage = builder.damage;
        this.burnDuration = builder.burnDuration;
        this.isReverted = false;

        ORIGINAL_STATES.computeIfAbsent(block, k -> k.getState());
        INSTANCES.computeIfAbsent(block, k -> new ArrayDeque<>())
                .offerFirst(this);

        matchState();
    }

    @Override
    public long getDurationMillis() {
        return duration;
    }

    @Override
    public long getRevertTimeMillis() {
        return revertTime;
    }

    @Override
    public void handleRevertTasks() {
        revert();
        revertTasks.forEach(c -> c.accept(this));
    }

    @Override
    public boolean isReverted() {
        return isReverted;
    }

    private void revert() {
        if (isReverted) {
            return;
        }

        isReverted = true;
        Deque<TempBlock> instanceQ = INSTANCES.get(block);
        if (!isActive(this)) {
            instanceQ.remove(this);
            return;
        }

        instanceQ.pollFirst();
        if (!instanceQ.isEmpty()) {
            instanceQ.peek().matchState();
            return;
        }

        INSTANCES.remove(block);
        ORIGINAL_STATES.remove(block).update(true, false);
    }

    private void matchState() {
        block.setBlockData(data, false);
    }

    /**
     * @return the Block this TempBlock instance was created at.
     */
    public Block block() {
        return block;
    }

    /**
     * @return the Ability responsible for Creating this TempBlock instance
     */
    public Ability ability() {
        return ability;
    }

    /**
     * Gets the amount of damage this TempBlock should deal to Entities if the
     * underlying Block is the damager in the
     * {@link org.bukkit.event.entity.EntityDamageByBlockEvent}
     *
     * <p> Negative values will result in the event's damage being unchanged.
     *
     * @return the damage an Entity should take when damaged by this TempBlock
     */
    public double damage() {
        return damage;
    }

    /**
     * Gets the amount of time an Entity should be burned for in milliseconds
     * if the underlying block is the combuster in the
     * {@link org.bukkit.event.entity.EntityCombustByBlockEvent}
     *
     * <p> Negative values will result in the event's duration being unchanged.
     *
     * @return the amount of time an Entity should burn for when ignited by
     * this TempBlock
     */
    public long burnDurationMillis() {
        return burnDuration;
    }

    /**
     * @return true if Abilities can use this TempBlock for sourcing or
     * similar interactions, false otherwise
     */
    public boolean isUsable() {
        return isUsable;
    }

    /**
     * @return true if Abilities can collide with this TempBlock,
     * false otherwise.
     */
    public boolean isCollidable() {
        return isCollidable;
    }

    /**
     * Checks if the provided has any active TempBlock instances mapped to it
     *
     * @param block the Block being checked
     * @return true if the Block is a TempBlock, false otherwise
     */
    public static boolean isTempBlock(Block block) {
        return get(block).isPresent();
    }

    /**
     * Gets the active TempBlock instance mapped to the provided block if
     * one exists.
     *
     * @param block the Block at which the TempBlock is being requested
     * @return an Optional with the active TempBlock
     */
    public static Optional<TempBlock> get(Block block) {
        if (block != null && INSTANCES.containsKey(block)) {
            return Optional.ofNullable(INSTANCES.get(block).peek());
        }
        return Optional.empty();
    }

    /**
     * Checks if the provided TempBlock is the active instance at its block.
     *
     * @param tempBlock the TempBlock instance being checked
     * @return true if the TempBlock is active, false otherwise
     */
    public static boolean isActive(TempBlock tempBlock) {
        return get(tempBlock.block).map(tempBlock::equals).orElse(false);
    }

    public static boolean isUsableTempBlock(Block block) {
        return get(block).map(TempBlock::isUsable).orElse(true);
    }

    /**
     * Checks if the provided Block is touching a TempBlock.
     *
     * @param block the Block being checked
     * @return true if the Block is touching a TempBlock, false otherwise
     */
    public static boolean isTouchingAny(Block block) {
        return !getInContactWith(block).isEmpty();
    }

    /**
     * Gets all TempBlock instances the provided Block is in contact with.
     *
     * @param block the Block touching the TempBlocks
     * @return a List of TempBlocks, may be empty
     */
    public static List<TempBlock> getInContactWith(Block block) {
        List<TempBlock> touching = new ArrayList<>();
        for (BlockFace blockFace : CARTESIAN_FACES) {
            get(block.getRelative(blockFace)).ifPresent(touching::add);
        }
        return touching;
    }

    public static TempBlockBuilder builder(Ability ability, BlockData data) {
        return new TempBlockBuilder(ability, data);
    }

    public static class TempBlockBuilder {

        private long duration;
        private List<Consumer<TempBlock>> revertTasks;

        private BlockData data;
        private Ability ability;
        private boolean isUsable;
        private boolean isCollidable;
        private double damage;
        private long burnDuration;

        public TempBlockBuilder(Ability ability, BlockData data) {
            this.data = data;
            this.ability = ability;
            this.duration = -1;
            this.revertTasks = new ArrayList<>();
            this.isUsable = false;
            this.isCollidable = true;
            this.damage = -1.0;
            this.burnDuration = -1;
        }

        public TempBlockBuilder setDuration(long durationMillis) {
            this.duration = durationMillis;
            return this;
        }

        public TempBlockBuilder addRevertTask(Consumer<TempBlock> revertTask) {
            this.revertTasks.add(revertTask);
            return this;
        }

        public TempBlockBuilder setData(BlockData data) {
            this.data = data;
            return this;
        }

        public TempBlockBuilder setAbility(Ability ability) {
            this.ability = ability;
            return this;
        }

        public TempBlockBuilder setUsable(boolean isUsable) {
            this.isUsable = isUsable;
            return this;
        }

        public TempBlockBuilder setCollidable(boolean isCollidable) {
            this.isCollidable = isCollidable;
            return this;
        }

        public TempBlockBuilder setDamage(double damage) {
            this.damage = damage;
            return this;
        }

        public TempBlockBuilder setBurnDuration(long burnDurationMillis) {
            this.burnDuration = burnDurationMillis;
            return this;
        }

        public Optional<TempBlock> buildAt(Block block) {
            Objects.requireNonNull(data, "TempBlock cannot be built with null BlockData.");
            Objects.requireNonNull(ability, "TempBlock cannot be built with null Ability.");

            // 0 Will prevent creation, -1 will make the block never revert
            if (duration == 0) {
                return Optional.empty();
            }

            TempBlock tb = new TempBlock(block, this);
            ElementalMagicApi.revertibleManager().register(tb);
            return Optional.of(tb);
        }
    }
}
