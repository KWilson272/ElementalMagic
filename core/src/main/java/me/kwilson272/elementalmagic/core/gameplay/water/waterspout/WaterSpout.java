package me.kwilson272.elementalmagic.core.gameplay.water.waterspout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;

public class WaterSpout extends CoreAbility {
    
    private static final BlockFace[] SPIRAL_FACES = {
        BlockFace.NORTH, BlockFace.NORTH_EAST,
        BlockFace.EAST, BlockFace.SOUTH_EAST,
        BlockFace.SOUTH, BlockFace.SOUTH_WEST,
        BlockFace.WEST, BlockFace.NORTH_WEST
    };

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private int height;
    private double flySpeed;
    private double hopPower;
    
    private int breakHeight;
    private boolean isInfinite;
    private long endTime;

    private int spiralIndex;

    private Map<Block, TempBlock> spoutBlocks;
    // Not a map because we revert every tick regardless 
    private List<TempBlock> spiralBlocks; 

	public WaterSpout(AbilityUser user, AbilityController controller) {
		super(user, controller);
    
        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        height = CONFIG.height;
        flySpeed = CONFIG.flySpeed;
        hopPower = CONFIG.hopPower;
        
        spiralIndex = 0;
        
        spoutBlocks = new HashMap<>();
        spiralBlocks = new ArrayList<>();
	}

    protected void hop() {
        Player player = user().player();
        if (player.isSneaking()) {
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();
            Vector dir = player.getEyeLocation().getDirection();
            effectHandler.setVelocity(player, this, dir.multiply(hopPower));
        }
    }

	@Override
	public boolean start() {
        AbilityManager manager = ElementalMagicApi.abilityManager();
        WaterWave wave = manager.getAbility(user(), WaterWave.class).orElse(null);
        if (wave != null && wave.isSourced()) {
            return false;
        }

        breakHeight = height + 4;
        isInfinite = duration < 0;
        endTime = System.currentTimeMillis() + duration;
        return getBase() != null;
	}

    private Block getBase() {
        Block block = user().player().getLocation().getBlock();
        for (int i = 0; i < breakHeight; ++i) {
            if (canSpoutOn(block)) {
                return block;
            } else if (BlockUtil.isSolid(block)) {
                return null;
            }
            block = block.getRelative(BlockFace.DOWN);
        }
        return null;
    }

    private boolean canSpoutOn(Block block) {
        return (AbilityUtil.isWater(block) && !spoutBlocks.containsKey(block))
            || AbilityUtil.isIce(block)
            || AbilityUtil.isSnow(block);
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false) 
                || (!isInfinite && System.currentTimeMillis() > endTime)) {
            return false;        
        }

        // Revert spiral blocks first so we can't spout on them
        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        spiralBlocks.forEach(revertManager::revert);
        spiralBlocks.clear();

        Block base = getBase();
        if (base == null) {
            return false;
        }
        
        Block footBlock = user().player().getLocation().getBlock();
        int heightDiff = footBlock.getY() - base.getY();
        if (heightDiff >= breakHeight) {
            return false;
        }

        int maxHeight = Math.min(heightDiff, height);
        manageSpout(base, maxHeight);
        manageSpiral(base, maxHeight);
        setFlying(heightDiff <= height);

        return true;
	}

    private void manageSpout(Block base, int height) {
        Set<Block> toRevert = new HashSet<>(spoutBlocks.keySet());

        Block block = base;
        for (int i = 0; i < height; ++i) {
            block = block.getRelative(BlockFace.UP);
            if (spoutBlocks.containsKey(block)) {
                toRevert.remove(block);

            } else if (!BlockUtil.isSolid(block)) {
                BlockData data = Material.WATER.createBlockData();
                TempBlock.builder(this, data)
                    .setCollidable(false)
                    .buildAt(block)
                    .ifPresent(tb -> spoutBlocks.put(tb.block(), tb));
            }
        }

        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        for (Block b : toRevert) {
            revertManager.revert(spoutBlocks.remove(b));
        }
    }

    private void manageSpiral(Block base, int height) {
        Location loc = base.getLocation().add(0.5, 0.5, 0.5);
        BlockData data = Material.WATER.createBlockData();
        ((Levelled) data).setLevel(1);

        double spacing = 0.5;
        spiralIndex = (spiralIndex + 1) % SPIRAL_FACES.length;
        for (double i = 0; i < height; i += spacing) {
            int idx = (spiralIndex + (int) i) % SPIRAL_FACES.length;
            BlockFace face = SPIRAL_FACES[idx];
            Block block = loc.getBlock().getRelative(face);
            loc.add(0, spacing, 0);

            if (!BlockUtil.isSolid(block)) {
                TempBlock.builder(this, data)
                    .setCollidable(false)
                    .buildAt(block)
                    .ifPresent(spiralBlocks::add);
            }
        }
    }

	@Override
	public void onDestruction() {
        user().addCooldown(controller().name(), cooldown);
        setFlying(false);

        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        spoutBlocks.values().forEach(revertManager::revert);
        spiralBlocks.forEach(revertManager::revert);
	}

    private void setFlying(boolean flying) {
        user().player().setAllowFlight(flying);
        user().player().setFlying(flying);
    }

    public double getSpeed() {
        return flySpeed;
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = WaterSpoutController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = -1;
        @Configure(path = CONFIG_PATH + "Height", config = Config.ABILITIES)
        private int height = 16;
        @Configure(path = CONFIG_PATH + "FlySpeed", config = Config.ABILITIES)
        private double flySpeed = 0.15;
        @Configure(path = CONFIG_PATH + "HopPower", config = Config.ABILITIES)
        private double hopPower = 0.9;
    }
}
