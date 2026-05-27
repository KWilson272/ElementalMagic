package me.kwilson272.elementalmagic.core.gameplay.fire.lightning;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.DustTransition;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.user.UserManager;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class Lightning extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private static final DustTransition DUST_OPTS = new DustTransition(
        Color.fromRGB(80, 240, 255),
        Color.fromRGB(10, 60, 255),
        0.8f
    );

    private long cooldown;
    private long chargeTime;
    private double range;
    private double speed;
    private double hitboxSize;
    private double damage;
    private double stunChance;
    private long stunDuration;
    private boolean chargeHitPlayers;
    private int subArcCount;
    private double subArcChance;
    private double minSubArcRange;
    private double maxSubArcRange;
    private double minSubArcAngle;
    private double maxSubArcAngle; 
    private boolean subArcInWater;
    private int waterArcCount;
    private double particleOffset;
    private int subDivisions;
    private double particleSpacing;
	
    private boolean isCharging;
    private long chargedTime;
    private int subArcsLeft;

    private List<Arc> arcs;
    private Bolt mainBolt;
    private List<Bolt> bolts;

    public Lightning(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        chargeTime = CONFIG.chargeTime;
        range = CONFIG.range;
        speed = CONFIG.speed;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        stunChance = CONFIG.stunChance;
        stunDuration = CONFIG.stunDuration;
        chargeHitPlayers = CONFIG.chargeHitPlayers;
        subArcCount = CONFIG.subArcCount;
        subArcChance = CONFIG.subArcChance;
        minSubArcRange = CONFIG.minSubArcRange;
        maxSubArcRange = CONFIG.maxSubArcRange;
        minSubArcAngle = CONFIG.minSubArcAngle;
        maxSubArcAngle = CONFIG.maxSubArcAngle;
        subArcInWater = CONFIG.subArcInWater;
        waterArcCount = CONFIG.waterArcCount;
        particleOffset = CONFIG.particleOffset;
        subDivisions = CONFIG.subDivisions;
        particleSpacing = CONFIG.particleSpacing;
        
        isCharging = true;

        arcs = new ArrayList<>();
        bolts = new ArrayList<>();
    }

	@Override
	public boolean start() {
        minSubArcAngle = Math.toRadians(minSubArcAngle);
        maxSubArcAngle = Math.toRadians(maxSubArcAngle);
        subArcsLeft = subArcCount;

        chargedTime = System.currentTimeMillis() + chargeTime;
	    return true;
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), isCharging, false)) {
            return false;
        }
        
        if (isCharging) {
            return progressCharging();
        } else {
            return progressBolts();
        }
    }

    private boolean progressCharging() {
        boolean isCharged = System.currentTimeMillis()  > chargedTime;
        if (!user().player().isSneaking()) {
            if (!isCharged) {
                return false;
            }
            spawnMainBolt();
            return true;
        }

        int count = isCharged ? 3 : 1;
        while (arcs.size() < count) {
            arcs.add(new Arc());
        }

        arcs.removeIf(arc -> !arc.progress());
        return true;
    }

    private void spawnMainBolt() {
        World world = user().player().getWorld();
        Location loc = user().player().getEyeLocation();
        Vector dir = loc.getDirection();

        mainBolt = new Bolt(loc, dir, range, true);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 0);

        isCharging = false;
        user().addCooldown(name(), cooldown); 
    }

    private boolean progressBolts() {
        if (mainBolt != null) {
            boolean remove = !mainBolt.progress();
            trySpawnSubArc();
            if (mainBolt.hitWater) {
                spawnWaterArcs();
            }
            if (remove) {
                mainBolt = null;
            }
        }

        bolts.removeIf(bolt -> !bolt.progress());
        return mainBolt != null || !bolts.isEmpty();
    } 

    private void spawnWaterArcs() {
        Location spawn = mainBolt.location;
        Random rand = ThreadLocalRandom.current();

        for (int i = 0; i < waterArcCount; ++i) {
            double randAngle = rand.nextDouble(2 * Math.PI);
            double x = Math.cos(randAngle);
            double z = Math.sin(randAngle);
            Vector dir = new Vector(x, 0, z);
            double range = rand.nextDouble(minSubArcRange, maxSubArcRange);
            bolts.add(new Bolt(spawn.clone(), dir, range, false));
        }
    }
    
    private void trySpawnSubArc() {
        if (subArcsLeft <= 0
                || ThreadLocalRandom.current().nextDouble(100) > subArcChance) {
            return;
        }
        --subArcsLeft;   

        Random rand = ThreadLocalRandom.current();
        double angleDev = rand.nextDouble(minSubArcAngle, maxSubArcAngle);
        double angleRot = rand.nextDouble(2 * Math.PI);

        Vector base = mainBolt.direction;
        Vector ortho = VectorUtil.getOrthogonal(base);
        Vector rot = VectorUtil.rotateAroundVector(base, ortho, angleRot);

        Vector vec = base.clone();
        vec.multiply(Math.cos(angleDev));
        vec.add(rot.multiply(Math.sin(angleDev)));
        
        Location loc = mainBolt.location.clone();
        double range = rand.nextDouble(minSubArcRange, maxSubArcRange);
        bolts.add(new Bolt(loc, vec, range, false));
    }

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "Lightning";
	}

    private void drawParticle(Location loc) {
        World world = loc.getWorld();
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 1, 0, 0, 0, DUST_OPTS);
    }

    private class Arc {

        private static final double LENGTH_INCREMENT = 0.15;

        private double curLength;
        private Vector particleDir;
        private Vector axis;

        private Arc() {
            curLength = 1.5; // Looks good around the player
            
            Random rand = ThreadLocalRandom.current();
            particleDir = new Vector(
                rand.nextDouble(-1, 1), 
                rand.nextDouble(-1, 1),
                rand.nextDouble(-1, 1)
            );
            Vector arcVec = new Vector(
                rand.nextDouble(-1, 1),
                rand.nextDouble(-1, 1),
                rand.nextDouble(-1, 1)
            );
            axis = particleDir.clone().crossProduct(arcVec).normalize();
            particleDir.clone().multiply(curLength);
        }

        private boolean progress() {
            particleDir = VectorUtil.rotateAroundVector(axis, particleDir, LENGTH_INCREMENT);
            Location spawnLoc = user().player().getLocation().add(0, 1, 0).add(particleDir);
            drawParticle(spawnLoc);
            
            World world = user().player().getWorld();
            world.playSound(spawnLoc, Sound.BLOCK_BEEHIVE_WORK, 1, 0.8F);
            if (ThreadLocalRandom.current().nextInt(4) == 0) {
                world.playSound(spawnLoc, Sound.ENTITY_CREEPER_HURT, 0.3f, 0);
            }

            curLength -= LENGTH_INCREMENT;
            return curLength > 0;
        }
    }

    private class Bolt  {
    
        private Location location; 
        private Vector direction;
        private double range;
        private boolean isMainBolt;
        private double rangeCounter;
        private boolean hitWater;

        Bolt(Location location, Vector direction, double range, boolean isMainBolt) {
            this.location = location;
            this.direction = direction;
            this.range = range;
            this.isMainBolt = isMainBolt;
            this.rangeCounter = 0;
            this.hitWater = false;
        }

        private boolean progress() {
            Location start = location.clone();

            double moveSpacing = 0.5;
            Vector dir = direction.clone().multiply(moveSpacing);
            for (double i = 0; i < speed; i += moveSpacing) {
                Location prevLoc = location.clone();
                location.add(dir);

                rangeCounter += moveSpacing;
                if (rangeCounter >= range) {
                    break;
                }

                if (BlockUtil.isSolid(location.getBlock()) 
                        || BlockUtil.collidesDiagonally(prevLoc, location, BlockUtil::isSolid)) {
                    location = prevLoc;
                    rangeCounter = range;
                    break;
                }

                if (AbilityUtil.isWater(location.getBlock()) 
                        && isMainBolt && subArcInWater) {
                    rangeCounter = range;
                    hitWater = true;
                    break;
                }

                affectEntities();
            }
            
            drawLightning(start, location);
            return rangeCounter < range;
        }

        private void drawLightning(Location start, Location end) {
            List<Location> keyLocs = new ArrayList<>();
            subDivide(start, end, subDivisions, particleOffset, keyLocs);

            for (int i = 1; i < keyLocs.size(); ++i) {
                Location prev = keyLocs.get(i-1);
                Location cur = keyLocs.get(i);
                drawBetween(prev, cur);
            }
        }
        
        private void subDivide(Location start, Location end, int subDivisions,
                               double offDist, List<Location> displayLocs) {
            if (subDivisions == 0) {
                return;
            }

            Vector dir = VectorUtil.getDirection(start, end).multiply(0.5);
            Location midPoint = start.clone().add(dir);

            double angle = ThreadLocalRandom.current().nextDouble(2 * Math.PI);
            Vector ortho = VectorUtil.getOrthogonal(dir);
            Vector offset = VectorUtil.rotateAroundVector(dir, ortho, angle);
            Location offsetPoint = midPoint.add(offset.multiply(offDist));

            --subDivisions;
            displayLocs.add(start);
            subDivide(start, offsetPoint, subDivisions, 0.25, displayLocs);
            displayLocs.add(offsetPoint);
            subDivide(offsetPoint, end, subDivisions, 0.25, displayLocs);
            displayLocs.add(end);
        }

        private void drawBetween(Location start, Location end) {
            Vector drawVec = VectorUtil.getDirection(start, end);
            drawVec.normalize().multiply(particleSpacing);

            Location loc = start.clone();
            double count = start.distance(end) / particleSpacing;
            for (double i = 0; i < count; i++) {
                drawParticle(loc.add(drawVec));
            }
        }

        private void affectEntities() {
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();
            for (Entity e : EntityUtil.getNearbyEntities(location, hitboxSize)) {
                if (!e.equals(user().player()) && e instanceof LivingEntity le) {
                    affect(le);
                    effectHandler.damageEntity(e, Lightning.this, damage);
                    effectHandler.stopMovement(le, Lightning.this, stunDuration);
                }
            }
        }

        private void affect(LivingEntity le) {
            boolean affectEntity = true;
            if (le instanceof Player p && chargeHitPlayers) {
                affectEntity = !chargePlayerLightning(p);
            }

            if (affectEntity) {
                EffectHandler effectHandler = ElementalMagicApi.effectHandler();
                effectHandler.damageEntity(le, Lightning.this, damage);

                if (ThreadLocalRandom.current().nextDouble(100) < stunChance) {
                    effectHandler.stopMovement(le, Lightning.this, stunDuration);
                }
            }

            World world = le.getWorld();
            Location loc = le.getLocation();
            world.spawnParticle(Particle.LAVA, loc, 1);
            world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1, 0);
        }
        
        private boolean chargePlayerLightning(Player player) {
            UserManager userManager = ElementalMagicApi.userManager();
            AbilityUser user = userManager.get(player).orElse(null);
            if (user == null) {
                return false;
            }

            AbilityManager abilManager = ElementalMagicApi.abilityManager();
            List<Lightning> lightnings = 
                abilManager.getUserAbilities(user, Lightning.class).toList();

            for (Lightning lightning : lightnings) {
                if (lightning.isCharging) {
                    lightning.chargedTime = 0; 
                    return true;
                }
            }
            
            return false;
        }
    }

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = LightningController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 12000;
        @Configure(path = CONFIG_PATH + "ChargeTime", config = Config.ABILITIES)
        private long chargeTime = 2000;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 56.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)       
        private double speed = 2.5;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)       
        private double hitboxSize = 2.25;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 3.0;
        @Configure(path = CONFIG_PATH + "StunChance", config = Config.ABILITIES)       
        private double stunChance = 20.0;
        @Configure(path = CONFIG_PATH + "StunDuration", config = Config.ABILITIES)       
        private long stunDuration = 1500;
        @Configure(path = CONFIG_PATH + "ChargeHitPlayers", config = Config.ABILITIES)
        private boolean chargeHitPlayers = true;
        @Configure(path = CONFIG_PATH + "SubArcs.Count", config = Config.ABILITIES)
        private int subArcCount = 4;
        @Configure(path = CONFIG_PATH + "SubArcs.Chance", config = Config.ABILITIES)
        private double subArcChance = 30.0;
        @Configure(path = CONFIG_PATH + "SubArcs.MinRange", config = Config.ABILITIES)
        private double minSubArcRange = 12.0;
        @Configure(path = CONFIG_PATH + "SubArcs.MaxRange", config = Config.ABILITIES)
        private double maxSubArcRange = 20.0;
        @Configure(path = CONFIG_PATH + "SubArcs.MinAngle", config = Config.ABILITIES)
        private double minSubArcAngle = 12.0;
        @Configure(path = CONFIG_PATH + "SubArcs.MaxAngle", config = Config.ABILITIES)
        private double maxSubArcAngle = 35.0; 
        @Configure(path = CONFIG_PATH + "SubArcs.InWater", config = Config.ABILITIES)
        private boolean subArcInWater = true;
        @Configure(path = CONFIG_PATH + "SubArcs.WaterCount", config = Config.ABILITIES)
        private int waterArcCount = 5;
        @Configure(path = CONFIG_PATH + "Appearance.ParticleOffset", config = Config.ABILITIES) 
        private double particleOffset = 0.8;
        @Configure(path = CONFIG_PATH + "Appearance.Subdivisions", config = Config.ABILITIES)
        private int subDivisions = 3;
        @Configure(path = CONFIG_PATH + "Appearance.ParticleSpacing", config = Config.ABILITIES)
        private double particleSpacing = 0.1;
    }
}
