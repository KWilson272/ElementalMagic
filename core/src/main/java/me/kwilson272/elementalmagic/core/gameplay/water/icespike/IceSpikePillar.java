package me.kwilson272.elementalmagic.core.gameplay.water.icespike;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.water.WaterAbility;
import me.kwilson272.elementalmagic.core.gameplay.water.WaterUsePolicy;
import me.kwilson272.elementalmagic.core.util.Entities;

public class IceSpikePillar extends WaterAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double selectRange;
    private int height;
    private double damage;
    private double knockUp;
    private double hitboxSize;

    private IcePillar pillar;

    public IceSpikePillar(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        height = CONFIG.height;
        damage = CONFIG.damage;
        knockUp = CONFIG.knockUp;
        hitboxSize = CONFIG.hitboxSize;
    }

    @Override
    public boolean start() {
        // Allow the player to target entities instead of the blocks beneath them;
        // it feels much nicer this way
        Player player = user().player();
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection();

        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        RayTraceResult result = world.rayTraceEntities(start, dir, selectRange, 1.25,
                e -> !e.equals(player) && effectHandler.canAffect(e));

        Block source = null;
        if (result != null && result.getHitEntity() != null) {
            Entity entity = result.getHitEntity();
            source = entity.getLocation().getBlock().getRelative(BlockFace.DOWN);
        }
        
        WaterUsePolicy opts = WaterUsePolicy.from(user())
            .setWater(false)
            .setPlant(false);

        if (source == null || !canUse(source, opts)) {
            source = selectSourceBlock(selectRange, opts);
        }

        if (source != null) {
            pillar = new Pillar(height, source, this);
            playIceSound(source.getLocation());
            return true;
        }
        return false;
    }

    @Override
    public boolean progress() {
        return user().canUse(controller(), false, false) && pillar.progress();
    }

    @Override
    public void onDestruction() {
        user().addCooldown("IceSpikePillar", cooldown);
        pillar.revertAll();
    }

    @Override
    public String name() {
        return "IceSpikePillar";
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
                    effectHandler.setVelocity(e, IceSpikePillar.this, knock);
                    effectHandler.damageEntity(e, IceSpikePillar.this, damage);
                }
            }
		}
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = IceSpikeController.CONFIG_PATH + "Pillar.";

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 1000;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 18.0;
        @Configure(path = CONFIG_PATH + "Height", config = Config.ABILITIES)
        private int height = 3;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "KnockUp", config = Config.ABILITIES)
        private double knockUp = 2.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.2;
    }
}
