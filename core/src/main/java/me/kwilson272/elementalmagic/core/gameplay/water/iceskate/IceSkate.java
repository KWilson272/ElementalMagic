package me.kwilson272.elementalmagic.core.gameplay.water.iceskate;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

public class IceSkate extends CoreAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private int power;
    private int duration;
    private PotionEffect speedEffect;

    public IceSkate(AbilityUser user, AbilityController controller) {
        super(user, controller);
        power = CONFIG.speedPower;
        duration = CONFIG.speedDuration;
    }

    @Override
    public boolean start() {
        speedEffect = new PotionEffect(PotionEffectType.SPEED, duration, power);
        return isOnIce();
    }

    public boolean isOnIce() {
        Location loc = user().player().getLocation();
        Block block = loc.getBlock().getRelative(BlockFace.DOWN);
        return (AbilityUtil.isIce(block));
    }

    @Override
    public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }
        
        Player player = user().player();
        World world = player.getWorld();
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        if (isOnIce() && player.isSprinting() 
                && effectHandler.addPotionEffect(player, this, speedEffect)) {
            // Spawn snow particles as if they were being kicked up by the player:
            double height = 0.2;
            double variance = 0.2;
            double speed = 0.15;
            for (int i = 0; i < 2; ++i) {
                ThreadLocalRandom rand = ThreadLocalRandom.current();
                double xOff = rand.nextDouble(-variance, variance);
                double zOff = rand.nextDouble(-variance, variance);
                Location disp = player.getLocation().add(xOff, height, zOff);
                world.spawnParticle(Particle.SNOWFLAKE, disp, 0, 0, speed, 0);
            }
        }

        return true;
    }

    @Override
    public void onDestruction() {

    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = IceSkateController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "SpeedPower", config = Config.ABILITIES)
        private int speedPower = 4;
        @Configure(path = CONFIG_PATH + "SpeedDuration", config = Config.ABILITIES)
        private int speedDuration = 60;
    }
}

