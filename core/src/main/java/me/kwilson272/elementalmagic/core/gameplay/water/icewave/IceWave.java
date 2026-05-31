package me.kwilson272.elementalmagic.core.gameplay.water.icewave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.collision.Sphere;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.water.WaterAbility;
import me.kwilson272.elementalmagic.core.gameplay.water.waterspout.WaterWave;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;

public class IceWave extends WaterAbility {

	protected static final ConfigValues CONFIG = new ConfigValues();

    private WaterWave wave;
    private long cooldown;
    private double damage;
    private double affectRadius;
    private double iceRadius;
    private double iceRevertDistance;
    private long iceRevertTime;
    private long globalFreezeCD;
    
    private boolean isFreezeMaintenance;
    private Set<Entity> noAffect;
    private List<TempBlock> iceBlocks;
    private Map<Location, Double> iceBalls;

    public IceWave(AbilityUser user, AbilityController controller, WaterWave wave) {
        super(user, controller);

        this.wave = wave;
        cooldown = CONFIG.cooldown;
        damage = CONFIG.damage;
        affectRadius = CONFIG.affectRadius;
        iceRadius = CONFIG.iceRadius;
        iceRevertDistance = CONFIG.iceRevertDistance;
        iceRevertTime = CONFIG.iceRevertTime;
        globalFreezeCD = CONFIG.globalFreezeCD;
        
        isFreezeMaintenance = false;
        noAffect = new HashSet<>();
        noAffect.add(user().player());
        iceBlocks = new ArrayList<>();
        iceBalls = new HashMap<>();
    }

	@Override
	public boolean start() {
        user().addCooldown("IceWave", cooldown);
        wave.setIce(true);
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        WaterWave wave = manager.getAbility(user(), WaterWave.class).orElse(null);
        if (!this.wave.equals(wave)) {
            isFreezeMaintenance = true;
            wave = null;
        }
        
        cleanIceBlocks();
        growIceBalls();

        if (!isFreezeMaintenance) {
            affectEntities();
            return true;
        }

        return !iceBlocks.isEmpty();
	}

    private void cleanIceBlocks() {
        World world = user().player().getWorld();
        Vector dir = user().player().getEyeLocation().getDirection();
        Location loc = user().player().getLocation().subtract(dir);
        double maxDist = iceRevertDistance * iceRevertDistance;

        Iterator<TempBlock> iter = iceBlocks.iterator();
        while (iter.hasNext()) {
            TempBlock tb = iter.next();
            if (!tb.block().getWorld().equals(world)) {
                ElementalMagicApi.revertibleManager().revert(tb);
                iter.remove();
            }

            Location bLoc = tb.block().getLocation().add(0.5, 0.5, 0.5);
            if (bLoc.distanceSquared(loc) > maxDist) {
                ElementalMagicApi.revertibleManager().revert(tb);
                iter.remove();
            }
        }
    }

    private void growIceBalls() {
        Map<Location, Double> newIceBalls = new HashMap<>();
        BlockData data = Material.ICE.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data)
            .setDuration(iceRevertTime);

        for (Location loc : iceBalls.keySet()) {
            double size = Math.min(iceRadius, iceBalls.get(loc) + 1);
            if (size < iceRadius) {
                newIceBalls.put(loc, size);
            }

            boolean frozeAny = false;
            for (Block b : Blocks.collectSphere(loc, size)) {
                if (!Blocks.isSolid(b) || TempBlock.isTempBlock(b)) {
                    frozeAny = true;
                    builder.buildAt(b).ifPresent(iceBlocks::add);
                }
            }

            if (frozeAny) {
                playIceSound(loc);    
            }
        }

        iceBalls = newIceBalls;
    }

    private void affectEntities() {
        World world = user().player().getWorld();
        Location loc = user().player().getLocation();
        BoundingVolume bv = Sphere.at(loc, affectRadius); 
        for (Entity e : Entities.getNearbyEntities(world, bv)) {
            if (!noAffect.contains(e) && e instanceof LivingEntity) {
                ElementalMagicApi.effectHandler().damageEntity(e, this, damage);
                noAffect.add(e);

                if (!user().isOnCooldown("GlobalFreeze")) {
                    user().addCooldown("GlobalFreeze", globalFreezeCD);
                    iceBalls.put(e.getLocation(), 0.0);
                }
            }
        }
    }

	@Override
	public void onDestruction() {
        if (wave != null) {
            wave.setIce(false);
        }
        
        for (TempBlock tb : iceBlocks) {
            ElementalMagicApi.revertibleManager().revert(tb);
        }
    }

    @Override
    public String name() {
        return "IceWave";
    }
    
    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = IceWaveController.CONFIG_PATH;
    
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 12000;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "AffectRadius", config = Config.ABILITIES) 
        private double affectRadius = 2.5;
        @Configure(path = CONFIG_PATH + "IceRadius", config = Config.ABILITIES)
        private double iceRadius = 3;
        @Configure(path = CONFIG_PATH + "IceRevertDistance", config = Config.ABILITIES)
        private double iceRevertDistance = 40.0;
        @Configure(path = CONFIG_PATH + "IceRevertTime", config = Config.ABILITIES)
        private long iceRevertTime = -1;
        @Configure(path = CONFIG_PATH + "GlobalFreezeCooldown", config = Config.ABILITIES)
        private long globalFreezeCD = 8000;
    }
}
