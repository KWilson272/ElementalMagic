package me.kwilson272.elementalmagic.core.gameplay.water.torrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.water.WaterAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class TorrentWave extends WaterAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();
    private static final double SPEED = 0.5;

    private long cooldown;
    private int maxRadius;
    private double curRadius;
    private double knockBack;
    private double hitboxSize;

    private List<Point> wavePoints;
    private Map<Block, TempBlock> waveBlocks;

    public TorrentWave(AbilityUser user, AbilityController controller) {
        super(user, controller);
        cooldown = CONFIG.cooldown;
        maxRadius = CONFIG.maxRadius;
        curRadius = 0;
        knockBack = CONFIG.knockBack;
        hitboxSize = CONFIG.hitboxSize;

        wavePoints = new ArrayList<>();
        waveBlocks = new HashMap<>();
    }

    @Override
    public boolean start() {
        initWave();

        // Rather than spamming clients with packets every tick we're going to
        // play a few here one time to give it a similar 'thick sound' feel
        Location loc = user().player().getLocation();
        playWaterSound(loc);
        playWaterSound(loc);
        playWaterSound(loc);

        user().addCooldown("TorrentWave", cooldown);
        return true;
    }

    private void initWave() {
        // The ring starts to look weird when the player is off-center
        Block block = user().player().getEyeLocation().getBlock();
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);

        double spacing = 1.0;
        double step = 2 * Math.asin(spacing / (2 * maxRadius));
        int count = (int) Math.ceil(Math.toRadians(360) / step);
        for (Vector v : Vectors.getRing(count)) {
            v.multiply(SPEED);
            wavePoints.add(new Point(loc.clone(), v));
            wavePoints.add(new Point(loc.clone().add(0, 1, 0), v));
            wavePoints.add(new Point(loc.clone().add(0, -1, 0), v));
        }
    }

    @Override
    public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }
        
        manageWave();
        curRadius += SPEED;
        return !waveBlocks.isEmpty() && curRadius < maxRadius;
    }

    private void manageWave() {
        BlockData data = Material.WATER.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data);
        Set<Block> toRevert = new HashSet<>(waveBlocks.keySet());

        Iterator<Point> pointIter = wavePoints.iterator();
        while (pointIter.hasNext()) {
            Point point = pointIter.next();
            Location loc = point.loc;
            Vector vec = point.dir;

            Block block = loc.getBlock();
            if (Blocks.isSolid(block)) {
                pointIter.remove();
                continue;
            }

            toRevert.remove(block);
            if (!waveBlocks.containsKey(block)) {
                builder.buildAt(block).ifPresent(tb -> waveBlocks.put(block, tb));
            }

            affectEntities(block, vec);
            loc.add(vec);
        }

        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        toRevert.forEach(b -> revertManager.revert(waveBlocks.remove(b)));
    }

    private void affectEntities(Block block, Vector dir) {
        World world = block.getWorld();
        // effectively normalize().multiply(knockBack)
        Vector knock = dir.clone().multiply(knockBack/SPEED);
        BoundingVolume bv = AABB.fromBlock(block, hitboxSize);

        for (Entity e : Entities.getNearbyEntities(world, bv)) {
            if (!e.equals(user().player())) {
                Vector vec = e.getVelocity().add(knock);
                ElementalMagicApi.effectHandler().setVelocity(e, this, vec);
            }
        }
    }

    @Override
    public void onDestruction() {
        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        waveBlocks.values().forEach(revertManager::revert);
    }

    @Override
    public String name() {
        return "TorrentWave";
    }

    private record Point(Location loc, Vector dir) {}

    protected static class ConfigValues {

        private static final String BASE_PATH = TorrentController.CONFIG_PATH + "Wave.";

        @Configure(path = BASE_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = BASE_PATH + "MaxRadius", config = Config.ABILITIES)
        private int maxRadius = 10;
        @Configure(path = BASE_PATH + "KnockBack", config = Config.ABILITIES)
        private double knockBack = 2.0;
        @Configure(path = BASE_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.5;
    }
}

