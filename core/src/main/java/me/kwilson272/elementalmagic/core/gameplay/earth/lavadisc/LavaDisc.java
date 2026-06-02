package me.kwilson272.elementalmagic.core.gameplay.earth.lavadisc;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.collision.Sphere;
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

public class LavaDisc extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private enum State {
        HOLDING,
        RECALLING,
        FIRED
    }

    private long cooldown;
    private double holdRange;
    private int maxRecalls;
    private long sourceRevertTime;
    private double speed;
    private double range;
    private double affectRadius;
    private double damage;
    private long burnDuration;
    private double verticalSensitivity;
    private double horizontalSensitivity;
    private double blockDamageRadius;
    private long blockDamageDuration;

    private State state;
    private int recallCounter;
    private double animAngle;

    private double iterSpeed;
    private double rangeCounter;
    private Location location;
    private Vector direction;

    public LavaDisc(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        holdRange = CONFIG.holdRange;
        maxRecalls = CONFIG.maxRecalls;
        sourceRevertTime = CONFIG.sourceRevertTime;
        speed = CONFIG.speed;
        range = CONFIG.range;
        affectRadius = CONFIG.affectRadius;
        damage = CONFIG.damage;
        burnDuration = CONFIG.burnDuration;
        verticalSensitivity = CONFIG.verticalSensitivity;
        horizontalSensitivity = CONFIG.horizontalSensitivity;
        blockDamageRadius = CONFIG.blockDamageRadius;
        blockDamageDuration = CONFIG.blockDamageDuration;
    
        state = State.HOLDING;
        recallCounter = 0;
        animAngle = 0;
    }

	@Override
	public boolean start() {
	    Block source = selectSource(holdRange);
        if (source == null) {
            return false;
        }

        BlockData data = Material.LAVA.createBlockData();
        ((Levelled) data).setLevel(2);
        TempBlock.builder(this, data)
            .setUsable(true)
            .setDuration(sourceRevertTime)
            .buildAt(source);
        
        iterSpeed = calculateIterSpeed();
        location = source.getLocation().add(0.5, 0.5, 0.5);
        return true;
    }

    private double calculateIterSpeed() {
        if (speed <= 1.0) {
            return speed;
        } else {
            int toRound = ((int) speed) + 5;
            double tenMod = Math.round(toRound / 10.0);
            return speed / (Math.floor(speed) + tenMod + 1);
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), true, false)) {
            return false;
        }
       
        if (state == State.HOLDING) {
            progressHolding();
            return true;
        }

        return advanceLocation();
    }

    private void renderDisc(Location loc) {
        World world = location.getWorld();

        Color hotter = Color.fromRGB(255, 100, 0);
        Color colder = Color.fromRGB(60, 20, 0);
    
        animAngle += Math.toRadians(10);
        double baseAngle = animAngle;
        double increment = Math.toRadians(7);
        double maxRadius = 0.8;
        for (double radius = 0; radius < maxRadius; radius += 0.1) {
            baseAngle -= increment;
    
            double t = (maxRadius - radius) / maxRadius;
            int r = (int) ((t * hotter.getRed()) + ((1 - t) * colder.getRed()));
            int g = (int) ((t * hotter.getGreen()) + ((1 - t) * colder.getGreen()));
            int b = (int) ((t * hotter.getBlue()) + ((1 - t) * colder.getBlue()));
            Color color = Color.fromRGB(r, g, b);
            DustOptions options = new DustOptions(color, 0.6f);

            int arms = 5;
            double step = 2 * Math.PI / arms;
            for (int i = 0; i < 5; ++i) {
                double angle = baseAngle + (step * i);
                Vector dir = Vectors.fromRadians(angle, 0).multiply(radius);
                Location disp = loc.clone().add(dir);
                world.spawnParticle(Particle.DUST, disp, 1, options); 
            }
        }

        if (ThreadLocalRandom.current().nextInt(15) == 0) {
            world.spawnParticle(Particle.LAVA, loc, 2, 0.3, 0, 0.3);
        }
    }

    private void progressHolding() {
        Player player = user().player();
        location = Entities.getTargetLocation(player, holdRange);
        renderDisc(location);

        if (!player.isSneaking()) {
            direction = user().player().getEyeLocation().getDirection();
            rangeCounter = range;
            state = State.FIRED;
        }
    }

    private boolean advanceLocation() {
        for (double i = 0; i < speed; i += iterSpeed) {
            direction = getDirection();
            Location prev = location.clone();
            location.add(direction.clone().multiply(iterSpeed));

            breakBlocks(location);
            if (Blocks.isSolid(location.getBlock()) 
                    || Blocks.collidesDiagonally(prev, location, Blocks::isSolid)) {
                return false;        
            }

            renderDisc(location);
            damageEntities(location);

            Player player = user().player();
            if (state == State.RECALLING) {
                Location hold = Entities.getTargetLocation(player, holdRange);
                if (location.distanceSquared(hold) <= 0.7 * 0.7) {
                    state = State.HOLDING;
                    return true;
                } else if (!player.isSneaking()) {
                    state = State.FIRED;
                    rangeCounter = range - location.distance(hold);
                }
            
            } else {
                rangeCounter -= iterSpeed;
                if (rangeCounter <= 0) {
                    return false;
                }       
                if (player.isSneaking() && recallCounter < maxRecalls) {
                    state = State.RECALLING;
                    recallCounter++;
                }
            }
        }

        return true;
    }

    private Vector getDirection() {
        if (state == State.RECALLING) {
            Player player = user().player();
            Location hold = Entities.getTargetLocation(player, holdRange);
            return Vectors.getDirection(location, hold).normalize();
        }

        Location eyeLoc = user().player().getEyeLocation();
        Vector target = Vectors.fromRotations(0, eyeLoc.getYaw());
        target.setY(eyeLoc.getDirection().getY());

        double vFactor = verticalSensitivity / 100;
        double hFactor = horizontalSensitivity / 100;
        double x = (hFactor * target.getX()) + ((1 - hFactor) * direction.getX());
        double y = (vFactor * target.getY()) + ((1 - vFactor) * direction.getY());
        double z = (hFactor * target.getZ()) + ((1 - hFactor) * direction.getZ());
        
        return new Vector(x, y, z).normalize();
    }

    private void breakBlocks(Location loc) {
        BlockData data = Material.AIR.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data)
            .setDuration(blockDamageDuration);

        for (Block b : Blocks.collectSphere(loc, blockDamageRadius)) {
            if (canBreak(b)) {
                builder.buildAt(b);
            }
        }
    }

    private boolean canBreak(Block block) {
        if (block.getType() == Material.BARRIER 
                || block.getType() == Material.BEDROCK) {
            return false;
        }

        return Blocks.isPlant(block) 
            || Blocks.isSnow(block) 
            || Blocks.isSolid(block); 
    }

    private void damageEntities(Location loc) {
        World world = loc.getWorld();
        BoundingVolume bv = Sphere.at(loc, affectRadius);
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();

        for (Entity e : Entities.getNearbyEntities(world, bv)) {
            if (e.equals(user().player())) {
                continue;
            }

            effectHandler.damageEntity(e, this, damage);
            if (e.getFireTicks() * 50 < burnDuration) {
                effectHandler.setFireDuration(e, this, burnDuration);
            }
            world.spawnParticle(Particle.LAVA, loc, 5, 0.1, 0, 0.1);
        }
    }

	@Override
	public void onDestruction() {
	    user().addCooldown(name(), cooldown);
    }

	@Override
	public String name() {
        return "LavaDisc";
	}

    protected static class ConfigValues {
   
        protected static final String CONFIG_PATH = LavaDiscController.CONFIG_PATH;
    
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 7800;
        @Configure(path = CONFIG_PATH + "HoldRange", config = Config.ABILITIES)
        private double holdRange = 3.0;
        @Configure(path = CONFIG_PATH + "MaxRecalls", config = Config.ABILITIES)
        private int maxRecalls = 1;
        @Configure(path = CONFIG_PATH + "SourceRevertTime", config = Config.ABILITIES)
        private long sourceRevertTime = 10000;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.0;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 18.0;
        @Configure(path = CONFIG_PATH + "AffectRadius", config = Config.ABILITIES)
        private double affectRadius = 1.5;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 3.0;
        @Configure(path = CONFIG_PATH + "BurnDuration", config = Config.ABILITIES)
        private long burnDuration = 500;
        @Configure(path = CONFIG_PATH + "VerticalSensitivity", config = Config.ABILITIES)
        private double verticalSensitivity = 10;
        @Configure(path = CONFIG_PATH + "HorizontalSensitivity", config = Config.ABILITIES)
        private double horizontalSensitivity = 80;
        @Configure(path = CONFIG_PATH + "BlockDamageRadius", config = Config.ABILITIES)
        private double blockDamageRadius = 1.8;
        @Configure(path = CONFIG_PATH + "BlockDamageDuration", config = Config.ABILITIES)
        private long blockDamageDuration = 10000;
    }
}
