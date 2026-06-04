package me.kwilson272.elementalmagic.core.gameplay.components;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.core.util.Blocks;

public class EarthPillar {
    
    private static final List<EarthPillar> pillars = new ArrayList<>();

    private enum State {
        RISING,
        IDLE,
        COLLAPSING
    }

    private Ability ability;
    private int height;
    private double speed;
    private Consumer<Location> moveCallback;
    private Consumer<Block> placeCallback;
    
    private State state;
    private boolean wasRising;
    private int movementCounter;
    private Location location;

    private Deque<PillarBlock> blocks;
    private Map<Block, TempBlock> affectedBlocks;

    public EarthPillar(Ability ability, Block block, int height, double speed, boolean isRising) {
        this.ability = ability;
        this.height = height;
        this.speed = speed;

        this.state = isRising ? State.RISING : State.COLLAPSING;
        this.wasRising = isRising;
        this.movementCounter = 0;
    
        this.blocks = new ArrayDeque<>();
        this.affectedBlocks = new HashMap<>();
        
        this.initBlocks(block);
        this.height = Math.min(this.height, this.blocks.size());

        if (!blocks.isEmpty()) {
            location = blocks.peekFirst().origin.getLocation().add(0.5, 0.5, 0.5);
            pillars.add(this);
        }
    }

    private void initBlocks(Block start) {
        BlockFace face = state == State.RISING ? BlockFace.DOWN : BlockFace.UP;
        for (int i = 0; i < height; ++i) {
            Block block = start.getRelative(face, i);
            if (!isUsable(block)) {
                break;
            }

            blocks.offerLast(new PillarBlock(block));
        }
    }

    private boolean isUsable(Block block) {
        if (!Blocks.canAbilityUse(block)) {
            return false;
        }

        return Blocks.isEarth(block)
            || Blocks.isSand(block)
            || Blocks.isMetal(block);
    }

    public boolean progress() {
        if (blocks.isEmpty() || state == State.IDLE) {
            state = State.IDLE;
            return false;
        }

        double remainder = speed;
        while (remainder > 0) {
            double travel = Math.min(remainder, 1);
            remainder -= 1;

            double y = state == State.RISING ? 1 : -1;
            Vector dir = new Vector(0, y * travel, 0);

            Block prev = location.getBlock();
            Block next = location.add(dir).getBlock();
            if (!next.equals(prev)) {
                if (Blocks.isSolid(next)) {
                    state = State.IDLE;
                    return false;
                }

                BlockFace face = state == State.RISING ? BlockFace.UP : BlockFace.DOWN;
                for (PillarBlock block : blocks) {
                    block.moveBlock(face);
                }

                if (placeCallback != null) {
                    placeCallback.accept(next);
                }

                if (anyAtOrigin()) {
                    revert();
                    state = State.IDLE;
                    return false;
                }

                if (++movementCounter >= height) {
                    state = State.IDLE;
                    remainder = 0;
                }
            }

            if (moveCallback != null) {
                moveCallback.accept(location);
            }
        }

        return true;
    }

    private boolean anyAtOrigin() {
        for (PillarBlock block : blocks) {
            if (block.origin.equals(block.current)) {
                return true;
            }
        }
        return false;
    }

    public void revert() {
        for (PillarBlock block : blocks) {
            block.revert();
        }
        blocks.clear();
        pillars.remove(this);
    }

    public boolean isViable() {
        return !blocks.isEmpty();
    }

    public boolean isIdle() {
        return state == State.IDLE;
    }

    public void setMoveCallback(Consumer<Location> callback) {
        this.moveCallback = callback;
    }

    public void setBlockPlaceCallback(Consumer<Block> callback) {
        this.placeCallback = callback;
    }

    public static EarthPillar getFromBlock(Block block) {
        for (EarthPillar pillar : pillars) {
            if (pillar.affectedBlocks.containsKey(block)) {
                return pillar;
            }
        }
        return null;
    }

    private class PillarBlock {
        
        private Block origin;
        private Block current;
        private TempBlockBuilder builder;
        private TempBlock originTemp;
        private TempBlock curTemp;

        PillarBlock(Block block) {
            this.origin = block;
            this.current = block;
            this.builder = TempBlock.builder(ability, origin.getBlockData())
                .setUsable(true);
            this.originTemp = null;
            this.curTemp = null;
        }

        void moveBlock(BlockFace face) {
            Block block = current.getRelative(face);
            if (curTemp != null) {
                ElementalMagicApi.revertibleManager().revert(curTemp);
            }

            if (originTemp == null) {
                BlockData data = Material.AIR.createBlockData();
                originTemp = TempBlock.builder(ability, data).
                    buildAt(origin).
                    orElse(null);
            }

            curTemp = builder.buildAt(block).orElse(null);

            // CRITICAL: The order of movement must start at the head 
            // block always or this WILL bug out & stop working.
            affectedBlocks.remove(current);
            affectedBlocks.put(block, curTemp);
            current = block;
        }

        void revert() {
            affectedBlocks.remove(current);

            if (curTemp != null) {
                ElementalMagicApi.revertibleManager().revert(curTemp);
                curTemp = null;
            }
            if (originTemp != null) {
                ElementalMagicApi.revertibleManager().revert(originTemp);
                originTemp = null;
            }
        }
    }
}
