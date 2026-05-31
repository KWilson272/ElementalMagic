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
import me.kwilson272.elementalmagic.core.gameplay.air.airagility.AirAgilityController;
import me.kwilson272.elementalmagic.core.gameplay.air.airblade.AirBladeController;
import me.kwilson272.elementalmagic.core.gameplay.air.airblast.AirBlastController;
import me.kwilson272.elementalmagic.core.gameplay.air.airbreath.AirBreathController;
import me.kwilson272.elementalmagic.core.gameplay.air.airburst.AirBurstController;
import me.kwilson272.elementalmagic.core.gameplay.air.airscooter.AirScooterController;
import me.kwilson272.elementalmagic.core.gameplay.air.airshield.AirShieldController;
import me.kwilson272.elementalmagic.core.gameplay.air.airslam.AirSlamController;
import me.kwilson272.elementalmagic.core.gameplay.air.airspout.AirSpoutController;
import me.kwilson272.elementalmagic.core.gameplay.air.airstream.AirStreamController;
import me.kwilson272.elementalmagic.core.gameplay.air.airsuction.AirSuctionController;
import me.kwilson272.elementalmagic.core.gameplay.air.airsweep.AirSweepController;
import me.kwilson272.elementalmagic.core.gameplay.air.airswipe.AirSwipeController;
import me.kwilson272.elementalmagic.core.gameplay.air.gracefuldescent.GracefulDescentController;
import me.kwilson272.elementalmagic.core.gameplay.air.sonicblast.SonicBlastController;
import me.kwilson272.elementalmagic.core.gameplay.chi.wallrun.WallRunController;
import me.kwilson272.elementalmagic.core.gameplay.fire.blaze.BlazeController;
import me.kwilson272.elementalmagic.core.gameplay.fire.combustion.CombustionController;
import me.kwilson272.elementalmagic.core.gameplay.fire.discharge.DischargeController;
import me.kwilson272.elementalmagic.core.gameplay.fire.fireball.FireBallController;
import me.kwilson272.elementalmagic.core.gameplay.fire.fireblast.FireBlastController;
import me.kwilson272.elementalmagic.core.gameplay.fire.firebreath.FireBreathController;
import me.kwilson272.elementalmagic.core.gameplay.fire.fireburst.FireBurstController;
import me.kwilson272.elementalmagic.core.gameplay.fire.firejet.FireJetController;
import me.kwilson272.elementalmagic.core.gameplay.fire.firekick.FireKickController;
import me.kwilson272.elementalmagic.core.gameplay.fire.fireshield.FireShieldController;
import me.kwilson272.elementalmagic.core.gameplay.fire.fireshots.FireShotsController;
import me.kwilson272.elementalmagic.core.gameplay.fire.firespin.FireSpinController;
import me.kwilson272.elementalmagic.core.gameplay.fire.firewheel.FireWheelController;
import me.kwilson272.elementalmagic.core.gameplay.fire.heatcontrol.HeatControlController;
import me.kwilson272.elementalmagic.core.gameplay.fire.immolate.ImmolateController;
import me.kwilson272.elementalmagic.core.gameplay.fire.jetblast.JetBlastController;
import me.kwilson272.elementalmagic.core.gameplay.fire.jetblaze.JetBlazeController;
import me.kwilson272.elementalmagic.core.gameplay.fire.lightning.LightningController;
import me.kwilson272.elementalmagic.core.gameplay.fire.lightningburst.LightningBurstController;
import me.kwilson272.elementalmagic.core.gameplay.fire.walloffire.WallOfFireController;
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
import me.kwilson272.elementalmagic.core.gameplay.water.waterblade.WaterBladeController;
import me.kwilson272.elementalmagic.core.gameplay.water.waterflow.WaterFlowController;
import me.kwilson272.elementalmagic.core.gameplay.water.watergimbal.WaterGimbalController;
import me.kwilson272.elementalmagic.core.gameplay.water.watermanipulation.WaterManipulationController;
import me.kwilson272.elementalmagic.core.gameplay.water.waterspout.WaterSpoutController;
import me.kwilson272.elementalmagic.core.listener.FallDamageListener;
import me.kwilson272.elementalmagic.core.listener.HeatControlHelper;
import me.kwilson272.elementalmagic.core.listener.MovementLimiter;
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
        Bukkit.getPluginManager().registerEvents(new HeatControlHelper(), this);
        Bukkit.getPluginManager().registerEvents(new FallDamageListener(), this);
        Bukkit.getPluginManager().registerEvents(new MovementLimiter(), this);
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
       
        // -- Air --
        registerAbility(new AirAgilityController());
        registerAbility(new AirBladeController());
        registerAbility(new AirBlastController());
        registerAbility(new AirBreathController());
        registerAbility(new AirBurstController());
        registerAbility(new AirScooterController());
        registerAbility(new AirShieldController());
        registerAbility(new AirSlamController());
        registerAbility(new AirSpoutController());
        registerAbility(new AirStreamController());
        registerAbility(new AirSuctionController());
        registerAbility(new AirSweepController());
        registerAbility(new AirSwipeController());
        registerAbility(new GracefulDescentController());
        registerAbility(new SonicBlastController());

        // -- Chi --
        registerAbility(new WallRunController());

        // -- Fire --
        registerAbility(new BlazeController());
        registerAbility(new CombustionController());
        registerAbility(new DischargeController());
        registerAbility(new FireBallController());
        registerAbility(new FireBlastController());
        registerAbility(new FireBreathController());
        registerAbility(new FireBurstController());
        registerAbility(new FireJetController());
        registerAbility(new FireKickController());
        registerAbility(new FireShieldController());
        registerAbility(new FireShotsController());
        registerAbility(new FireSpinController());
        registerAbility(new FireWheelController());
        registerAbility(new HeatControlController());
        registerAbility(new ImmolateController());
        registerAbility(new JetBlastController());
        registerAbility(new JetBlazeController());
        registerAbility(new LightningController());
        registerAbility(new LightningBurstController());
        registerAbility(new WallOfFireController());

        // -- Water --
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
        registerAbility(new WaterBladeController());
        registerAbility(new WaterFlowController());
        registerAbility(new WaterGimbalController());
        registerAbility(new WaterManipulationController());
        registerAbility(new WaterSpoutController());
    }
    
    private void registerAbility(AbilityController controller) {
        ElementalMagicApi.abilityStorage().registerController(controller);
        ElementalMagicApi.activationManager().registerController(controller);
    }
}
