package me.kwilson272.elementalmagic.core.gameplay.earth.lavaflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;

public class LavaFlow extends EarthAbility {


	protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long formDelay;
    private double meltPercent;
    private double selectRange;
    private double clickRadius;
    private double sneakRadius;
    private long lavaDuration;
    private double lavaDamage;

    private boolean isSneak;

    private int meltPerTick;
    private long meltStartTime;
    private List<Block> candidateBlocks;

    public LavaFlow(AbilityUser user, AbilityController controller, boolean isSneak) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        formDelay = CONFIG.formDelay;
        meltPercent = CONFIG.meltPercent;
        selectRange = CONFIG.selectRange;
        clickRadius = CONFIG.clickRadius;
        sneakRadius = CONFIG.sneakRadius;
        lavaDuration = CONFIG.lavaDuration;
        lavaDamage = CONFIG.lavaDamage;

        this.isSneak = isSneak;

        candidateBlocks = new ArrayList<>();
    }

	@Override
	public boolean start() {
	    if (isSneak) {
            Location loc = user().player().getLocation().add(0, -1, 0);
            Set<Block> safeBlocks = Set.copyOf(Blocks.collectSphere(loc, 2));
            initMeltCandidates(loc, sneakRadius, safeBlocks);

        } else {
            Player player = user().player();
            Block target = Entities.getTargetBlock(player, selectRange, Blocks::isSolid);
            if (Blocks.isSolid(target)) {
                Location loc = target.getLocation().add(0.5, 0.5, 0.5);
                initMeltCandidates(loc, clickRadius, Set.of());
            }
        }
        
        if (candidateBlocks.isEmpty()) {
            return false;
        }
    
        Collections.shuffle(candidateBlocks);
        meltPerTick = (int) Math.max(1, candidateBlocks.size() * meltPercent / 100);
        meltStartTime = System.currentTimeMillis() + formDelay;

        user().addCooldown(name(), cooldown);
        return true;
    }

    private void initMeltCandidates(Location loc, double radius, Set<Block> exclude) {
        for (Block b : Blocks.collectCircle(loc, radius)) {
            
            int limit = 15;
            for (int i = 0; i < limit; ++i) {
                Block above = b.getRelative(BlockFace.UP);
                if (Blocks.isSolid(b) && !Blocks.isSolid(above)) {
                    break;
                } else if (Blocks.isSolid(above)) {
                    b = b.getRelative(BlockFace.UP);
                } else {
                    b = b.getRelative(BlockFace.DOWN); 
                }
            }

            if (b != null && !exclude.contains(b) && isUsableEarth(b) 
                    && !Blocks.isWater(b.getRelative(BlockFace.UP))) {
                candidateBlocks.add(b);
            }
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false; 
        }

        if (System.currentTimeMillis() < meltStartTime) {
            return true;
        }
        
        meltBlocks();
        return !candidateBlocks.isEmpty();
	}
    
    private void meltBlocks() {
        BlockData lavaData = Material.LAVA.createBlockData();
        TempBlockBuilder lavaBuilder = TempBlock.builder(this, lavaData)
            .setDuration(lavaDuration)
            .setDamage(lavaDamage)
            .setUsable(true);

        BlockData airData = Material.AIR.createBlockData();
        TempBlockBuilder airBuilder = TempBlock.builder(this, airData)
            .setDuration(lavaDuration);

        for (int i = 0; i < meltPerTick; ++i) {
            if (candidateBlocks.isEmpty()) {
                break;
            }

            Block block = candidateBlocks.removeLast();
            if (!isUsableEarth(block)) {
                continue;
            }

            lavaBuilder.buildAt(block);
            Block above = block.getRelative(BlockFace.UP);
            if (!Blocks.isSolid(above) && !Blocks.isLiquid(above)) {
                airBuilder.buildAt(block.getRelative(BlockFace.UP));
            }

            World world = block.getWorld();
            Location loc = block.getLocation().add(0.5, 1, 0.5);
            world.spawnParticle(Particle.LAVA, loc, 1);
        }
    }

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "LavaFlow";
	}

    protected static class ConfigValues {
    
        private static final String CONFIG_PATH = LavaFlowController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 15000;
        @Configure(path = CONFIG_PATH + "FormDelay", config = Config.ABILITIES)
        private long formDelay = 300;
        @Configure(path = CONFIG_PATH + "MeltPercent", config = Config.ABILITIES)
        private double meltPercent = 10.0;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 15.0;
        @Configure(path = CONFIG_PATH + "ClickRadius", config = Config.ABILITIES)
        private double clickRadius = 4.0;
        @Configure(path = CONFIG_PATH + "SneakRadius", config = Config.ABILITIES)
        private double sneakRadius = 6.0;
        @Configure(path = CONFIG_PATH + "LavaDuration", config = Config.ABILITIES)
        private long lavaDuration = 10000;
        @Configure(path = CONFIG_PATH + "LavaDamage", config = Config.ABILITIES)
        private double lavaDamage = 2.0;
    }
}

