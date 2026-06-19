package me.kwilson272.elementalmagic.core.gameplay.earth.earthblast;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class EarthBlast extends EarthAbility {
    
    protected static final ConfigValues CONFIG = new ConfigValues();

    private enum State {
        SOURCED,
        SETTING_UP,
        TRAVELING
    }

    private long cooldown;
    private long sourceRevertTime;
    private double selectRange;
    private double range;
    private double speed;
    private double damage;
    private double knockback;
    private double hitboxSize;
    private double redirectRange;
    private int cpsCap;
    private int setupTicks;
    private boolean canHitOwner;

    private State state;
    private AbilityUser originalOwner;
    private World world;
    private int ticksLived;

    private Location tether;
    private Location location;
    private Vector toTarget;
    private Block setupBlock;
    private Block destination;

    private Block source;
    private TempBlock sourceTemp;
    private TempBlockBuilder builder;
    private TempBlock blastBlock;

    private Map<AbilityUser, Queue<Long>> clickTracker;

	public EarthBlast(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        sourceRevertTime = CONFIG.sourceRevertTime;
        selectRange = CONFIG.selectRange;
        range = CONFIG.range;
        speed = CONFIG.speed;
        damage = CONFIG.damage;
        knockback = CONFIG.knockback;
        hitboxSize = CONFIG.hitboxSize;
        redirectRange = CONFIG.redirectRange;
        cpsCap = CONFIG.cpsCap;
        setupTicks = CONFIG.setupTicks;
        canHitOwner = CONFIG.canHitOwner;

        state = State.SOURCED;
        originalOwner = user;
        world = user.player().getWorld();
        ticksLived = 0;

	    clickTracker = new HashMap<>();
    }

	@Override
	public boolean start() {
        source = selectSource(selectRange);
        if (source == null) {
            return false;
        }

        BlockData data = source.getBlockData();
        builder = TempBlock.builder(this, data);

        BlockData focusData = getSourceFocusData(source);
        sourceTemp = TempBlock.builder(this, focusData)
            .buildAt(source)
            .orElse(null);

        location = source.getLocation().add(0.5, 0.5, 0.5);
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), isSourced(), false)) {
            return false;
        }
        
        if (state == State.SOURCED) {
            return isSourceViable(); 
        } else {
            ++ticksLived;
            removeExpiredClicks();
            return advanceLocation();
        }
	}

    private boolean isSourceViable() {
        Location eyeLoc = user().player().getEyeLocation();
        Location sourceLoc = source.getLocation().add(0.5, 0.5, 0.5);
        double maxdist = Math.pow(selectRange + 2, 2);
        return eyeLoc.getWorld().equals(sourceLoc.getWorld())
            && eyeLoc.distanceSquared(sourceLoc) <= maxdist;
    }

    private boolean advanceLocation() {
        double remainder = speed;
        while (remainder > 0) {
            double travel = Math.min(remainder, 1);
            remainder -= 1;

            Vector dir = getDirection();
            Location prev = location.clone();
            location.add(dir.clone().multiply(travel));

            Block oldBlock = prev.getBlock();
            Block newBlock = location.getBlock(); 
            if (!oldBlock.equals(newBlock)) {
                if (collidesWith(newBlock) 
                        || Blocks.collidesDiagonally(prev, location, this::collidesWith)) {
                    return false;
                }
                if (blastBlock != null) {
                    ElementalMagicApi.revertibleManager().revert(blastBlock);
                    blastBlock = builder.buildAt(newBlock).orElse(null);
                }
            }
            
            if (state == State.SETTING_UP && newBlock.getY() == setupBlock.getY()) {
                Location from = newBlock.getLocation();
                Location to = destination.getLocation();
                toTarget = Vectors.getDirection(from, to).normalize();
                state = State.TRAVELING;
            }

            if (hitEntities(dir)
                    || newBlock.equals(destination)
                    || location.distanceSquared(tether) >= range * range) { 
                return false;
            }
        }

        return true;
    }

    private Vector getDirection() {
        if (state == State.TRAVELING) {
            return toTarget.clone();
        } else if (ticksLived <= setupTicks) {
            return getSafeDirection(); 
        } else {
            double yDiff = setupBlock.getY() - location.getBlockY();
            return new Vector(0, Math.copySign(1, yDiff), 0);
        }
    }

    private Vector getSafeDirection() {
        double yDiff = setupBlock.getY() - location.getBlockY();
        double y = Math.copySign(1, yDiff);

        Vector setupDir = new Vector(0, y, 0);
        Block next = location.clone().add(setupDir).getBlock();
        if (!collidesWith(next)) {
            return setupDir; 
        }

        // Use block locations to make direction more consistent
        Location from = location.getBlock().getLocation();
        Location to = destination.getLocation();
        Vector toTarget = Vectors.getDirection(from, to);
        next = location.clone().add(toTarget.setY(0).normalize()).getBlock();
        if (!collidesWith(next)) {
            return toTarget;
        }
    
        return new Vector(0, -y, 0);
    }

    private boolean collidesWith(Block block) {
        if (!Blocks.isSolid(block) && !Blocks.isLiquid(block)) {
            return false;
        }

        // Avoid the blast colliding with itself
        return blastBlock == null || !block.equals(blastBlock.block());
    }

    private boolean hitEntities(Vector direction) {
        Vector knock = direction.multiply(knockback);
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        boolean affected = false;

        for (Entity e : Entities.getNearbyEntities(location, hitboxSize)) {
            if (!e.equals(user().player()) 
                    && (canHitOwner || !e.equals(originalOwner.player()))) {
                affected |= effectHandler.setVelocity(e, this, knock);
                affected |= effectHandler.damageEntity(e, this, damage);
            }
        }

        return affected;
    }

	@Override
	public void onDestruction() {
        if (sourceTemp != null) {
            ElementalMagicApi.revertibleManager().revert(sourceTemp);
        }

        if (blastBlock != null) {
            ElementalMagicApi.revertibleManager().revert(blastBlock);
        }
	}

	@Override
	public String name() {
        return "EarthBlast";
	}

    protected boolean isSourced() {
        return state == State.SOURCED;
    }

    protected void handleLeftClick(AbilityUser user) {
        if (!canRedirect(user)) {
            return;
        }
    
        Player player = user.player();
        World world = player.getWorld();

        Block block = Entities.getTargetBlock(player, range, this::collidesWith);

        Location start = player.getEyeLocation();
        Vector dir = start.getDirection();
        RayTraceResult trace = world.rayTraceEntities(start, dir, range, 1.5,
                e -> canTarget(user, e, block));
        Entity entity = trace != null ? trace.getHitEntity() : null;
        
        redirect(user, block, entity);
    }

    private boolean canRedirect(AbilityUser user) {
        if (!user.player().getWorld().equals(world) 
                || isAtCPSCap(user)
                // Attempt to prevent the user from redirecting blasts into 
                // the hole it rises out of
                || (state == State.SETTING_UP && ticksLived <= setupTicks)) {
            return false;        
        }

        if (!user.equals(user()) 
                && (isSourced() || !isInUsersSight(user))) {
            return false;
        }

        Location eye = user.player().getEyeLocation();
        return eye.distanceSquared(location) <= range * range; 
    }

    private boolean canTarget(AbilityUser user, Entity entity, Block targetBlock) {
        if (entity.equals(user.player()) 
                || !(entity instanceof LivingEntity le)
                || !ElementalMagicApi.effectHandler().canAffect(entity)) { 
            return false;
        }

        Location eye = user.player().getEyeLocation();
        Location blockLoc = targetBlock.getLocation().add(0.5, 0.5, 0.5);
        double blockDist = eye.distanceSquared(blockLoc);
        return le.getLocation().distanceSquared(eye) <= blockDist 
            || le.getEyeLocation().distanceSquared(eye) <= blockDist;
    }

    public boolean isInUsersSight(AbilityUser user) {
        Location eye = user.player().getEyeLocation();
        Vector dir = eye.getDirection();
        Block block = location.getBlock();
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        Vector toCenter = Vectors.getDirection(eye, center);

        double distance = toCenter.length();
        if (distance == 0 || !Double.isFinite(distance)) {
            return false;
        }

        double bound = Math.atan(redirectRange / distance);
        double angle = dir.angle(toCenter);
        return angle <= bound;
    }

    private void redirect(AbilityUser user, Block targetBlock, Entity targetEntity) {
        AbilityManager abilManager = ElementalMagicApi.abilityManager();
        if (!user().equals(user) && !abilManager.changeOwner(this, user)) {
            return;
        }

        // prevent immediately sending the blast into the ground if its 

        Location targetLoc = targetEntity != null ?
                targetEntity.getLocation() : targetBlock.getLocation();

        if (targetEntity == null) {
            // If we go with the exact target block, players will send blasts into
            // the ground when really they wanted it to glide across the floor.
            // We will try to check if the player was looking at the top of the block
            // and raise the target location if so, to make it feel better to use
            Player player = user.player();
            BlockFace face = Entities.getTargetFace(player, targetBlock);
            if (face == BlockFace.UP) {
                targetLoc.add(0, 1, 0);
            }
        }

        destination = targetLoc.getBlock();

        if (state == State.SETTING_UP) {
            // Allow the user to bypass the blast rising the complete amount
            // as long as the blast is outside the setup tick window, which
            // is checked prior to this method call
            state = State.TRAVELING;
        }

        if (state == State.SOURCED) {
            consumeSource();
            setupBlock = setUpFromSource(targetLoc);
            user.addCooldown(name(), cooldown);
            playEarthSound(location);
            state = State.SETTING_UP;

        } else {
            Location start = location.getBlock().getLocation();
            Location end = destination.getLocation();
            toTarget = Vectors.getDirection(start, end).normalize();
        }


        // If the final dest is an entity location and the entity moves away, the
        // blast will still remove even if the entity isn't there AND the ability
        // is not out of range. Attempt to mitigate this as it feels 'buggy'
        if (targetEntity != null) {
            Location loc = state == State.SETTING_UP ?
                setupBlock.getLocation() : location.getBlock().getLocation();
            Vector toDest = Vectors.getDirection(loc, destination.getLocation());
            toDest.normalize().multiply(range);
            destination = loc.clone().add(toDest).getBlock();
        }

        tether = user.player().getEyeLocation();
        logClick(user);
    }

    public Block setUpFromSource(Location target) {
        double riseY = target.getBlockY();
        double yDiff = target.getBlockY() - location.getBlockY();
        if (yDiff <= 2) {
            riseY = location.getBlockY() + 2;
        }
        Location riseLoc = location.clone();
        riseLoc.setY(riseY);

        blastBlock = builder.buildAt(location.getBlock()).orElse(null);
        return riseLoc.getBlock();
    }

    private void consumeSource() {
        if (sourceTemp != null) {
            ElementalMagicApi.revertibleManager().revert(sourceTemp);
            sourceTemp = null;
        }

        BlockData data = Material.AIR.createBlockData();
        TempBlock.builder(this, data)
            .setDuration(sourceRevertTime)
            .buildAt(source);
    }

    private void removeExpiredClicks() {
        long time = System.currentTimeMillis();
        for (AbilityUser user : clickTracker.keySet()) {
            Queue<Long> clickTimes = clickTracker.get(user);
            while (!clickTimes.isEmpty() &&
                    // Poll regardless if this is the only user in the queue so
                    // stockpiled clicks don't impact later fights
                    (clickTimes.peek() < time || clickTracker.size() == 1)) {
                clickTimes.poll();
            }
        }
    }

    private void logClick(AbilityUser user) {
        Queue<Long> clickTimes = 
            clickTracker.computeIfAbsent(user, k -> new ArrayDeque<>());
        long oneSecond = 1000;
        clickTimes.add(System.currentTimeMillis() + oneSecond);
    }

    private boolean isAtCPSCap(AbilityUser user) {
        Queue<Long> clickTimes =
                clickTracker.computeIfAbsent(user, k -> new ArrayDeque<>());
        return clickTracker.size() > 1 && clickTimes.size() >= cpsCap;
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = EarthBlastController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 900;
        @Configure(path = CONFIG_PATH + "SourceRevertTime", config = Config.ABILITIES)
        private long sourceRevertTime = 10000;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 9.0;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 20.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "Knockback", config = Config.ABILITIES)
        private double knockback = 0.3;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.2;
        @Configure(path = CONFIG_PATH + "RedirectRange", config = Config.ABILITIES)
        private double redirectRange = 3.0;
        @Configure(path = CONFIG_PATH + "CpsCap", config = Config.ABILITIES)
        private int cpsCap = 7;
        @Configure(path = CONFIG_PATH + "SetupTicks", config = Config.ABILITIES)
        private int setupTicks = 2;
        @Configure(path = CONFIG_PATH + "CanHitOwner", config = Config.ABILITIES)
        private boolean canHitOwner = false;
    }
}
