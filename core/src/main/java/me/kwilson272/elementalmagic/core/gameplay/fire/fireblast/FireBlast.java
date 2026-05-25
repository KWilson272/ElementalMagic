package me.kwilson272.elementalmagic.core.gameplay.fire.fireblast;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.components.Ray;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;

public class FireBlast extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double range;
    private double speed;
    private double damage;
    private double knockback;
    private double hitboxSize;
    private double fireRadius;
    private long fireDuration;
    private double fireDamage;
    private long burnDuration;

    private FireBlastRay ray;

	public FireBlast(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        range = CONFIG.range;
        speed = CONFIG.speed;
        damage = CONFIG.damage;
        knockback = CONFIG.knockback;
        hitboxSize = CONFIG.hitboxSize;
        fireRadius = CONFIG.fireRadius;
        fireDuration = CONFIG.fireDuration;
        fireDamage = CONFIG.fireDamage;
        burnDuration = CONFIG.burnDuration;
	}

	@Override
	public boolean start() {
        Location eyeLoc = user().player().getEyeLocation();
        if (BlockUtil.isLiquid(eyeLoc.getBlock())) {
            return false;
        }
        
        ray = new FireBlastRay(eyeLoc, eyeLoc.getDirection());
        user().addCooldown("FireBlast", cooldown);
        return true;
	}

	@Override
	public boolean progress() {
        return user().canUse(controller(), false, false) && ray.progress();
	}

	@Override
	public void onDestruction() {
	}

    @Override
    public String name() {
        return "FireBlast";
    }

    private boolean affectEntities(Location loc) {
        boolean affected = false;
        Vector knock = ray.direction.clone().multiply(knockback);
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();

        for (Entity e : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
            if (!e.equals(user().player()) && e instanceof LivingEntity) {
                affected |= effectHandler.setVelocity(e, this, knock);
                affected |= effectHandler.damageEntity(e, this, damage);
                if (e.getFireTicks() * 50 < burnDuration) {
                    affected |= effectHandler.setFireDuration(e, this, burnDuration);
                }
            }
        }

        return affected;
    }

    private void igniteAround(Location location) {
        TempBlockBuilder builder = TempBlock.builder(this, getFireData())
            .setDuration(fireDuration).setDamage(fireDamage);

        for (Block b : BlockUtil.collectSphere(location, fireRadius)) {
            if (!BlockUtil.isSolid(b) && !BlockUtil.isLiquid(b) 
                    && BlockUtil.isSolid(b.getRelative(BlockFace.DOWN))) {
                builder.buildAt(b);
            }
        }
    }

    private class FireBlastRay extends Ray {

        private final Vector direction;

        public FireBlastRay(Location location, Vector direction) {
			super(location, speed, range);
            this.direction = direction;
		}

		@Override
		public boolean collides(Block block) {
            if (BlockUtil.isSolid(block)) {
                Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                igniteAround(loc);
                return true;
            }

            return BlockUtil.isLiquid(block);
		}

		@Override
		public boolean moveTo(Location loc) {
            World world = loc.getWorld();
            Particle particle = getFireParticle();
            world.spawnParticle(particle, loc, 2, 0.3, 0.3, 0.3, 0.01);
            if (ThreadLocalRandom.current().nextInt(5) == 0) {
                world.spawnParticle(Particle.SMOKE, loc, 1, 0.2, 0.2, 0.2, 0);
                playFireSound(loc);
            }
            
            return !affectEntities(loc);
		}

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = FireBlastController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 1500;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 21.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)       
        private double speed = 1.25;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 3.0;
        @Configure(path = CONFIG_PATH + "Knockback", config = Config.ABILITIES)
        private double knockback = 0.5;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)       
        private double hitboxSize = 1.2;
        @Configure(path = CONFIG_PATH + "FireRadius", config = Config.ABILITIES)       
        private double fireRadius = 2.0;
        @Configure(path = CONFIG_PATH + "FireDuration", config = Config.ABILITIES)       
        private long fireDuration = 1000;
        @Configure(path = CONFIG_PATH + "FireDamage", config = Config.ABILITIES)       
        private double fireDamage = 1.0;
        @Configure(path = CONFIG_PATH + "BurnDuration", config = Config.ABILITIES)       
        private long burnDuration = 0;
    }
}
