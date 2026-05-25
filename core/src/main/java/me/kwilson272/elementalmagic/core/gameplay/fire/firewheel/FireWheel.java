package me.kwilson272.elementalmagic.core.gameplay.fire.firewheel;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;

public class FireWheel extends FireAbility {

	protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double radius;
    private double speed;
    private int maxClimb;
    private double range;
    private double hitboxSize;
    private double damage;
    private double knockback;
    private long burnDuration;

    private double rangeCounter;
    private Location location;
    private Vector direction;

    private List<Vector> ringVecs;

    public FireWheel(AbilityUser user, AbilityController controller) {
        super(user, controller);
        
        cooldown = CONFIG.cooldown;
        radius = CONFIG.radius;
        speed = CONFIG.speed;
        maxClimb = CONFIG.maxClimb;
        range = CONFIG.range;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        knockback = CONFIG.knockback;
        burnDuration = CONFIG.burnDuration;
        
        rangeCounter = 0;
        ringVecs = new ArrayList<>();
    }

	@Override
	public boolean start() {
        location = user().player().getLocation();

        Block base = getBase();
        if (base == null) {
            return false;
        }
        
        initVecs();
        user().addCooldown("FireWheel", cooldown);
        return true;
	}

    private Block getBase() {
        Block block = location.getBlock();
        for (int i = 0; i < maxClimb; ++i) {
            Block above = block.getRelative(BlockFace.UP);
            if (!BlockUtil.isSolid(above) && BlockUtil.isSolid(block)
                    && !AbilityUtil.isWater(above)) {
                return block;
            } else if (BlockUtil.isSolid(above)) {
                block = above;   
            } else {
                block = block.getRelative(BlockFace.DOWN);
            }
        } 
      
        Block above = block.getRelative(BlockFace.UP);
        if (BlockUtil.isSolid(block) && !BlockUtil.isSolid(above)
                && !BlockUtil.isLiquid(above)) {
            return block;
        }
        return null;
    }       

    private void initVecs() {
        Location loc = user().player().getEyeLocation();
        double yaw = Math.toRadians(loc.getYaw());
        double x = -Math.sin(yaw);
        double z = Math.cos(yaw);
        direction = new Vector(x, 0, z);

        double spacing = 0.25;
        double step = Math.asin(spacing / (2 * radius));
        int count = (int) Math.ceil(2 * Math.PI / step);

        for (int i = 0; i < count; ++i) {
            double angle = i * step;
            double y = Math.sin(angle) * radius;
            double xzMag = Math.cos(angle) * radius;
            ringVecs.add(new Vector(x * xzMag, y, z * xzMag));
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }
        
        return advanceLocation();
	}

    private boolean advanceLocation() {
        double iterSpeed = calculateIterSpeed();
        Vector travel = direction.clone().multiply(iterSpeed);

        for (double i = 0; i < speed; i += iterSpeed) {
            location.add(travel);
            Block base = getBase();
            if (base == null) {
                return false; 
            }
            
            // We want the location to be the center of the wheel 
            double y = base.getY() + 1 + radius;
            location.setY(y);
            affectEntities();

            rangeCounter += iterSpeed;
            if (rangeCounter >= range) {
                return false;
            }
        }

        displayWheel();
        return true;
    }

    private double calculateIterSpeed() {
        if (speed <= 1.0) {
            return speed;
        } else {
            int toRound = ((int) speed) + 5;
            double tenMod = Math.round(toRound / 10.0);
            return speed / (Math.floor(speed) + tenMod + 1);
        }
    }



    private void affectEntities() {
        Vector knock = direction.clone().multiply(knockback);
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();

        for (Entity e : EntityUtil.getNearbyEntities(location, hitboxSize)) {
            if (e.equals(user().player())) {
                continue;
            }
           
            effectHandler.setVelocity(e, this, knock);
            effectHandler.damageEntity(e, this, damage);
            if (e.getFireTicks() * 50 < burnDuration) {
                effectHandler.setFireDuration(e, this, burnDuration);
            }
        }
    }

    private void displayWheel() {
        World world = user().player().getWorld();
        Particle particle = getFireParticle();

        for (int i = 0; i < ringVecs.size(); ++i) {
            Vector ringVec = ringVecs.get(i);
            Location disp = location.clone().add(ringVec);
            world.spawnParticle(particle, disp, 1, 0, 0, 0, 0.003);    
        }

        world.spawnParticle(Particle.SMOKE, location, 1, 0.5, 0.5, 0.5, 0.01);
        playFireSound(location);
    }

    @Override
    public void onDestruction() {
    }

    @Override
    public String name() {
        return "FireWheel";
    }

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = FireWheelController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 5000;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 1.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.1;
        @Configure(path = CONFIG_PATH + "MaxClimb", config = Config.ABILITIES)
        private int maxClimb = 7;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 30;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.2;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 3.0;
        @Configure(path = CONFIG_PATH + "knockback", config = Config.ABILITIES)
        private double knockback = 0.8;
        @Configure(path = CONFIG_PATH + "burnDuration", config = Config.ABILITIES)
        private long burnDuration = 1000;
    }
}
