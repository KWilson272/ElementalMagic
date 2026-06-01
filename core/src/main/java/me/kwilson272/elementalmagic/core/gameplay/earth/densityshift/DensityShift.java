package me.kwilson272.elementalmagic.core.gameplay.earth.densityshift;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;

public class DensityShift extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long sandDuration;
    private double sandRadius;

	public DensityShift(AbilityUser user, AbilityController controller) {
		super(user, controller);
        sandDuration = CONFIG.sandDuration;
        sandRadius = CONFIG.sandRadius;
	}

	@Override
	public boolean start() {
        BlockData data = Material.SAND.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data)
            .setDuration(sandDuration)
            .setUsable(true);
        
        Location loc = user().player().getLocation();
        for (Block b : Blocks.collectSphere(loc, sandRadius)) {
            if (isUsableEarth(b)) {
                builder.buildAt(b);
            }
        }

        return false;
	}

	@Override
	public boolean progress() {
        return false;
	}

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "DensityShift";
	}

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = DensityShiftController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "SandDuration", config = Config.ABILITIES)
        private long sandDuration = 500;
        @Configure(path = CONFIG_PATH + "SandRadius", config = Config.ABILITIES)
        private double sandRadius = 2.2;
    }
}
