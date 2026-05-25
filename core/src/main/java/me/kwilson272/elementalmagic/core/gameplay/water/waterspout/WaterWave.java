package me.kwilson272.elementalmagic.core.gameplay.water.waterspout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.components.TravelingSource;
import me.kwilson272.elementalmagic.core.gameplay.components.TravelingSource.TravelState;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterUtil;

public class WaterWave extends CoreAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private enum State {
        SOURCED,
        SOURCE_TRAVELING,
        CHARGING,
        SHRINKING,
        FLYING
    }

    private long cooldown;
    private double selectRange;
    private long chargeTime;
    private double chargeRadius;
    private long flightDuration;
    private double flightSpeed;
    private double waveRadius;
    private long revertTime;
    private boolean isWaterCollidable;

    private State state;

    // SOURCED
    private Block source;

    // SOURCE_TRAVELING
    private TravelingSource travelingSource;

    // CHARGING
    private long chargedTime;
    private double animAngle;
    private Map<Block, TempBlock> ringBlocks;

    // FLYING
    private long endTime;
    private boolean isIce;
    // Avoid getting caught in ice that we spawn 
    private Location lastLoc;

    public WaterWave(AbilityUser user, AbilityController controller) {
        super(user, controller);
    
        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        chargeTime = CONFIG.chargeTime;
        chargeRadius = CONFIG.chargeRadius;
        flightDuration = CONFIG.flightDuration;
        flightSpeed = CONFIG.flightSpeed;
        waveRadius = CONFIG.waveRadius;
        revertTime = CONFIG.revertTime;
        isWaterCollidable = CONFIG.isWaterCollidable;
    
        state = State.SOURCED;
        ringBlocks = new HashMap<>();
    }

	@Override
	public boolean start() {
        source = WaterUtil.getSourceBlock(user(), selectRange);
        return source != null;
	}
    

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        } else if (state != State.SHRINKING && state != State.FLYING 
                && !user().getSelectedBindName().equals("WaterSpout")) {
            return false;
        }
        
        return switch (state) {
            case SOURCED -> progressSourced();
            case SOURCE_TRAVELING -> progressSourceTraveling();
            case CHARGING -> progressCharging();
            case SHRINKING -> progressShrinking();
            case FLYING -> progressFlying();
        };
	}

    private boolean progressSourced() {
        if (!isSourceViable()) {
            return false;
        }
        
        if (user().player().isSneaking()) {
            initSourceTraveling();
        }

        WaterUtil.playSourceSelectedEffect(source);
        return true;
    }

    private boolean isSourceViable() {
        Location eyeLoc = user().player().getEyeLocation();
        Location sourceLoc = source.getLocation().add(0.5, 0.5, 0.5);
        double maxDist = Math.pow(selectRange + 1, 2);

        return eyeLoc.getWorld().equals(sourceLoc.getWorld())
            && sourceLoc.distanceSquared(eyeLoc) <= maxDist
            && WaterUtil.canUse(source, user());
    }

    private void initSourceTraveling() {
        Location loc = source.getLocation().add(0.5, 0.5, 0.5);
        BlockData data = Material.WATER.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data)
            .setCollidable(isWaterCollidable);

        travelingSource = new TravelingSource(loc, 1, true, builder);
        state = State.SOURCE_TRAVELING;
    }

    private boolean progressSourceTraveling() {
        if (!user().player().isSneaking()) {
            return false;
        }

        Location eyes = user().player().getEyeLocation();
        var tState = travelingSource.moveTowards(eyes, chargeRadius);
        if (tState == TravelState.ARRIVED) {
            initCharging(travelingSource.getLocation());
            travelingSource.revertBlocks();
            travelingSource = null;
        }
            
        return tState != TravelState.BLOCKED;
    }

    private void initCharging(Location loc) {
        Location eyes = user().player().getEyeLocation();
        Vector toSource = VectorUtil.getDirection(eyes, loc);
        animAngle = Math.atan2(-toSource.getX(), toSource.getY());
        chargedTime = System.currentTimeMillis() + chargeTime;
        state = State.CHARGING;
    }

    private boolean progressCharging() {
        manageRing(260);
        animAngle += Math.toRadians(45);
        
        boolean isCharged = System.currentTimeMillis() > chargedTime;
        if (user().player().isSneaking()) {
            return true;
        } else if (isCharged) {
            user().addCooldown("WaterWave", cooldown);
            state = State.SHRINKING;
            return true;
        }
        
        return false;
    }

    private void manageRing(double angle) {
        if (chargeRadius <= 0) {
            return;
        }

        BlockData data = Material.WATER.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data)
            .setCollidable(isWaterCollidable);

        double blockSpacing = 0.5;
        double step = 2 * Math.asin(blockSpacing / (2 * chargeRadius));
        int count = (int) Math.ceil(Math.toRadians(angle) / step);
        Set<Block> toRevert = new HashSet<>(ringBlocks.keySet());
        
        for (int i = 0; i < count; ++i) {
            double rad = animAngle + (i * step);
            double x = Math.cos(rad) * chargeRadius;
            double z = Math.sin(rad) * chargeRadius;
            Location loc = user().player().getEyeLocation().add(x, 0, z);
            Block block = loc.getBlock();

            if (BlockUtil.isSolid(block)) {
                continue;
            } else if (ringBlocks.containsKey(block)) {
                toRevert.remove(block);
                continue;
            }

            builder.buildAt(block).ifPresent(tb -> {
                ringBlocks.put(block, tb);
            });

        }

        for (Block block : toRevert) {
            TempBlock tb = ringBlocks.remove(block);
            ElementalMagicApi.revertibleManager().revert(tb);
        }
   
    }

    private boolean progressShrinking() {
        if (chargeRadius <= 0) {
            initFlying();
            return true;
        }
        manageRing(360);
        chargeRadius -= 0.2;
        return true;
    }

    private void initFlying() {
        for (TempBlock tb : ringBlocks.values()) {
            ElementalMagicApi.revertibleManager().revert(tb);
        }
        ringBlocks.clear();
        
        // So we don't have to manage null values:
        Player player = user().player();
        Location loc = player.getLocation();
        Vector dir = player.getEyeLocation().getDirection();
        lastLoc = loc.add(dir.multiply(-2));
        lastLoc.add(0, -1, 0);

        endTime = System.currentTimeMillis() + flightDuration;
        state = State.FLYING;
    }

    private boolean progressFlying() {
        if (user().player().isSneaking() 
                || System.currentTimeMillis() > endTime) {
            return false;
        }
        
        applyVelocity();
        drawWave();

        Player player = user().player();
        Vector dir = player.getEyeLocation().getDirection().multiply(1);
        lastLoc = player.getLocation().add(0, -1, 0).subtract(dir);
        
        return true;
    }

    private void applyVelocity() {
        Player player = user().player();
        double factor = (double) (endTime - System.currentTimeMillis()) / flightDuration;
        Vector dir = player.getEyeLocation().getDirection();
        dir.multiply(flightSpeed * factor);
        
        ElementalMagicApi.effectHandler().setVelocity(player, this, dir);
        user().player().setFallDistance(0);
    }

    private void drawWave() {
        BlockData data = isIce ?
            Material.ICE.createBlockData() : Material.WATER.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data)
            .setCollidable(!isIce && !isWaterCollidable)
            .setDuration(revertTime);

        for (Block b : BlockUtil.collectSphere(lastLoc, waveRadius)) {
            if (!BlockUtil.isSolid(b)) {
                builder.buildAt(b);
            }
        }
    }

	@Override
	public void onDestruction() {
        if (travelingSource != null) {
            travelingSource.revertBlocks();
        }
        for (TempBlock tb : ringBlocks.values()) {
            ElementalMagicApi.revertibleManager().revert(tb);
        }
        ringBlocks.clear();
    }

    @Override
    public String name() {
        return "WaterWave";
    }

    protected boolean isSourced() {
        return state == State.SOURCED;
    }

    public boolean canIceWave() {
        return state == State.FLYING 
            || state == State.SHRINKING;
    }

    public void setIce(boolean ice) {
        isIce = ice;
    }

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = WaterSpoutController.CONFIG_PATH + "Wave.";

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 2500;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 12.0;
        @Configure(path = CONFIG_PATH + "ChargeTime", config = Config.ABILITIES)
        private long chargeTime = 200;
        @Configure(path = CONFIG_PATH + "ChargeRadius", config = Config.ABILITIES)
        private double chargeRadius = 3.5;
        @Configure(path = CONFIG_PATH + "FlightDuration", config = Config.ABILITIES)
        private long flightDuration = 1800;
        @Configure(path = CONFIG_PATH + "FlightSpeed", config = Config.ABILITIES)
        private double flightSpeed = 1.5;
        @Configure(path = CONFIG_PATH + "WaveRadius", config = Config.ABILITIES)
        private double waveRadius = 1.5;
        @Configure(path = CONFIG_PATH + "RevertTime", config = Config.ABILITIES)
        private long revertTime = 1750;
        @Configure(path = CONFIG_PATH + "IsWaterCollidable", config = Config.ABILITIES)
        private boolean isWaterCollidable = false;
    }
}

