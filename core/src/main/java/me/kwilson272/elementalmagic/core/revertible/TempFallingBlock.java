package me.kwilson272.elementalmagic.core.revertible;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.revertible.Revertible;

public class TempFallingBlock implements Revertible {

    private static final Map<FallingBlock, TempFallingBlock> INSTANCES = new HashMap<>();

    private final long duration;
    private final long revertTime;
    private boolean isReverted;
    
    private final FallingBlock fallingBlock;
    private final Ability ability;
    private final boolean isCollidable;
    private final List<Consumer<TempFallingBlock>> revertTasks;

    private TempFallingBlock(FallingBlock fb, TempFallingBlockBuilder builder) {
         this.duration = builder.duration;
         this.revertTime = System.currentTimeMillis() + duration;
         this.fallingBlock = fb;
         this.ability = builder.ability;
         this.isCollidable = builder.isCollidable;
         this.revertTasks = builder.revertTasks;
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
        if (isReverted) {
            return;
        }

        INSTANCES.remove(fallingBlock);
        fallingBlock.remove();
        isReverted = true;
        
        revertTasks.forEach(c -> c.accept(this));
	}

	@Override
	public boolean isReverted() {
        return isReverted;
	}

    /**
     * @return the {@link FallingBlock} underlying this {@code TempFallingBlock}.
     */
    public FallingBlock fallingBlock() {
        return fallingBlock;
    }

    /**
     * @return the {@link Ability} that created this {@code TempFallingBlock}.
     */
    public Ability ability() {
        return ability; 
    }

    /**
     * @return true if abilities should collide with this 
     * {@code TempFallingBlock}, false otherwise.
     */
    public boolean isCollidable() {
        return isCollidable;
    }

    public static boolean isTempFallingBlock(FallingBlock fb) {
        return INSTANCES.containsKey(fb);
    }

    public static Optional<TempFallingBlock> get(FallingBlock fb) {
        return Optional.ofNullable(INSTANCES.get(fb)); 
    }

    public static TempFallingBlockBuilder builder(Ability ability, BlockData data) {
        return new TempFallingBlockBuilder(ability, data); 
    }

    public static class TempFallingBlockBuilder {
        
        private long duration;
        private Ability ability;
        private BlockData blockData;
        private boolean isCollidable;
        private List<Consumer<TempFallingBlock>> revertTasks;

        public TempFallingBlockBuilder(Ability ability, BlockData data) {
            this.duration = 1;
            this.ability = ability; 
            this.blockData = data;
            this.isCollidable = false;
            this.revertTasks = new ArrayList<>();
        }

        public TempFallingBlockBuilder setDuration(long durationMillis) {
            this.duration = durationMillis;
            return this;
        }

        public TempFallingBlockBuilder setAbility(Ability ability) {
            this.ability = ability;
            return this;
        }

        public TempFallingBlockBuilder setBlockData(BlockData data) {
            this.blockData = data;
            return this;
        }

        public TempFallingBlockBuilder setCollidable(boolean isCollidable) {
            this.isCollidable = isCollidable;
            return this;
        }

        public TempFallingBlockBuilder addRevertTask(Consumer<TempFallingBlock> revertTask) {
            this.revertTasks.add(revertTask);
            return this;
        }

        public TempFallingBlock buildAt(Location loc) {
            Objects.requireNonNull(ability, "TempFallingBlock cannot be built with null Ability.");
            Objects.requireNonNull(blockData, "TempFallingBlocks cannot be built with null BlockData.");
            Objects.requireNonNull(loc.getWorld(), "TempFallingBlocks cannot be built from a location with a null world.");
            
            World world = loc.getWorld();
            FallingBlock fb = world.spawnFallingBlock(loc, blockData);
            fb.setCancelDrop(true);
            fb.setDropItem(false);
            
            // The purpose of duration in this class is to avoid forever-present 
            // fallingblocks, so we need to ensure they can always revert
            this.duration = Math.max(1, this.duration);
            TempFallingBlock tfb = new TempFallingBlock(fb, this);
            ElementalMagicApi.revertibleManager().register(tfb);
            return tfb;
        }
    }
}
