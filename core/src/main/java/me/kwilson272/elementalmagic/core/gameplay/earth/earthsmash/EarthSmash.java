package me.kwilson272.elementalmagic.core.gameplay.earth.earthsmash;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class EarthSmash extends EarthAbility {

	protected static final ConfigValues CONFIG = new ConfigValues();

    private enum State {
        CHARGING,
        SOURCING,
        IDLE,
        GRABBED,
        FIRED,
        FLYING
    }

    private long cooldown;
    private long chargeDuration;
    private double selectRange;
    private double hitboxSize;
    private double sourceKnockup;
    private long sourceRevertTime;
    private long lifetime;
    private double grabRange;
    private double speed;
    private double damage;
    private double knockback;
    private double fireRange;
    private double flightSpeed;
    private long flightDuration;
    private boolean flightSlotSwapping;

    private State state;
    private World world;
    private Location location;
    private Map<Block, BlockRepresenter> smashData;
    private Map<Block, TempBlock> affectedBlocks;

    // Charging
    private long chargedTime;

    // Sourcing
    private Block source;
    private Set<Block> sourceBlocks;

    // Idle
    private long endTime;
    private long remainingTime;
    
    // Grabbed
    private double renderRange;
    
    // Fired
    private Location tether;
    private Vector direction;
    
    // Riding
    private long flightEndTime;

    public EarthSmash(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        chargeDuration = CONFIG.chargeDuration;
        selectRange = CONFIG.selectRange;
        hitboxSize = CONFIG.hitboxSize;
        sourceKnockup = CONFIG.sourceKnockup;
        sourceRevertTime = CONFIG.sourceRevertTime;
        lifetime = CONFIG.lifetime;
        grabRange = CONFIG.grabRange;
        speed = CONFIG.speed;
        damage = CONFIG.damage;
        knockback = CONFIG.knockback;
        fireRange = CONFIG.fireRange;
        flightSpeed = CONFIG.flightSpeed;
        flightDuration = CONFIG.flightDuration;
        flightSlotSwapping = CONFIG.flightSlotSwapping;

        state = State.CHARGING;
        world = user.player().getWorld();
        smashData = new HashMap<>();
        affectedBlocks = new HashMap<>();
        sourceBlocks = new HashSet<>();
    }

    @Override
	public boolean start() {
        chargedTime = System.currentTimeMillis() + chargeDuration;
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)
                || !user().player().getWorld().equals(world)) {
            return false;
        }

        if ((state == State.IDLE || state == State.GRABBED) 
                && System.currentTimeMillis() > endTime) {
            return false;
        }

        return switch (state) {
            case CHARGING -> progressCharging();
            case SOURCING -> progressSourcing();
            case IDLE -> progressIdle(); // Above checks handle removal conditions
            case GRABBED -> progressGrabbed();
            case FIRED -> progressFired();
            case FLYING -> progressFlying();
        };
	}

    @Override
	public void onDestruction() {
        for (Block block : affectedBlocks.keySet()) {
            BlockData data = smashData.get(block).data();
            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            world.spawnParticle(Particle.BLOCK, loc, 5, 0, 0, 0, data);

            TempBlock tb = affectedBlocks.get(block);
            ElementalMagicApi.revertibleManager().revert(tb);
        
        }
	}

	@Override
	public String name() {
        return "EarthSmash";
	}

/*********************************
* General
**********************************/

    private void removeInvalidBlocks() {
        Iterator<Block> iter = affectedBlocks.keySet().iterator();
        while (iter.hasNext()) {
            Block block = iter.next();
            TempBlock tb = affectedBlocks.get(block);
            if (tb.isReverted()) {
                smashData.remove(block);
                iter.remove();
                continue;
            }

            BlockData data = smashData.get(block).data();
            if (!block.getBlockData().equals(data)) {
                smashData.remove(block);
                iter.remove();
            }
        }
    }

    private void revertSmash() {
        for (TempBlock tb : affectedBlocks.values()) {
            ElementalMagicApi.revertibleManager().revert(tb);
        }
        affectedBlocks.clear();
    }

    private void renderSmash() {
        Map<Block, BlockRepresenter> newData = new HashMap<>();
        for (BlockRepresenter representer : smashData.values()) {
            Location loc = location.clone().add(representer.offset());
            Block block = loc.getBlock();

            if (Blocks.isSolid(block) && !sourceBlocks.contains(block)) {
                continue;
            }

            TempBlock.builder(this, representer.data).buildAt(block)
                .ifPresent(tb -> {
                    affectedBlocks.put(block, tb);
                    newData.put(block, representer);
                });
        }

        smashData = newData;
    }

/*********************************
* Charging 
**********************************/
    
    private boolean progressCharging() {
        if (!user().getSelectedBindName().equals(name())) {
            return false;
        }

        if (System.currentTimeMillis() > chargedTime) {
            if (!user().player().isSneaking()) {
                return initSourcing();
            } 

            Player player = user().player();
            Location loc = Entities.getTargetLocation(player, selectRange);
            world.spawnParticle(Particle.SMOKE, loc, 5, 1.0, 0.3, 1.0, 0);
            return true;
        }

        return user().player().isSneaking();
    }

/*********************************
* Sourcing 
**********************************/

    private boolean initSourcing() {
        source = selectSource(selectRange);
        if (source == null) {
            return false;
        }

        location = source.getLocation().add(0.5, 0.5, 0.5);
        initSmashData();
        if (smashData.size() < 4) {
            return false;
        }

        consumeSource();
        playEarthSound(location);
        state = State.SOURCING;
        user().addCooldown(name(), cooldown);
        return true;
    }

// To maintain the appearance of EarthSmash, the math utilized in these two 
// following methods is taken directly from ProjectKorra's EarthSmash.
    private void initSmashData() {
        Location loc = location.clone().add(0, -2, 0);
        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                for (int z = -1; z <= 1; ++z) {
                    if ((Math.abs(x) + Math.abs(y) + Math.abs(z)) % 2 != 0) {
                        continue;
                    }

                    Vector offset = new Vector(x, y, z);
                    Block block = loc.clone().add(offset).getBlock();
                    if (isUsableEarth(block)) {
                        BlockData data = block.getBlockData();
                        smashData.put(block, new BlockRepresenter(offset, data));
                    }
                }
            }
        }
    }

    private void consumeSource() {
        BlockData data = Material.AIR.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data)
            .setDuration(sourceRevertTime);

        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 0; ++y) {
                for (int z = -1; z <= 1; ++z) {
                    if ((Math.abs(x) + Math.abs(z)) % 2 == 0 && y == -1) {
                        continue;
                    }
                   
                    Block block = location.clone().add(x, y, z).getBlock();
                    if (isUsableEarth(block)) {
                        builder.buildAt(block)
                            .ifPresent(tb -> sourceBlocks.add(block));
                    }   
                }
            }
        }
    }

    private boolean progressSourcing() {
        if (smashData.isEmpty()) {
            return false;
        }

        revertSmash();
        renderSmash();
        knockEntitiesUp();
        location.add(new Vector(0, 1, 0));
        if (location.getBlockY() - source.getY() >= 4) {
            endTime = System.currentTimeMillis() + lifetime;
            state = State.IDLE;
        }

        return true;
    }
    
    private void knockEntitiesUp() {
        Set<Entity> noAffect = new HashSet<>();
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        Vector knock = new Vector(0, sourceKnockup, 0);

        for (Block block : affectedBlocks.keySet()) {
            BoundingVolume bv = AABB.fromBlock(block, hitboxSize);
            for (Entity e : Entities.getNearbyEntities(world, bv)) {
                if (!noAffect.contains(e)) {
                    effectHandler.setVelocity(e, this, knock);
                    noAffect.add(e);
                }
            } 
        }
    }

/*********************************
* Idle
**********************************/
    
    private boolean progressIdle() {
        removeInvalidBlocks();
        return !smashData.isEmpty() && !affectedBlocks.isEmpty();
    }

/*********************************
* Grabbed 
**********************************/

    protected boolean grab(AbilityUser user) {
        if (state != State.IDLE && state != State.FIRED) {
            return false;
        }

        Player player = user.player();
        Block target = Entities.getTargetBlock(player, grabRange,
                b -> affectedBlocks.containsKey(b) || Blocks.isSolid(b));
        if (!affectedBlocks.containsKey(target)) {
            return false;
        }

        if (!user.equals(user()) 
                && !ElementalMagicApi.abilityManager().changeOwner(this, user)) {
            return false;
        }
        
        if (state == State.FIRED) {
            endTime = System.currentTimeMillis() + remainingTime;
        }

        renderRange = location.distance(player.getEyeLocation());
        state = State.GRABBED;
        return true;
    }

    private boolean progressGrabbed() {
        if (!user().player().isSneaking()
                || !user().getSelectedBindName().equals("EarthSmash")) {
            state = State.IDLE;
            return true;
        }

        Location loc = user().player().getEyeLocation();
        Vector direction = loc.getDirection();
        loc.add(direction.multiply(renderRange));

        if (isLocationSafe(loc))  {
            location = loc;
            revertSmash();
            renderSmash();
        }
        return true;
    }

    private boolean isLocationSafe(Location center) {
        for (BlockRepresenter representer : smashData.values()) {
            Location loc = center.clone().add(representer.offset());
            Block block = loc.getBlock();

            if (Blocks.isSolid(block) && !sourceBlocks.contains(block)
                    && !affectedBlocks.containsKey(block)) {
                return false;
            }
        }

        return true;
    }

/*********************************
* Fired 
**********************************/

    protected void fire() {
        if (state != State.GRABBED) {
            return;
        }

        direction = user().player().getEyeLocation().getDirection();
        tether = user().player().getEyeLocation();
        
        playEarthSound(location);
        remainingTime = endTime - System.currentTimeMillis();
        state = State.FIRED;
    }   

    private boolean progressFired() {
        double remainder = speed;
        while (remainder > 0) {
            double travel = Math.min(remainder, 1);
            remainder--;

            location.add(direction.clone().multiply(travel));
            revertSmash();
            renderSmash();
            if (affectedBlocks.size() < 4) {
                return false;
            }

            affectEntities();

            if (location.distanceSquared(tether) > fireRange * fireRange) {
                return false;
            }
        }

        return true;
    }

    private void affectEntities() {
        Set<Entity> noAffect = new HashSet<>();
        noAffect.add(user().player());
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();

        for (Block block : affectedBlocks.keySet()) {
            BoundingVolume bv = AABB.fromBlock(block, hitboxSize);
            for (Entity e : Entities.getNearbyEntities(world, bv)) {
                if (!noAffect.contains(e)) {
                    Vector knock = Vectors.getDirection(location, e.getLocation());
                    knock.normalize().multiply(knockback);
                    effectHandler.setVelocity(e, this, knock);
                    effectHandler.damageEntity(e, this, damage);
                    noAffect.add(e);
                }
            }
        }
    }

/*********************************
* Flying
**********************************/

    protected void ride(AbilityUser user) {
        if (state != State.GRABBED && state != State.IDLE) {
            return;
        }
        
        Player player = user.player();
        Block target = Entities.getTargetBlock(player, 5, Blocks::isSolid);
        if (!affectedBlocks.containsKey(target) || (!user.equals(user())
                && ElementalMagicApi.abilityManager().changeOwner(this, user))) {
            return;
        }
        
        Location loc = location.clone().add(0, 2, 0);
        loc.setDirection(player.getEyeLocation().getDirection());
        player.teleport(loc);

        flightEndTime = System.currentTimeMillis() + flightDuration;
        state = State.FLYING;
    }

    private boolean progressFlying() {
        if (!user().player().isSneaking()
                || System.currentTimeMillis() > flightEndTime
                || (!flightSlotSwapping && !user().getSelectedBindName().equals(name()))) {
            return false; 
        }

        Player player = user().player();
        Vector dir = player.getEyeLocation().getDirection().multiply(flightSpeed);
        ElementalMagicApi.effectHandler().setVelocity(player, this, dir);
        dragEntities(dir);

        // This number comes from PK; prevent the player from clipping
        if (dir.getY() <= -0.35) {
            location = player.getLocation().add(0, -3, 0);
        } else {
            location = player.getLocation().add(0, -2, 0);
        }
        revertSmash();
        renderSmash();

        return true;
    }

    private void dragEntities(Vector velocity) {
        Set<Entity> noAffect = new HashSet<>();
        noAffect.add(user().player());
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();

        for (Block block : affectedBlocks.keySet()) {
            block = block.getRelative(BlockFace.UP);
            BoundingVolume bv = AABB.fromBlock(block, hitboxSize);
            for (Entity e : Entities.getNearbyEntities(world, bv)) {
                if (!noAffect.contains(e)) {
                    effectHandler.setVelocity(e, this, velocity);
                    noAffect.add(e);
                }
            }
        }
    }

    private record BlockRepresenter(Vector offset, BlockData data) {} 

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = EarthSmashController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 5000;
        @Configure(path = CONFIG_PATH + "ChargeDuration", config = Config.ABILITIES)
        private long chargeDuration = 1500;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 7.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.8;
        @Configure(path = CONFIG_PATH + "SourceKnockup", config = Config.ABILITIES)
        private double sourceKnockup = 0.7;
        @Configure(path = CONFIG_PATH + "SourceRevertTime", config = Config.ABILITIES)
        private long sourceRevertTime = 5000;
        @Configure(path = CONFIG_PATH + "Lifetime", config = Config.ABILITIES)
        private long lifetime = 20000;
        @Configure(path = CONFIG_PATH + "GrabRange", config = Config.ABILITIES)
        private double grabRange = 12;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 3.0;
        @Configure(path = CONFIG_PATH + "Knockback", config = Config.ABILITIES)
        private double knockback = 1.5;
        @Configure(path = CONFIG_PATH + "FireRange", config = Config.ABILITIES)
        private double fireRange = 25;
        @Configure(path = CONFIG_PATH + "FlightSpeed", config = Config.ABILITIES)
        private double flightSpeed = 0.7;
        @Configure(path = CONFIG_PATH + "FlightDuration", config = Config.ABILITIES)
        private long flightDuration = 3000;
        @Configure(path = CONFIG_PATH + "FlightSlotSwapping", config = Config.ABILITIES)
        private boolean flightSlotSwapping = false;
    }
}
