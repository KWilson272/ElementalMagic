package me.kwilson272.elementalmagic.core.gameplay.fire.blaze;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;

public class Blaze extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double range;
    private double speed;
    private double angle;
    private double fireDamage;
    private long fireDuration;

    private List<BlazeStream> streams;

	public Blaze(AbilityUser user, AbilityController controller, boolean isSneak) {
		super(user, controller);

        if (isSneak) {
            cooldown = CONFIG.ringCooldown;
            range = CONFIG.ringRange;
            speed = CONFIG.ringSpeed;
            angle = 360;
            fireDamage = CONFIG.ringFireDamage;
            fireDuration = CONFIG.ringFireDuration;
        } else {
            cooldown = CONFIG.coneCooldown;
            range = CONFIG.coneRange;
            speed = CONFIG.coneSpeed;
            angle = CONFIG.coneAngle;
            fireDamage = CONFIG.coneFireDamage;
            fireDuration = CONFIG.coneFireDuration;
        }

        streams = new ArrayList<>();
	}

	@Override
	public boolean start() {
        initStreams();
        if (streams.isEmpty()) {
            return false;
        }

        user().addCooldown("Blaze", cooldown);
        return true;
	}

    private void initStreams() {
        double blockSpacing = 0.5;
        double rad = Math.toRadians(angle);
        double step = 2 * Math.asin(blockSpacing / (2 * range));
        double count = (int) Math.ceil(rad / step); // 180 deg blade
        
        Location eyeLoc = user().player().getEyeLocation();
        Location loc = user().player().getLocation();
        double start = Math.toRadians(eyeLoc.getYaw()) - (count/2 * step);
        for (int i = 0; i <= count; ++i) {
            double spawnAngle = start + (i * step);
            double x = -Math.sin(spawnAngle);
            double z = Math.cos(spawnAngle);
            Vector dir = new Vector(x, 0, z);
            // Don't burn the player by spawning it in their feet
            Location spawn = loc.clone().add(dir.clone().multiply(2));
            Block block = getSafeBlock(spawn);

            if (block != null) {
                streams.add(new BlazeStream(block, dir));
            }
        }
    }

    private Block getSafeBlock(Location location) {
        Block block = location.getBlock();
        for (int i = 0; i < 4; ++i) {
            if (block.getType().isSolid()) {
                block = block.getRelative(BlockFace.UP);
            } else if (!block.getRelative(BlockFace.DOWN).getType().isSolid()) {
                block = block.getRelative(BlockFace.DOWN);
            } else if (!Blocks.isWater(block)) {
                return block;
            }
        }

        if (!block.getType().isSolid() && !Blocks.isWater(block)
                && block.getRelative(BlockFace.DOWN).getType().isSolid()) {
            return block;
        }
        return null;
    }        
    
	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }

        streams.removeIf(stream -> !stream.progress());
        return !streams.isEmpty();
	}

	@Override
	public void onDestruction() {
	}

    @Override
    public String name() {
        return "Blaze";
    }

    private class BlazeStream {
        
        private Location location;
        private Vector direction;
        private double rangeCounter;
        private Block lastBlock;

        public BlazeStream(Block block, Vector direction) {
            this.location = block.getLocation().add(0.5, 0.5, 0.5);
            this.direction = direction;
            this.rangeCounter = range;
            lastBlock = block;
            createFire(block);
        }

        private void createFire(Block block) {
            TempBlock.builder(Blaze.this, getFireData())
                .setDuration(fireDuration)
                .setDamage(fireDamage)
                .buildAt(block);
        }

        private boolean progress() {
            double remainder = speed;
            while (remainder > 0) {
                // Allows for heatcontrol users to stop blaze from advancing
                if (lastBlock.getType() != Material.FIRE 
                        && lastBlock.getType() != Material.SOUL_FIRE) {
                    return false;
                }

                double travel = Math.min(remainder, 1);
                location.add(direction.clone().multiply(travel));
                Block block = getSafeBlock(location);
                if (block == null || Blocks.isLiquid(block)) {
                    return false;
                }

                location.setY(block.getY() + 0.5);
                if (!lastBlock.equals(block)) {
                    lastBlock = block;
                    createFire(block);
                }
                
                remainder--;
                rangeCounter -= travel;
                if (rangeCounter <= 0) {
                    return false;
                }
            }

            return true;
        }
    }
    
    protected static class ConfigValues {
    
        private static final String CONFIG_PATH =  BlazeController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Ring.Cooldown", config = Config.ABILITIES)
        private long ringCooldown = 1000;
        @Configure(path = CONFIG_PATH + "Ring.Range", config = Config.ABILITIES)
        private double ringRange = 7.0;
        @Configure(path = CONFIG_PATH + "Ring.Speed", config = Config.ABILITIES)
        private double ringSpeed = 1.0;
        @Configure(path = CONFIG_PATH + "Ring.FireDamage", config = Config.ABILITIES)       
        private double ringFireDamage = 1.0;
        @Configure(path = CONFIG_PATH + "Ring.FireDuration", config = Config.ABILITIES)       
        private long ringFireDuration = 350;

        @Configure(path = CONFIG_PATH + "Cone.Cooldown", config = Config.ABILITIES)
        private long coneCooldown = 1000;
        @Configure(path = CONFIG_PATH + "Cone.Range", config = Config.ABILITIES)
        private double coneRange = 15.0;
        @Configure(path = CONFIG_PATH + "Cone.Speed", config = Config.ABILITIES)
        private double coneSpeed = 0.75;
        @Configure(path = CONFIG_PATH + "Cone.Angle", config = Config.ABILITIES)
        private double coneAngle = 45.0;
        @Configure(path = CONFIG_PATH + "Cone.FireDamage", config = Config.ABILITIES)
        private double coneFireDamage = 1.0;
        @Configure(path = CONFIG_PATH + "Cone.FireDuration", config = Config.ABILITIES)
        private long coneFireDuration = 350;
    }
}
