package me.kwilson272.elementalmagic.core.gameplay.water.frostbreath;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.water.waterspout.WaterSpout;

public class FrostBreath extends CoreAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();
    
    private long cooldown;
    private long duration;
    private double range;
    private double spread;
    private long frostDuration;
    private boolean freezeAbilityWater;
    private boolean doIceTrap;
    private long iceTrapDuration;
    private long globalFreezeCooldown;
    
    private boolean isInfinite;
    private long endTime;
    private boolean frozeAny;

	public FrostBreath(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        range = CONFIG.range;
        spread = CONFIG.spread;
        frostDuration = CONFIG.frostDuration;
        freezeAbilityWater = CONFIG.freezeAbilityWater;
        doIceTrap = CONFIG.doIceTrap;
        iceTrapDuration = CONFIG.iceTrapDuration;
        globalFreezeCooldown = CONFIG.globalFreezeCooldown;

        frozeAny = false;
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

        createBreath();
        return true;
	}

    private void createBreath() {
        double radius = spread;
        double spacing = 1.0;
        Location loc = user().player().getEyeLocation();
        Vector dir = loc.getDirection().multiply(spacing);

        for (double i = 0; i <= range; i += spacing) {
            if (BlockUtil.isSolid(loc.getBlock())) {
                break;
            }

            displayParticles(loc, radius);
            freezeBlocks(loc, radius);
            affectEntities(loc, radius);
            radius += spread;

            loc.add(dir);
        }
    }

    private void displayParticles(Location loc, double radius) {
        World world = user().player().getWorld();
        
        double offset = radius * 0.75;
        world.spawnParticle(Particle.SNOWFLAKE, loc, 1, offset, offset, offset, 0.01);
        world.spawnParticle(Particle.ITEM_SNOWBALL, loc, 1, offset, offset, offset);

        if ( ThreadLocalRandom.current().nextBoolean()) {
            spawnColoredSpell(loc, radius, Color.fromRGB(220, 220, 240));
        } else {
            spawnColoredSpell(loc, radius, Color.fromRGB(180, 230, 230));
        }
    }

    private void spawnColoredSpell(Location loc, double radius, Color color) {
        double xOff = ThreadLocalRandom.current().nextDouble(-radius, radius);
        double yOff = ThreadLocalRandom.current().nextDouble(-radius, radius);
        double zOff = ThreadLocalRandom.current().nextDouble(-radius, radius);
        Location randLoc = loc.clone().add(xOff, yOff, zOff);
        loc.getWorld().spawnParticle(Particle.ENTITY_EFFECT, randLoc, 0, color);
    }

    private void freezeBlocks(Location loc, double radius) {
        BlockData iceData = Material.ICE.createBlockData();
        BlockData snowData = Material.SNOW.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, iceData)
            .setUsable(true)
            .setDuration(frostDuration);

        for (Block b : BlockUtil.collectSphere(loc, radius)) {
            Block above = b.getRelative(BlockFace.UP);
            if (BlockUtil.isSolid(above) && !AbilityUtil.isWater(above)) {
                continue;
            }

            if (AbilityUtil.isWater(b) && isWaterFreezable(b)) {
                builder.setData(iceData).buildAt(b);
            } else if (!BlockUtil.isSolid(b) 
                    && BlockUtil.isSolid(b.getRelative(BlockFace.DOWN))) {
                builder.setData(snowData).buildAt(b);        
            }
        }
    }
    
    private boolean isWaterFreezable(Block block) {
        TempBlock tb = TempBlock.get(block).orElse(null);
        if (tb == null || tb.isUsable()) {
            return true;
        }

        Ability ability = tb.ability();
        // Blacklist WaterSpout for comfort while playing
        return freezeAbilityWater && !(ability instanceof WaterSpout);
    }

    private void affectEntities(Location loc, double radius) {
        if (!doIceTrap) {
            return;
        }

        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        for (Entity entity : EntityUtil.getNearbyEntities(loc, radius)) {
            if (entity.equals(user().player()) 
                    || !(entity instanceof LivingEntity le) 
                    || !effectHandler.canAffect(le)) {
                continue;
            }
            createIceTrap(entity);
            frozeAny = true;
        }
    }

    private void createIceTrap(Entity entity) {
        BlockData data = Material.ICE.createBlockData();
        TempBlockBuilder trapBuilder = TempBlock.builder(this, data)
            .setDuration(iceTrapDuration);
    
        // TODO: Figure out ice trap shape later
        for (Block block : BlockUtil.collectSphere(entity.getLocation(), 2)) {
            if (!BlockUtil.isSolid(block)) {
                trapBuilder.buildAt(block);
            }
        }
    }

	@Override
	public void onDestruction() {
        user().addCooldown("FrostBreath", cooldown);
        if (frozeAny) {
            user().addCooldown("GlobalFreeze", globalFreezeCooldown);
        }
	}

    @Override
    public String name() {
        return "FrostBreath";
    }

    protected static class ConfigValues {
    
        private static final String CONFIG_PATH = FrostBreathController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 5600;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)       
        private long duration = 4500;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 15.0;
        @Configure(path = CONFIG_PATH + "Spread", config = Config.ABILITIES)       
        private double spread = 0.2;
        @Configure(path = CONFIG_PATH + "FrostDuration", config = Config.ABILITIES)       
        private long frostDuration = 7500;
        @Configure(path = CONFIG_PATH + "FreezeAbilityWater", config = Config.ABILITIES)
        private boolean freezeAbilityWater = true;
        @Configure(path = CONFIG_PATH + "DoIceTrap", config = Config.ABILITIES)       
        private boolean doIceTrap = true;
        @Configure(path = CONFIG_PATH + "IceTrapDuration", config = Config.ABILITIES)       
        private long iceTrapDuration = 3000;
        @Configure(path = CONFIG_PATH + "GlobalFreezeCooldown", config = Config.ABILITIES)
        private long globalFreezeCooldown = 7000;
    }
}
