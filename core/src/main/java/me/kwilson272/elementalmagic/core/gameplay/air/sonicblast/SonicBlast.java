package me.kwilson272.elementalmagic.core.gameplay.air.sonicblast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.air.AirAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class SonicBlast extends AirAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long chargeDuration;
    private double speed;
    private double range;
    private double hitboxSize;
    private double damage;
    private double nauseaChance;
    private int nauseaPower;
    private int nauseaDuration;
    private double blindChance;
    private int blindPower;
    private int blindDuration;

    private boolean isCharging;
    private long chargedTime;
    private double animAngle;

    private double rangeCounter;
    private Location location;
    private Vector direction;

    private List<Vector> ringVecs;

    public SonicBlast(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        chargeDuration = CONFIG.chargeDuration;
        speed = CONFIG.speed;
        range = CONFIG.range;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        nauseaChance = CONFIG.nauseaChance;
        nauseaPower = CONFIG.nauseaPower;
        nauseaDuration = CONFIG.nauseaDuration;
        blindChance = CONFIG.blindChance;
        blindPower = CONFIG.blindPower;
        blindDuration = CONFIG.blindDuration;

        isCharging = true;
        rangeCounter = 0;
        ringVecs = new ArrayList<>();
	}
	
    @Override
	public boolean start() {
        chargedTime = System.currentTimeMillis() + chargeDuration;
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), isCharging, false)) {
            return false;
        }
        
        if (!isCharging) {

            return advanceLocation();
        }

        if (System.currentTimeMillis() > chargedTime) {
            playChargeRing();
            if (!user().player().isSneaking()) {
                initFired();
                isCharging = false;
                user().addCooldown(name(), cooldown);
                return true;
            }
        }

        return user().player().isSneaking();
	}

    private boolean advanceLocation() {
        World world = location.getWorld();
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1, 0);

        double spacing = 0.2;
        double remainder = speed;
        while (remainder > 0) {
            double travel = Math.min(spacing, remainder);
            remainder -= spacing;
            
            Vector move = direction.clone().multiply(travel);
            Location prev = location.clone();
            location.add(move);

            Block oldBlock = prev.getBlock();
            Block newBlock = location.getBlock();
            if (BlockUtil.isSolid(newBlock) 
                    || BlockUtil.collidesDiagonally(prev, location, BlockUtil::isSolid)) {
                return false;            
            }

            displayRing();
            if (!oldBlock.equals(newBlock)) {
                affectEntities(); 
            }

            rangeCounter += travel;
            if (rangeCounter > range) {
                return false;
            }
        }

        return true;
    }

    private void displayRing() {
        for (Vector vec : ringVecs) {
            Location display = location.clone().add(vec);
            playAirParticles(display);
        }
    }

    private void affectEntities() {
        PotionEffect blind = 
            new PotionEffect(PotionEffectType.BLINDNESS, blindPower, blindDuration);
        PotionEffect nausea = 
            new PotionEffect(PotionEffectType.NAUSEA, nauseaPower, nauseaDuration);
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();

        for (Entity e : EntityUtil.getNearbyEntities(location, hitboxSize)) {
            if (e.equals(user().player()) || !(e instanceof LivingEntity le)) {
                continue;
            }
        
            effectHandler.damageEntity(e, this, damage);
            if (ThreadLocalRandom.current().nextDouble(100) < blindChance) {
                effectHandler.addPotionEffect(le, this, blind);
            }
            if (ThreadLocalRandom.current().nextDouble(100) < nauseaChance) {
                effectHandler.addPotionEffect(le, this, nausea);
            }
        }
    }

    private void playChargeRing() {
        Location center = user().player().getLocation().add(0, 0.7, 0);

        double radius = 1.5;
        double step = Math.toRadians(10);
        for (int i = 0; i < 2; ++i) {
            animAngle -= step;
            double x = Math.cos(animAngle) * radius;
            double z = Math.sin(animAngle) * radius;
            Vector v = new Vector(x, 0, z);
            playAirParticles(center.clone().add(v), 1, 0, 0, 0);
        }
    }

    private void initFired() {
        location = user().player().getEyeLocation();
        direction = location.getDirection();
        Vector ortho = VectorUtil.getOrthogonal(direction);
        for (int i = 0; i < 360; i += 30) {
            double angle = Math.toRadians(i);
            ringVecs.add(VectorUtil.rotateAroundVector(direction, ortho, angle));
        }   
    }
    
	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "SonicBlast";
	}

    protected static class ConfigValues {

        private static final String CONFIG_PATH = SonicBlastController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 5500;
        @Configure(path = CONFIG_PATH + "ChargeDuration", config = Config.ABILITIES)
        private long chargeDuration = 1700;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.0;
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private double range = 50;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.5;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "NauseaChance", config = Config.ABILITIES)
        private double nauseaChance = 10;
        @Configure(path = CONFIG_PATH + "NauseaPower", config = Config.ABILITIES)
        private int nauseaPower = 1;
        @Configure(path = CONFIG_PATH + "NauseaDuration", config = Config.ABILITIES)
        private int nauseaDuration = 90;
        @Configure(path = CONFIG_PATH + "BlindChance", config = Config.ABILITIES)
        private double blindChance = 30;
        @Configure(path = CONFIG_PATH + "BlindPower", config = Config.ABILITIES)
        private int blindPower = 1;
        @Configure(path = CONFIG_PATH + "BlindDuration", config = Config.ABILITIES)
        private int blindDuration = 90;   
    }
}

