package me.kwilson272.elementalmagic.core.gameplay.fire.firejet;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;

public class FireJet extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double speed;
    private long duration;
    private boolean canUseInLiquid;
    private boolean extinguishUser;

    private boolean isInfinite;
    private long startTime;

	public FireJet(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        speed = CONFIG.speed;
        duration = CONFIG.duration;
        canUseInLiquid = CONFIG.canUseInLiquid;
        extinguishUser = CONFIG.extinguishUser;
	}

	@Override
	public boolean start() {
        Block footBlock = user().player().getLocation().getBlock();
        if (BlockUtil.isLiquid(footBlock)) {
            return false;
        }
        
        isInfinite = duration < 0;
        startTime = System.currentTimeMillis();
        user().addCooldown("FireJet", cooldown);
        return true;
	}

	@Override
    public boolean progress() {
        if (!user().canUse(controller(), false, false)
                || (!isInfinite && System.currentTimeMillis() - startTime > duration)) {
            return false; 
        }

        Block footBlock = user().player().getLocation().getBlock();
        if (!canUseInLiquid && BlockUtil.isLiquid(footBlock)) {
            return false;
        }

        if (extinguishUser) {
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();
            effectHandler.setFireDuration(user().player(), this, 0);
        }
        
        playEffects();
        applyVelocity();
        return true;
	}

    private void playEffects() {
        World world = user().player().getWorld();
        Location loc = user().player().getLocation();
        world.spawnParticle(getFireParticle(), loc, 10, 0.6, 0.6, 0.6, 0.01);
        world.spawnParticle(Particle.SMOKE, loc, 3, 0.6, 0.6, 0.6, 0.01);
        playFireSound(loc);
    }

    private void applyVelocity() {
        // Math taken from ProjectKorra 1.8.0-BETA-9
        long timeElapsed = duration - (System.currentTimeMillis() - startTime);
        double timeFactor = 1 - (timeElapsed / (2.0 * duration));
        Vector dir = user().player().getEyeLocation().getDirection();
        dir.multiply(timeFactor * speed);

        ElementalMagicApi.effectHandler().setVelocity(user().player(), this, dir);
        user().player().setFallDistance(0);
    }

	@Override
	public void onDestruction() {
	}

    @Override
    public String name() {
        return "FireJet";
    }

    public void setDuration(long duration) {
        this.duration = duration;
        this.isInfinite = duration < 0; 
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    protected static class ConfigValues {
   
        private static final String CONFIG_PATH = FireJetController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 7200;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 0.9;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 2250;
        @Configure(path = CONFIG_PATH + "CanUseInLiquid", config = Config.ABILITIES)
        private boolean canUseInLiquid = true;
        @Configure(path = CONFIG_PATH + "ExtinguishUser", config = Config.ABILITIES)
        private boolean extinguishUser = true;
    }
}
