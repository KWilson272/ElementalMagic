package me.kwilson272.elementalmagic.core.gameplay.air.airbreath;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.air.AirAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;

public class AirBreath extends AirAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double range;
    private double hitboxSize;
    private double pushSelf;
    private double pushOthers;
    private double extinguishRadius;
    private long extinguishTime;
    private double solidifyRadius;
    private long solidifyTime;
    private boolean extinguishPlayers;

    private boolean isInfinite;
    private long endTime;

	public AirBreath(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        range = CONFIG.range;
        hitboxSize = CONFIG.hitboxSize;
        pushSelf = CONFIG.pushSelf;
        pushOthers = CONFIG.pushOthers;
        extinguishRadius = CONFIG.extinguishRadius;
        extinguishTime = CONFIG.extinguishTime;
        solidifyRadius = CONFIG.solidifyRadius;
        solidifyTime = CONFIG.solidifyTime;
        extinguishPlayers = CONFIG.extinguishPlayers;
	}

	@Override
	public boolean start() {
        isInfinite = duration < 0;
        endTime = System.currentTimeMillis() + duration;
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), true, false)
                || !user().player().isSneaking()
                || (!isInfinite && System.currentTimeMillis() > endTime)) {
            return false;
        }

        playAirSound(user().player().getEyeLocation());     
        manageBreath();
        return true;
	}

    private void manageBreath() {
        Location loc = user().player().getEyeLocation();
        double spacing = 0.5;
        Vector dir = loc.getDirection().multiply(spacing);

        boolean hitSolidBlock = false;
        for (double i = 0; i <= range; i += spacing) {
            Location newLoc = loc.clone().add(dir);
            Block block = newLoc.getBlock();

            if (Blocks.isSolid(block)) {
                hitSolidBlock = true;
                break;
            }

            playAirParticles(loc, 1, 0.5, 0.5, 0.5);
            affectEntities(loc, dir);
            extinguishFire(loc);
            solidifyLava(loc);

            loc.add(dir);
        }

        if (hitSolidBlock) {
            Player player = user().player();
            Vector knock = dir.normalize().multiply(-pushSelf);
            ElementalMagicApi.effectHandler().setVelocity(player, this, knock);
        }
    }

    private void affectEntities(Location loc, Vector direction) {
        Vector knock = direction.clone().normalize().multiply(pushOthers);
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();

        for (Entity e : Entities.getNearbyEntities(loc, hitboxSize)) {
            if (!e.equals(user().player())) {
                effectHandler.setVelocity(e, this, knock);
            }

            if (extinguishPlayers && e.getFireTicks() > 0) {
                effectHandler.setFireDuration(e, this, 0);
            }
        }
    }

    private void extinguishFire(Location loc) {
        BlockData data = Material.AIR.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data)
            .setDuration(extinguishTime);

        for (Block b : Blocks.collectSphere(loc, extinguishRadius)) {
            if (b.getType() == Material.FIRE || b.getType() == Material.SOUL_FIRE) {
                builder.buildAt(b);
            }
        }
    }

    private void solidifyLava(Location loc) {
        BlockData data = Material.OBSIDIAN.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data)
            .setDuration(solidifyTime);

        for (Block b : Blocks.collectSphere(loc, solidifyRadius)) {
            if (b.getType() == Material.LAVA) {
                builder.buildAt(b);
            }
        }
    }

	@Override
	public void onDestruction() {
        user().addCooldown(name(), cooldown);
	}

	@Override
	public String name() {
        return "AirBreath";
	}
    
    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = AirBreathController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 6000;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 1000;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 12;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.0;
        @Configure(path = CONFIG_PATH + "PushSelf", config = Config.ABILITIES)
        private double pushSelf = 1.5;
        @Configure(path = CONFIG_PATH + "PushOthers", config = Config.ABILITIES)
        private double pushOthers = 0.5;
        @Configure(path = CONFIG_PATH + "ExtinguishRadius", config = Config.ABILITIES)
        private double extinguishRadius = 2.0;
        @Configure(path = CONFIG_PATH + "ExtinguishTime", config = Config.ABILITIES)
        private long extinguishTime = 5000;
        @Configure(path = CONFIG_PATH + "SolidifyRadius", config = Config.ABILITIES)
        private double solidifyRadius = 2.0;
        @Configure(path = CONFIG_PATH + "SolidifyTime", config = Config.ABILITIES)
        private long solidifyTime = 10000;
        @Configure(path = CONFIG_PATH + "ExtinguishPlayers", config = Config.ABILITIES)
        private boolean extinguishPlayers = true;
 
    }
}
