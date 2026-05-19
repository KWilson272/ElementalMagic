package me.kwilson272.elementalmagic.core.gameplay.util;

import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreElement;

/**
 * Config class for water sourcing.
 */
public class WaterSourceOptions {

    private boolean useWater;
    private boolean useIce;
    private boolean useSnow;
    private boolean usePlant;

    public WaterSourceOptions(AbilityUser user) {
        this.useWater = user.canUseElement(CoreElement.WATER);
        this.useIce = user.canUseElement(CoreElement.ICE);
        this.useSnow = useIce; // prevent duplicate check
        this.usePlant = user.canUseElement(CoreElement.PLANT);
    }

    public WaterSourceOptions noWater() {
        this.useWater = false;
        return this;
    }

    public WaterSourceOptions noIce() {
        this.useIce = false;
        return this;
    }

    public WaterSourceOptions noSnow() {
        this.useSnow = false;
        return this;
    }

    public WaterSourceOptions noPlant() {
        this.usePlant = false;
        return this;
    }

    public boolean useWater() {
        return useWater;
    }

    public boolean useIce() {
        return useIce;
    }

    public boolean useSnow() {
        return useSnow;
    }

    public boolean usePlant() {
        return usePlant;
    }
}

