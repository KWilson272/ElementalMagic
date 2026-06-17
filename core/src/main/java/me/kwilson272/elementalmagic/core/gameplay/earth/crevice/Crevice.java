package me.kwilson272.elementalmagic.core.gameplay.earth.crevice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class Crevice extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private enum State {
        OPENING,
        IDLE,
        CLOSING,
    }

    private long cooldown;
    private int range;
    private double speed;
    private int depth;
    private int width;
    private long duration;
    private double hitboxSize;
    private double knockup;
    private double closeRange;
    private double closeRadius;
    private int closeCount;

    private State state;

    private double rangeCounter;
    private Location location;
    private Vector travelDir;
    private Vector sideDir;
    private long revertTime;

    private Deque<CrevicePillar> openPillars;
    private Deque<CrevicePillar> idlePillars;
    private List<CrevicePillar> closePillars;
    private Set<Block> affectedBlocks;

    public Crevice(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        range = CONFIG.range;
        speed = CONFIG.speed;
        depth = CONFIG.depth;
        width = CONFIG.width;
        duration = CONFIG.duration;
        hitboxSize = CONFIG.hitboxSize;
        knockup = CONFIG.knockup;
        closeRange = CONFIG.closeRange;
        closeRadius = CONFIG.closeRadius;
        closeCount = CONFIG.closeCount;

        state = State.OPENING;
        openPillars = new ArrayDeque<>();
        idlePillars = new ArrayDeque<>();
        closePillars = new ArrayList<>();
        affectedBlocks = new HashSet<>();
    }

    @Override
    public boolean start() {
        rangeCounter = range;

        Location eyes = user().player().getEyeLocation();
        travelDir = Vectors.fromRotations(0, eyes.getYaw());
        sideDir = Vectors.fromRotations(0, eyes.getYaw() + 90);

        Block block = getTopBlock(eyes.add(travelDir).getBlock());
        if (block == null) {
            return false;
        }

        location = block.getLocation().add(0.5, 0.5, 0.5);
        return true;
    }

    @Override
    public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            // Ensures that active pillars still fall but no more get placed
            rangeCounter = 0;
        }

        if (state == State.OPENING) {
            advanceLocation();
            manageOpeningPillars();

            if (rangeCounter <= 0 && openPillars.isEmpty()) {
                user().addCooldown(name(), cooldown);
                revertTime = System.currentTimeMillis() + duration;
                state = State.IDLE;
            }

        } else if (state == State.CLOSING) {
            for (int i = 0; i < closeCount && !idlePillars.isEmpty(); ++i) {
                closePillars.add(idlePillars.pollFirst());
            }
            closePillars.removeIf(pillar -> !pillar.close());
            return !closePillars.isEmpty() || !idlePillars.isEmpty();

        } else if (System.currentTimeMillis() > revertTime) {
            state = State.CLOSING;
        }

        return true;
    }

    private void createPillars() {
        for (double i = -width; i <= width; i += 0.5) {
            Vector offset = sideDir.clone().multiply(i);
            Block spawn = location.clone().add(offset).getBlock();
            spawn = getTopBlock(spawn);
            if (spawn == null || affectedBlocks.contains(spawn)) {
                continue;
            }

            affectedBlocks.add(spawn);
            openPillars.offerLast(new CrevicePillar(spawn));
            if (ThreadLocalRandom.current().nextInt(Math.max(1, width)) == 0) {
                playEarthSound(spawn.getLocation());
            }
        }
    }

    private void advanceLocation() {
        double remainder = speed;
        while (remainder > 0) {
            // Advance by 0.5 so we can avoid gaps in the crevice
            double travel = Math.min(0.5, remainder);
            remainder -= 0.5;
            rangeCounter -= travel;

            Block oldBlock = location.getBlock();
            location.add(travelDir.clone().multiply(travel));
            Block newBlock = location.getBlock();
            
            if (!newBlock.equals(oldBlock)) {
                newBlock = getTopBlock(newBlock);
                if (newBlock == null) {
                    rangeCounter = 0;
                    return;
                }

                location.setY(newBlock.getY());
                // shifs us one block to the left right or not at all 
                double offset = ThreadLocalRandom.current().nextInt(-1, 2); 
                location.add(sideDir.clone().multiply(offset));
            }

            createPillars();
            if (rangeCounter <= 0) {
                return;
            }
        }
    }

    private Block getTopBlock(Block block) {
        for (int i = 0; i < depth; ++i) {
            Block above = block.getRelative(BlockFace.UP);
            if (Blocks.isSolid(block) && !Blocks.isSolid(above)) {
                break;
            } else if (Blocks.isSolid(above)) {
                block = above;
            } else {
                block = block.getRelative(BlockFace.DOWN);
            }
        }

        Block above = block.getRelative(BlockFace.UP);
        return isUsableEarth(block) && !Blocks.isSolid(above) ? block : null;
    }

    private void manageOpeningPillars() {
        Iterator<CrevicePillar> iter = openPillars.iterator();
        while (iter.hasNext()) {
            CrevicePillar pillar = iter.next();
            if (!pillar.open()) {
                idlePillars.offerLast(pillar);
                iter.remove();
            }
        }
    }

    @Override
    public void onDestruction() {
        openPillars.forEach(CrevicePillar::revert);
        idlePillars.forEach(CrevicePillar::revert);
        closePillars.forEach(CrevicePillar::revert);
    }

    @Override
    public String name() {
        return "Crevice";
    }

    protected boolean isOpening() {
        return state == State.OPENING;
    }

    protected void close(AbilityUser user) {
        if (state == State.CLOSING) {
            return;
        }

        Player player = user.player();
        Location center = Entities.getTargetLocation(player, closeRange);
        for (Block block : Blocks.collectSphere(center, closeRadius)) {
            if (affectedBlocks.contains(block)) {
                state = State.CLOSING;
                while (!openPillars.isEmpty()) {
                    idlePillars.offerLast(openPillars.pollFirst());
                }
                return;
            }
        }
    }

    private class CrevicePillar {

        private Block block;
        private Deque<TempBlock> airBlocks;
        private int depthCounter;

        public CrevicePillar(Block start) {
            block = start;
            airBlocks = new ArrayDeque<>();
            depthCounter = depth;
        }

        private boolean open() {
            BlockData airData = Material.AIR.createBlockData();
            TempBlock.builder(Crevice.this, airData)
                    .buildAt(block)
                    .ifPresent(tb -> {
                        airBlocks.offerLast(tb);
                        affectedBlocks.add(block);
                    });

            block = block.getRelative(BlockFace.DOWN);
            return isUsableEarth(block) && --depthCounter > 0;
        }

        private boolean close() {
            if (airBlocks.isEmpty()) {
                return false;
            }

            TempBlock tb = airBlocks.pollLast();
            ElementalMagicApi.revertibleManager().revert(tb);

            Vector knock = new Vector(0, knockup, 0);
            Block above = tb.block().getRelative(BlockFace.UP);
            BoundingVolume bv = AABB.fromBlock(above, hitboxSize);
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();
            for (Entity e : Entities.getNearbyEntities(above.getWorld(), bv)) {
                effectHandler.setVelocity(e, Crevice.this, knock, 3);
            }
            return true;
        }

        private void revert() {
            airBlocks.forEach(ElementalMagicApi.revertibleManager()::revert);
            airBlocks.clear();
        }
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = CreviceController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 15000;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private int range = 100;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 0.8;
        @Configure(path = CONFIG_PATH + "Depth", config = Config.ABILITIES)
        private int depth = 10;
        @Configure(path = CONFIG_PATH + "SideWidth", config = Config.ABILITIES)
        private int width = 1;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 10000;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.0;
        @Configure(path = CONFIG_PATH + "Knockup", config = Config.ABILITIES)
        private double knockup = 1.0;
        @Configure(path = CONFIG_PATH + "CloseRange", config = Config.ABILITIES)
        private double closeRange = 10;
        @Configure(path = CONFIG_PATH + "CloseRadius", config = Config.ABILITIES)
        private double closeRadius = 2.0;
        @Configure(path = CONFIG_PATH + "CloseCount", config = Config.ABILITIES)
        private int closeCount = 3;
    }
}
