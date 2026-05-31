package me.kwilson272.elementalmagic.core.gameplay.fire.jetblaze;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.fire.firejet.FireJet;
import me.kwilson272.elementalmagic.core.util.Entities;

public class JetBlaze extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double speed;
    private double hitboxSize;
    private double damage;
    private long burnDuration;

    private int counter;
    private FireJet firejet;
    private Set<Entity> noAffect;

	public JetBlaze(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        speed = CONFIG.speed;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        burnDuration = CONFIG.burnDuration;
        
        counter = 0;
        noAffect = new HashSet<>();
        noAffect.add(user.player());
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
        user().addCooldown("JetBlaze", cooldown);
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
      
        playEffects();
        affectEntities();
        return true;
    }

    private void playEffects() {
        World world = user().player().getWorld();
        Location loc = user().player().getEyeLocation().subtract(0, 1, 0);
        Vector dir = loc.getDirection();

        double x = -dir.getX();
        double y = -dir.getY();
        double z = -dir.getZ();
        for (int i = 0; i < 7; ++i) {
            double offset = hitboxSize / 3;
            Location display  = loc.clone().add(
                ThreadLocalRandom.current().nextDouble(-offset, offset),
                ThreadLocalRandom.current().nextDouble(-offset, offset),
                ThreadLocalRandom.current().nextDouble(-offset, offset)
            );            
            world.spawnParticle(Particle.LARGE_SMOKE, display, 0, x, y, z, 0.5);
        }

        ++counter;
        if (counter % 2 == 0) {
            world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1, 0);
        }
    }

    private void affectEntities() {
        Location loc = user().player().getLocation();
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        for (Entity e : Entities.getNearbyEntities(loc, hitboxSize)) {
            if (!noAffect.contains(e)) {
                effectHandler.damageEntity(e, this, damage);
                if (e.getFireTicks() * 50 < burnDuration) {
                    effectHandler.setFireDuration(e, this, burnDuration);
                }
            }
        }
    }

	@Override
	public void onDestruction() {
	}

    @Override
    public String name() {
        return "JetBlaze";
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = JetBlazeController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 8700;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 2850;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.35;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.5;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "BurnDuration", config = Config.ABILITIES)
        private long burnDuration = 1000;
    }
}
