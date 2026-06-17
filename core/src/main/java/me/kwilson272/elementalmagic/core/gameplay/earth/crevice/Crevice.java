package me.kwilson272.elementalmagic.core.gameplay.earth.crevice;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.core.util.Entities;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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
    private int depth;
    private int width;
    private long duration;
    private double hitboxSize;
    private double knockup;
    private double closeRange;
    private double closeRadius;

    private State state;

    private int rangeCounter;
    private Block curBlock;
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
        depth = CONFIG.depth;
        width = CONFIG.width;
        duration = CONFIG.duration;
        hitboxSize = CONFIG.hitboxSize;
        knockup = CONFIG.knockup;
        closeRange = CONFIG.closeRange;
        closeRadius = CONFIG.closeRadius;

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

        Block block = eyes.add(travelDir).getBlock();
        curBlock = getTopBlock(block);
        return curBlock != null;
    }

    @Override
    public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            // Ensures that active pillars still fall but no more get placed
            rangeCounter = 0;
        }

        if (state == State.OPENING) {
            if (rangeCounter > 0) {
                createPillars();
                advanceLocation();
            }
            manageOpeningPillars();

            if (rangeCounter <= 0 && openPillars.isEmpty()) {
                user().addCooldown(name(), cooldown);
                revertTime = System.currentTimeMillis() + duration;
                state = State.IDLE;
            }

        } else if (state == State.CLOSING) {
            if (!idlePillars.isEmpty()) {
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
        Set<Block> created = new HashSet<>();
        Location loc = curBlock.getLocation().add(0.5, 0.5, 0.5);
        for (double i = -width; i <= width; i += 0.5) {
            Vector offset = sideDir.clone().multiply(i);
            Block spawn = loc.clone().add(offset).getBlock();
            if (affectedBlocks.contains(spawn) || created.contains(spawn)) {
                continue;
            }

            spawn = getTopBlock(spawn);
            if (spawn == null) {
                continue;
            }

            created.add(spawn);
            openPillars.offerLast(new CrevicePillar(spawn));
            if (ThreadLocalRandom.current().nextInt(Math.max(1, width)) == 0) {
                playEarthSound(spawn.getLocation());
            }
        }
    }

    private void advanceLocation() {
        rangeCounter--;
        Location loc = curBlock.getLocation().add(0.5, 0.5, 0.5);
        // We either have -1 or 1 for random offset, or 0 for no offset
        int offset = ThreadLocalRandom.current().nextInt(-1, 2);
        loc.add(travelDir).add(sideDir.clone().multiply(offset));

        curBlock = getTopBlock(loc.getBlock());
        if (curBlock == null) {
            rangeCounter = 0;
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
                block = block.getRelative(BlockFace.UP);
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
    }
}
