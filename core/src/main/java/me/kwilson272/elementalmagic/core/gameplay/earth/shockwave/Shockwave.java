package me.kwilson272.elementalmagic.core.gameplay.earth.shockwave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.components.EarthPillar;
import me.kwilson272.elementalmagic.core.gameplay.components.EarthPillar.PillarState;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;

public class Shockwave extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long chargeDuration;
    private double fallHeight;
    private double hitboxSize;
    private double knockback;
    private double knockup;
    private double ringRange;
    private double ringSpeed;
    private double ringDamage;
    private double coneRange;
    private double coneSpeed;
    private double coneDamage;
    private double coneAngle;

    private boolean isFall;
    private boolean isCharging;
    private long chargedTime;

    private Set<Block> affectedBlocks;
    private List<Ripple> ripples;

	public Shockwave(AbilityUser user, AbilityController controller, boolean isFall) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        chargeDuration = CONFIG.chargeDuration;
        fallHeight = CONFIG.fallHeight;
        hitboxSize = CONFIG.hitboxSize;
        knockback = CONFIG.knockback;
        knockup = CONFIG.knockup;
        ringRange = CONFIG.ringRange;
        ringSpeed = CONFIG.ringSpeed;
        ringDamage = CONFIG.ringDamage;
        coneRange = CONFIG.coneRange;
        coneSpeed = CONFIG.coneSpeed;
        coneDamage = CONFIG.coneDamage;
        coneAngle = CONFIG.coneAngle;

        this.isFall = isFall;
        isCharging = !isFall;

        affectedBlocks = new HashSet<>();
        ripples = new ArrayList<>();
	}

	@Override
	public boolean start() {
        if (isFall) {
            if (user().player().getFallDistance() < fallHeight) {
                return false;
            }

            initRipples(360, true);
            if (ripples.isEmpty()) {
                return false;
            }

            isCharging = false;
            user().addCooldown(name(), cooldown);
            return true;
        }

        chargedTime = System.currentTimeMillis() + chargeDuration;
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), isCharging, false)) {
            return false; 
        }

        if (!isCharging) {
            ripples.removeIf(ripple -> !ripple.progress());
            return !ripples.isEmpty();
        }

        if (System.currentTimeMillis() > chargedTime) {
            playChargeEffect();
            if (!user().player().isSneaking()) {
                initRipples(360, true);
                isCharging = false;
                user().addCooldown(name(), cooldown);
            }
            return true;
        }

        return user().player().isSneaking();
	}

    private void playChargeEffect() {
        World world = user().player().getWorld();
        Location loc = user().player().getEyeLocation();
        Vector dir = loc.getDirection().multiply(1.5);
        world.spawnParticle(Particle.SMOKE, loc.add(dir), 1, 0, 0, 0, 0);
    }

    protected void fire() {
        if (!isCharging || System.currentTimeMillis() < chargedTime) {
            return;
        }

        initRipples(coneAngle, true);
        isCharging = false;
        user().addCooldown(name(), cooldown);
    }

    private void initRipples(double angle, boolean isRing) {
        double range;
        double speed;
        double damage;
        if (isRing) {
            range = ringRange;
            speed = ringSpeed;
            damage = ringDamage;
        } else {
            range = coneRange;
            speed = coneSpeed;
            damage = coneDamage;
        }

        Block spawn = getSafeBlock(user().player().getLocation().getBlock());
        if (spawn == null) {
            return;
        }
        Location loc = spawn.getLocation().add(0.5, 0.5, 0.5);

        double spacing = 0.5;
        double step = 2 * Math.asin(spacing / (2 * range));
        double baseYaw = Math.toRadians(user().player().getEyeLocation().getYaw());
        int count = (int) Math.ceil(Math.toRadians(angle) / step);

        for (int i = -count/2; i <= count/2; ++i) {
            double rad = baseYaw + (step * i);
            double x = -Math.sin(rad);
            double z = Math.cos(rad);
            Vector dir = new Vector(x, 0, z);

            ripples.add(new Ripple(loc.clone(), dir, range, speed, damage));
        }
    }
    
    private Block getSafeBlock(Block block) {
        for (int i = 0; i < 2; ++i) {
            Block above = block.getRelative(BlockFace.UP);
            if (affectedBlocks.contains(block)) {
                return block;
            } else if (Blocks.isSolid(block) && !Blocks.isSolid(above)) {
                break;
            } else if (Blocks.isSolid(above)) {
                block = above;
            } else {
                block = block.getRelative(BlockFace.DOWN);
            }
        }

        Block above = block.getRelative(BlockFace.UP);
        if (!isUsableEarth(block) || Blocks.isSolid(above)) {
            return null;
        }
        
        return block;
    }

	@Override
	public void onDestruction() {
        ripples.forEach(Ripple::revert); 
    }

	@Override
	public String name() {
        return "Shockwave";
	}

    protected boolean isCharging() {
        return isCharging;
    }

    private class Ripple {
    
        private Location location;
        private Vector direction;
        private double range;
        private double speed;
        private double damage;
        private double knockback;
        
        private boolean isTraveling;
        private Map<EarthPillar, Block> pillars;

        public Ripple(Location location, Vector direction, double range, 
                                            double speed, double damage) {
            this.location = location;
            this.direction = direction;
            this.range = range;
            this.speed = speed;
            this.damage = damage;

            this.isTraveling = true;
            this.pillars = new HashMap<>();
        }

        private boolean progress() {
            if (isTraveling) {
                advanceLocation();
            }
            progressPillars();
            return isTraveling || !pillars.isEmpty();
        }

        private void advanceLocation() {
            double remainder = speed;
            while (remainder > 0) {
                double travel = Math.min(remainder, 1);
                remainder--;

                Vector move = direction.clone().multiply(travel);
                Block prev = location.getBlock();
                Block next = location.add(move).getBlock();
                if (!prev.equals(next)) {
                    Block block = getSafeBlock(next);
                    if (block == null) {
                        isTraveling = false;
                        break;
                    } else if (affectedBlocks.contains(block)) {
                        break;
                    }

                    var pillar = new EarthPillar(Shockwave.this, block, 2, 1, true);
                    pillar.setMoveCallback(this::playSound);
                    pillar.setBlockPlaceCallback(this::affectEntities);
                    pillars.put(pillar, block);
                    affectedBlocks.add(block);
                }

                range -= travel;
                if (range <= 0) {
                    isTraveling = false;
                }
            }       
        }

        private void playSound(Location loc) {
            int chance = (int) Math.max(1, affectedBlocks.size() / 2);
            if (ThreadLocalRandom.current().nextInt(chance) == 0) {
                playEarthSound(loc);
            }
        }

        private void affectEntities(Block block) {
            World world = block.getWorld();
            BoundingVolume bv = AABB.fromBlock(block, hitboxSize);
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();

            Vector knock = direction.clone().multiply(knockback);
            knock.setY(knockup);

            for (Entity e : Entities.getNearbyEntities(world, bv)) {
                if (!e.equals(user().player())) {
                    effectHandler.setVelocity(e, Shockwave.this, knock);
                    effectHandler.damageEntity(e, Shockwave.this, damage);
                }
            }
        }

        private void progressPillars() {
            Iterator<EarthPillar> iter = pillars.keySet().iterator();
            while (iter.hasNext()) {
                EarthPillar pillar = iter.next();
                if (pillar.getState() == PillarState.IDLE) {
                    pillar.collapse();
                    continue;
                }

                if (!pillar.progress() && pillar.getState() != PillarState.IDLE) {
                    affectedBlocks.remove(pillars.get(pillar));
                    pillar.revert();
                    iter.remove();
                }
            }
        }

        private void revert() {
            for (EarthPillar pillar : pillars.keySet()) {
                affectedBlocks.remove(pillars.get(pillar));
                pillar.revert();
            }
            pillars.clear();
        }
    }

    protected static class ConfigValues {
       
        private static final String CONFIG_PATH = ShockwaveController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 8000;
        @Configure(path = CONFIG_PATH + "ChargeDuration", config = Config.ABILITIES)
        private long chargeDuration = 1500;
        @Configure(path = CONFIG_PATH + "FallHeight", config = Config.ABILITIES)
        private double fallHeight = 12.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.0;
        @Configure(path = CONFIG_PATH + "Knockback", config = Config.ABILITIES)
        private double knockback = 2.0;
        @Configure(path = CONFIG_PATH + "Knockup", config = Config.ABILITIES)
        private double knockup = 0.5;
        @Configure(path = CONFIG_PATH + "RingRange", config = Config.ABILITIES)
        private double ringRange = 12.0;
        @Configure(path = CONFIG_PATH + "RingSpeed", config = Config.ABILITIES)
        private double ringSpeed = 1.0;
        @Configure(path = CONFIG_PATH + "RingDamage", config = Config.ABILITIES)
        private double ringDamage = 3.0;
        @Configure(path = CONFIG_PATH + "ConeRange", config = Config.ABILITIES)
        private double coneRange = 15.0;
        @Configure(path = CONFIG_PATH + "ConeSpeed", config = Config.ABILITIES)
        private double coneSpeed = 1.0;
        @Configure(path = CONFIG_PATH + "ConeDamage", config = Config.ABILITIES)
        private double coneDamage = 3.0;
        @Configure(path = CONFIG_PATH + "ConeAngle", config = Config.ABILITIES)
        private double coneAngle = 40.0;
    }
}
