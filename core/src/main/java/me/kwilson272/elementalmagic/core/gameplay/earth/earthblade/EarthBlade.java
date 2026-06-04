package me.kwilson272.elementalmagic.core.gameplay.earth.earthblade;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.components.EarthPillar;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class EarthBlade extends EarthAbility {
        
    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private boolean canSwapSlots;
    private double range;
    private double travelSpeed;
    private int maxHeight;
    private int maxClimb;
    private double riseSpeed;
    private double hitboxSize;
    private double damage;
    private double knockup;
    private long revertTime;
    
    private int curHeight;
    private int heightIncInterval;
    private int heightCounter;
    private boolean isAdvancing;
    private double rangeCounter;

    private long endTime;

    private Location location;
    private Vector direction;

    private List<EarthPillar> pillars;

	public EarthBlade(AbilityUser user, AbilityController controller) {
		super(user, controller);

	    cooldown = CONFIG.cooldown;
        canSwapSlots = CONFIG.canSwapSlots;
        range = CONFIG.range;
        travelSpeed = CONFIG.travelSpeed;
        maxHeight = CONFIG.maxHeight;
        maxClimb = CONFIG.maxClimb;
        riseSpeed = CONFIG.riseSpeed;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        knockup = CONFIG.knockup;
        revertTime = CONFIG.revertTime;

        curHeight = 1;
        heightCounter = 0;
        isAdvancing = true;
        rangeCounter = 0;

        pillars = new ArrayList<>();
    }

	@Override
	public boolean start() {
        Location eyeLoc = user().player().getEyeLocation();
        direction = Vectors.fromRotations(0, eyeLoc.getYaw());
       
        location = user().player().getLocation().add(0, -1, 0);
        location.add(direction.clone().multiply(2));
        Block block = getSafeBlock(location.getBlock());
        if (block == null) {
            return false;
        }

        location.setY(block.getY());
        heightIncInterval = (int) Math.max(1, range / maxHeight);
        user().addCooldown(name(), cooldown);
        return true;
    }



	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }
    
        if (isAdvancing  && (!advanceLocation() 
                    || !user().getSelectedBindName().equals("EarthBlade"))) {
            endTime = System.currentTimeMillis() + revertTime;
            isAdvancing = false; 
        }
       
        boolean roseAny = false;
        for (EarthPillar pillar : pillars) {
            if (!pillar.isIdle()) {
                pillar.progress();
                roseAny = true;
            }
        }

        return isAdvancing || roseAny || System.currentTimeMillis() < endTime; 
	}

    private boolean advanceLocation() {
        double remainder = travelSpeed;
        while (remainder > 0) {
            double travel = Math.min(remainder, 1);
            remainder--;

            Vector move = direction.clone().multiply(travel);
            Block prev = location.getBlock();
            Block next = location.add(move).getBlock(); 
            if (!prev.equals(next)) {
                Block block = getSafeBlock(next);
                if (block == null) {
                    return false;
                }

                EarthPillar pillar = new EarthPillar(this, block, curHeight, riseSpeed, true);
                pillar.setBlockPlaceCallback(this::playSound);
                pillar.setMoveCallback(this::affectEntities);
                pillars.add(pillar);

                if (++heightCounter % heightIncInterval == 0 && curHeight < maxHeight) {
                    ++curHeight;
                }
            }

            rangeCounter += travel;
            if (rangeCounter >= range) {
                return false;
            }
        }

        return true;
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

    private void playSound(Block block) {
        int chance = (int) Math.max(1, pillars.size() / 3);  
        if (ThreadLocalRandom.current().nextInt(chance) == 0) {
            playEarthSound(block.getLocation()); 
        }
    }

    private void affectEntities(Location loc) {
        Vector knock = new Vector(0, knockup, 0);
        for (Entity e : Entities.getNearbyEntities(loc, hitboxSize)) {
            if (!e.equals(user().player())) {
                ElementalMagicApi.effectHandler().setVelocity(e, this, knock);
                ElementalMagicApi.effectHandler().damageEntity(e, this, damage);
            }
        }
    }

	@Override
	public void onDestruction() {
        pillars.forEach(EarthPillar::revert);
	}

	@Override
	public String name() {
        return "EarthBlade";
	}

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = EarthBladeController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 8200;
        @Configure(path = CONFIG_PATH + "CanSwapSlots", config = Config.ABILITIES)
        private boolean canSwapSlots = false;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 16;
        @Configure(path = CONFIG_PATH + "TravelSpeed", config = Config.ABILITIES)
        private double travelSpeed = 0.7;
        @Configure(path = CONFIG_PATH + "MaxHeight", config = Config.ABILITIES)
        private int maxHeight = 7;
        @Configure(path = CONFIG_PATH + "MaxClimb", config = Config.ABILITIES)
        private int maxClimb = 3;
        @Configure(path = CONFIG_PATH + "RiseSpeed", config = Config.ABILITIES)
        private double riseSpeed = 0.5;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.75;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "Knockup", config = Config.ABILITIES)
        private double knockup = 1.5;
        @Configure(path = CONFIG_PATH + "RevertTime", config = Config.ABILITIES)
        private long revertTime = 7500;
    }
}
