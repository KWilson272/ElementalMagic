package me.kwilson272.elementalmagic.core.gameplay.earth.earthgrab;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.components.EarthPillar;
import me.kwilson272.elementalmagic.core.gameplay.components.EarthPillar.PillarState;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class EarthGrab extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();
    
    private long cooldown;
    private long duration;
    private double selectRange;
    private double radius;
    private int height;
    private double riseSpeed;
    private double knockup;
    private double hitboxSize;

    private boolean isRising;
    private long revertTime;

    private List<EarthPillar> pillars;

	public EarthGrab(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        selectRange = CONFIG.selectRange;
        radius = CONFIG.radius;
        height = CONFIG.height;
        riseSpeed = CONFIG.riseSpeed;
        knockup = CONFIG.knockup;
        hitboxSize = CONFIG.hitboxSize;

        isRising = true;
        pillars = new ArrayList<>();
	}

	@Override
	public boolean start() {
	    Entity target = getTargetEntity();
        if (target == null) {
            return false;
        }

        Location loc = target.getLocation().add(0, -1, 0);
        initPillars(loc, radius, height);
        initPillars(loc, radius + 1, height - 1);
        if (pillars.isEmpty()) {
            return false;
        }

        user().addCooldown(name(), cooldown);
        return true;
    }

    private Entity getTargetEntity() {
        World world = user().player().getWorld();
        Location start = user().player().getEyeLocation();
        Vector direction = start.getDirection();

        RayTraceResult result = world.rayTraceEntities(
                start, 
                direction, 
                selectRange, 
                1.25, 
                this::canTarget
        );

        return result != null ? result.getHitEntity() : null;
    }

    private boolean canTarget(Entity entity) {
        return !entity.equals(user().player())
            && entity instanceof LivingEntity
            && ElementalMagicApi.effectHandler().canAffect(entity);
    }

    private void initPillars(Location loc, double radius, int height) {
        double spacing = 0.5;
        double step = 2 * Math.asin(spacing / (2 * radius));
        int count = (int) Math.ceil(2 * Math.PI / step);
        for (int i = 0; i < count; ++i) {
            double angle = step * i;
            Vector vec = Vectors.fromRadians(angle, 0);
            Location spawn = loc.clone().add(vec.multiply(radius));

            Block block = getSafeBlock(spawn.getBlock());
            if (block != null) {
                var pillar = new EarthPillar(this, block, height, riseSpeed, isRising);
                pillar.setMoveCallback(this::knockEntities);
                pillar.setBlockPlaceCallback(this::playSound);
                pillars.add(pillar);
            }
        }
    }

    private Block getSafeBlock(Block block) {
        for (int i = 0; i < height; ++i) {
            Block above = block.getRelative(BlockFace.UP);
            if (EarthPillar.getFromBlock(block) != null) {
                return null;
            } else if (Blocks.isSolid(block) && !Blocks.isSolid(above)) {
                break;
            } else if (Blocks.isSolid(above)) {
                block = above;
            } else {
                block = block.getRelative(BlockFace.DOWN);
            }
        }

        Block above = block.getRelative(BlockFace.UP);
        if (!isUsableEarth(block) || Blocks.isSolid(above)
                || EarthPillar.getFromBlock(block) != null) {
            return null;
        }
    
        return block;
    }

    private void knockEntities(Location loc) {
        Vector knock = new Vector(0, knockup, 0);
        for (Entity e : Entities.getNearbyEntities(loc, hitboxSize)) {
            ElementalMagicApi.effectHandler().setVelocity(e, this, knock);
        }
    }

    private void playSound(Block block) {
        int chance = Math.max(1, pillars.size() / 3);
        if (ThreadLocalRandom.current().nextInt(chance) == 0) {
            playEarthSound(block.getLocation());
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }

        if (!isRising) {
            return System.currentTimeMillis() < revertTime;
        }

        boolean roseAny = false;
        for (EarthPillar pillar : pillars) {
            if (pillar.getState() != PillarState.IDLE) {
                pillar.progress();
                roseAny = true;
            }
        }

        if (!roseAny) {
            revertTime = System.currentTimeMillis() + duration;
            isRising = false;
        }

        return true;
	}

	@Override
	public void onDestruction() {
        pillars.forEach(EarthPillar::revert);
    }

	@Override
	public String name() {
        return "EarthGrab";
	}

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = EarthGrabController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 8000;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 12000;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 18;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 3.0; 
        @Configure(path = CONFIG_PATH + "Height", config = Config.ABILITIES)
        private int height = 3;
        @Configure(path = CONFIG_PATH + "RiseSpeed", config = Config.ABILITIES)
        private double riseSpeed = 1.0;
        @Configure(path = CONFIG_PATH + "Knockup", config = Config.ABILITIES)
        private double knockup = 0.7;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.2;
    } 
}
