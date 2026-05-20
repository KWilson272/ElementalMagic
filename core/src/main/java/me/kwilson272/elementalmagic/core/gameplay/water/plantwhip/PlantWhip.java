package me.kwilson272.elementalmagic.core.gameplay.water.plantwhip;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.components.BlockStream;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterSourceOptions;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterUtil;

public class PlantWhip extends CoreAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double selectRange;
    private double range;
    private double speed;
    private double damage;
    private double knockback;
    private double hitboxSize;
    private long revertTime;
    
    private Block source;
    private boolean isFired;
    private Whip whip;

	public PlantWhip(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        range = CONFIG.range;
        speed = CONFIG.speed;
        damage = CONFIG.damage;
        knockback = CONFIG.knockback;
        hitboxSize = CONFIG.hitboxSize;
        revertTime = CONFIG.revertTime;

        isFired = false;
	}

	@Override
	public boolean start() {
        var opts = new WaterSourceOptions(user()).noWater().noIce().noSnow();
        source = WaterUtil.getSourceBlock(user(), selectRange, opts);
        return source != null;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), !isFired, false)) {
            return false;
        }
        
        if (!isFired) {
            if (!isSourceViable()) {
                return false;
            }
            WaterUtil.playSourceSelectedEffect(source);
            return true;
        }
         
        return whip.progress();
	}

    private boolean isSourceViable() {
        Location eyeLoc = user().player().getEyeLocation();
        Location sourceLoc = source.getLocation().add(0.5, 0.5, 0.5);
        if (!eyeLoc.getWorld().equals(sourceLoc.getWorld())) {
            return false;
        }

        double maxDist = Math.pow(selectRange + 1, 2);
        var opts = new WaterSourceOptions(user()).noWater().noIce().noSnow();
        return eyeLoc.distanceSquared(sourceLoc) <= maxDist
            && WaterUtil.canUse(source, opts);
    }

	@Override
	public void onDestruction() {
	}
    
    protected void fire() {
        if (isFired) {
            return;
        }

        double targRange = selectRange + range;
        Location target = EntityUtil.getTarget(user().player(), targRange);
        Location start = source.getLocation().add(0.5, 0.5, 0.5);
        Vector direction = VectorUtil.getDirection(start, target).normalize();
        
        isFired = true;
        whip = new Whip(start, direction);
        user().addCooldown("PlantWhip", cooldown);
    }

    protected boolean isFired() {
        return isFired;
    }

    private class Whip extends BlockStream {
        
        private Vector direction;
        private BlockData data;

        public Whip(Location location, Vector direction) {
            super(location, speed, range);
            this.direction = direction;
            this.data = AbilityUtil.getSolidPlant(location.getBlock());
        }

		@Override
		public boolean collidesWith(Block block) {
            if (!BlockUtil.isSolid(block) || block.equals(source)) {
                return false;
            }

            Ability abil = TempBlock.get(block)
                .map(TempBlock::ability)
                .orElse(null);
            return !PlantWhip.this.equals(abil);
		}

		@Override
		public void createBlock(Block block) {
            affectEntities(block);

            TempBlock.builder(PlantWhip.this, data)
                .setDuration(revertTime + ThreadLocalRandom.current().nextLong(250))
                .setUsable(true)
                .buildAt(block)
                .ifPresent(tb -> {
                    WaterUtil.playPlantSound(tb.block().getLocation());
                });
        }

        private void affectEntities(Block block) {
            EffectHandler handler = ElementalMagicApi.effectHandler();
            BoundingVolume bv = AABB.fromBlock(block, hitboxSize);
            World world = block.getWorld();

            Vector knock = direction.clone().multiply(knockback);
            for (Entity e : EntityUtil.getNearbyEntities(world, bv)) {
                if (e instanceof LivingEntity le && !e.equals(user().player())) {
                    handler.setVelocity(le, PlantWhip.this, knock);
                    handler.damageEntity(le, PlantWhip.this, damage);
                }
            }
        }

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }

    protected static class ConfigValues {
    
        private static final String CONFIG_PATH = PlantWhipController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 4500;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 16;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)       
        private double range = 20;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)       
        private double speed = 1.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)       
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "Knockback", config = Config.ABILITIES)       
        private double knockback = 0.5;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)       
        private double hitboxSize = 2.0;
        @Configure(path = CONFIG_PATH + "RevertTime", config = Config.ABILITIES)       
        private long revertTime = 7000;
    }
}
