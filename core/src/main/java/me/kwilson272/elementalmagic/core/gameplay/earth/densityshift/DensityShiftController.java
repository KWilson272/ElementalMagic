package me.kwilson272.elementalmagic.core.gameplay.earth.densityshift;

import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

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
import me.kwilson272.elementalmagic.core.util.Blocks;

public class DensityShiftController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Earth.DensityShift.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Through quick thinking, Earth users can shift the ground beneath them to sand to avoid fall damage.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Activates automatically on fall damage.";

    public DensityShiftController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(DensityShift.CONFIG);
    }

    @Activator(requireSelected = false)
    public Collection<Ability> onFall(AbilityUser user, FallDamageActivation activation) {
        if (!user.canUse(this, false, false)) {
            return List.of();
        }

        Block impact = getImpactBlock(user);
        if (impact != null) {
            activation.setDamage(0);
            // We need an ability instance for tempblocks
            return List.of(new DensityShift(user, this));
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
        return (user.canUseElement(CoreElement.EARTH) && Blocks.isEarth(block))
            || (user.canUseElement(CoreElement.SAND) && Blocks.isSand(block))
            || (user.canUseElement(CoreElement.METAL) && Blocks.isMetal(block));
    }

	@Override
	public String name() {
        return "DensityShift";
	}

	@Override
	public Element element() {
        return CoreElement.SAND;
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
