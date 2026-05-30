package me.kwilson272.elementalmagic.core.gameplay.air.airscooter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.air.AirAbility;

/**
 * To preserve the 'feel' of AirScooter, much of the math in this class
 * is from ProjectKorra 1.8.0-BETA-9 AirScooter. All credit reserved for 
 * the original authors & maintainers.
 */
public class AirScooter extends AirAbility {

	protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double speed;
    private int maxHeight;
    private double verticalForce;
    private double waterForceMod;

    private boolean isInfinite;
    private long endTime;

    private List<Vector> sphereVecs;

    public AirScooter(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        speed = CONFIG.speed;
        maxHeight = CONFIG.maxHeight;
        verticalForce = CONFIG.verticalForce;
        waterForceMod = CONFIG.waterForceMod;

        sphereVecs = new ArrayList<>();
    }

	@Override
	public boolean start() {
        Location loc = user().player().getLocation().add(0, -0.2, 0);
        Block block = loc.getBlock();
        if (block.getType().isSolid()) {
            return false;
        }
        
        Block base = getSupportBlock();
        if (base == null || !user().player().isSprinting()) {
            return false;
        }

        Location eyeLoc = user().player().getEyeLocation();
        if (BlockUtil.isLiquid(eyeLoc.getBlock())) {
            return false;
        }

        isInfinite = duration < 0;
        endTime = System.currentTimeMillis() + duration;
        initSphereVecs();
        return true;
	}

    private void initSphereVecs() {
        for (int theta = 0; theta < 180; theta += 45) {
            for (int phi = 0; phi < 360; phi += 45) {
                double rTheta = Math.toRadians(theta);
                double rPhi = Math.toRadians(phi);

                double x = 0.5 * Math.sin(rTheta) * Math.cos(rPhi);
                double y = 0.5 * Math.cos(rTheta);
                double z = 0.5 * Math.sin(rPhi) * Math.sin(rTheta);

                sphereVecs.add(new Vector(x, y, z));
            }
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)
                || (!isInfinite && System.currentTimeMillis() > endTime)) {
            return false;
        }

        Block base = getSupportBlock();
        if (base == null) {
            return false;
        }

        Location eyeLoc = user().player().getEyeLocation();
        if (BlockUtil.isLiquid(eyeLoc.getBlock())) {
            return false;
        }

        handleVelocity(base);
        drawScooter();
        if (ThreadLocalRandom.current().nextInt(4) == 0) {
            playAirSound(user().player().getLocation());
        }

        return true;
	}

    private Block getSupportBlock() {
        Block block = user().player().getEyeLocation().getBlock();
        for (int i = 0; i <= maxHeight; i++) {
            if (block.getType().isSolid() || block.isLiquid()) {
                return block;
            }
            block = block.getRelative(BlockFace.DOWN);
        }
        return null;
    }
    
    private void handleVelocity(Block base) {
        Location loc = user().player().getEyeLocation();
        double yaw = Math.toRadians(loc.getYaw());
        double x = -Math.sin(yaw);
        double z = Math.cos(yaw);
        Vector velocity = new Vector(x, 0, z).multiply(speed);

        double yDiff = user().player().getLocation().getY() - base.getY();
        double dY = Math.abs(yDiff - 2.4);

        if (yDiff > 2.75) {
            velocity.setY(-verticalForce * dY * dY);
        } else if (yDiff < 2) {
            velocity.setY(verticalForce * dY * dY);
        }

        if (base.isLiquid()) {
            velocity.setY(velocity.getY() * waterForceMod);
        }
            
        Player player = user().player();
        player.setSprinting(false);
        ElementalMagicApi.effectHandler().removePotionEffect(player, this, PotionEffectType.SPEED);
        ElementalMagicApi.effectHandler().setVelocity(player, this, velocity);
    }

    private void drawScooter() {
        Location center = user().player().getLocation().add(0, -0.5, 0);
        for (Vector v : sphereVecs) {
            playAirParticles(center.clone().add(v), 1, 0, 0, 0);
        }
    }
    
	@Override
	public void onDestruction() {
        user().addCooldown(name(), cooldown);
	}

	@Override
	public String name() {
        return "AirScooter";
	}

    protected static class ConfigValues {

        private static final String CONFIG_PATH = AirScooterController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = -1;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 0.6;
        @Configure(path = CONFIG_PATH + "MaxHeight", config = Config.ABILITIES)
        private int maxHeight = 7;
        @Configure(path = CONFIG_PATH + "VerticalForce", config = Config.ABILITIES)
        private double verticalForce = 0.25;
        @Configure(path = CONFIG_PATH + "WaterForceMod", config = Config.ABILITIES)
        private double waterForceMod = 0.85;

    }
}
