package me.kwilson272.elementalmagic.core.gameplay.earth.lavathrow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.components.BlockStream;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class LavaThrow extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double selectRange;
    private double sourceRadius;
    private int throwCount;
    private double range;
    private double speed;
    private double hitboxSize;
    private double damage;
    private double lavaDamage;
    private long burnDuration;

    private int remainingThrows;

    private Block source;
    private List<Block> candidateSources;
    private List<LavaStream> streams;

    public LavaThrow(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        sourceRadius = CONFIG.sourceRadius;
        throwCount = CONFIG.throwCount;
        range = CONFIG.range;
        speed = CONFIG.speed;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        lavaDamage = CONFIG.lavaDamage;
        burnDuration = CONFIG.burnDuration;

        candidateSources = new ArrayList<>();
        streams = new ArrayList<>();
    }

    @Override
    public boolean start() {
        source = selectLavaSource(selectRange);
        if (source == null) {
            return false;
        }

        List<Block> blocks = getSourceBlocks();
        selectCandidateSources(blocks);
        return !candidateSources.isEmpty();
    }

    private List<Block> getSourceBlocks() {
        List<Block> sources = new ArrayList<>();
        Location center = source.getLocation();
        for (Block block : Blocks.collectCircle(center, sourceRadius)) {
            block = getHighestSource(block);
            if (block != null) {
                sources.add(block);
            }
        }

        return sources;
    }

    private Block getHighestSource(Block block) {
        int limit = (int) Math.ceil(sourceRadius);
        for (int i = 0; i <= limit; ++i) {
            Block above = block.getRelative(BlockFace.UP);
            if (Blocks.isLava(block) && !Blocks.isSolid(above)
                    && !Blocks.isLiquid(above)) {
                return block;
            } else if (Blocks.isSolid(block) || Blocks.isLiquid(above)) {
                block = above;
            } else {
                block = block.getRelative(BlockFace.DOWN);
            }
        }

        Block above = block.getRelative(BlockFace.UP);
        if (Blocks.isLava(block) && !Blocks.isSolid(above)
                && !Blocks.isLiquid(above)) {
            return block;
        }
        return null;
    }

    private void selectCandidateSources(List<Block> sources) {
        int count = Math.min(throwCount, sources.size());
        if (count <= 0) {
            return;
        }

        for (int i = 0; i < count; ++i) {
            int upper = sources.size() - i;
            int idx = ThreadLocalRandom.current().nextInt(upper);
            Block block = sources.get(idx);

            // We need to put the block in the last open position in the
            // sources list so we can ensure we don't choose duplicates
            candidateSources.add(block);
            sources.set(idx, sources.get(upper-1));
            sources.set(upper-1, block);
        }
    }

    @Override
    public boolean progress() {
        if (!user().canUse(controller(), remainingThrows > 0, false)
                || !isSourceViable()) {
            remainingThrows = 0;
        }

        streams.removeIf(stream -> !stream.progress());
        return remainingThrows > 0 || !streams.isEmpty();
    }

    private boolean isSourceViable() {
        Location sourceLoc = source.getLocation().add(0.5, 0.5, 0.5);
        Location eyeLoc = user().player().getEyeLocation();
        double maxDist = Math.pow(selectRange + 2, 2);
        return sourceLoc.getWorld().equals(eyeLoc.getWorld())
                && sourceLoc.distanceSquared(eyeLoc) <= maxDist;
    }

    @Override
    public void onDestruction() {
        user().addCooldown(name(), cooldown);
    }

    @Override
    public String name() {
        return "LavaThrow";
    }

    protected void fire() {
        if (remainingThrows <= 0 || candidateSources.isEmpty()) {
            return;
        }

        Block block = candidateSources.removeLast();
        streams.add(new LavaStream(block, getTarget()));
    }

    private Location getTarget() {
        Player player = user().player();
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection();
        double targetRange = selectRange + range;

        RayTraceResult result = world.rayTraceEntities(
                start,
                dir,
                targetRange,
                1.25,
                e -> !e.equals(user().player())
                        && ElementalMagicApi.effectHandler().canAffect(e)
        );

        if (result != null && result.getHitEntity() != null) {
            return result.getHitEntity().getLocation();
        } else {
           return Entities.getTargetLocation(player, targetRange);
        }
    }

    private class LavaStream extends BlockStream {

        private boolean isRising;
        private Block riseBlock;
        private Vector direction;

        public LavaStream(Block block, Location target) {
            super(block.getLocation().add(0.5, 0.5, 0.5), speed, range);

            isRising = true;
            riseBlock = getRiseBlock(block, target);

            if (riseBlock.equals(target.getBlock())) {
                direction = user().player().getEyeLocation().getDirection();
            } else {
                Location riseLoc = riseBlock.getLocation().add(0.5, 0.5, 0.5);
                direction = Vectors.getDirection(riseLoc, target).normalize();
            }
        }

        private Block getRiseBlock(Block block, Location target) {
            int yDiff = target.getBlockY() - block.getY();
            int limit = Math.clamp(yDiff, 1, 3);

            for (int i = 0; i < limit; ++i) {
                Block above = block.getRelative(BlockFace.UP);
                if (!Blocks.isSolid(above)) {
                    block = above;
                } else {
                    break;
                }
            }

            return block;
        }

        @Override
        public boolean collidesWith(Block block) {
            return Blocks.isSolid(block) || Blocks.isWater(block);
        }

        @Override
        public void createBlock(Block block) {
            BlockData data = Material.LAVA.createBlockData();
            TempBlock.builder(LavaThrow.this, data)
                    .setDamage(lavaDamage)
                    .setBurnDuration(burnDuration)
                    .setDuration(250)
                    .buildAt(block);

            if (isRising && block.getY() >= riseBlock.getY()) {
                isRising = false;
            }
        }

        @Override
        public Vector getDirection() {
            return isRising ? new Vector(0, 1, 0) : direction.clone();
        }
    }

    protected static final class ConfigValues {

        private static final String CONFIG_PATH = LavaThrowController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 5300;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 12.0;
        @Configure(path = CONFIG_PATH + "SourceRadius", config = Config.ABILITIES)
        private double sourceRadius = 2.0;
        @Configure(path = CONFIG_PATH + "ThrowCount", config = Config.ABILITIES)
        private int throwCount = 3;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 18;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.5;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "LavaDamage", config = Config.ABILITIES)
        private double lavaDamage = 0.0;
        @Configure(path = CONFIG_PATH + "BurnDuration", config = Config.ABILITIES)
        private long burnDuration = 0;
    }
}
