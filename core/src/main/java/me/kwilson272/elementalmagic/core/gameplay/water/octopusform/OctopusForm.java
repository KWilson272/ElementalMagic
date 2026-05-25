package me.kwilson272.elementalmagic.core.gameplay.water.octopusform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.components.TravelingSource;
import me.kwilson272.elementalmagic.core.gameplay.components.TravelingSource.TravelState;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterUtil;

public class OctopusForm extends CoreAbility {

    protected static ConfigValues CONFIG = new ConfigValues();

    private enum State {
        SOURCED,
        SOURCE_TRAVELING,
        FORM
    }

    private long cooldown;
    private double selectRange;
    private long duration;
    private double angleInc;
    private double radius;
    private double damage;
    private double hitboxSize;
    private double knockback;
    private double knockup;

    private State state;
    
    private Block source;
    private TravelingSource travelingSource;
    
    private int tick;
    private boolean isInfinite;
    private long endTime;
    private double angle; 
    private double angleOffset;
    private Map<Block, TempBlock> affectedBlocks;

    public OctopusForm(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        duration = CONFIG.duration;
        angleInc = CONFIG.angleInc;
        radius = CONFIG.radius;
        damage = CONFIG.damage;
        hitboxSize = CONFIG.hitboxSize;
        knockback = CONFIG.knockback;
        knockup = CONFIG.knockup;

        state = State.SOURCED;

        affectedBlocks = new HashMap<>();
    }

	@Override
	public boolean start() {
        angleInc = Math.toRadians(angleInc);
        source = WaterUtil.getSourceBlock(user(), selectRange);
        return source != null;
	}

	@Override
	public boolean progress() {
	    if (!user().canUse(controller(), true, false)) {
            return false;
        }

        return switch(state) {
            case SOURCED -> progressSourced();
            case SOURCE_TRAVELING -> progressSourceTraveling();
            case FORM -> progressForm();
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
        Location loc = user().player().getEyeLocation();
        Location sourceLoc = source.getLocation().add(0.5, 0.5, 0.5);
        double maxDist = Math.pow(selectRange + 1, 2);

        return loc.getWorld().equals(source.getWorld())
            && loc.distanceSquared(sourceLoc) <= maxDist
            && WaterUtil.canUse(source, user());
    }

    private void initSourceTraveling() {
        BlockData data = Material.WATER.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data);
        Location location = source.getLocation();
        travelingSource = new TravelingSource(location, 1, false, builder); 
        
        state = State.SOURCE_TRAVELING;
    }

    private boolean progressSourceTraveling() {
        Location dest = user().player().getLocation();
        var tState = travelingSource.moveTowards(dest, radius);
        if (tState == TravelState.ARRIVED) {
            initForm(travelingSource.getLocation());
            travelingSource.revertBlocks();
            travelingSource = null;
        }

        return tState != TravelState.BLOCKED;
    }

    private void initForm(Location location) {
        Location feet = user().player().getLocation();
        Vector toSource = VectorUtil.getDirection(feet, location);
        double angle = Math.atan2(-toSource.getX(), toSource.getZ());
        double yaw = user().player().getEyeLocation().getYaw();
        angleOffset = Math.toRadians(yaw) - angle;
        
        tick = 0;
        isInfinite = duration < 0;
        endTime = System.currentTimeMillis() + duration;

        state = State.FORM;
    }

    private boolean progressForm() {
        if (!user().player().isSneaking() 
                || (!isInfinite && System.currentTimeMillis() > endTime)){
            return false;
        }
        
        revertOctopus();
        drawOctopus();

        return true;
    }

    private void revertOctopus() {
        for (TempBlock tb : affectedBlocks.values()) {
            ElementalMagicApi.revertibleManager().revert(tb);
        }
        affectedBlocks.clear();
    }

    private void drawOctopus() {
        tick++;
        angle += angleInc;
        angle = Math.min(angle, 2 * Math.PI);

        Location center = user().player().getLocation();
        Location eyes = user().player().getEyeLocation();
        double startAngle = Math.toRadians(eyes.getYaw()) + angleOffset;

        double spacing = 0.5;
        double step = 2 * Math.asin(spacing / (2 * radius));
        int count = (int) Math.ceil(angle / step);
        
        for (int i = 0; i < count; ++i) {
            double rad = startAngle + (i * step);
            double x = -Math.sin(rad) * radius;
            double z = Math.cos(rad) * radius;
            Block b = center.clone().add(x, 0, z).getBlock();

            if (!BlockUtil.isSolid(b)) {
                addWater(b, 0);
            }
        }

        if (angle != Math.PI * 2) {
            return;
        }

        double armSpacing = Math.PI * 2 / 8;
        for (int i = 0; i < 8; ++i) {
            double rad = startAngle + (i * armSpacing);
            double x = -Math.sin(rad) * radius;
            double z = Math.cos(rad) * radius;
            Block b = center.clone().add(x, 0, z).getBlock();
            drawArm(b, tick + i);
        }
    }

    private void addWater(Block block, int fillLevel) {
        if (!affectedBlocks.containsKey(block)) {
            BlockData data = Material.WATER.createBlockData();
            ((Levelled) data).setLevel(fillLevel);
            TempBlock.builder(this, data).buildAt(block)
                .ifPresent(tb -> affectedBlocks.put(block, tb));
        }
    }

    private void drawArm(Block block, int animIndex) {
        animIndex = (animIndex / 3) % 3;

        block = block.getRelative(BlockFace.UP);
        addWater(block, 0);
        block = block.getRelative(BlockFace.UP);
        addWater(block, animIndex == 0 ? 0 : 1);

        if (animIndex != 0) {
            Location center = user().player().getEyeLocation();
            Location blockLoc = block.getLocation().add(0.5, 0.5, 0.5);
            Vector toCenter = VectorUtil.getDirection(blockLoc, center);
            toCenter.setY(0);
            toCenter.normalize().multiply(animIndex == 1 ? -1 : 1);
            addWater(blockLoc.add(toCenter).getBlock(), 0);
        }
    }

    protected boolean isSourced() {
        return state == State.SOURCED;
    }

    protected void attack() {
        if (angle < Math.PI * 2) {
            return;
        }

        Location center = user().player().getLocation();
        Set<Entity> noAffect = new HashSet<>();
        noAffect.add(user().player());

        for (Block b : affectedBlocks.keySet()) {
            BoundingVolume bv = AABB.fromBlock(b, hitboxSize); 
            Vector knock = VectorUtil.getDirection(center, b.getLocation());
            knock.normalize();
            knock.setY(knockup);
            knock.multiply(knockback);
            
            World world = b.getWorld();
            for (Entity e : EntityUtil.getNearbyEntities(world, bv)) {
                if (!noAffect.contains(e)) {
                    ElementalMagicApi.effectHandler().setVelocity(e, this, knock);
                    ElementalMagicApi.effectHandler().damageEntity(e, this, damage);
                }
            }
        }
    }

	@Override
	public void onDestruction() {
        if (state == State.FORM) {
            user().addCooldown("OctopusForm", cooldown);
        }

        if (travelingSource != null) {
            travelingSource.revertBlocks();
        }
        
        revertOctopus();
	}

    @Override
    public String name() {
        return "OctopusForm";
    }

    protected static class ConfigValues {
    
        private static final String CONFIG_PATH = OctopusFormController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 14;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = -1;
        @Configure(path = CONFIG_PATH + "AngleIncrement", config = Config.ABILITIES)
        private double angleInc = 45;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 4;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.0;
        @Configure(path = CONFIG_PATH + "Knoackback", config = Config.ABILITIES)
        private double knockback = 1.2;
        @Configure(path = CONFIG_PATH + "Knockup", config = Config.ABILITIES)
        private double knockup = 0.2;
    }
}
