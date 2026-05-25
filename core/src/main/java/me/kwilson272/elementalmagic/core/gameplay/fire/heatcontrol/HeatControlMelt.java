package me.kwilson272.elementalmagic.core.gameplay.fire.heatcontrol;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.water.icewave.IceWave;
import me.kwilson272.elementalmagic.core.gameplay.water.surge.SurgeWave;
import me.kwilson272.elementalmagic.core.gameplay.water.torrent.Torrent;

public class HeatControlMelt extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double selectRange;
    private double clickRadius;
    private double sneakRadius;
    private long revertTime;
    private boolean meltUnusable;
    
    private boolean isSneak;

	public HeatControlMelt(AbilityUser user, AbilityController controller, boolean isSneak) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        clickRadius = CONFIG.clickRadius;
        sneakRadius = CONFIG.sneakRadius;
        revertTime = CONFIG.revertTime;
        meltUnusable = CONFIG.meltUnusable;
        this.isSneak = isSneak;
	}

	@Override
	public boolean start() {
        Player player = user().player();
        if (isSneak) {
            meltAround(player.getLocation(), sneakRadius);
        } else {
            Block b = BlockUtil.getTargetBlock(player, selectRange, BlockUtil::isSolid);
            meltAround(b.getLocation().add(0.5, 0.5, 0.5), clickRadius);
        }

        user().addCooldown("HeatControlMelt", cooldown); 
        return false;
	}

    private void meltAround(Location loc, double radius) {
        for (Block b : BlockUtil.collectSphere(loc, radius)) {
            if (canMelt(b)) {
                melt(b);
            }
        }
    }

    private boolean canMelt(Block block) {
        if (!AbilityUtil.isSnow(block) && !AbilityUtil.isIce(block)) {
            return false;
        }

        TempBlock tb = TempBlock.get(block).orElse(null);
        return tb == null || tb.isUsable() || (meltUnusable 
                && (tb.ability() instanceof Torrent 
                || tb.ability() instanceof SurgeWave
                || tb.ability() instanceof IceWave));
    }

    private void melt(Block block) {
        if (AbilityUtil.isSnow(block)) {
            BlockData data = Material.AIR.createBlockData();
            TempBlock.builder(this, data).setDuration(revertTime).buildAt(block);
            return;
        }

        TempBlock tb = TempBlock.get(block).orElse(null);
        if (tb != null) {
            ElementalMagicApi.revertibleManager().revert(tb);    
        } else {
            BlockData data = Material.WATER.createBlockData();
            TempBlock.builder(this, data).setDuration(revertTime)
                .setUsable(true).buildAt(block);
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
        
        private static final String CONFIG_PATH = HeatControlController.CONFIG_PATH + "Melt.";
    
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
        @Configure(path = CONFIG_PATH + "MeltUnusable", config = Config.ABILITIES)
        private boolean meltUnusable = true;
    }
}
