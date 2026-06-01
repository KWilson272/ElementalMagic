package me.kwilson272.elementalmagic.core.gameplay.earth.earthline;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class EarthLine extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double selectRange;
    private double range;
    private int maxClimb;
    private double speed;
    private double hitboxSize;
    private double damage;
    private double knockback;
    private double knockup;

    private boolean isFired;
    private Block sourceBlock;
    private TempBlock sourceTemp;

    private Location origin;
    private Location location;
    private Block prevBlock;
    private Vector direction;

	public EarthLine(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        range = CONFIG.range;
        maxClimb = CONFIG.maxClimb;
        speed = CONFIG.speed;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        knockback = CONFIG.knockback;
        knockup = CONFIG.knockup;

        isFired = false;
	}

	@Override
	public boolean start() {
        sourceBlock = selectSource(selectRange);
        if (sourceBlock == null) {
            return false;
        }

        BlockData data = getSourceFocusData(sourceBlock);
        sourceTemp = TempBlock.builder(this, data)
            .buildAt(sourceBlock)
            .orElse(null);
        
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), true, false) 
                || !user().player().getWorld().equals(sourceBlock.getWorld())) {
            return false;
        }
        return isFired ? advanceLocation() : isSourceViable();
	}

    private boolean isSourceViable() {
        Location sourceLoc = sourceBlock.getLocation().add(0.5, 0.5, 0.5);
        Location eyeLoc = user().player().getEyeLocation();
        double maxDist = Math.pow(selectRange + 2, 2);

        return sourceLoc.getWorld().equals(eyeLoc.getWorld())
            && sourceLoc.distanceSquared(eyeLoc) <= maxDist;
    }

    private boolean advanceLocation() {
        double remainder = speed;
        while (remainder > 0) {
            double travel = Math.min(remainder, 1);
            remainder--;
        
            if (user().player().isSneaking()) {
                direction = getDirection();
            }

            location.add(direction.clone().multiply(travel));
            Block block = location.getBlock();
            if (!block.equals(prevBlock)) {
                block = getSafeBlock(block);
                if (block == null) {
                    return false;
                }
                
                prevBlock = block;
                location.setY(block.getY());
                createFallingBlock(block);
                playEarthSound(location);
            }

            affectEntities();

            if (location.distanceSquared(origin) > range * range) {
                return false;
            }
        }
        return true;
    }

    private Vector getDirection() {
        Player player = user().player();
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection();
        double targetRange = selectRange + range;

        RayTraceResult result = world.rayTraceEntities(
                start,
                dir, 
                targetRange,
                1.25,
                this::canTarget
        );

        Location target;
        if (result != null && result.getHitEntity() != null) {
            target = result.getHitEntity().getLocation();
        } else {
            target = Entities.getTargetLocation(player, targetRange);
        }

        Vector direction = Vectors.getDirection(origin, target);
        direction.setY(0);
        direction.normalize();

        return direction;
    }

    private boolean canTarget(Entity e) {
        return !e.equals(user().player())
            && e instanceof LivingEntity
            && !(e instanceof FallingBlock)
            && ElementalMagicApi.effectHandler().canAffect(e);
    }

    private Block getSafeBlock(Block block) {
        for (int i = 0; i < maxClimb; ++i) {
            Block above = block.getRelative(BlockFace.UP);
            if (Blocks.isSolid(block) && !Blocks.isSolid(above)) {
                break;
            } else if (Blocks.isSolid(above)) {
                block = above;
            } else {
                block = block.getRelative(BlockFace.DOWN);
            }
        }

        Block above = block.getRelative(BlockFace.UP);
        return isUsableEarth(block) && !Blocks.isSolid(above) ? block : null;
    }

    private void affectEntities() {
        // Ensure we're at the center y level of the block
        Location loc = location.clone().add(0, 0.5, 0);
        Vector knock = direction.clone().multiply(knockback);
        knock.setY(knockup);
        
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        for (Entity e : Entities.getNearbyEntities(loc, hitboxSize)) {
            if (!e.equals(user().player()) && !(e instanceof FallingBlock)) {
                effectHandler.setVelocity(e, this, knock);
                effectHandler.damageEntity(e, this, damage);
            }
        }
    }

    private void createFallingBlock(Block block) {
        BlockData data = block.getBlockData();
        BlockData airData = Material.AIR.createBlockData();
        TempBlock.builder(this, airData).setDuration(500).buildAt(block);

        Location spawnLoc = block.getLocation().add(0.5, 0.5, 0.5);
        Vector velocity = new Vector(0, 0.2, 0);
        
        TempFallingBlock tfb = TempFallingBlock.builder(this, data)
            .setCollidable(false)
            .setDuration(500)
            .buildAt(spawnLoc);

        tfb.fallingBlock().setVelocity(velocity);
        // Undo this so the animation doesn't look weird
        tfb.fallingBlock().setCancelDrop(false);
    }

	@Override
	public void onDestruction() {
        if (sourceTemp != null) {
            ElementalMagicApi.revertibleManager().revert(sourceTemp);
        }
	}

	@Override
	public String name() {
        return "EarthLine";
	}

    protected boolean isSourced() {
        return !isFired;
    }

    protected void fire() {
        if (isFired) {
            return;
        }

        location = sourceBlock.getLocation().add(0.5, 0, 0.5);
        origin = location.clone();
        direction = getDirection();

        ElementalMagicApi.revertibleManager().revert(sourceTemp);
        sourceTemp = null;
        
        isFired = true;
        user().addCooldown(name(), cooldown);
    }

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = EarthLineController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 6200;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 10.0;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 22;
        @Configure(path = CONFIG_PATH + "MaxClimb", config = Config.ABILITIES)
        private int maxClimb = 2;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.25;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.6;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 3.0;
        @Configure(path = CONFIG_PATH + "Knockback", config = Config.ABILITIES)
        private double knockback = 0.7;
        @Configure(path = CONFIG_PATH + "Knockup", config = Config.ABILITIES)
        private double knockup = 0.4;
    }
}
