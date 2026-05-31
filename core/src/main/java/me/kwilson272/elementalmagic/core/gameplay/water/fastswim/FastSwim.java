package me.kwilson272.elementalmagic.core.gameplay.water.fastswim;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.water.waterspout.WaterSpout;
import me.kwilson272.elementalmagic.core.util.Blocks;

public class FastSwim extends CoreAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double swimSpeed;

    private boolean isInfinite;
    private long endTime;
    
    public FastSwim(AbilityUser user, AbilityController controller) {
        super(user, controller);
        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        swimSpeed = CONFIG.swimSpeed;
    }

    @Override
    public boolean start() {
        endTime = System.currentTimeMillis() + duration;
        isInfinite = duration < 0;
        return canSwim();
    }

    private boolean canSwim() {
        return user().getSelectedBind()
                .map(ac -> !ac.canActivateBy(Action.SNEAK_DOWN)).orElse(true);
    }

    @Override
    public boolean progress() {
        if (!canSwim() 
                || !user().player().isSneaking()
                || !user().canUse(controller(), false, true)
                || (!isInfinite && System.currentTimeMillis() > endTime)) {
            return false;
        }

        // Don't remove but don't push  - allows the user to fastswim off of
        // spout without having to re-sneak; Comfort!
        if (ElementalMagicApi.abilityManager().hasAbility(user(), WaterSpout.class)) {
            return true;
        }


        Player player = user().player();
        Block foot = player.getLocation().getBlock();
        if (Blocks.isWater(foot)) {
            Vector dir = player.getEyeLocation().getDirection();
            dir.multiply(swimSpeed);
            ElementalMagicApi.effectHandler().setVelocity(player, this, dir);
        }
        return true;
    }

    @Override
    public void onDestruction() {
        user().addCooldown(controller().name(), cooldown);
    }
    
    @Override
    public String name() {
        return "FastSwim";
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = FastSwimController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = -1;
        @Configure(path = CONFIG_PATH + "SwimSpeed", config = Config.ABILITIES)
        private double swimSpeed = 0.85;
    }
}
