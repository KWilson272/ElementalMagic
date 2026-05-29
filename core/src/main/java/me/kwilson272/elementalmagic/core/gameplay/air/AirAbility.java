package me.kwilson272.elementalmagic.core.gameplay.air;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.Spell;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;

public abstract class AirAbility extends CoreAbility {
    
    private static final Spell PARTICLE_DATA = new Spell(Color.fromRGB(255, 255, 255), 0.8f);

	public AirAbility(AbilityUser user, AbilityController controller) {
		super(user, controller);
	}

    public void playAirParticles(Location loc) {
        playAirParticles(loc, 1, 0.1, 0.1, 0.1); 
    }

    public void playAirParticles(Location loc, int count, 
                                 double offX, double offY, double offZ) { 
        World world = loc.getWorld();
        world.spawnParticle(Particle.EFFECT, loc, count, offX, offY, offZ, PARTICLE_DATA); 
    }

    public void playAirSound(Location loc) {
        World world = loc.getWorld();
        world.playSound(loc, Sound.ENTITY_CREEPER_HURT, 1, 2);
    }
}
