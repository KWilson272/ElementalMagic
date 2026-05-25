package me.kwilson272.elementalmagic.core.gameplay.water.phasechange;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterSourceOptions;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PhaseChangeFreeze extends CoreAbility {
    
    private static final BlockFace[] CHECK_FACES = {
        BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, 
        BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH
    };

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double range;
    private double radius;
    private double revertDistance;
    private List<TempBlock> tempBlocks;

    public PhaseChangeFreeze(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;;
        range = CONFIG.range;
        radius = CONFIG.radius;
        revertDistance = CONFIG.revertDistance;
        tempBlocks = new ArrayList<>();
    }

    @Override
    public boolean start() {
        user().addCooldown("PhaseChangeFreeze", cooldown);

        Player player = user().player();
        Block target = BlockUtil.getTargetBlock(player, range, this::isCollidable);
        Location center = target.getLocation().add(0.5, 0.5, 0.5);

        freezeSphere(center);
        if (!tempBlocks.isEmpty()) {
            WaterUtil.playIceSound(center);
        }

        return true;
    }

    private boolean isCollidable(Block block) {
        // Don't collide on water that we couldn't freeze, it feels better if
        // PhaseChange works through un-usable abilities like WaterSpout
        return BlockUtil.isSolid(block) || (BlockUtil.isLiquid(block)
                && TempBlock.get(block).map(TempBlock::isUsable).orElse(true));
    }
    
    private void freezeSphere(Location center) {
        BlockData data = Material.ICE.createBlockData();
        var iceBuilder = TempBlock.builder(this, data).setUsable(true);
        var opts = new WaterSourceOptions(user()).noPlant().noIce().noSnow();
        
        Location eyeLoc = user().player().getEyeLocation();
        double revertDistSqrd = revertDistance * revertDistance;

        for (Block block : BlockUtil.collectSphere(center, radius)) {
            // Ensure we don't freeze blocks that get instantly reverted
            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            double distSqrd = loc.distanceSquared(eyeLoc);
            if (!WaterUtil.canUse(block, opts) || distSqrd >= revertDistSqrd) {
                continue;
            }

            iceBuilder.buildAt(block).ifPresent(tb -> {
                tempBlocks.add(tb);
                trySpawnParticle(tb.block());
            });
        }
    }

    private void trySpawnParticle(Block block) {
        if (ThreadLocalRandom.current().nextInt(4) != 0) {
            return;
        }
        
        World world = block.getWorld();
        for (BlockFace face : CHECK_FACES) {
            if (block.getRelative(face).getType() != Material.AIR) {
                continue;
            }
            
            // Get center to ensure we are equally far apart from any face
            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            loc.add(face.getModX()/2.0, face.getModY()/2.0, face.getModZ()/2.0);
            world.spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.3, 0.125, 0.3, 0);
        }
    }

    @Override
    public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }

        revertFarBlocks();
        return !tempBlocks.isEmpty();
    }

    private void revertFarBlocks() {
        Location eyeLoc = user().player().getEyeLocation();
        double maxDistSqrd = revertDistance * revertDistance;
        RevertibleManager revertmanager = ElementalMagicApi.revertibleManager();

        Iterator<TempBlock> iter = tempBlocks.iterator();
        while (iter.hasNext()) {
            TempBlock tb = iter.next();
            if (tb.isReverted()) {
                iter.remove();
                continue;
            }

            Location blockCenter = tb.block().getLocation().add(0.5, 0.5, 0.5);
            if (blockCenter.distanceSquared(eyeLoc) >= maxDistSqrd) {
                revertmanager.revert(tb);
                iter.remove();
            }
        }
    }

    @Override
    public void onDestruction() {
        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        tempBlocks.forEach(revertManager::revert);
    }

    @Override
    public String name() {
        return "PhaseChangeFreeze";
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = PhaseChangeController.CONFIG_PATH + "Freeze.";

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double range = 25.0;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 3.8;
        @Configure(path = CONFIG_PATH + "RevertDistance", config = Config.ABILITIES)
        private double revertDistance = 25.0;
    }
}

