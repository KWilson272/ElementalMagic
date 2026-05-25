package me.kwilson272.elementalmagic.core.gameplay.water.surge;

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
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterUtil;

public class SurgeWall extends CoreAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double selectRange;
    private long sourceRevertTime;
    private long duration;
    private double radius;
    private double holdRange;

    private boolean isSourced;
    private Block source;

    private boolean isInfinite;
    private long endTime;

    private Map<Block, TempBlock> affectedBlocks;

    public SurgeWall(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        sourceRevertTime = CONFIG.sourceRevertTime;
        duration = CONFIG.duration;
        radius = CONFIG.radius;
        holdRange = CONFIG.holdRange;

        isSourced = true;
        affectedBlocks = new HashMap<>();
    }

    @Override
    public boolean start() {
        source = WaterUtil.getSourceBlock(user(), selectRange);
        return source != null;
    }

    protected boolean isSourced() {
        return isSourced;
    }

    @Override
    public boolean progress() {
        if (!user().canUse(controller(), true, false)) {
            return false;
        }
        
        if (isSourced) {
            if (!isSourceViable()) {
                return false;
            }
            if (user().player().isSneaking()) {
                setHolding();
            }
        } else if (!user().player().isSneaking() 
                || (!isInfinite && System.currentTimeMillis() > endTime)) {
            return false;        
        } else {
            manageWall();
        }

        return true;
    }

    private boolean isSourceViable() {
        Location loc = source.getLocation().add(0.5, 0.5, 0.5);
        Location eye = user().player().getEyeLocation();
        double maxDist = Math.pow(selectRange + 1, 2);

        return loc.distanceSquared(eye) <= maxDist 
            && WaterUtil.canUse(source, user());
    }

    private void setHolding() {
        isSourced = false;
        isInfinite = duration < 0;
        endTime = System.currentTimeMillis() + duration;
        WaterUtil.consumeSource(this, source, sourceRevertTime);
        WaterUtil.playIceSound(user().player().getEyeLocation());
    }

    private void manageWall() {
        Location center = getWallCenter();
        Vector dir = user().player().getEyeLocation().getDirection();
        Vector ortho = VectorUtil.getOrthogonal(dir);

        Set<Block> toRevert = new HashSet<>(affectedBlocks.keySet());
        BlockData iceData = Material.ICE.createBlockData();
        TempBlockBuilder ice = TempBlock.builder(this, iceData);

        double blockSpacing = 0.5;
        double step = 2 * Math.asin(blockSpacing / (2 * radius));
        for (double angle = 0; angle < Math.PI * 2; angle += step) {
            Vector vec = VectorUtil.rotateAroundVector(dir, ortho, angle);
            vec.multiply(0.5);

            Location loc = center.clone();
            for (double i = 0; i <= radius; i += blockSpacing) {
                Block block = loc.getBlock();
                loc.add(vec);

                if (affectedBlocks.containsKey(block)) {
                    toRevert.remove(block);
                } else if (!BlockUtil.isSolid(block)) {
                    ice.buildAt(block)
                        .ifPresent(tb ->affectedBlocks.put(block, tb));
                }
            }
        }

        for (Block block : toRevert) {
            TempBlock tb = affectedBlocks.remove(block);
            ElementalMagicApi.revertibleManager().revert(tb);
        }
    }
    
    private Location getWallCenter() {
        Player player = user().player();
        Block target = BlockUtil.getTargetBlock(player, holdRange, b ->
            BlockUtil.isSolid(b) && !affectedBlocks.containsKey(b)
        );
        
        Vector dir = user().player().getEyeLocation().getDirection();
        Location center = target.getLocation().add(0.5, 0.5, 0.5);
        if (BlockUtil.isSolid(target) && !affectedBlocks.containsKey(target)) {
            center.subtract(dir);
        }

        return center;
    }

    @Override
    public void onDestruction() {
        if (!isSourced) {
            RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
            affectedBlocks.values().forEach(revertManager::revert);
            user().addCooldown("SurgeWall", cooldown);
        }
    }
    
    @Override
    public String name() {
        return "SurgeWall";
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = SurgeController.CONFIG_PATH + "Wall.";

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 8;
        @Configure(path = CONFIG_PATH + "SourceRevertTime", config = Config.ABILITIES)
        private long sourceRevertTime = 10000;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = -1;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 2.0;
        @Configure(path = CONFIG_PATH + "HoldRange", config = Config.ABILITIES)
        private double holdRange = 5;
    }
}

