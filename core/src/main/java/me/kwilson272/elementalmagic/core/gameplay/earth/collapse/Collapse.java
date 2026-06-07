package me.kwilson272.elementalmagic.core.gameplay.earth.collapse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.components.EarthPillar;
import me.kwilson272.elementalmagic.core.gameplay.components.EarthPillar.PillarState;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;

public class Collapse extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double selectRange;
    private double radius;
    private double speed;
    private int height;

    private boolean isActive;
    private long revertTime;
    private List<EarthPillar> createdPillars;
    /* So we don't double track pillars for reversions */
    private List<EarthPillar> existingPillars;

	public Collapse(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        selectRange = CONFIG.selectRange;
        radius = CONFIG.radius;
        speed = CONFIG.speed;
        height = CONFIG.height;

        isActive = true;
        createdPillars = new ArrayList<>();
        existingPillars = new ArrayList<>();
	}

	@Override
	public boolean start() {
        Player player = user().player();
        Block target = Entities.getTargetBlock(player, selectRange, Blocks::isSolid);
        if (!Blocks.isSolid(target)) {
            return false;
        }

        initPillars(target.getLocation().add(0.5, 0.5, 0.5));
        if (createdPillars.isEmpty() && existingPillars.isEmpty()) {
            return false;
        }

        user().addCooldown(name(), cooldown);
        return true;
	}

    private void initPillars(Location loc) {
        for (Block block : Blocks.collectCircle(loc, radius)) {
            block = getCollapseStart(block);
            if (block == null) {
                continue;
            }  

            EarthPillar pillar = EarthPillar.getFromBlock(block);
            if (pillar != null) {
                pillar.collapse();
                pillar.setMoveCallback(null);
                existingPillars.add(pillar);
            } else {
                pillar = new EarthPillar(this, block, height, speed, false);
                createdPillars.add(pillar);
            }

            pillar.setBlockPlaceCallback(this::playSound);
        }
    }

    private Block getCollapseStart(Block block) {
        for (int i = 0; i < height; ++i) {
            Block below = block.getRelative(BlockFace.DOWN);
            if (EarthPillar.getFromBlock(block) != null) {
                return block;
            } else if (!Blocks.isSolid(below) && Blocks.isSolid(block)) {
                break;
            } else if (Blocks.isSolid(below)) {
                block = below;
            } else {
                block = block.getRelative(BlockFace.UP);
            }
        }

        Block below = block.getRelative(BlockFace.DOWN);
        if (!Blocks.isSolid(block) || Blocks.isSolid(below)
                || !isUsableEarth(block)) {
            return null;
        }
        return block;
    }

    private void playSound(Block block) {
        double total = existingPillars.size() + createdPillars.size();
        int chance = (int) Math.max(1, total / 3);
        if (ThreadLocalRandom.current().nextInt(chance) == 0) {
            playEarthSound(block.getLocation());
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }

        if (!isActive) {
            return System.currentTimeMillis() < revertTime;
        }

        boolean roseAny = false;
        for (EarthPillar pillar : createdPillars) {
            if (pillar.getState() == PillarState.COLLAPSING) {
                pillar.progress();
                roseAny = true;
            }
        }

        Iterator<EarthPillar> iter = existingPillars.iterator();
        while (iter.hasNext()) {
            EarthPillar pillar = iter.next();
            roseAny = true;
            if (!pillar.progress()) {
                iter.remove();
            }
        }

        if (!roseAny) {
            revertTime = System.currentTimeMillis() + duration;
            isActive = false;
        }

        return true;
	}

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "Collapse";
	}

    protected static class ConfigValues {

        private static final String CONFIG_PATH = CollapseController.CONFIG_PATH;
   
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 15000;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 20;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 5;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.0;
        @Configure(path = CONFIG_PATH + "Height", config = Config.ABILITIES)
        private int height = 5;
    }
}
