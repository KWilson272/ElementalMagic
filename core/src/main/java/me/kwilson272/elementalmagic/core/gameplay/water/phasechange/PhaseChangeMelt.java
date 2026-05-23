package me.kwilson272.elementalmagic.core.gameplay.water.phasechange;

import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterUtil;
import me.kwilson272.elementalmagic.core.gameplay.water.icewave.IceWave;
import me.kwilson272.elementalmagic.core.gameplay.water.surge.SurgeWave;
import me.kwilson272.elementalmagic.core.gameplay.water.torrent.Torrent;

public class PhaseChangeMelt extends CoreAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private long endTime;
    private boolean isInfinite;
    private double range;
    private double radius;
    private long revertTime;

    public PhaseChangeMelt(AbilityUser user, AbilityController controller) {
        super(user, controller);
        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        range = CONFIG.range;
        radius = CONFIG.radius;
        revertTime = CONFIG.revertTime;
    }

    @Override
    public boolean start() {
        isInfinite = duration < 0;
        endTime = System.currentTimeMillis() + duration;
        return true;
    }

    @Override
    public boolean progress() {
        if (!user().player().isSneaking()
                || !user().canUse(controller(), true, true) 
                || (!isInfinite && System.currentTimeMillis() > endTime)) {
            return false;
        }

        Player player = user().player();
        Block target = BlockUtil.getTargetBlock(player, range, BlockUtil::isSolid);
        Location center = target.getLocation().add(0.5, 0.5, 0.5);
        if (meltSphere(center)) {
            WaterUtil.playWaterSound(center);
        }

        return true;
    }

    private boolean meltSphere(Location center) {
        BlockData waterData = Material.WATER.createBlockData();
        BlockData airData = Material.AIR.createBlockData();
        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        
        TempBlockBuilder blockBuilder = TempBlock.builder(this, waterData)
            .setDuration(revertTime)
            .setUsable(true);

        boolean melted = false;
        for (Block block : BlockUtil.collectSphere(center, radius)) {
            if (!isMeltable(block)) {
                continue;
            }
    
            melted = false;
            BlockData data = AbilityUtil.isIce(block) ? waterData : airData;
            TempBlock.get(block).ifPresentOrElse(revertManager::revert,
                () -> blockBuilder.setData(data).buildAt(block));
        }

        return melted;
    }

    private boolean isMeltable(Block block) {
        if (!AbilityUtil.isIce(block) && !AbilityUtil.isSnow(block)) {
            return false;
        }
        
        TempBlock tb = TempBlock.get(block).orElse(null);
        if (tb == null || tb.isUsable()) {
            return true;
        }

        // Melt these abilities regardless of usability, for gameplay 
        Ability ability = tb.ability();
        return ability instanceof SurgeWave 
            || ability instanceof Torrent
            || ability instanceof IceWave;
    }

    @Override
    public void onDestruction() {
        user().addCooldown("PhaseChangeMelt", cooldown);
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = PhaseChangeController.CONFIG_PATH + "Melt.";

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "AbilityDuration", config = Config.ABILITIES)
        private long duration = -1;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double range = 25.0;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 4.0;
        @Configure(path = CONFIG_PATH + "RevertTime", config = Config.ABILITIES)
        private long revertTime = 30000;
    }
}

