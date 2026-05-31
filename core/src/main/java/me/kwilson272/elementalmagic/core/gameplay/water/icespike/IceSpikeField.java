package me.kwilson272.elementalmagic.core.gameplay.water.icespike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.water.WaterAbility;
import me.kwilson272.elementalmagic.core.gameplay.water.WaterUsePolicy;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;

public class IceSpikeField extends WaterAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double radius;
    private int height;
    private double damage;
    private double knockUp;
    private double hitboxSize;

    private List<IcePillar> pillars;

    public IceSpikeField(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        radius = CONFIG.radius;
        height = CONFIG.height;
        damage = CONFIG.damage;
        knockUp = CONFIG.knockUp;
        hitboxSize = CONFIG.hitboxSize;
    }

    @Override
    public boolean start() {
        // We are going to prioritize sourcing blasts over fields since blasts
        // are used way more often.
        AbilityManager abilManager = ElementalMagicApi.abilityManager();
        if (abilManager.getUserAbilities(user(), IceSpikeBlast.class)
                .anyMatch(IceSpikeBlast::isSourced)) {
            return false;
        }

        List<Block> blocks = getBlocks();
        if (blocks.isEmpty()) {
            return false;
        }

        pillars = initPillars(blocks);
        if (!pillars.isEmpty()) {
            user().addCooldown("IceSpikeField", cooldown);
            return true;
        }
        return false;
    }

    private List<Block> getBlocks() {
        List<Block> blocks = new ArrayList<>();

        // Don't place blocks within this radius, or we might suffocate the player
        double safe = 1.5;
        WaterUsePolicy opts = WaterUsePolicy.from(user())
            .setWater(false)
            .setPlant(false);
        
        Location loc = user().player().getLocation();
        for (Block b : Blocks.collectCircle(loc, radius)) {
            Location center = b.getLocation().add(0.5, 0.5, 0.5);
            if (center.distanceSquared(loc) <= safe * safe) {
                continue;
            }

            // Get the first possible block that we could place a spike on
            // in the provided block column, to support diff/levels
            Block check = b.getRelative(BlockFace.DOWN, height-1);
            while (check.getY() - b.getY() < height) {
                if (!Blocks.isSolid(check.getRelative(BlockFace.UP))
                        && canUse(check, opts)) {
                    break;
                }
                check = check.getRelative(BlockFace.UP);
            }

            if (canUse(check, opts)) {
                blocks.add(check);
            }
        }

        return blocks;
    }

    private List<IcePillar> initPillars(List<Block> blocks) {
        List<IcePillar> pillars = new ArrayList<>();

        double percentage = 0.15; // Looks good, that's why
        // We need to ensure at least one spike gets created or players will
        // think that the move 'doesn't work' sometimes
        int max = Math.max((int) (blocks.size() * percentage), 1);

        Collections.shuffle(blocks);
        for (int i = 0; i < max; ++i) {
            Block block = blocks.get(i);
            pillars.add(new Pillar(height, block, this));
            if (ThreadLocalRandom.current().nextInt(max) == 0) {
                World world = block.getWorld();
                Location loc = block.getLocation();
                world.playSound(loc, Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
            }
        }

        return pillars;
    }

    @Override
    public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }
        pillars.removeIf(pillar -> !pillar.progress());
        return !pillars.isEmpty();
    }

    @Override
    public void onDestruction() {
        pillars.forEach(IcePillar::revertAll);
    }

    @Override
    public String name() {
        return "IceSpikeField";
    }

    private class Pillar extends IcePillar {

		public Pillar(int maxHeight, Block base, Ability ability) {
			super(maxHeight, base, ability);
		}

		@Override
		public void onBlockPlace(Block block) {
            World world = block.getWorld();
            BoundingVolume bv = AABB.fromBlock(block, hitboxSize);
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();
    
            Vector knock = new Vector(0, knockUp, 0);
            for (Entity e : Entities.getNearbyEntities(world, bv)) {
                if (!e.equals(user().player())) {
                    effectHandler.setVelocity(e, IceSpikeField.this, knock);
                    effectHandler.damageEntity(e, IceSpikeField.this, damage);
                }
            }
		}
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = IceSpikeController.CONFIG_PATH + "Field.";

        @Configure(path = CONFIG_PATH + "COOLDOWN", config = Config.ABILITIES)
        private long cooldown = 1000;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 5.0;
        @Configure(path = CONFIG_PATH + "Height", config = Config.ABILITIES)
        private int height = 3;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 1.0;
        @Configure(path = CONFIG_PATH + "KnockUp", config = Config.ABILITIES)
        private double knockUp = 1.5;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.2;
    }
}

