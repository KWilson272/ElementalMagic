package me.kwilson272.elementalmagic.core.gameplay.fire.jetblast;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.fire.firejet.FireJet;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.water.icewave.IceWave;
import me.kwilson272.elementalmagic.core.gameplay.water.surge.SurgeWave;
import me.kwilson272.elementalmagic.core.gameplay.water.torrent.Torrent;

public class JetBlast extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double speed;
    private boolean meltIce;
    private double meltRadius;
    private long meltDuration;
    private boolean meltUnusable;

    private FireJet firejet;

	public JetBlast(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        speed = CONFIG.speed;
        meltIce = CONFIG.meltIce;
        meltRadius = CONFIG.meltRadius;
        meltDuration = CONFIG.meltDuration;
        meltUnusable = CONFIG.meltUnusable;
	}

	@Override
	public boolean start() {
        AbilityManager manager = ElementalMagicApi.abilityManager();
        firejet = manager.getAbility(user(), FireJet.class).orElse(null);
        if (firejet == null) {
            return false;
        }

        firejet.setSpeed(speed);
        firejet.setDuration(duration);

        World world = user().player().getWorld();
        Location loc = user().player().getLocation();
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 0.5f);
        world.spawnParticle(Particle.EXPLOSION, loc, 5, 0.5, 0.5, 0.5);

        user().addCooldown("JetBlast", cooldown);
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        FireJet fj = manager.getAbility(user(), FireJet.class).orElse(null);
        if (!firejet.equals(fj)) {
            return false;
        }
       
        if (meltIce) {
            meltIce();
        }
        return true;
	}

    private void meltIce() {
        Location loc = user().player().getLocation();
        for (Block b : BlockUtil.collectSphere(loc, meltRadius)) {
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
            TempBlock.builder(this, data).setDuration(meltDuration).buildAt(block);
            return;
        }

        TempBlock tb = TempBlock.get(block).orElse(null);
        if (tb != null) {
            ElementalMagicApi.revertibleManager().revert(tb);    
        } else {
            BlockData data = Material.WATER.createBlockData();
            TempBlock.builder(this, data).setDuration(meltDuration)
                .setUsable(true).buildAt(block);
        }
    }

	@Override
	public void onDestruction() {
	}

    protected static class ConfigValues {

        private static final String CONFIG_PATH = JetBlastController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 8700;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 1500;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.75;
        @Configure(path = CONFIG_PATH + "MeltIce", config = Config.ABILITIES)
        private boolean meltIce = true;
        @Configure(path = CONFIG_PATH + "MeltRadius", config = Config.ABILITIES)
        private double meltRadius = 2.0;
        @Configure(path = CONFIG_PATH + "MeltDuration", config = Config.ABILITIES)
        private long meltDuration = 10000; 
        @Configure(path = CONFIG_PATH + "MeltUnusable", config = Config.ABILITIES)
        private boolean meltUnusable = true;
    }
}
