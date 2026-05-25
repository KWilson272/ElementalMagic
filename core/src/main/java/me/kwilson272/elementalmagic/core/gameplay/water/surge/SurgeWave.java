package me.kwilson272.elementalmagic.core.gameplay.water.surge;

import java.security.cert.CertPathBuilderException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterUtil;

public class SurgeWave extends CoreAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private enum State {
        SOURCED,
        FIRED,
        FREEZE_MAINTENANCE
    }

    private long cooldown;
    private long sourceRevertTime;
    private double selectRange;
    private double range;
    private double maxRadius;
    private double hitboxSize;
    private double knockBack;
    private double knockUp;
    private double pushCutoff;
    private double freezeRadius;
    private long freezeRevertTime;
    private boolean isFreezeUsable;
    private long globalFreezeCD;

    private State state;

    // SOURCED
    private Block source;

    // FIRED
    private double radius;
    private double rangeCounter;
    private boolean doFreeze;

    private Location location;
    private Vector direction;
    private List<Vector> drawVecs;
    private List<TempBlock> waveBlocks;

    // FREEZE_MAINTENANCE
    private List<TempBlock> iceBlocks;

    public SurgeWave(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        sourceRevertTime = CONFIG.sourceRevertTime;
        selectRange = CONFIG.selectRange;
        range = CONFIG.range;
        maxRadius = CONFIG.radius;
        hitboxSize = CONFIG.hitboxSize;
        knockBack = CONFIG.knockBack;
        knockUp = CONFIG.knockUp;
        pushCutoff = CONFIG.pushCutoff;
        freezeRadius = CONFIG.freezeRadius;
        freezeRevertTime = CONFIG.freezeRevertTime;
        isFreezeUsable = CONFIG.isFreezeUsable;
        globalFreezeCD = CONFIG.globalFreezeCD;

        state = State.SOURCED;

        radius = 0.5;
        rangeCounter = 0;
        doFreeze = false;

        drawVecs = new ArrayList<>();
        waveBlocks = new ArrayList<>();
        iceBlocks = new ArrayList<>();
    }

    protected boolean destroyOnSneak() {
        if (state == State.FIRED) {
            // Surge cancelling should remove in 2 sneaks
            doFreeze = !doFreeze;
            return !doFreeze;
        }
        return state == State.FREEZE_MAINTENANCE || state == State.SOURCED;
    }

    protected boolean handleLeftClick() {
        if (state == State.SOURCED) {
            WaterUtil.consumeSource(this, source, sourceRevertTime);
            state = State.FIRED;
            initWave();

            user().addCooldown("SurgeWave", cooldown);
            return true;
        }
        return false;
    }

    private void initWave() {
        location = source.getLocation().add(0.5, 0.5, 0.5);
        Location target = EntityUtil.getTarget(user().player(), range);
        direction = VectorUtil.getDirection(location, target).normalize();
        Vector ortho = VectorUtil.getOrthogonal(direction);

        double blockSpacing = 0.5;
        double step = 2 * Math.asin(blockSpacing / (2 * maxRadius));
        for (double angle = 0; angle < Math.PI * 2; angle += step) {
            Vector vec = VectorUtil.rotateAroundVector(direction, ortho, angle);
            drawVecs.add(vec.multiply(0.5));
        }
    }

    @Override
    public boolean start() {
        source = WaterUtil.getSourceBlock(user(), selectRange);
        return source != null;
    }

    @Override
    public boolean progress() {
        if (!user().canUse(controller(), state == State.SOURCED, false)) {
            return false;
        }

        switch (state) {
            case SOURCED -> {
                if (!isSourceViable()) {
                    return false;
                }
                WaterUtil.playSourceSelectedEffect(source);
            }

            case FIRED -> {
                location.add(direction);
                rangeCounter++;
                if (rangeCounter > range || !isLocationPassable()) {
                    return false;
                }

                if (radius < maxRadius) {
                    radius = Math.min(maxRadius, radius + 0.5);
                }

                clearWave();
                createWave();
                affectEntities();
                WaterUtil.playWaterSound(location);

                // If doFreeze is true the state should have changed to
                // FREEZE_MAINTENANCE, so don't remove
                return !waveBlocks.isEmpty() || state == State.FREEZE_MAINTENANCE;
            }

            case FREEZE_MAINTENANCE -> {
                checkIceBlocks();
                return !iceBlocks.isEmpty();
            }
        }
        return true;
    }

    private boolean isSourceViable() {
        Location eyes = user().player().getEyeLocation();
        Location loc = source.getLocation().add(0.5, 0.5, 0.5);
        // Added range for comfort
        double maxDist = Math.pow(selectRange + 1, 2);
        return WaterUtil.canUse(source, user()) 
            && loc.distanceSquared(eyes) <= maxDist;
    }

    private boolean isLocationPassable() {
        Block block = location.getBlock();
        return !BlockUtil.isSolid(block) ||
                // Allows surge to interact more nicely with PhaseChange ice
                (TempBlock.isUsableTempBlock(block) && AbilityUtil.isIce(block));
    }

    private void clearWave() {
        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        waveBlocks.forEach(revertManager::revert);
        waveBlocks.clear();
    }

    private void createWave() {
        Set<Block> created = new HashSet<>();
        for (Vector vec : drawVecs) {
            Location loc = location.clone();

            for (double i = 0; i <= radius; i += 0.5) {
                Block block = loc.getBlock();
                loc.add(vec);
                
                if (BlockUtil.isSolid(block) || created.contains(block)) {
                    continue;
                }

                BlockData data = WaterUtil.getFilledData(block, 0);
                TempBlock.builder(this, data).buildAt(block).ifPresent(tb -> {
                    waveBlocks.add(tb);
                    created.add(block);
                });
            }
        }
    }

    private void affectEntities() {
        Collection<Entity> entities = getHitEntities();
       
        // This is just how PK handles surge knock
        Vector knock = direction.clone();
        knock.setY(knock.getY() * knockUp);
        knock.multiply(knockBack);
        double maxSpeed = pushCutoff * pushCutoff;

        boolean affected = false;
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        for (Entity entity : entities) {
            if (entity.getVelocity().lengthSquared() > maxSpeed) {
                continue;
            }
            Vector velocity = entity.getVelocity().add(knock);
            // Don't freeze if we've only affected the player
            affected |= (effectHandler.setVelocity(entity, this, velocity) 
                    && !entity.equals(user().player()));
        }

        if (affected && doFreeze && !user().isOnCooldown("GlobalFreeze")) {
            clearWave();
            createIceBall();
            state = State.FREEZE_MAINTENANCE;
            user().addCooldown("GlobalFreeze", globalFreezeCD);
        }
    }

    private Collection<Entity> getHitEntities() {
        Set<Entity> entities = new HashSet<>();
        
        World world = location.getWorld();
        double hitDist = hitboxSize * hitboxSize;
        for (TempBlock tb : waveBlocks) {
            Location blockLoc = tb.block().getLocation().add(0.5, 0.5, 0.5);
            BoundingVolume bv = AABB.fromBlock(tb.block(), hitboxSize);

            for (Entity e : EntityUtil.getNearbyEntities(world, bv)) {
                // Surge push feels quite poor when using complete hitboxes
                if (e.getLocation().distanceSquared(blockLoc) <= hitDist) {
                    entities.add(e);
                }
            }
        }
        
        return entities;
    }

    private void createIceBall() {
        World world = location.getWorld();
        BlockData data = Material.ICE.createBlockData();
        TempBlock.TempBlockBuilder ice = TempBlock.builder(this, data)
                .setUsable(isFreezeUsable).setDuration(freezeRevertTime);
        
        for (Block b : BlockUtil.collectSphere(location, freezeRadius)) {
            ice.buildAt(b).ifPresent(tb -> {
                iceBlocks.add(tb);
                if (ThreadLocalRandom.current().nextInt(5) == 0) {
                    Location loc = b.getLocation().add(0.5, 0.5, 0.5);
                    Particle particle = Particle.SNOWFLAKE;
                    world.spawnParticle(particle, loc, 1, 0.5, 0.5, 0.5, 0);
                }
            });
        }
    }

    private void checkIceBlocks() {
        double maxDist = range * range;
        List<TempBlock> toRemove = new ArrayList<>();
        for (TempBlock tb : iceBlocks) {
            if (tb.isReverted()) {
                toRemove.add(tb);
                continue;
            }
        
            Location eyeLoc = user().player().getEyeLocation();
            Location blockLoc = tb.block().getLocation().add(0.5, 0.5, 0.5);
            if (eyeLoc.distanceSquared(blockLoc) > maxDist) {
                ElementalMagicApi.revertibleManager().revert(tb);
                toRemove.add(tb);
            }
        }

        toRemove.forEach(iceBlocks::remove);
    }

    @Override
    public void onDestruction() {
        clearWave();
        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        iceBlocks.forEach(revertManager::revert);
    }

    @Override
    public String name() {
        return "SurgeWave";
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = SurgeController.CONFIG_PATH + "Wave.";

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 12;
        @Configure(path = CONFIG_PATH + "SourceRevertTime", config = Config.ABILITIES)
        private long sourceRevertTime = 10000;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 20;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 2.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.0;
        @Configure(path = CONFIG_PATH + "KnockBack", config = Config.ABILITIES)
        private double knockBack = 1.0;
        @Configure(path = CONFIG_PATH + "KnockUp", config = Config.ABILITIES)
        private double knockUp = 0.2;
        @Configure(path = CONFIG_PATH + "PushCutoff", config = Config.ABILITIES)
        private double pushCutoff = 2.5;
        @Configure(path = CONFIG_PATH + "FreezeRadius", config = Config.ABILITIES)
        private double freezeRadius = 3.0;
        @Configure(path = CONFIG_PATH + "FreezeRevertTime", config = Config.ABILITIES)
        private long freezeRevertTime = -1;
        @Configure(path = CONFIG_PATH + "IsFreezeUsable", config = Config.ABILITIES)
        private boolean isFreezeUsable = false;
        @Configure(path = CONFIG_PATH + "GlobalFreezeCooldown", config = Config.ABILITIES)
        private long globalFreezeCD = 0;
    }
}
