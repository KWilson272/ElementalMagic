package me.kwilson272.elementalmagic.core.gameplay.water.torrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
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
import me.kwilson272.elementalmagic.core.gameplay.components.TravelingSource;
import me.kwilson272.elementalmagic.core.gameplay.components.TravelingSource.TravelState;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterUtil;

public class Torrent extends CoreAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    protected static enum State {
        SOURCED,
        SOURCE_TRAVELING,
        CHARGING,
        FIRED,
        FREEZE_MAINTENANCE
    }

    private long cooldown;
    private long chargeTime;
    private boolean chargeUnderwater;
    private long sourceRevertTime;
    private double selectRange;
    private double ringDamage;
    private double ringHitbox;
    private double ringKnockBack;
    private double ringRadius;
    private boolean collidableRing;
    private double streamRange;
    private double streamDamage;
    private boolean doDoubleHit;
    private double streamDrag;
    private double streamHitbox;
    private boolean collidableStream;
    private long freezeRevertTime;
    private long freezeCooldown;
    private int freezeRadiusStart;
    private int freezeRadiusMax;

    private State state;

    // SOURCED
    private Block source;

    // SOURCE_TRAVELING
    TravelingSource movingSource;

    // CHARGING
    private long chargedTime;
    private double startAngle;
    private double ringAngle;
    private boolean fire;
    private boolean isCharged;

    // FIRED
    private Location location;
    private Vector direction;
    private boolean collided;
    private boolean freeze;
    private double freezeRadius;

    private boolean hitTwice;
    private Set<Entity> hitEntities;
    private Deque<TempBlock> blocks;
    private Map<Block, TempBlock> affectedBlocks;

    // FREEZE_MAINTENANCE
    private Map<Block, TempBlock> frozenBlocks;

    public Torrent(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        chargeTime = CONFIG.chargeTime;
        chargeUnderwater = CONFIG.chargeUnderwater;
        selectRange = CONFIG.selectRange;
        sourceRevertTime = CONFIG.sourceRevertTime;
        ringDamage = CONFIG.ringDamage;
        ringHitbox = CONFIG.ringHitbox;
        ringKnockBack = CONFIG.ringKnockBack;
        ringRadius = CONFIG.ringRadius;
        collidableRing = CONFIG.collidableRing;
        streamRange = CONFIG.streamRange;
        streamDamage = CONFIG.streamDamage;
        doDoubleHit = CONFIG.doDoubleHit;
        streamDrag = CONFIG.streamDrag;
        streamHitbox = CONFIG.streamHitbox;
        collidableStream = CONFIG.collidableStream;
        freezeRevertTime = CONFIG.freezeRevertTime;
        freezeCooldown = CONFIG.freezeCooldown;
        freezeRadiusStart = CONFIG.freezeRadiusStart;
        freezeRadiusMax = CONFIG.freezeRadiusMax;

        state = State.SOURCED;

        ringAngle = 0;
        fire = false;
        isCharged = false;
        collided = false;
        freeze = false;

        hitTwice = false;
        hitEntities = new HashSet<>();
        blocks = new ArrayDeque<>();
        affectedBlocks = new HashMap<>();
        frozenBlocks = new HashMap<>();
    }

    @Override
    public boolean start() {
        freezeRadius = freezeRadiusStart;
        source = WaterUtil.getSourceBlock(user(), selectRange);
        return source != null;
    }

    protected boolean isCharged() {
        return state == State.CHARGING && isCharged;
    }

    protected boolean handleLeftClick() {
        if (state == State.FREEZE_MAINTENANCE) {
            return false;
        }

        if (state != State.SOURCED) {
            fire = true;
        } 
        if (state == State.FIRED && !collided 
                && !user().isOnCooldown("GlobalFreeze")) {
            freeze = true;
        }
        return true;
    }

    @Override
    public boolean progress() {
        boolean checkBinds = state != State.FREEZE_MAINTENANCE;
        if (!user().canUse(controller(), checkBinds, false)) {
            return false;
        } 

        if (!user().player().isSneaking() && state != State.SOURCED) {
            cleanOldBlocks(blocks.size());
            state = State.FREEZE_MAINTENANCE;
        }
        
        return switch (state) {
            case SOURCED -> progressSourced(); 
            case SOURCE_TRAVELING -> progressSourceTraveling();
            case CHARGING -> progressCharging();
            case FIRED -> progressFired();
            case FREEZE_MAINTENANCE -> progressFreezeMaintenance();
        };
    }

/*********************************
 * GENERAL
 *********************************/
    private void placeWater(Block block) {
        if (block.getType() == Material.WATER) {
            World world = block.getWorld();
            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            world.spawnParticle(Particle.BUBBLE, loc, 2, 0.5, 0.5, 0.5, 0);
        }

        boolean collidable = state == State.FIRED ? collidableStream : collidableRing;
        BlockData data = Material.WATER.createBlockData();
        TempBlock.builder(this, data)
            .setCollidable(collidable)
            .buildAt(block)
            .ifPresent(tb -> {
                affectedBlocks.put(block, tb);
                blocks.offerFirst(tb);
            });
    }

    private void cleanOldBlocks(int amount) {
        int counter = 0;
        while(!blocks.isEmpty() && counter < amount) {
            TempBlock tb = blocks.pollLast();
            ElementalMagicApi.revertibleManager().revert(tb);
            affectedBlocks.remove(tb.block());
            ++counter;
        }
    }

    @Override
    public void onDestruction() {
        if (movingSource != null) {
            movingSource.revertBlocks();
        }

        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        blocks.forEach(revertManager::revert);
        frozenBlocks.values().forEach(revertManager::revert);
    }

    @Override
    public String name() {
        return "Torrent";
    }

/*********************************
 * SOURCED
 *********************************/
    private boolean progressSourced() {
        if (!isSourceViable()) {
            return false;
        }
        
        if (user().player().isSneaking()) {
            initTravelingSource();
        }
    
        WaterUtil.playSourceSelectedEffect(source);
        return true;
    }

    private boolean isSourceViable() {
        Location eyeLoc = user().player().getEyeLocation();
        Location sourceLoc = source.getLocation().add(0.5, 0.5, 0.5);
        double maxDist = Math.pow(selectRange + 1, 2);
        if (!eyeLoc.getWorld().equals(sourceLoc.getWorld())
                || eyeLoc.distanceSquared(sourceLoc) > maxDist) {
            return false;
        }
        
        return eyeLoc.getWorld().equals(sourceLoc.getWorld())
            && eyeLoc.distanceSquared(sourceLoc) <= maxDist
            && WaterUtil.canUse(source, user());
    }

/*********************************
 * SOURCE TRAVELING
 *********************************/
    private void initTravelingSource() {
        Location loc = source.getLocation();
        BlockData data = Material.WATER.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data)
            .setCollidable(collidableRing);
        movingSource = new TravelingSource(loc, 1, true, builder);

        WaterUtil.consumeSource(this, source, sourceRevertTime);
        state = State.SOURCE_TRAVELING;
    }

    private boolean progressSourceTraveling() {
        Location dest = user().player().getEyeLocation();
        var tState = movingSource.moveTowards(dest, ringRadius);
        if (tState == TravelState.ARRIVED) {
            initCharging();
        }

        return tState != TravelState.BLOCKED;
    }

/*********************************
 * CHARGING
 *********************************/
    private void initCharging() {
        initializeRingAngle(movingSource.getLocation());
        movingSource.revertBlocks();
        movingSource = null;

        chargedTime = System.currentTimeMillis() + chargeTime;
        state = State.CHARGING;
    }

    private void initializeRingAngle(Location startLoc) {
        Location eyeLoc = user().player().getEyeLocation();
        Vector toSource = VectorUtil.getDirection(eyeLoc, startLoc);
        startAngle = Math.atan2(toSource.getX(), toSource.getZ());
        startAngle = Math.toDegrees(startAngle);
    }

    private boolean progressCharging() {
        if (System.currentTimeMillis() > chargedTime) {
            isCharged = true;
        }

        if (isCharged && fire) {
            initFired();
        }

        progressRing();
        if (ThreadLocalRandom.current().nextInt(7) == 0) {
            WaterUtil.playWaterSound(user().player().getLocation());
        }
        return !blocks.isEmpty();
    }

    private void progressRing() {
        Set<Block> toRevert = new HashSet<>(affectedBlocks.keySet());

        double startRad = Math.toRadians(startAngle);
        double angleRad = Math.toRadians(ringAngle);
        double blockSpacing = 1.0;
        double step = 2 * Math.asin(blockSpacing / (2 * ringRadius));
        Location center = user().player().getEyeLocation();

        for (double i = startRad; i >= startRad - angleRad; i -= step) {
            double x = Math.sin(i) * ringRadius;
            double z = Math.cos(i) * ringRadius;
            Block block = center.clone().add(x, 0, z).getBlock();

            if (BlockUtil.isSolid(block)) {
                continue;
            }

            if (affectedBlocks.containsKey(block)) {
                TempBlock tb = affectedBlocks.get(block);
                // re-insert to keep order
                blocks.remove(tb);
                blocks.offerFirst(tb);
            } else if (chargeUnderwater || !AbilityUtil.isWater(block)) {
                placeWater(block);
            }

            toRevert.remove(block);
            if (affectedBlocks.containsKey(block)) {
                doRingDamage(block, angleRad);
            }
        }

        // I want to keep the animation as close to PK's as possible:
        final double animInc = 30;
        final double growthFactor = 20;
        final double maxAngle = 220;
        startAngle -= animInc;
        ringAngle = Math.min(maxAngle, ringAngle + growthFactor);

        for (Block block : toRevert) {
            TempBlock tb = affectedBlocks.remove(block);
            ElementalMagicApi.revertibleManager().revert(tb);
            blocks.remove(tb);
        }
    }

    private void doRingDamage(Block block, double ringAngle) {
        // Rotate the angle by 90 degrees to get a tangent vector
        ringAngle += Math.toRadians(90);
        double x = -Math.sin(ringAngle) * ringKnockBack;
        double z = -Math.cos(ringAngle) * ringKnockBack;
        Vector knock = new Vector(x, 0, z);
        
        World world = user().player().getWorld();
        BoundingVolume bv = AABB.fromBlock(block, ringHitbox);
        for (Entity e : EntityUtil.getNearbyEntities(world, bv)) {
            if (!e.equals(user().player())) {
                ElementalMagicApi.effectHandler().setVelocity(e, this, knock);
                ElementalMagicApi.effectHandler().damageEntity(e, this, ringDamage);
            }
        }
    }

/*********************************
 * FIRED
 *********************************/
    private void initFired() {
        Block leader = blocks.peekFirst().block();
        location = leader.getLocation().add(0.5, 0.5, 0.5);
        user().addCooldown("Torrent", cooldown);
        state = State.FIRED;
    }

    private boolean progressFired() {
        progressStream();
        if (!hitTwice) {
            affectEntities();
        }
        
        handleFreeze();
        if (blocks.isEmpty() && (!freeze || freezeRadius > freezeRadiusMax)) {
            state = State.FREEZE_MAINTENANCE;
        }
        return true;
    }

    private void progressStream() {
        if (collided) {
            cleanOldBlocks(1);
            return;
        }

        Location target = getTargetedLocation();
        direction = VectorUtil.getDirection(location, target).normalize();
        Block oldBlock = location.getBlock();
        Block newBlock = location.add(direction).getBlock();

        double maxDist = streamRange * streamRange;
        Location eyeLoc = user().player().getEyeLocation();
        if (eyeLoc.distanceSquared(location) > maxDist) {
            collided = true;
            freeze = false; // No freezes in air
            doDoubleHit = false;
        } else if (BlockUtil.isSolid(newBlock)) {
            collided = true;    
        }

        if (!oldBlock.equals(newBlock) && !collided) {
            placeWater(newBlock);
            cleanOldBlocks(1);
        }
    }

    private Location getTargetedLocation() {
        // Ensures that the stream will go out of range
        double range = streamRange + 2;

        LivingEntity entity = user().player();
        Block targBlock = BlockUtil.getTargetBlock(entity, range, BlockUtil::isSolid);
        Location blockLoc = targBlock.getLocation().add(0.5, 0.5, 0.5);

        World world = entity.getWorld();
        Location start = entity.getEyeLocation();
        Vector dir = start.getDirection();

        RayTraceResult trace = world.rayTraceEntities(start, dir, range, 1.5,
                e -> canTarget(e, targBlock));
        Entity hit = trace != null ? trace.getHitEntity() : null;
        return hit != null ? hit.getLocation() : blockLoc;
    }

    private boolean canTarget(Entity entity, Block targetBlock) {
        if (entity.equals(user().player())
                || !ElementalMagicApi.effectHandler().canAffect(entity) 
                || !(entity instanceof LivingEntity le) 
                || hitEntities.contains(entity)) {
            return false;
        }

        Location eye = user().player().getEyeLocation();
        Location blockLoc = targetBlock.getLocation().add(0.5, 0.5, 0.5);
        double blockDist = eye.distanceSquared(blockLoc);
        return le.getLocation().distanceSquared(eye) <= blockDist 
            || le.getEyeLocation().distanceSquared(eye) <= blockDist;
    }

    private void affectEntities() {
        Set<Entity> affected = new HashSet<>();
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();

        Block before = null;
        for (TempBlock tb : blocks) {
            Block block = tb.block();
            Vector knock = before == null ? direction.clone() :
                VectorUtil.getDirection(block.getLocation(), before.getLocation());
            knock.normalize().multiply(streamDrag);
            

            World world = block.getWorld();
            BoundingVolume bv = AABB.fromBlock(block, streamHitbox);
            for (Entity entity : EntityUtil.getNearbyEntities(world, bv)) {
                if (entity.equals(user().player())) {
                    continue;
                }

                effectHandler.setVelocity(entity, this, knock);
                if (affected.contains(entity)
                    // Don't allow items to trigger double hit removals
                    || !(entity instanceof LivingEntity)) {
                    continue;
                }

                if (!hitEntities.contains(entity) || (doDoubleHit && collided)) {
                    ((LivingEntity) entity).setNoDamageTicks(0);
                    effectHandler.damageEntity(entity, this, streamDamage);
                    affected.add(entity);
                    hitTwice = hitEntities.contains(entity);
                    hitEntities.add(entity);
                }
            }
            before = block;
        }
    }

    private void handleFreeze() {
        if (!collided || !freeze || freezeRadius > freezeRadiusMax) {
            return;
        }

        if (!user().isOnCooldown("GlobalFreeze")) {
            user().addCooldown("GlobalFreeze", freezeCooldown);
        }

        for (Block b : BlockUtil.collectSphere(location, freezeRadius)) {
            freezeBlock(b);
        }

        WaterUtil.playIceSound(location);
        ++freezeRadius;
    }

    private void freezeBlock(Block block) {
        if (frozenBlocks.containsKey(block) || BlockUtil.isSolid(block)) {
            return;
        }

        BlockData iceData = Material.ICE.createBlockData();
        TempBlock.builder(this, iceData)
            .setDuration(freezeRevertTime)
            .buildAt(block)
            .ifPresent(tb -> {
                frozenBlocks.put(block, tb);
                if (ThreadLocalRandom.current().nextInt(5) == 0) {
                    World world = block.getWorld();
                    Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                    Particle particle = Particle.SNOWFLAKE;
                    world.spawnParticle(particle, loc, 1, 0.5, 0.5, 0.5, 0);
                }
            });
    }

/*********************************
 * FREEZE MAINTENANCE
 *********************************/
    private boolean progressFreezeMaintenance() {
        Location userLoc = user().player().getEyeLocation();
        double maxDist = Math.pow(streamRange + 2, 2);
        List<Block> toRevert = new ArrayList<>();

        for (Block block : frozenBlocks.keySet()) {
            Location blockLoc = block.getLocation().add(0.5, 0.5, 0.5);
            if (blockLoc.distanceSquared(userLoc) >= maxDist 
                    || frozenBlocks.get(block).isReverted()) {
                toRevert.add(block);
            }
        }

        for (Block block : toRevert) {
            TempBlock tb = frozenBlocks.remove(block);
            ElementalMagicApi.revertibleManager().revert(tb);
        }

        return !frozenBlocks.isEmpty();
    }

/*********************************
 * CONFIG
 *********************************/
    protected static class ConfigValues {

        private static final String BASE_PATH = TorrentController.CONFIG_PATH;
        private static final String RING_PATH = TorrentController.CONFIG_PATH + "Ring.";
        private static final String STREAM_PATH = TorrentController.CONFIG_PATH + "Stream.";

        @Configure(path = BASE_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 2000;
        @Configure(path = BASE_PATH + "ChargeTime", config = Config.ABILITIES)
        private long chargeTime = 400;
        @Configure(path = BASE_PATH + "CanChargeUnderWater", config = Config.ABILITIES)
        private boolean chargeUnderwater = false;
        @Configure(path = BASE_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 18.0;
        @Configure(path = BASE_PATH + "SourceRevertTime", config = Config.ABILITIES)
        private long sourceRevertTime = 10000;
        @Configure(path = RING_PATH + "Damage", config = Config.ABILITIES)
        private double ringDamage = 2.0;
        @Configure(path = RING_PATH + "HitboxSize", config = Config.ABILITIES)
        private double ringHitbox = 1.2;
        @Configure(path = RING_PATH + "KnockBack", config = Config.ABILITIES)
        private double ringKnockBack = 1.5;
        @Configure(path = RING_PATH + "Radius", config = Config.ABILITIES)
        private double ringRadius = 3.0;
        @Configure(path = RING_PATH + "IsRingCollidable", config = Config.ABILITIES)
        private boolean collidableRing = true;
        @Configure(path = STREAM_PATH + "Range", config = Config.ABILITIES)
        private double streamRange = 30.0;
        @Configure(path = STREAM_PATH + "Damage", config = Config.ABILITIES)
        private double streamDamage = 2.0;
        @Configure(path = STREAM_PATH + "DoDoubleHit", config = Config.ABILITIES)
        private boolean doDoubleHit = true;
        @Configure(path = STREAM_PATH + "DragStrength", config = Config.ABILITIES)
        private double streamDrag = 1.0;
        @Configure(path = STREAM_PATH + "HitboxSize", config = Config.ABILITIES)
        private double streamHitbox = 1.2;
        @Configure(path = STREAM_PATH + "FreezeRevertTime", config = Config.ABILITIES)
        private long freezeRevertTime = -1;
        @Configure(path = STREAM_PATH + "FreezeRadiusStart", config = Config.ABILITIES)
        private int freezeRadiusStart = 1;
        @Configure(path = STREAM_PATH + "FreezeRadiusMax", config = Config.ABILITIES)
        private int freezeRadiusMax = 3;
        @Configure(path = STREAM_PATH + "GlobalFreezeCooldown", config = Config.ABILITIES)
        private long freezeCooldown = 0;
        @Configure(path = STREAM_PATH + "IsStreamCollidable", config = Config.ABILITIES)
        private boolean collidableStream = false;
    }
}
