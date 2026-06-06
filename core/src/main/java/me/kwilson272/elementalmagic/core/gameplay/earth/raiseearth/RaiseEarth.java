package me.kwilson272.elementalmagic.core.gameplay.earth.raiseearth;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.components.EarthPillar;
import me.kwilson272.elementalmagic.core.gameplay.components.EarthPillar.PillarState;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class RaiseEarth extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double speed;
    private double clickSelectRange;
    private int clickHeight;
    private double sneakSelectRange;
    private int sneakHeight;
    private int sneakWidth;
    private double hitboxSize;
    private double knockup;

    private boolean isSneak;
    private boolean isRising;
    private long revertTime;
    private List<EarthPillar> pillars;

	public RaiseEarth(AbilityUser user, AbilityController controller, boolean isSneak) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        speed = CONFIG.speed;
        clickSelectRange = CONFIG.clickSelectRange;
        clickHeight = CONFIG.clickHeight;
        sneakSelectRange = CONFIG.sneakSelectRange;
        sneakHeight = CONFIG.sneakHeight;
        sneakWidth = CONFIG.sneakWidth;
        hitboxSize = CONFIG.hitboxSize;
        knockup = CONFIG.knockup;
    
        this.isSneak = isSneak;
        isRising = true;
        pillars = new ArrayList<>();
	}

	@Override
	public boolean start() {
        initPillars();
        if (pillars.isEmpty()) {
            return false;
        }

        revertTime = System.currentTimeMillis() + duration;
        user().addCooldown(name(), cooldown);
        return true;
	}
    
    private void initPillars() {
        List<Block> blocks = new ArrayList<>();
        if (!isSneak) {
            Player player = user().player();
            Block target = Entities.getTargetBlock(player, clickSelectRange, Blocks::isSolid);
            if (Blocks.isSolid(target)) {
                blocks.add(target);
            }

        } else {
            blocks = getWallBase();
        }

        int height = isSneak ? sneakHeight : clickHeight;
        for (Block block : blocks) {
            Block start = getPillarStart(block, height);
            if (start != null) {
                EarthPillar pillar = new EarthPillar(this, start, height, speed, true); 
                pillar.setBlockPlaceCallback(b -> playEarthSound(b.getLocation()));
                pillar.setMoveCallback(this::affectEntities);
                pillars.add(pillar);
            }
        }
    }

    private List<Block> getWallBase() {
        Player player = user().player();
        Block block = Entities.getTargetBlock(player, sneakSelectRange, Blocks::isSolid);
        if (!Blocks.isSolid(block)) {
            return List.of();
        }
        
        Vector vec = Vectors.fromRotations(0, player.getEyeLocation().getYaw());
        double x = vec.getX();
        vec.setX(vec.getZ());
        vec.setZ(-x);
    
        List<Block> blocks = new ArrayList<>();
        Location center =  block.getLocation().add(0.5, 0.5, 0.5);
        for (int i = -sneakWidth/2; i <= sneakWidth/2; ++i) {
            Location loc = center.clone().add(vec.clone().multiply(i));
            blocks.add(loc.getBlock());
        }

        return blocks;
    }

    private Block getPillarStart(Block block, int height) {
        for (int i = 0; i < height; ++i) {
            Block above = block.getRelative(BlockFace.UP);
            if (Blocks.isSolid(block) && !Blocks.isSolid(above)) {
                break;
            } else if (Blocks.isSolid(above)) {
                block = above;
            } else {
                block = block.getRelative(BlockFace.DOWN);
            }
        }

        Block above = block.getRelative(BlockFace.UP);
        if (!isUsableEarth(block) || Blocks.isSolid(above)
                || EarthPillar.getFromBlock(block) != null) {
            return null;
        }
        
        return block;
    }

    private void affectEntities(Location loc) {
        Vector knock = new Vector(0, knockup, 0);
        for (Entity e : Entities.getNearbyEntities(loc, hitboxSize)) {
            ElementalMagicApi.effectHandler().setVelocity(e, this, knock);
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false) 
                || pillars.isEmpty()) {
            return false;
        }

        pillars.removeIf(pillar -> !pillar.isViable());
        if (!isRising) {
            return System.currentTimeMillis() < revertTime;
        }

        boolean roseAny = false;
        for (EarthPillar pillar : pillars) {
            if (pillar.getState() != PillarState.IDLE) {
                pillar.progress();
                roseAny = true; 
            }
        }

        if (!roseAny) {
            isRising = false;
        }

        return true;
	}

	@Override
	public void onDestruction() {
        for (EarthPillar pillar : pillars) {
            pillar.revert();
        }
        pillars.forEach(EarthPillar::revert);
	}

	@Override
	public String name() {
	    return "RaiseEarth";
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = RaiseEarthController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 1000;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 15000;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 0.75;
        @Configure(path = CONFIG_PATH + "ClickSelectRange", config = Config.ABILITIES)
        private double clickSelectRange = 15;
        @Configure(path = CONFIG_PATH + "ClickHeight", config = Config.ABILITIES)
        private int clickHeight = 6;
        @Configure(path = CONFIG_PATH + "SneakSelectRange", config = Config.ABILITIES)
        private double sneakSelectRange = 15;
        @Configure(path = CONFIG_PATH + "SneakHeight", config = Config.ABILITIES)
        private int sneakHeight = 5;
        @Configure(path = CONFIG_PATH + "SneakWidth", config = Config.ABILITIES)
        private int sneakWidth = 4;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.6;
        @Configure(path = CONFIG_PATH + "Knockup", config = Config.ABILITIES)
        private double knockup = 0.6;
    }
}
