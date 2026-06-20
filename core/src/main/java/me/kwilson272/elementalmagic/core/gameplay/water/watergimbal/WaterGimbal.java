package me.kwilson272.elementalmagic.core.gameplay.water.watergimbal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.components.TravelingSource;
import me.kwilson272.elementalmagic.core.gameplay.components.TravelingSource.TravelState;
import me.kwilson272.elementalmagic.core.gameplay.water.WaterAbility;
import me.kwilson272.elementalmagic.core.gameplay.water.WaterUsePolicy;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class WaterGimbal extends WaterAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();
    
    private long cooldown;
    private double selectRange;
    private double ringSize;
    private double animAngle;
    private double range;
    private double speed;
    private double damage;
    private double hitboxSize;
    private boolean allowIceSource;
    private boolean allowSnowSource;
    private boolean allowPlantSource;

    private boolean isSourced;
    private TravelingSource source;

    private double ringAngle;
    private boolean firedFirst;
    private boolean firedSecond;
    private boolean renderFirst;
    private boolean renderSecond;
    private Location locFirst;
    private Location locSecond;

    private TempBlockBuilder waterBuilder;
    private List<TempBlock> ringBlocks;
    private List<GimbalStream> streams;

    public WaterGimbal(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        ringSize = CONFIG.ringSize;
        animAngle = CONFIG.animAngle;
        range = CONFIG.range;
        speed = CONFIG.speed;
        damage = CONFIG.damage;
        hitboxSize = CONFIG.hitboxSize;
       
        ringAngle = 0;
        isSourced = true;
        firedFirst = false;
        firedSecond = false;
        renderFirst = true;
        renderSecond = true;
        
        BlockData data = Material.WATER.createBlockData();
        waterBuilder = TempBlock.builder(this, data).setCollidable(false);
        ringBlocks = new ArrayList<>();
        streams = new ArrayList<>();
	}

    @Override
    public boolean start() {
        var opts = new WaterUsePolicy();
        opts.setIce(allowIceSource)
            .setSnow(allowSnowSource)
            .setPlant(allowPlantSource)
            .validate(user());

        Block block = selectSourceBlock(selectRange, opts); 
        if (block == null) {
            return false;
        }
        
        animAngle = Math.toRadians(animAngle);
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        source = new TravelingSource(loc, 1, false, waterBuilder);
        return true;
    }


    @Override
    public boolean progress() {
        if (!user().canUse(controller(), false, false)
                || ((!firedFirst || !firedSecond) && !user().player().isSneaking())) {
            return false;
        }
        
        if (isSourced) {
            return progressSourced();

        } else {
            clearRings();
            progressRings();
            fireBlasts();
            streams.removeIf(stream -> !stream.progress());
            return renderFirst || renderSecond || !streams.isEmpty();
        }
    }

    private boolean progressSourced() {
        World world = source.getLocation().getWorld();
        if (!world.equals(user().player().getWorld())) {
            return false;
        }

        Location loc = user().player().getEyeLocation();
        var tState = source.moveTowards(loc, 2.0);
        if (tState == TravelState.ARRIVED) {
            isSourced = false;
            source.revertBlocks();
            source = null;
        }

        return tState != TravelState.BLOCKED;
    }

    private void clearRings() {
        for (TempBlock tb : ringBlocks) {
            ElementalMagicApi.revertibleManager().revert(tb);
        }
        ringBlocks.clear();
    }

    private void progressRings() {
        if (!renderFirst && !renderSecond) {
            return;
        }

        Location loc = user().player().getEyeLocation();
        
        double yawRad = Math.toRadians(loc.getYaw());
        double x = -Math.sin(yawRad);
        double z = Math.cos(yawRad);
        Vector base = new Vector(x, 0, z);
        
        double angle = Math.toRadians(60);
        double rotYaw = Math.toRadians(loc.getYaw() + 90);
        double rotX = -Math.sin(rotYaw);
        double rotY = Math.sin(angle);
        double rotZ = Math.cos(rotYaw);

        Vector vecFirst = new Vector(rotX, rotY, rotZ);
        Vector vecSecond = new Vector(-rotX, rotY, -rotZ);
        
        double blockSpacing = 0.5;
        double step = 2 * Math.asin(blockSpacing / (2 * ringSize));
        int count = (int) Math.ceil(Math.toRadians(180) / step);
    
        for (int i = 0; i < count; ++i) {
            double rad = ringAngle + (i * step);
            double cosRad = Math.cos(rad) * ringSize;
            double sinRad = Math.sin(rad) * ringSize;
            Vector compBase = base.clone().multiply(cosRad);

            if (renderFirst) {
                Vector compFirst = vecFirst.clone().multiply(sinRad);
                locFirst = loc.clone().add(compFirst.add(compBase));
                addRingBlock(locFirst.getBlock());
            }

            if (renderSecond) {
                Vector compSecond = vecSecond.clone().multiply(sinRad);
                locSecond = loc.clone().add(compSecond.add(compBase));
                addRingBlock(locSecond.getBlock());
            }
        }

        ringAngle += animAngle;
    }

    private void addRingBlock(Block block) {
        Block eyeBlock = user().player().getEyeLocation().getBlock();
        double yDiff = eyeBlock.getY() - block.getY();
        double y = Math.copySign(1, yDiff);

        while (Blocks.isSolid(block) && block.getY() != eyeBlock.getY()) {
            block = block.getLocation().add(0, y, 0).getBlock();
        }

        if (!Blocks.isSolid(block)) {
            BlockData data = Material.WATER.createBlockData();
            TempBlock.builder(this, data).buildAt(block)
                .ifPresent(ringBlocks::add);
        }
    }

    private void fireBlasts() {
        Location eyeLoc = user().player().getEyeLocation();
        if (firedFirst && renderFirst 
                && locFirst.getBlockY() <= eyeLoc.getBlockY()) {
            playWaterSound(locFirst);
            streams.add(new GimbalStream(locFirst));
            renderFirst = false;
        } 
        if (firedSecond && renderSecond
                && locSecond.getBlockY() <= eyeLoc.getBlockY()) {
            playWaterSound(locSecond);
            streams.add(new GimbalStream(locSecond));
            renderSecond = false;
        }
    }

    @Override
    public void onDestruction() {
        if (source != null) {
            source.revertBlocks();
        }
        if (!isSourced) {
            user().addCooldown("WaterGimbal", cooldown);
        }
        
        clearRings();
    }

    @Override
    public String name() {
        return "WaterGimbal";
    }

    protected void fire() {
        if (!firedFirst) {
            firedFirst = true; 
        } else {
            firedSecond = true;
        }
    }

    private class GimbalStream {

        private Location location;
        private double rangeCounter;
        private Set<Entity> noAffect;

        GimbalStream(Location location) {
            this.location = location;
            this.rangeCounter = range;
            this.noAffect = new HashSet<>();
            this.noAffect.add(user().player());
        }

        boolean progress() {
            Player player = user().player();
            // The targeting is kind of weird unless we add the 5 
            Block block = Entities.getTargetBlock(player, range + 5, Blocks::isSolid);
            Location targ = block.getLocation().add(0.5, 0.5, 0.5);
            
            double remainder = speed;
            while (remainder > 0) {
                double travel = Math.min(remainder, 1);
                remainder--;

                Vector dir = Vectors.getDirection(location, targ);
                dir.normalize().multiply(travel);
                Block oldBlock = location.getBlock();
                Block newBlock = location.add(dir).getBlock();

                if (!oldBlock.equals(newBlock)) {
                    if (Blocks.isSolid(newBlock)) {
                        // Attempt to slide over ground if not at destination
                        BlockFace safeFace = !newBlock.equals(targ.getBlock()) ?
                            BlockFace.UP : BlockFace.SELF;
                        newBlock = newBlock.getRelative(safeFace);
                        if (Blocks.isSolid(newBlock)) {
                            return false;
                        }
                    }
                    waterBuilder.setDuration(250).buildAt(newBlock);
                }
               
                affectEntities();
                rangeCounter -= travel;
                if (rangeCounter <= 0) {
                    return false;
                }
            }

            return true;
        }

        void affectEntities() {
            World world = location.getWorld();
            BoundingVolume bv = AABB.at(location, hitboxSize);
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();

            for (Entity e : Entities.getNearbyEntities(world, bv)) {
                if (!noAffect.contains(e)) {
                    effectHandler.damageEntity(e, WaterGimbal.this, damage);
                }
            }
        }
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = WaterGimbalController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 8000;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 16;
        @Configure(path = CONFIG_PATH + "RingSize", config = Config.ABILITIES)
        private double ringSize = 3.5;
        @Configure(path = CONFIG_PATH + "AnimAngle", config = Config.ABILITIES)
        private double animAngle = 25;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 30;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 2.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.5;
        @Configure(path = CONFIG_PATH + "AllowIceSource", config = Config.ABILITIES)
        private boolean allowIceSource = false;
        @Configure(path = CONFIG_PATH + "AllowSnowSource", config = Config.ABILITIES)
        private boolean allowSnowSource = false;
        @Configure(path = CONFIG_PATH + "AllowPlantSource", config = Config.ABILITIES)
        private boolean allowPlantSource = false;
    }
}
