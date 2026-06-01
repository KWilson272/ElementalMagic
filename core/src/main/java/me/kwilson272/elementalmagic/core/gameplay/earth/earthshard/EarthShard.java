package me.kwilson272.elementalmagic.core.gameplay.earth.earthshard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.revertible.TempFallingBlock;
import me.kwilson272.elementalmagic.core.revertible.TempFallingBlock.TempFallingBlockBuilder;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class EarthShard extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double selectRange;
    private double targetRange;
    private int maxShards;
    private double riseSpeed;
    private double throwSpeed;
    private double hitboxSize;
    private double damage;

    private boolean isFired;
    private int shardCount;

    private List<TempBlock> holes;
    private List<TempBlock> tempBlocks; 
    private Map<TempFallingBlock, Location> fallingBlocks;

    public EarthShard(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        targetRange = CONFIG.targetRange;
        maxShards = CONFIG.maxShards;
        riseSpeed = CONFIG.riseSpeed;
        throwSpeed = CONFIG.throwSpeed;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;

        isFired = false;
        shardCount = 0;

        holes = new ArrayList<>();
        tempBlocks = new ArrayList<>();
        fallingBlocks = new HashMap<>(); 
	}


	@Override
	public boolean start() {
        source();
        return !fallingBlocks.isEmpty();
	}

    protected void source() {
        if (isFired || shardCount >= maxShards) {
            return;
        }

        Block block = selectSource(selectRange);
        if (block == null) {
            return;
        }
    
        BlockData data = block.getBlockData();
        TempBlock.builder(this, Material.AIR.createBlockData())
            .buildAt(block).ifPresent(holes::add);
       
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        TempFallingBlock tfb = TempFallingBlock.builder(this, data)
            .setCollidable(false).setDuration(20000).buildAt(loc);

        fallingBlocks.put(tfb, loc);
        tfb.fallingBlock().setVelocity(new Vector(0, riseSpeed, 0));
        shardCount++;

        playEarthSound(loc);
    }

    protected void launch() {
        if (isFired) {
            return;
        }

        convertSolidToFalling();

        Location target = getTarget();   
        for (TempFallingBlock tfb : fallingBlocks.keySet()) {
            FallingBlock fb = tfb.fallingBlock();
            Vector dir = Vectors.getDirection(fb.getLocation(), target);
            // This formula comes from Jedcore
            dir.normalize().multiply(throwSpeed).add(new Vector(0, 0.2, 0)); 
            fb.setVelocity(dir);
        }   

        for (TempBlock tb : holes) {
            ElementalMagicApi.revertibleManager().revert(tb);
        }
        holes.clear();
        
        isFired = true;
        user().addCooldown(name(), cooldown);
    }

    protected boolean isFired() {
        return isFired;
    }

    private void convertSolidToFalling() {
        TempFallingBlockBuilder builder = TempFallingBlock.builder(this, null)
            .setCollidable(false)
            .setDuration(20000);

        for (TempBlock tb : tempBlocks) {
            Location loc = tb.block().getLocation();
            BlockData data = tb.block().getBlockData();
            TempFallingBlock tfb = builder.setBlockData(data).buildAt(loc);
            fallingBlocks.put(tfb, loc);

            ElementalMagicApi.revertibleManager().revert(tb);
        }

        tempBlocks.clear();
    }

    private Location getTarget() {
        Player player = user().player();
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();

        RayTraceResult result = world.rayTraceEntities(
                start,
                direction, 
                targetRange,
                1.25,
                this::canTarget
        );

        if (result != null && result.getHitEntity() != null) {
            return result.getHitEntity().getLocation(); 
        } else {
            return Entities.getTargetLocation(player, targetRange);
        }
    }

    private boolean canTarget(Entity e) {
        return !e.equals(user().player())  && e instanceof LivingEntity
            && ElementalMagicApi.effectHandler().canAffect(e);
    }

	@Override
	public boolean progress() {
	    if (!user().canUse(controller(), true, false)) {
            return false;
        }
        
        removeDeadBlocks();

        if (!isFired) {
            solidifyRisingBlocks(); 
            return true;
        }
    
        affectEntities();
        return !fallingBlocks.isEmpty();
    }

    private void removeDeadBlocks() {
        Iterator<TempFallingBlock> iter = fallingBlocks.keySet().iterator();
        while (iter.hasNext()) {
            TempFallingBlock tfb = iter.next();
            if (tfb.fallingBlock().isDead()) {
                ElementalMagicApi.revertibleManager().revert(tfb);
                iter.remove();
            }
        }
    }

    private void solidifyRisingBlocks() {
        Iterator<TempFallingBlock> iter = fallingBlocks.keySet().iterator();
        while (iter.hasNext()) {
            TempFallingBlock tfb = iter.next();
            Location source = fallingBlocks.get(tfb);
            Location fbLoc = tfb.fallingBlock().getLocation();

            double yDiff = fbLoc.getY() - source.getBlockY();
            if (yDiff >= 3) {
                Block block = source.add(0, 3, 0).getBlock(); 
                BlockData data = tfb.fallingBlock().getBlockData();
                TempBlock.builder(this, data).buildAt(block)
                    .ifPresent(tempBlocks::add);

                iter.remove();
                ElementalMagicApi.revertibleManager().revert(tfb);
            }
        }
    }

    private void affectEntities() {
        Iterator<TempFallingBlock> iter = fallingBlocks.keySet().iterator();
        while (iter.hasNext()) {
            TempFallingBlock tfb = iter.next();

            boolean affected = false;
            Location loc = tfb.fallingBlock().getLocation().add(0.5, 0.5, 0.5);
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();

            for (Entity e : Entities.getNearbyEntities(loc, hitboxSize)) {
                if (e.equals(user().player()) || e.equals(tfb.fallingBlock())
                        || !(e instanceof LivingEntity)) {
                    continue;
                }
                
                if (effectHandler.damageEntity(e, this, damage)) {
                    ((LivingEntity) e).setNoDamageTicks(0);
                    affected = true;
                }
            }

            if (affected) {
                playShatterEffect(tfb);
                iter.remove();
            }
        }
    }

    private void playShatterEffect(TempFallingBlock tfb) {
        BlockData data = tfb.fallingBlock().getBlockData();
        Location loc = tfb.fallingBlock().getLocation().add(0.5, 0.5, 0.5);
        World world = tfb.fallingBlock().getWorld();
        world.spawnParticle(Particle.BLOCK, loc, 20, 0, 0, 0, data);
    }

	@Override
	public void onDestruction() {
	    for (TempBlock tb : holes) {
            ElementalMagicApi.revertibleManager().revert(tb);
        }
        for (TempBlock tb : tempBlocks) {
            ElementalMagicApi.revertibleManager().revert(tb);
        }
        for (TempFallingBlock tfb : fallingBlocks.keySet()) {
            playShatterEffect(tfb);
            ElementalMagicApi.revertibleManager().revert(tfb);
        }
    }

	@Override
	public String name() {
        return "EarthShard";
	}

    protected static class ConfigValues {

        private static final String CONFIG_PATH = EarthShardController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 4400;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 8.0;
        @Configure(path = CONFIG_PATH + "TargetRange", config = Config.ABILITIES)
        private double targetRange = 35.0;
        @Configure(path = CONFIG_PATH + "MaxShards", config = Config.ABILITIES)
        private int maxShards = 3;
        @Configure(path = CONFIG_PATH + "RiseSpeed", config = Config.ABILITIES)
        private double riseSpeed = 0.8;
        @Configure(path = CONFIG_PATH  + "ThrowSpeed", config = Config.ABILITIES)
        private double throwSpeed = 2.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.75;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 1.0;
    }
}
