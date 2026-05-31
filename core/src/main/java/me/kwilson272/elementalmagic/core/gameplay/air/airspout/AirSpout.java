package me.kwilson272.elementalmagic.core.gameplay.air.airspout;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.air.AirAbility;

public class AirSpout extends AirAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double height;
    private double flySpeed;

    private double breakHeight;
    private boolean isInfinite;
    private long endTime;

	public AirSpout(AbilityUser user, AbilityController controller) {
		super(user, controller);
        
        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        height = CONFIG.height;
        flySpeed = CONFIG.flySpeed;
	}

	@Override
	public boolean start() {
        breakHeight = height + 4;
        isInfinite = duration < 0;
        endTime = System.currentTimeMillis() + duration;
        
        Block base = getBaseBlock();
        if (base == null) {
            return false;
        }

        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)
                || (!isInfinite && System.currentTimeMillis() > endTime)) {
            return false;
        }

        Block base = getBaseBlock();
        if (base == null) {
            return false;
        }

        Player player = user().player();
        double yDiff = player.getLocation().getBlockY() - base.getY();
        if (yDiff > breakHeight) {
            return false;
        }
        
        setFlying(yDiff <= height);
        ElementalMagicApi.effectHandler()
            .removePotionEffect(player, this, PotionEffectType.SPEED);

        drawSpout(yDiff);
        playAirSound(user().player().getLocation());    
        return true;
	}

    private Block getBaseBlock() {
        Location loc = user().player().getLocation();
        Block block = loc.getBlock();

        for (int i = 0; i <= breakHeight; ++i) {
            if (BlockUtil.isSolid(block) || BlockUtil.isLiquid(block)) {
                return block;
            }
            block = block.getRelative(BlockFace.DOWN);
        }
        return null;
    }

    private void drawSpout(double yDiff) {
        double spacing = 0.5;
        Location loc = user().player().getLocation();
        for (double i = 0; i <= yDiff; i += 0.5) {
            playAirParticles(loc, 1, 0.4, 0.4, 0.4);
            loc.add(0, -spacing, 0);
        }
    }

    private void setFlying(boolean flying) {
        user().player().setAllowFlight(flying);
        user().player().setFlying(flying);
    }

	@Override
	public void onDestruction() {
        setFlying(false);
        user().addCooldown(name(), cooldown);
	}

	@Override
	public String name() {
        return "AirSpout";
	}

    public double getSpeed() {
        return flySpeed;
    }

    protected static class ConfigValues {
    
        private static final String CONFIG_PATH = AirSpoutController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = -1;
        @Configure(path = CONFIG_PATH + "Height", config = Config.ABILITIES)
        private double height = 10;
        @Configure(path = CONFIG_PATH + "FlySpeed", config = Config.ABILITIES)
        private double flySpeed = 0.15;
    }
}
