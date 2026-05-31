package me.kwilson272.elementalmagic.core.gameplay.water.waterblade;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.components.TravelingSource;
import me.kwilson272.elementalmagic.core.gameplay.components.TravelingSource.TravelState;
import me.kwilson272.elementalmagic.core.gameplay.water.WaterAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;

public class WaterBlade extends WaterAbility {
    
    protected static final ConfigValues CONFIG = new ConfigValues();

    private enum State {
        SOURCED,
        SOURCE_TRAVELING,
        HOLDING,
        FIRED
    }

    private long cooldown;
    private double selectRange;
    private double sourceSpeed;
    private double range;
    private double speed;
    private int length;
    private double maxRadius;
    private double damage;
    private double knockback;
    private double hitboxSize;
    private boolean canSwapSlots;

    private State state;
    private BlockData bladeData;
    private boolean isSnow;
    private boolean isIce;
    private boolean isPlant;
    private TempBlockBuilder blockBuilder;

    // SOURCED
    private Block source;

    // SOURCE_TRAVELING
    private TravelingSource travelingSource;

    // HOLDING
    private double animAngle;

    // FIRED
    private double curRadius;
    private double growthFactor;
    private double rangeCounter;
    private Location location;
    private Vector direction;
    private Map<Vector, Location> bladeVecs;
    private Deque<List<TempBlock>> layers;
    private Set<Entity> noAffect;

    public WaterBlade(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        sourceSpeed = CONFIG.sourceSpeed;
        range = CONFIG.range;
        speed = CONFIG.speed;
        length = CONFIG.length;
        maxRadius = CONFIG.maxRadius;
        damage = CONFIG.damage;
        knockback = CONFIG.knockback;
        hitboxSize = CONFIG.hitboxSize;
        canSwapSlots = CONFIG.canSwapSlots;
        
        state = State.SOURCED;
        isSnow = false;
        isIce = false;
        isPlant = false;
        animAngle = 0;

        bladeVecs = new HashMap<>();
        layers = new ArrayDeque<>();
        noAffect = new HashSet<>();
        noAffect.add(user.player());
    }

	@Override
	public boolean start() {
        source = selectSourceBlock(selectRange); 
        return source != null;
	}


	@Override
	public boolean progress() {
        boolean checkSelected = state != State.FIRED || !canSwapSlots;
        if (!user().canUse(controller(), checkSelected, false)) {
            return false;
        }

        return switch(state) {
            case SOURCED -> progressSourced();
            case SOURCE_TRAVELING -> progressSourceTraveling();
            case HOLDING -> progressHolding();
            case FIRED -> progressFired();
        };
	}

    private boolean progressSourced() {
        if (!isSourceViable()) {
            return false;
        }

        if (user().player().isSneaking()) {
            initSourceTraveling();
        }

        playSourceSelectedEffect(source);
        return true;
    }

    private boolean isSourceViable() {
        Location eyeLoc = user().player().getEyeLocation();
        Location sourceLoc = source.getLocation().add(0.5, 0.5, 0.5);
        double maxDist = Math.pow(selectRange + 1, 2);

        return eyeLoc.getWorld().equals(sourceLoc.getWorld())
            && eyeLoc.distanceSquared(sourceLoc) <= maxDist
            && canUse(source, user());
    }

    private void initSourceTraveling() {
        Location loc = source.getLocation().add(0.5, 0.5, 0.5);
        
        // Water default because some water blocks have things 
        // inside them and it would look bad, ex: Kelp 
        bladeData = Material.WATER.createBlockData();
        if (Blocks.isIce(source)) {
            bladeData = source.getBlockData();
            isIce = true;
        } else if (Blocks.isSnow(source)) {
            bladeData = Material.SNOW_BLOCK.createBlockData();
            isSnow = true;
        } else if (Blocks.isPlant(source)) {
            bladeData = getSolidPlant(source);
            isPlant = true;
        } 
        
        blockBuilder = TempBlock.builder(this, bladeData);
        travelingSource = new TravelingSource(loc, sourceSpeed, false, blockBuilder);
        state = State.SOURCE_TRAVELING;
    }

    private boolean progressSourceTraveling() {
        World world = user().player().getWorld();
        if (!world.equals(travelingSource.getLocation().getWorld())
                || !user().player().isSneaking()) {
            return false;
        }

        Location loc = user().player().getEyeLocation();
        var tState = travelingSource.moveTowards(loc, 1);
        if (tState == TravelState.ARRIVED) {
            travelingSource.revertBlocks();
            travelingSource = null;
            state = State.HOLDING;
        }

        return tState != TravelState.BLOCKED;
    }

    private boolean progressHolding() {
        if (!user().player().isSneaking()) {
            initFired();
        }

        double spacing = 0.25;
        double radius = 1.25;
        double step = 2 * Math.asin(spacing / (2 * radius));
        for (int i = 0; i < 5; ++i) {
            animAngle += step;
            double x = Math.cos(animAngle) * radius;
            double z = Math.sin(animAngle) * radius;
            Location loc = user().player().getLocation().add(x, 1, z);
            playParticles(loc);
        }

        return true;
    }

    private void initFired() {
        initSpawnLocation();
        initBladeVecs();
        direction = user().player().getEyeLocation().getDirection();
    
        curRadius = 0.5;
        rangeCounter = range;
        // 1.5 * so we can reach full size before we terminate
        growthFactor = 1.5 * (maxRadius - curRadius) / range;

        user().addCooldown("WaterBlade", cooldown);
        state = State.FIRED;
    }

    private void initBladeVecs() {
        double blockSpacing = 0.5;
        double step = 2 * Math.asin(blockSpacing / (2 * maxRadius));
        double count = (int) Math.ceil(Math.PI / step); // 180 deg blade

        Location eyes = user().player().getEyeLocation();
        double yawRad = Math.toRadians(eyes.getYaw());
        double pitchRad = Math.toRadians(-eyes.getPitch() - 90);
        double x = -Math.sin(yawRad);
        double z = Math.cos(yawRad);

        for (int i = 0; i <= count; ++i) {
            double angle = pitchRad + (i * step);
            double y = Math.sin(angle);
            double xzMag = Math.cos(angle);
            Vector v = new Vector(x * xzMag, y, z * xzMag);
            bladeVecs.put(v, location);
        }
    }

    private void initSpawnLocation() {
        // Try not to spawn in the player b/c suffocation problems
        double spawnDist = 1.5;
        double checkStep = 0.2;
        Location eyes = user().player().getEyeLocation();
        Vector dir = eyes.getDirection();
        location = eyes.add(dir.clone().multiply(spawnDist));
        dir.multiply(checkStep);

        while (Blocks.isSolid(location.getBlock()) && spawnDist > 0) {
            location.subtract(dir); 
            spawnDist -= checkStep;
        }
    }

    private boolean progressFired() {
        if (rangeCounter <= 0) {
            if (layers.size() == 0) {
                return false;
            }
            cleanLayers((int) Math.ceil(speed));
            return true;
        }
        
        double remainder = speed; 
        while (remainder > 0) {
            double travel = Math.min(remainder, 1);
            location.add(direction.clone().multiply(travel));

            if (curRadius < maxRadius) {
                curRadius += growthFactor;
                // We need to do this so the increase in radius doesn't give us
                // a pseudo range boost and cause spaces in the blade layers
                location.subtract(direction.clone().multiply(growthFactor));
            }
            
            fillBladeLayer();
            playSound();
            remainder -= 1;
            rangeCounter -= travel;
            if (rangeCounter <= 0) {
                break;
            }
        }
        
        cleanLayers(layers.size() - length);
        return true;
    }

    private void fillBladeLayer() {
        Set<Block> affected = new HashSet<>();
        List<TempBlock> created = new ArrayList<>();
        Map<Vector, Location> newVecs = new HashMap<>();
        
        for (Vector vector : bladeVecs.keySet()) {
            Vector dir = vector.clone().multiply(curRadius);
            Location prevLoc = bladeVecs.get(vector);
            Location loc = location.clone().add(dir);
            Block block = loc.getBlock();
           
            if (isCollidable(block) 
                    || Blocks.collidesDiagonally(prevLoc, loc, this::isCollidable)) {
                continue;
            }
            
            newVecs.put(vector, loc);
            if (!affected.contains(block)) {
                blockBuilder.buildAt(block).ifPresent(tb -> {
                    created.add(tb);
                    affectEntities(loc);
                    playParticles(loc);
                    affected.add(block);
                });
            }
        }
        
        layers.offerFirst(created);
        bladeVecs = newVecs;
    }

    private boolean isCollidable(Block block) {
        if (!Blocks.isSolid(block)) {
            return false;
        }

        TempBlock tb = TempBlock.get(block).orElse(null);
        return tb == null || (tb.isCollidable() && !tb.ability().equals(this));
    }

    private void affectEntities(Location loc) {
        World world = location.getWorld();
        BoundingVolume bv = AABB.at(loc, hitboxSize);
        Vector knock = direction.clone().multiply(knockback);
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();

        for (Entity e : Entities.getNearbyEntities(world, bv)) {
            if (noAffect.contains(e)) {
                continue;    
            }

            boolean affected = false;
            affected |= effectHandler.setVelocity(e, this, knock, 1);
            affected |= effectHandler.damageEntity(e, this, damage);
            if (affected) {
                noAffect.add(e);
            }
        }
    }

    private void playParticles(Location loc) {
        World world = loc.getWorld();
        
        if (isSnow) {
            world.spawnParticle(Particle.SNOWFLAKE, loc, 1, 0.3, 0.3, 0.3, 0); 
        } else if (!isIce && !isPlant) {
            world.spawnParticle(Particle.FALLING_WATER, loc, 1, 0.2, 0.2, 0.2); 
        } else {
            world.spawnParticle(Particle.BLOCK, loc, 1, 0.2, 0.2, 0.2, bladeData);
        }
    }

    private void playSound() {
        World world = location.getWorld();
        if (isSnow) {
            playSnowSound(location); 
        } else if (isIce) {
            Sound sound = Sound.ITEM_AXE_STRIP;
            world.playSound(location, sound, 1.5f, 0.65f);
        } else if (isPlant) {
            playPlantSound(location); 
        } else {
            playWaterSound(location);
        }
    }

    private void cleanLayers(int count) {
        for (int i = 0; i < count; ++i) {
            for (TempBlock tb : layers.pollLast()) {
                ElementalMagicApi.revertibleManager().revert(tb);
            }
        }
    }

	@Override
	public void onDestruction() {
        if (travelingSource != null) {
            travelingSource.revertBlocks();
        }

        cleanLayers(layers.size());
	}

    @Override
    public String name() {
        return "WaterBlade";
    }

    protected boolean isSourced() {
        return state == State.SOURCED;
    }

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = WaterBladeController.CONFIG_PATH;
    
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 6400;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 12;
        @Configure(path = CONFIG_PATH + "SourceSpeed", config = Config.ABILITIES)
        private double sourceSpeed = 1.2;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 18;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.0;
        @Configure(path = CONFIG_PATH + "Length", config = Config.ABILITIES)
        private int length = 3;
        @Configure(path = CONFIG_PATH + "MaxRadius", config = Config.ABILITIES)
        private double maxRadius = 3.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 3.0;
        @Configure(path = CONFIG_PATH + "Knockback", config = Config.ABILITIES)
        private double knockback = 1.2;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.5;
        @Configure(path = CONFIG_PATH + "CanSwapSlots", config = Config.ABILITIES)
        private boolean canSwapSlots = true;
    }
}
