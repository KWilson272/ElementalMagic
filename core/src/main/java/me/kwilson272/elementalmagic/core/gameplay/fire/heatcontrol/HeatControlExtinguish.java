package me.kwilson272.elementalmagic.core.gameplay.fire.heatcontrol;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;

public class HeatControlExtinguish extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double selectRange;
    private double clickRadius;
    private double sneakRadius;
    private long revertTime;
    
    private boolean isSneak;

	public HeatControlExtinguish(AbilityUser user, AbilityController controller, boolean isSneak) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        clickRadius = CONFIG.clickRadius;
        sneakRadius = CONFIG.sneakRadius;
        revertTime = CONFIG.revertTime;
        this.isSneak = isSneak;
	}

	@Override
	public boolean start() {
        Player player = user().player();
        if (isSneak) {
            extinguishAround(player.getLocation(), sneakRadius);
        } else {
            Block b = BlockUtil.getTargetBlock(player, selectRange, BlockUtil::isSolid);
            extinguishAround(b.getLocation().add(0.5, 0.5, 0.5), clickRadius);
        }

        user().addCooldown("HeatControlExtinguish", cooldown); 
        return false;
	}

    private void extinguishAround(Location loc, double radius) {
        BlockData data = Material.AIR.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data)
            .setDuration(revertTime);
    
        boolean extinguishedAny = false;
        for (Block b : BlockUtil.collectSphere(loc, radius)) {
            Material type = b.getType();
            if (type == Material.FIRE || type == Material.SOUL_FIRE) {
                extinguishedAny = true;
                builder.buildAt(b);     
            }
        }

        if (extinguishedAny) {
            World world = loc.getWorld();
            world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1, 1);
        }
    }

	@Override
	public boolean progress() {
        return false;
	}

	@Override
	public void onDestruction() {
	}

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = HeatControlController.CONFIG_PATH + "Extinguish.";
    
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 25.0;
        @Configure(path = CONFIG_PATH + "ClickRadius", config = Config.ABILITIES)
        private double clickRadius = 4.0;
        @Configure(path = CONFIG_PATH + "SneakRadius", config = Config.ABILITIES)
        private double sneakRadius = 6.0;
        @Configure(path = CONFIG_PATH + "RevertTime", config = Config.ABILITIES)
        private long revertTime = 20000;
    }
}
