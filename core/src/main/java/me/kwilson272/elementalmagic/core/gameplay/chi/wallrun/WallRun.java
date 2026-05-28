package me.kwilson272.elementalmagic.core.gameplay.chi.wallrun;

import java.awt.Taskbar.Feature;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityStorage;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;

public class WallRun extends CoreAbility {

	protected static final ConfigValues CONFIG = new ConfigValues();

    private static final BlockFace[] CHECK_FACES = {
        BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST
    };

    private long cooldown;
    private long duration;
    private double power;
    private List<String> validElements;
    private List<Element> elements;

    private boolean isInfinite;
    private long endTime;

    public WallRun(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        power = CONFIG.power;
        validElements = CONFIG.validElements;
        elements = new ArrayList<>();
    }

	@Override
	public boolean start() {
        Location loc = user().player().getLocation();
        Block feetBlock = loc.getBlock();
        if (!user().player().isSprinting() && !AbilityUtil.isWater(feetBlock)) {
            return false;
        }

        cacheElements();
        if (!canWallRun()) {
            return false;
        }
    
        isInfinite = duration < 0;
        endTime = System.currentTimeMillis() + duration;
        pushPlayer();
        return true;
	}

    private void cacheElements() {
        AbilityStorage storage = ElementalMagicApi.abilityStorage();
        for (String s : validElements) {
            storage.getElement(s).ifPresent(elements::add);
        }
    }

    private boolean canWallRun() {
        boolean canUseAny = false;
        for (Element element : elements) {
            if (user().canUseElement(element)) {
                canUseAny = true;
                break;
            }
        }

        if (!canUseAny) {
            return false;
        }
         
        Location loc = user().player().getLocation();
        Block below = loc.add(0, -0.01, 0).getBlock();
        if (below.getType().isSolid()) {
            return false;
        }

        return isNextToSolidBlock(user().player().getLocation());
    }

    private boolean isNextToSolidBlock(Location loc) {
        for (BlockFace face : CHECK_FACES) {
            Block block = loc.getBlock().getRelative(face);
            Material material = block.getType();
            if (material.isSolid() && material != Material.BARRIER) {
                return true;
            }
        }
        return false;
    }

	@Override
	public boolean progress() {
        if (user().isOnCooldown(name()) || !canWallRun()
                || (!isInfinite && System.currentTimeMillis() > endTime)) {
            return false;
        }
        
        pushPlayer();
        return true;
	}

    private void pushPlayer() {
        Player player = user().player();
        Vector dir = player.getEyeLocation().getDirection().multiply(power);
        ElementalMagicApi.effectHandler().setVelocity(player, this, dir);

        World world = user().player().getWorld();
        Location loc = user().player().getLocation();
        BlockData data = Material.STONE.createBlockData();
        world.spawnParticle(Particle.BLOCK, loc, 2, 0.2, 0.2, 0.2, data);
        world.spawnParticle(Particle.CRIT, loc, 3, 0.2, 0.2, 0.2, 0); 
    }


	@Override
	public void onDestruction() {
        user().addCooldown(name(), cooldown);
	}

	@Override
	public String name() {
        return "WallRun";
	}
    
    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = WallRunController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = -1;
        @Configure(path = CONFIG_PATH + "Power", config = Config.ABILITIES)
        private double power = 1.15;
        @Configure(path = CONFIG_PATH + "ValidElements", config = Config.ABILITIES)
        private List<String> validElements = List.of("Air", "Fire", "Chi");
    }
}
