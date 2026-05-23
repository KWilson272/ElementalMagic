package me.kwilson272.elementalmagic.core.gameplay.water.hydrosink;

import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.activation.Activator;
import me.kwilson272.elementalmagic.api.activation.activations.FallDamageActivation;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbilityController;
import me.kwilson272.elementalmagic.core.ability.CoreElement;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class HydroSinkController extends CoreAbilityController {

    private static final String CONFIG_PATH = "Abilities.Water.HydroSink.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Ice users are able to negate their fall on ice or snow!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Activates automatically on fall damage.";

    public HydroSinkController() {
        ElementalMagicApi.configManager().configure(this);
    }

    @Activator(requireSelected = false)
    public Collection<Ability> onFall(AbilityUser user, FallDamageActivation activation) {
        if (!user.canUse(this, false, false)) {
            return List.of();
        }

        Block impact = getImpactBlock(user);
        if (impact != null) {
            playParticles(user, impact);
            activation.setDamage(0);
        }

        return List.of();
    }

    private Block getImpactBlock(AbilityUser user) {
        Player player = user.player();
        Location impactLoc = player.getLocation().add(0, -0.2, 0);
        BoundingBox userBox = player.getBoundingBox();
        userBox.expand(0.5);

        // Get ANY possible block that could stop the fall damage, it isn't the
        // most accurate, however it feels better gameplay wise
        Block footBlock = impactLoc.getBlock();
        for (int x = -1; x <= 1; ++x) {
            for (int y = 0; y <= 1; ++y) {
                for (int z = -1; z <= 1; ++z) {
                    Block block = footBlock.getRelative(x, y, z);
                    BoundingBox testBox = block.getBoundingBox();
                    if (!testBox.overlaps(userBox)) {
                        continue;
                    }

                    if (canPreventFall(user, block)) {
                        return block;
                    }
                }
            }
        }
        
        return null;
    }

    private boolean canPreventFall(AbilityUser user, Block block) {
        return (user.canUseElement(CoreElement.PLANT) && AbilityUtil.isPlant(block) 
                && block.getType().isSolid())
            || (user.canUseElement(CoreElement.ICE) && (AbilityUtil.isIce(block) 
                || AbilityUtil.isSnow(block)));
    }

    private void playParticles(AbilityUser user, Block block) {
        Player player = user.player(); 
        Location loc = player.getLocation();
        World world = player.getWorld();

        double radius = 0.7; // Looks good
        Particle particle = AbilityUtil.isIce(block) || AbilityUtil.isSnow(block) ?
                                        Particle.SNOWFLAKE : Particle.FALLING_WATER;

        for (Vector v : VectorUtil.getRing(15)) {
            Vector offset = v.clone().multiply(radius);
            Location display = loc.clone().add(offset);
            double x = v.getX();
            double y = 0.2;
            double z = v.getZ();
            double speed = 0.2;
            world.spawnParticle(particle, display, 0, x, y, z, speed);
        }
    }

	@Override
	public String name() {
        return "HydroSink";
	}

	@Override
	public Element element() {
        return CoreElement.WATER;
	}

	@Override
	public String description() {
        return description;
	}

	@Override
	public String instructions() {
        return instructions;
	}

	@Override
	public boolean isBindable() {
        return false;
	}

	@Override
	public boolean isPassive() {
        return true;
	}

	@Override
	public boolean canActivateBy(Action action) {
        return false;
	}
}
