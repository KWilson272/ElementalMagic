package me.kwilson272.elementalmagic.core.gameplay.earth.earthkick;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.revertible.TempFallingBlock;
import me.kwilson272.elementalmagic.core.revertible.TempFallingBlock.TempFallingBlockBuilder;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class EarthKick extends EarthAbility {
   
    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double sourceAngle;
    private int blockCount;
    private double minVertAngle;
    private double maxVertAngle;
    private double horizAngle;
    private double speed;
    private double hitboxSize;
    private double damage;
    private boolean canMultiHit;

    private List<TempFallingBlock> fallingBlocks;
    private Set<Entity> noAffect;
    
	public EarthKick(AbilityUser user, AbilityController controller) {
		super(user, controller);
	    
        cooldown = CONFIG.cooldown;
        sourceAngle = CONFIG.sourceAngle;
        blockCount = CONFIG.blockCount;
        minVertAngle = CONFIG.minVertAngle;
        maxVertAngle = CONFIG.maxVertAngle;
        horizAngle = CONFIG.horizAngle;
        speed = CONFIG.speed;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        canMultiHit = CONFIG.canMultiHit;

        fallingBlocks = new ArrayList<>();
        noAffect = new HashSet<>();
        noAffect.add(user.player());
    }

	@Override
	public boolean start() {
        double pitch = user().player().getEyeLocation().getPitch();
        if (pitch < sourceAngle) {
            return false;
        }

	    Location sourceLoc = user().player().getLocation().add(0, -0.8, 0);
        Block source = sourceLoc.getBlock();
        if (!isUsableEarth(source)) {
            return false;
        }

        initBlocks(source);
        playEarthSound(source.getLocation());
        user().addCooldown(name(), cooldown);
        return true;
    }

    private void initBlocks(Block source) {
        BlockData data = source.getBlockData();
        TempFallingBlockBuilder builder = TempFallingBlock.builder(this, data)
            .setCollidable(false)
            .setDuration(30000);
        Location spawnLoc = user().player().getLocation().add(0, 1.1, 0);

        Random rand = ThreadLocalRandom.current();
        double baseYaw = user().player().getEyeLocation().getYaw();
        for (int i = 0; i < blockCount; ++i) {
            double pitch = -rand.nextDouble(minVertAngle, maxVertAngle);
            double yaw = baseYaw + rand.nextDouble(-horizAngle/2, horizAngle/2);
            Vector dir = Vectors.fromRotations(pitch, yaw);

            TempFallingBlock tfb = builder.buildAt(spawnLoc);
            tfb.fallingBlock().setVelocity(dir.multiply(speed));
            fallingBlocks.add(tfb);
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }

        Iterator<TempFallingBlock> iter = fallingBlocks.iterator();
        while (iter.hasNext()) {
            TempFallingBlock tfb = iter.next();
            FallingBlock fb = tfb.fallingBlock();
            if (fb.isDead()) {
                iter.remove();
                continue;
            }

            affectEntities(fb);
            playParticles(fb);
        }

        return !fallingBlocks.isEmpty();
	}

    private void affectEntities(FallingBlock fb) {
        Location loc = fb.getLocation().add(0.5, 0.5, 0.5);
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        for (Entity e : Entities.getNearbyEntities(loc, hitboxSize)) {
            if (noAffect.contains(e)) {
                continue;
            }

            if (effectHandler.damageEntity(e, this, damage) && !canMultiHit) {
                noAffect.add(e);
            }
        }
    }

    private void playParticles(FallingBlock fb) {
        World world = fb.getWorld();
        Location loc = fb.getLocation().add(0.5, 0.5, 0.5);
        BlockData data = fb.getBlockData();
        world.spawnParticle(Particle.BLOCK, loc, 2, 0.3, 0.3, 0.3, data);
    }

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "EarthKick";
	}

    protected static class ConfigValues {

        private static final String CONFIG_PATH = EarthKickController.CONFIG_PATH;
    
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 4800;
        @Configure(path = CONFIG_PATH + "SourceAngle", config = Config.ABILITIES)
        private double sourceAngle = 35.0;
        @Configure(path = CONFIG_PATH + "BlockCount", config = Config.ABILITIES)
        private int blockCount = 15;
        @Configure(path = CONFIG_PATH + "MinVertAngle", config = Config.ABILITIES)
        private double minVertAngle = 15;
        @Configure(path = CONFIG_PATH + "MaxVertAngle", config = Config.ABILITIES)
        private double maxVertAngle = 30;
        @Configure(path = CONFIG_PATH + "HorizontalAngle", config = Config.ABILITIES)
        private double horizAngle = 45.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 0.6;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES) 
        private double hitboxSize = 2.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "CanMultiHit", config = Config.ABILITIES)
        private boolean canMultiHit = true;

    }
}
