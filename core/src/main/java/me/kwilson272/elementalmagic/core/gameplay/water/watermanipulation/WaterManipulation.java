package me.kwilson272.elementalmagic.core.gameplay.water.watermanipulation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
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
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.components.BlockBlast;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterUtil;

public class WaterManipulation extends CoreAbility {

    private static final int STREAM_SIZE = 3;
    private static final int HEAD_LEVEL = 0;
    private static final int BODY_LEVEL = 1;

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long srcRevertTime;
    private double selectRange;
    private double range;
    private double damage;
    private double knockBack;
    private double hitboxSize;
    private double redirectRange;
    private int cpsCap;

    private World world;
    private Block source;
    private Location tether;
    private BlockBlast blast;

    private Deque<TempBlock> streamBlocks;
    private Map<AbilityUser, Queue<Long>> clickTracker;

    public WaterManipulation(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        srcRevertTime = CONFIG.srcRevertTime;
        selectRange = CONFIG.selectRange;
        range = CONFIG.range;
        damage = CONFIG.damage;
        knockBack = CONFIG.knockBack;
        hitboxSize = CONFIG.hitboxSize;
        redirectRange = CONFIG.redirectRange;
        cpsCap = CONFIG.cpsCap;
    
        world = user.player().getWorld();

        streamBlocks = new ArrayDeque<>(STREAM_SIZE);
        clickTracker = new HashMap<>();
    }

    @Override
    public boolean start() {
        source = WaterUtil.getSourceBlock(user(), selectRange);
        if (source == null) {
            return false;
        }

        blast = new Blast(source.getLocation(), range);
        return true;
    }

    protected boolean isSourced() {
        return blast.getState() == BlockBlast.State.SOURCED;
    }
    
    protected void handleLeftClick(AbilityUser user) {
        if (!canRedirect(user)) {
            return;
        }
    
        Player player = user().player();
        World world = player.getWorld();

        Block block = BlockUtil.getTargetBlock(player, range, BlockUtil::isSolid);

        Location start = player.getEyeLocation();
        Vector dir = start.getDirection();
        RayTraceResult trace = world.rayTraceEntities(start, dir, range, 1.5,
                e -> canTarget(user, e, block));
        Entity entity = trace != null ? trace.getHitEntity() : null;
        
        redirect(user, block, entity);
    }

    private boolean canRedirect(AbilityUser user) {
        if (!user.player().getWorld().equals(world) 
                || isAtCPSCap(user)) {
            return false;        
        }

        if (!user.equals(user()) 
                && (isSourced() || !blast.isTargetedBy(user, redirectRange))) {
            return false;
        }

        Location eye = user.player().getEyeLocation();
        Location wmLoc = blast.getLocation();
        double maxDist = range * range;
        return eye.distanceSquared(wmLoc) <= maxDist;
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

    private void redirect(AbilityUser user, Block targetBlock, Entity targetEntity) {
        AbilityManager abilManager = ElementalMagicApi.abilityManager();
        if (!user().equals(user) && !abilManager.changeOwner(this, user)) {
            return;
        }

        // If manip is fired from being sourced, the tether is based on the
        // source location, which players use to get a range boost. We need
        // to adjust the target block location so we can preserve this behavior
        // or the blast class will remove it 'prematurely'
        if (isSourced()) {
            // Block based distances feel better to players if centered
            tether = source.getLocation().add(0.5,0.5, 0.5);
            Player player = user.player();
            // This is technically overkill for most situations, the tether
            // distance check will ensure that the blast is removed accurately
            double max = selectRange + range;
            targetBlock = BlockUtil.getTargetBlock(player, max, BlockUtil::isSolid);
        } else {
            tether = user.player().getEyeLocation();
        }
        
        logClick(user());
        blast.redirect(user(), targetBlock, targetEntity);
    }

    @Override
    public boolean progress() {
        if (!user().canUse(controller(), isSourced(), false)) {
            return false;
        }
        
        if (!isSourced()) {
            removeExpiredClicks();
            return blast.progress() && !hitEntities()
                && tether.distanceSquared(blast.getLocation()) <= range * range;
        }
        
        WaterUtil.playSourceSelectedEffect(source);
        return isSourceViable();
    }

    private void removeExpiredClicks() {
        long time = System.currentTimeMillis();
        for (AbilityUser user : clickTracker.keySet()) {
            Queue<Long> clickTimes = clickTracker.get(user);
            while (!clickTimes.isEmpty() && clickTimes.peek() < time) {
                clickTimes.poll();
            }
        }
    }

    private boolean isAtCPSCap(AbilityUser user) {
        Queue<Long> clickTimes =
                clickTracker.computeIfAbsent(user, k -> new ArrayDeque<>());
        // Don't bother capping CPS if the manip isn't under contest, it may
        // make it feel more clunky than necessary
        return clickTracker.size() > 1 && clickTimes.size() >= cpsCap;
    }

    private void logClick(AbilityUser user) {
        Queue<Long> clickTimes = 
            clickTracker.computeIfAbsent(user, k -> new ArrayDeque<>());
        long oneSecond = 1000;
        clickTimes.add(System.currentTimeMillis() + oneSecond);
    }

    private boolean hitEntities() {
        boolean affected = false;

        Block block = blast.getLocation().getBlock();
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        Vector knock = blast.getDirection().multiply(knockBack);
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();

        for (Entity entity : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
            if (entity.equals(user().player())) {
                continue;
            }
            affected |= effectHandler.setVelocity(entity, this, knock);
            affected |= effectHandler.damageEntity(entity, this, damage);
        }
        return affected;
    }

    private boolean isSourceViable() {
        Location eyeLoc = user().player().getEyeLocation();
        Block blastBlock = blast.getLocation().getBlock();
        Location loc = blastBlock.getLocation().add(0.5, 0.5, 0.5);
        double dist = eyeLoc.distanceSquared(loc);
        // + 1 makes it less strict to those sourcing at the edge of the range
        double removalDist = Math.pow(selectRange + 1, 2);
        return dist <= removalDist && WaterUtil.canUse(source, user());
    }

    @Override
    public void onDestruction() {
        streamBlocks.forEach(tb -> {
            ElementalMagicApi.revertibleManager().revert(tb);
            Location loc = tb.block().getLocation().add(0.5, 0.5, 0.5);
            world.spawnParticle(Particle.FALLING_WATER, loc, 3, 0.3, 0.3, 0.3);
        });
    }

/*********************************
 * BLAST
 *********************************/
    private class Blast extends BlockBlast {

        public Blast(Location location, double range) {
            super(location, range);
        }

        @Override
        public boolean isCollidable(Block block) {
            return BlockUtil.isSolid(block);
        }

        @Override
        public void moveTo(Block block) {
            if (AbilityUtil.isWater(block)) {
                Location display = block.getLocation().add(0.5, 0.5, 0.5);
                world.spawnParticle(Particle.BUBBLE, display, 3, 0.5, 0.5, 0.5, 0);
            }
            
            if (!streamBlocks.isEmpty() && !streamBlocks.peekFirst().isReverted()
                    // Only adjust the data of the block we created
                    && TempBlock.isActive(streamBlocks.peekFirst())) {
                TempBlock oldHead = streamBlocks.peekFirst();
                Block b = oldHead.block();
                BlockData bData = WaterUtil.getFilledData(b, BODY_LEVEL);
                b.setBlockData(bData, false);
            }

            BlockData headData = WaterUtil.getFilledData(block, HEAD_LEVEL);
            TempBlock.builder(WaterManipulation.this, headData)
                    .setCollidable(false)
                    .buildAt(block)
                    .ifPresent(streamBlocks::offerFirst);

            while (streamBlocks.size() > STREAM_SIZE) {
                TempBlock tb = streamBlocks.pollLast();
                ElementalMagicApi.revertibleManager().revert(tb);
            }

            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            if (ThreadLocalRandom.current().nextInt(3) == 0) {
                world.spawnParticle(Particle.FALLING_WATER, loc, 2, 0.2, 0.2, 0.2, 0);
            }
        }

        @Override
        public Block setUpFromSource(Location target) {
            Location loc = blast.getLocation();
            double riseY = target.getBlockY();
            double yDiff = target.getBlockY() - loc.getBlockY();
            if (yDiff <= 2) {
                riseY = loc.getBlockY() + 2;
            }
            Location riseLoc = loc.clone();
            riseLoc.setY(riseY);

            WaterUtil.playWaterSound(loc);
            WaterUtil.consumeSource(WaterManipulation.this, loc.getBlock(), srcRevertTime);
            user().addCooldown(controller().name(), cooldown);

            // Ensure we render water AT the current block or manip will appear
            // to emerge from the block ABOVE the source
            moveTo(blast.getLocation().getBlock());
            return riseLoc.getBlock();
        }
    }

/*********************************
 * CONFIG
 *********************************/
    protected static class ConfigValues {

        private static final String CONFIG_PATH = WaterManipulationController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 650;
        @Configure(path = CONFIG_PATH + "SourceRevertTime", config = Config.ABILITIES)
        private long srcRevertTime = 10000;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 18.0;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 20.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "KnockBack", config = Config.ABILITIES)
        private double knockBack = 0.3;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.2;
        @Configure(path = CONFIG_PATH + "RedirectRange", config = Config.ABILITIES)
        private double redirectRange = 3.0;
        @Configure(path = CONFIG_PATH + "CpsCap", config = Config.ABILITIES)
        private int cpsCap = 7;
    }
}
