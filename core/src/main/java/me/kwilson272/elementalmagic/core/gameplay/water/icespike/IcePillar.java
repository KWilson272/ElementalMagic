package me.kwilson272.elementalmagic.core.gameplay.water.icespike;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;

public abstract class IcePillar {

    private static final BlockFace[] FRICTION_FACES = { 
        BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST 
    };

    private enum State {
        RISING,
        PAUSED,
        FALLING
    }

    private int maxHeight;
    private Block base;    
    private Block current;
    private BlockData pillarData;
    private TempBlockBuilder blockBuilder;

    private State state;
    private Deque<TempBlock> blocks;

    public IcePillar(int maxHeight, Block base, Ability ability) {
        this.maxHeight = maxHeight;
        this.base = base;
        this.current = base;
        this.pillarData = getPillarData();
        this.blockBuilder = TempBlock.builder(ability, pillarData);

        this.state = State.RISING;
        this.blocks = new ArrayDeque<>();
    }

    private BlockData getPillarData() {
        if (AbilityUtil.isSnow(base)) {
             return Material.SNOW_BLOCK.createBlockData();
        } else if (AbilityUtil.isIce(base)) {
            return base.getBlockData();
        } else {
            return Material.ICE.createBlockData();
        } 
    }

    public boolean progress() {
        switch (state) {
            case RISING -> {
                current = current.getRelative(BlockFace.UP);
                blockBuilder.buildAt(current).ifPresent(tb -> {
                    blocks.offerFirst(tb);
                    onBlockPlace(current);
                });
                playFrictionParticles();

                if (current.getY() - base.getY() >= maxHeight) {
                    state = State.PAUSED;
                }
            }

            case PAUSED -> {
                // Pause for a tick so the spikes don't look so fast
                state = State.FALLING;
            }

            case FALLING -> {
                if (blocks.isEmpty()) {
                    return false;
                }
                ElementalMagicApi.revertibleManager().revert(blocks.pollFirst());
            }
        }
        return true;
    }

    // Plays particles at the boundary between blocks as if they were scraping
    // together and breaking particles off
    private void playFrictionParticles() {
        for (BlockFace face : FRICTION_FACES) {
            Block block = base.getRelative(face);
            if (!block.getType().isSolid()) {
                continue;
            }

            // Simplifies math if we treat everything as from the center
            // Add 1.1 so we don't play particles inside the two blocks
            Location loc = base.getLocation().add(0.5, 1.1, 0.5);
            Vector vec = face.getDirection().multiply(0.5);
            loc.add(vec);

            // Gets a perpendicular vector so we can run across the boundary
            // this math is fine here because we won't ever have a y component
            Vector lineVec = new Vector(-vec.getZ(), 0, vec.getX());
            // Linevec is still of length 0.5 here, so this puts us at the corner
            loc.subtract(lineVec);
            // Gives us a length of 0.25 to iterate along the path with
            lineVec.multiply(0.5);

            World world = block.getWorld();
            BlockData bData = block.getBlockData();
            Particle particle = Particle.BLOCK;
            ThreadLocalRandom rand = ThreadLocalRandom.current();

            for (int i = 0; i < 5; ++i) {
                if (rand.nextInt(5) == 0) {
                    // Alternate between them so we don't get spammy
                    BlockData data = rand.nextBoolean() ? bData : pillarData;
                    world.spawnParticle(particle, loc, 1, 0, 0, 0, data);
                }
                loc.add(lineVec);
            }
        }
    }

    public void revertAll() {
        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        blocks.forEach(revertManager::revert);
    }

    public abstract void onBlockPlace(Block block);
}

