package me.kwilson272.elementalmagic.core.gameplay.fire;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.ability.CoreElement;

public abstract class FireAbility extends CoreAbility {

	public FireAbility(AbilityUser user, AbilityController controller) {
		super(user, controller);
	}
    
    public BlockData getFireData() {
        return canUseBlueFire(user()) ? 
            Material.FIRE.createBlockData() : Material.SOUL_FIRE.createBlockData();
    }

    public Particle getFireParticle() {
        return canUseBlueFire(user()) ? Particle.FLAME : Particle.SOUL_FIRE_FLAME;

    }

    public static boolean canUseBlueFire(AbilityUser user) {
        String perm = CoreElement.FIRE.permission() + ".bluefire";
        return user.player().hasPermission(perm);
    }
}
