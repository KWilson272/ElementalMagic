package me.kwilson272.elementalmagic.core;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ElementalMagicPlugin;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityStorage;
import me.kwilson272.elementalmagic.core.user.UserManagerImpl;
import me.kwilson272.elementalmagic.core.ability.AbilityManagerImpl;
import me.kwilson272.elementalmagic.core.ability.AbilityStorageImpl;
import me.kwilson272.elementalmagic.core.ability.CoreElement;
import me.kwilson272.elementalmagic.core.activation.ActivationManagerImpl;
import me.kwilson272.elementalmagic.core.command.MasterCommand;
import me.kwilson272.elementalmagic.core.config.ConfigManagerImpl;
import me.kwilson272.elementalmagic.core.database.UserStorageImpl;
import me.kwilson272.elementalmagic.core.display.BoardManager;
import me.kwilson272.elementalmagic.core.effect.EffectHandlerImpl;
import me.kwilson272.elementalmagic.core.gameplay.water.fastswim.FastSwimController;
import me.kwilson272.elementalmagic.core.gameplay.water.frostbreath.FrostBreathController;
import me.kwilson272.elementalmagic.core.gameplay.water.hydrosink.HydroSinkController;
import me.kwilson272.elementalmagic.core.gameplay.water.iceblast.IceBlastController;
import me.kwilson272.elementalmagic.core.gameplay.water.iceskate.IceSkateController;
import me.kwilson272.elementalmagic.core.gameplay.water.icespike.IceSpikeController;
import me.kwilson272.elementalmagic.core.gameplay.water.icewall.IceWallController;
import me.kwilson272.elementalmagic.core.gameplay.water.icewave.IceWaveController;
import me.kwilson272.elementalmagic.core.gameplay.water.icicle.IcicleController;
import me.kwilson272.elementalmagic.core.gameplay.water.octopusform.OctopusFormController;
import me.kwilson272.elementalmagic.core.gameplay.water.phasechange.PhaseChangeController;
import me.kwilson272.elementalmagic.core.gameplay.water.plantwhip.PlantWhipController;
import me.kwilson272.elementalmagic.core.gameplay.water.surge.SurgeController;
import me.kwilson272.elementalmagic.core.gameplay.water.torrent.TorrentController;
import me.kwilson272.elementalmagic.core.gameplay.water.waterflow.WaterFlowController;
import me.kwilson272.elementalmagic.core.gameplay.water.watermanipulation.WaterManipulationController;
import me.kwilson272.elementalmagic.core.gameplay.water.waterspout.WaterSpoutController;
import me.kwilson272.elementalmagic.core.listener.SpoutListener;
import me.kwilson272.elementalmagic.core.revertible.RevertibleManagerImpl;
import me.kwilson272.elementalmagic.core.revertible.TempBlockListener;
import me.kwilson272.elementalmagic.core.activation.ActivationListener;
import me.kwilson272.elementalmagic.core.user.UserListener;

public class ElementalMagicPluginImpl extends ElementalMagicPlugin {
    
    private BukkitTask tickTask;

    @Override
    public void onLoad() {
        ElementalMagicApi.registerPlugin(this);
        ElementalMagicApi.registerActivationManager(new ActivationManagerImpl());
        ElementalMagicApi.registerAbilityManager(new AbilityManagerImpl());
        ElementalMagicApi.registerAbilityStorage(new AbilityStorageImpl());
        ElementalMagicApi.registerConfigManager(new ConfigManagerImpl());
        ElementalMagicApi.registerEffectHandler(new EffectHandlerImpl());
        ElementalMagicApi.registerLogger(this.getLogger());
        ElementalMagicApi.registerRevertibleManager(new RevertibleManagerImpl());
        ElementalMagicApi.registerUserManager(new UserManagerImpl());
        ElementalMagicApi.registerUserStorage(new UserStorageImpl());
    }

    @Override
    public void onEnable() {
        // Enable these first so the other managers can use it
        ElementalMagicApi.configManager().enable();
        ElementalMagicApi.userStorage().enable();
        storeCoreElements();
        storeCoreAbilities();

        Bukkit.getPluginManager().registerEvents(new ActivationListener(), this);
        Bukkit.getPluginManager().registerEvents(new BoardManager(), this);
        Bukkit.getPluginManager().registerEvents(new UserListener(), this);
        Bukkit.getPluginManager().registerEvents(new SpoutListener(), this);
        Bukkit.getPluginManager().registerEvents(new TempBlockListener(), this);
        tickTask = Bukkit.getScheduler().runTaskTimer(this, this::tick, 1, 1);

        Bukkit.getPluginCommand("elementalmagic").setExecutor(new MasterCommand());

        ElementalMagicApi.abilityManager().enable();
        ElementalMagicApi.abilityStorage().enable();
        ElementalMagicApi.activationManager().enable();
        ElementalMagicApi.revertibleManager().enable();
        ElementalMagicApi.userManager().enable();
    }
    
    private void tick() {
        ElementalMagicApi.abilityManager().progressAll();
        ElementalMagicApi.activationManager().createPassives();
        ElementalMagicApi.effectHandler().clearVelocityPriorities();
        ElementalMagicApi.revertibleManager().revertExpired();
    }
   
    @Override
    public void reload() {
        disable(false);
        onEnable();
    }

    private void disable(boolean shutDown) {
        HandlerList.unregisterAll();
        tickTask.cancel();

        ElementalMagicApi.abilityManager().disable(shutDown);
        ElementalMagicApi.abilityStorage().disable(shutDown);
        ElementalMagicApi.activationManager().disable(shutDown);
        ElementalMagicApi.effectHandler().disable(shutDown);
        ElementalMagicApi.revertibleManager().disable(shutDown);
        ElementalMagicApi.userManager().disable(shutDown);
    
        // Disable these last for last-minute usage
        ElementalMagicApi.userStorage().disable(shutDown);
        ElementalMagicApi.configManager().disable(shutDown);
    }

    @Override
    public void onDisable() {
        disable(true);
    }

    private void storeCoreElements() {
        AbilityStorage abilityStorage = ElementalMagicApi.abilityStorage();
        abilityStorage.registerElement(CoreElement.AIR);
        abilityStorage.registerElement(CoreElement.SOUND);
        abilityStorage.registerElement(CoreElement.FLIGHT);
        abilityStorage.registerElement(CoreElement.AVATAR);
        abilityStorage.registerElement(CoreElement.CHI);
        abilityStorage.registerElement(CoreElement.WEAPONRY);
        abilityStorage.registerElement(CoreElement.EARTH);
        abilityStorage.registerElement(CoreElement.SAND);
        abilityStorage.registerElement(CoreElement.MUD);
        abilityStorage.registerElement(CoreElement.METAL);
        abilityStorage.registerElement(CoreElement.LAVA);
        abilityStorage.registerElement(CoreElement.FIRE);
        abilityStorage.registerElement(CoreElement.LIGHTNING);
        abilityStorage.registerElement(CoreElement.COMBUSTION);
        abilityStorage.registerElement(CoreElement.WATER);
        abilityStorage.registerElement(CoreElement.ICE);
        abilityStorage.registerElement(CoreElement.PLANT);
        abilityStorage.registerElement(CoreElement.HEALING);
        abilityStorage.registerElement(CoreElement.BLOOD);
    }

    private void storeCoreAbilities() {
        // Per-element alphabetical ordering!!
        registerAbility(new FastSwimController());
        registerAbility(new FrostBreathController());
        registerAbility(new HydroSinkController());
        registerAbility(new IceBlastController());
        registerAbility(new IceSkateController());
        registerAbility(new IceSpikeController());
        registerAbility(new IceWallController());
        registerAbility(new IceWaveController());
        registerAbility(new IcicleController());
        registerAbility(new OctopusFormController());
        registerAbility(new PhaseChangeController());
        registerAbility(new PlantWhipController());
        registerAbility(new SurgeController());
        registerAbility(new TorrentController());
        registerAbility(new WaterFlowController());
        registerAbility(new WaterManipulationController());
        registerAbility(new WaterSpoutController());
    }
    
    private void registerAbility(AbilityController controller) {
        ElementalMagicApi.abilityStorage().registerController(controller);
        ElementalMagicApi.activationManager().registerController(controller);
    }
}
