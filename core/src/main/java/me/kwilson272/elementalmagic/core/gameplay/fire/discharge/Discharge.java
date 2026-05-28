package me.kwilson272.elementalmagic.core.gameplay.fire.discharge;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.components.Ray;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class Discharge extends FireAbility {

	protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double range;
    private double speed;
    private double hitboxSize;
    private double damage;
    private double knockForce;
    private boolean canSwapSlots;
    private int numBranches;
    private double minBranchAngle;
    private double maxBranchAngle;
    private double maxOffset;

    private List<Branch> branches;

    public Discharge(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        range = CONFIG.range;
        speed = CONFIG.speed;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        knockForce = CONFIG.knockForce;
        canSwapSlots = CONFIG.canSwapSlots;
        numBranches = CONFIG.numBranches;
        minBranchAngle = CONFIG.minBranchAngle;
        maxBranchAngle = CONFIG.maxBranchAngle;
        maxOffset = CONFIG.maxOffset;

        branches = new ArrayList<>();
    }

	@Override
	public boolean start() {
        initBranches();
        user().addCooldown(name(), cooldown);
        return true;
	}

    private void initBranches() {
        Location loc = user().player().getEyeLocation();
        Vector dir = loc.getDirection();
        Vector ortho = VectorUtil.getOrthogonal(dir);

        // Always render at least once branch in the center so there 
        // aren't any 'bad splits' where branches avoid it entirely.
        branches.add(new Branch(loc.clone(), dir.clone()));
        
        double minAngle = Math.toRadians(minBranchAngle);
        double maxAngle = Math.toRadians(maxBranchAngle);
        Random rand = ThreadLocalRandom.current();
        for (int i = 0; i < numBranches; ++i) {
            double angle = rand.nextDouble(2 * Math.PI); 
            Vector rot = VectorUtil.rotateAroundVector(dir, ortho, angle);

            double devAngle = rand.nextDouble(minAngle, maxAngle); 
            Vector vec = dir.clone().multiply(Math.cos(devAngle));
            vec.add(rot.multiply(Math.sin(devAngle)));

            branches.add(new Branch(loc.clone(), vec));
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), !canSwapSlots, false)) {
            return false;
        }
        
        branches.removeIf(branch -> !branch.progress());
        return !branches.isEmpty();
	}

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "Discharge";
	}

    private class Branch extends Ray {
        
        private static final DustTransition DUST_OPTS = new DustTransition(
            Color.fromRGB(120, 240, 255),
            Color.fromRGB(60, 100, 255),
            0.8f
        );

        private Location location;
        private Vector direction;

        public Branch(Location location, Vector direction) {
			super(location, speed, range);
            this.location = location;
            this.direction = direction;
		}

		@Override
		public boolean collides(Block block) {
            return BlockUtil.isSolid(block);
		}

		@Override
		public boolean moveTo(Location loc) {
            affectEntities(loc);
            renderElectricity(loc);
            location = loc;

            if (ThreadLocalRandom.current().nextInt(5) == 0) {
                World world = loc.getWorld();
                world.playSound(loc, Sound.ENTITY_CREEPER_PRIMED, 1, 0);
            }

            return true;
    	}

        private void renderElectricity(Location loc) {
            double angle = ThreadLocalRandom.current().nextDouble(2 * Math.PI);
            Vector ortho = VectorUtil.getOrthogonal(direction);
            Vector rot = VectorUtil.rotateAroundVector(direction, ortho, angle);

            double offset = ThreadLocalRandom.current().nextDouble(maxOffset);
            Location midPoint = location.clone();
            midPoint.add(direction.clone().multiply(offset));
            midPoint.add(rot.multiply(offset));

            drawBetween(location, midPoint);
            drawBetween(midPoint, loc);
        }
    
        private void affectEntities(Location loc) {
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();
            for (Entity e : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
                if (e.equals(user().player()) || !(e instanceof LivingEntity)) {
                    continue;
                }

                Vector knock = VectorUtil.getDirection(loc, e.getLocation());
                knock.normalize().multiply(knockForce);
                effectHandler.setVelocity(e, Discharge.this, knock);
                effectHandler.damageEntity(e, Discharge.this, damage);

                Random rand = ThreadLocalRandom.current();
                for (int i = 0; i < 5; ++i) {
                    Location display = loc.clone().add(
                        rand.nextDouble(),
                        rand.nextDouble(),
                        rand.nextDouble()
                    );
                    drawParticle(display);
                }
            }
        }

        private void drawBetween(Location start, Location end) {
            double spacing = 0.1;

            Location loc = start.clone();
            Vector dir = VectorUtil.getDirection(start, end);
            dir.normalize().multiply(spacing);

            int count = (int) Math.ceil(location.distance(end) / spacing);
            for (int i = 0; i < count; ++i) {
                drawParticle(loc);
                loc.add(dir);
            }
        }

        private void drawParticle(Location loc) {
            World world = loc.getWorld();
            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 
                                             1, 0, 0, 0, DUST_OPTS);
        }

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = DischargeController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 7200;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 23;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.2;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 3.0;
        @Configure(path = CONFIG_PATH + "KnockForce", config = Config.ABILITIES)
        private double knockForce = 0.8;
        @Configure(path = CONFIG_PATH + "CanSwapSlots", config = Config.ABILITIES)
        private boolean canSwapSlots = false;
        @Configure(path = CONFIG_PATH + "NumBranches", config = Config.ABILITIES)
        private int numBranches = 6;
        @Configure(path = CONFIG_PATH + "MinBranchAngle", config = Config.ABILITIES)
        private double minBranchAngle = 2.0;
        @Configure(path = CONFIG_PATH + "MaxBranchAngle", config = Config.ABILITIES)
        private double maxBranchAngle = 5.0;
        @Configure(path = CONFIG_PATH + "MaxOffset", config = Config.ABILITIES)
        private double maxOffset = 0.5;

    }
}
