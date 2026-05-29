package me.kwilson272.elementalmagic.core.gameplay.air.airagility;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;

public class AirAgility extends CoreAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private int speedPower;
    private int speedDuration;
    private int jumpPower;
    private int jumpDuration;

    public AirAgility(AbilityUser user, AbilityController controller) {
		super(user, controller);

        speedPower = CONFIG.speedPower;
        speedDuration = CONFIG.speedDuration;
        jumpPower = CONFIG.jumpPower;
        jumpDuration = CONFIG.jumpDuration;
	}

	@Override
	public boolean start() {
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)
                || !user().player().isSprinting()) {
            return false;
        }

        PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, 
                speedDuration, speedPower);
        PotionEffect jump = new PotionEffect(PotionEffectType.JUMP_BOOST, 
                jumpDuration, jumpPower);

        Player player = user().player(); 
        ElementalMagicApi.effectHandler().addPotionEffect(player, this, speed);
        ElementalMagicApi.effectHandler().addPotionEffect(player, this, jump);

        return true;
	}

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "AirAgility";
	}

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = AirAgilityController.CONFIG_PATH;
     
        @Configure(path = CONFIG_PATH + "SpeedPower", config = Config.ABILITIES)
        private int speedPower = 4; 
        @Configure(path = CONFIG_PATH + "SpeedDuration", config = Config.ABILITIES)
        private int speedDuration = 60;
        @Configure(path = CONFIG_PATH + "JumpPower", config = Config.ABILITIES)
        private int jumpPower = 3;
        @Configure(path = CONFIG_PATH + "JumpDuration", config = Config.ABILITIES)
        private int jumpDuration = 60; 

    }
}
