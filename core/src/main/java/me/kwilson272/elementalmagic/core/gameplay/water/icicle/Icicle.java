package me.kwilson272.elementalmagic.core.gameplay.water.icicle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.components.BlockStream;
import me.kwilson272.elementalmagic.core.gameplay.water.WaterAbility;
import me.kwilson272.elementalmagic.core.gameplay.water.WaterUsePolicy;
import me.kwilson272.elementalmagic.core.gameplay.water.phasechange.PhaseChangeFreeze;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class Icicle extends WaterAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();
    
    private long cooldown;
    private long revertTime;
    private double selectRange;
    private int icicleCount;
    private double range;
    private double speed;
    private double damage;
    private double hitboxSize;
    private double knockback;
    private double breakRadius;
    private boolean breakUnusableIce;
    private boolean allowWaterSource;
    private boolean allowSnowSource;
    private boolean allowPlantSource;
  
    private WaterUsePolicy usePolicy;
    private boolean hasFired;
    private boolean canFire;
    private Block source;
    private List<Spike> spikes;

    public Icicle(AbilityUser user, AbilityController controller) {
		super(user, controller);
        
        cooldown = CONFIG.cooldown;
        revertTime = CONFIG.revertTime;
        selectRange = CONFIG.selectRange;
        icicleCount = CONFIG.icicleCount;
        range = CONFIG.range;
        speed = CONFIG.speed;
        damage = CONFIG.damage;
        hitboxSize = CONFIG.hitboxSize;
        knockback = CONFIG.knockback;
        breakRadius = CONFIG.breakRadius;
        breakUnusableIce = CONFIG.breakUnusableIce;
        allowWaterSource = CONFIG.allowWaterSource;
        allowSnowSource = CONFIG.allowSnowSource;
        allowPlantSource = CONFIG.allowPlantSource;

        hasFired = false;
        canFire = true;
        spikes = new ArrayList<>();
	}

	@Override
	public boolean start() {
        usePolicy = new WaterUsePolicy();
        usePolicy.setWater(allowWaterSource)
                 .setSnow(allowSnowSource)
                 .setPlant(allowPlantSource)
                 .validate(user());

        source = selectSourceBlock(selectRange, usePolicy);
        return source != null;
	}

    protected boolean hasFired() {
        return hasFired;
    }

    protected void fire() {
        if (!canFire) {
            return;
        }

        hasFired = true;
        canFire = --icicleCount > 0;

        // So no configs make targeting awkward
        double targRange = selectRange + range;
        Location target = Entities.getTargetLocation(user().player(), targRange);
        Location start = source.getLocation().add(0.5, 0.5, 0.5);
        Vector direction = Vectors.getDirection(start, target).normalize();
        
        spikes.add(new Spike(start, direction));
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), true, false)
                || (!hasFired && !isSourceViable())) {
            return false;
        }
        
        playSourceSelectedEffect(source);
        spikes.removeIf(spike -> !spike.progress());
        return canFire || !spikes.isEmpty();
	}

    private boolean isSourceViable() {
        Location eyeLoc = user().player().getEyeLocation();
        Location sourceLoc = source.getLocation().add(0.5, 0.5, 0.5);
        double maxDist = Math.pow(selectRange + 1, 2);
        
        return eyeLoc.distanceSquared(sourceLoc) <= maxDist 
            && canUse(source, usePolicy);
    }

	@Override
	public void onDestruction() {
        if (hasFired) {
            user().addCooldown("Icicle", cooldown);
        }
	}

    @Override
    public String name() {
        return "Icicle";
    }

    private class Spike extends BlockStream {
        
        private Vector direction;

        Spike(Location location, Vector direction) {
            super(location, speed, range);
            this.direction = direction;
        }
    
        @Override
        public boolean collidesWith(Block block) {
            if (block.equals(source) || !Blocks.isSolid(block) 
                    || canBreak(block)) {
                return false;
            }

            TempBlock tb = TempBlock.get(block).orElse(null);
            if (tb == null) {
                return true;
            }

            return !(tb.ability() instanceof Icicle)
                && !(tb.ability() instanceof PhaseChangeFreeze);
        }
        
		@Override
		public void createBlock(Block block) {
            if (canBreak(block)) {
                breakIce(block);
            }
            
            affectEntities(block);
            BlockData data = Material.ICE.createBlockData();
            TempBlock.builder(Icicle.this, data)
                .setDuration(revertTime)
                .addRevertTask(this::playEffects)
                .buildAt(block)
                .ifPresent(this::playEffects);
		}

        private boolean canBreak(Block block) {
            if (!Blocks.isIce(block)) {
                return false;
            }

            TempBlock tb = TempBlock.get(block).orElse(null);
            if (tb == null) {
                return true;
            }

            Ability abil = tb.ability();
            // Don't break PhaseChange for less annoying gameplay
            if (tb.isUsable() && !(abil instanceof PhaseChangeFreeze)) {
                return true;
            }

            return breakUnusableIce 
                && !(abil instanceof Icicle) 
                && !(abil instanceof PhaseChangeFreeze);
        }

        private void breakIce(Block block) {
            BlockData airData = Material.AIR.createBlockData();
            TempBlockBuilder airBuilder = TempBlock.builder(Icicle.this, airData)
                .setCollidable(false)
                .setUsable(true)
                .setDuration(revertTime);


            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            for (Block b : Blocks.collectSphere(loc, breakRadius)) {
                if (!canBreak(block)) continue;

                airBuilder.buildAt(b).ifPresent(tb -> {
                    World World = b.getWorld();
                    Particle particle = Particle.BLOCK;
                    BlockData breakData = b.getBlockData();
                    Location display = b.getLocation().add(0.5, 0.5, 0.5);
                    World.spawnParticle(particle, display, 2, breakData);
                });
            }
        }

        private void affectEntities(Block block) {
            Vector knock = direction.clone().multiply(knockback);
            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            EffectHandler handler = ElementalMagicApi.effectHandler();

            for (Entity e : Entities.getNearbyEntities(loc, hitboxSize)) {
                if (!e.equals(user().player()) && e instanceof LivingEntity le) {
                    handler.setVelocity(le, Icicle.this, knock);
                    handler.damageEntity(le, Icicle.this, damage);
                }
            }
        }

        private void playEffects(TempBlock tb) {
            Block block = tb.block();
            // If it's been reverted or isn't the active block - looks weird
            if (!Blocks.isIce(block)) {
                return;
            }

            World world = tb.block().getWorld();
            Particle particle = Particle.BLOCK;
            BlockData data = tb.block().getBlockData();
            Location loc = tb.block().getLocation().add(0.5, 0.5, 0.5);
            world.spawnParticle(particle, loc, 3, 0, 0, 0, data);

            if (ThreadLocalRandom.current().nextInt(4) == 0) {
                world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
            }
        }

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }
    
    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = IcicleController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 5600;
        @Configure(path = CONFIG_PATH + "RevertTime", config = Config.ABILITIES)
        private long revertTime = 6500;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 12;
        @Configure(path = CONFIG_PATH + "IcicleCount", config = Config.ABILITIES)
        private int icicleCount = 3;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 14;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 2.5;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "Knockback", config = Config.ABILITIES)
        private double knockback = 1.5;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.0;
        @Configure(path = CONFIG_PATH + "IceBreakRadius", config = Config.ABILITIES)
        private double breakRadius = 2.0;
        @Configure(path = CONFIG_PATH + "BreakUnusableIce", config = Config.ABILITIES)
        private boolean breakUnusableIce = true;
        @Configure(path = CONFIG_PATH + "AllowWaterSource", config = Config.ABILITIES)
        private boolean allowWaterSource = false;
        @Configure(path = CONFIG_PATH + "AllowSnowSource", config = Config.ABILITIES)
        private boolean allowSnowSource = true;
        @Configure(path = CONFIG_PATH + "AllowPlantSource", config = Config.ABILITIES)
        private boolean allowPlantSource = false;
    }
}
