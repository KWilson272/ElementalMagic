package me.kwilson272.elementalmagic.core.gameplay.earth.catapult;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;

public class Catapult extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double push;
    private double sneakModifier;
    private int maxBlockFactor;
    private int velocityTicks;

    private Vector force;
    // Simulating the weird distance stuff beta9 did for continuous velocity
    private int progressCounter;

	public Catapult(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        push = CONFIG.push;
        sneakModifier = CONFIG.sneakModifier;
        maxBlockFactor = CONFIG.maxBlockFactor;
        velocityTicks = CONFIG.velocityTicks;
        progressCounter = 0;
	}

	@Override
	public boolean start() {
        int blockCount = countEarthBlocks();
        if (blockCount <= 0) {
            return false;
        }
   
        if (user().player().isSneaking()) {
            blockCount *= sneakModifier;
        }

        // This is a slight adjustment to beta9's math
        Player player = user().player();
        Location loc = player.getEyeLocation();
        force = loc.getDirection().multiply(push * blockCount / maxBlockFactor);
        // Without this velocity the move won't work & feel bad, so don't put on cd 
        if (!ElementalMagicApi.effectHandler().setVelocity(player, this, force)) {
            return false;
        }

        playEarthSound(loc);
        user().addCooldown(name(), cooldown);
        return false;
	}

    private int countEarthBlocks() {
        Location loc = user().player().getEyeLocation();
        Vector dir = loc.getDirection().multiply(-1);
        
        // We need to do this because the first few blocks behind the players
        // head are unlikely to be earth & to preserve old behavior
        boolean seenEarth = false;
        int blockCount = 0;
        for (int i = 0; i <= maxBlockFactor; ++i) {
            Block block = loc.getBlock();
            if (Blocks.isEarth(block) || Blocks.isSand(block) || Blocks.isMetal(block)) {
                ++blockCount;
                seenEarth = true;
            } else if (seenEarth || i >= 4) {
                break;
            } 

            loc.add(dir);
        }

        return blockCount;
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }

        Player player = user().player();
        // Also exactly from Beta9
        Vector push = force.clone();
        push.setY(player.getVelocity().getY());
        ElementalMagicApi.effectHandler().setVelocity(player, this, push);
        
        return ++progressCounter < velocityTicks;
	}

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "Catapult";
	}

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = CatapultController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "Push", config = Config.ABILITIES)
        private double push = 2.8;
        @Configure(path = CONFIG_PATH + "SneakModifier", config = Config.ABILITIES)
        private double sneakModifier = 0.5;
        @Configure(path = CONFIG_PATH + "MaxBlockFactor", config = Config.ABILITIES)
        private int maxBlockFactor = 6;
        @Configure(path = CONFIG_PATH + "VelocityTicks", config = Config.ABILITIES)
        private int velocityTicks = 2;
    } 
}
