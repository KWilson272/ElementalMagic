package me.kwilson272.elementalmagic.core.gameplay.water;

import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreElement;

/**
 * Configuration object for which water blocks can or cannot be included
 * in sourcing and usage functions.
 */
public class WaterUsePolicy {

    private boolean useWater;
    private boolean useIce;
    private boolean useSnow;
    private boolean usePlant;
   
    public WaterUsePolicy() {
        useWater = true;
        useIce = true;
        useSnow = true;
        usePlant = true;
    }

    public WaterUsePolicy setWater(boolean useWater) {
        this.useWater = useWater;
        return this;
    }

    public WaterUsePolicy setIce(boolean useIce) {
        this.useIce = useIce;
        return this;
    }

    public WaterUsePolicy setSnow(boolean useSnow) {
        this.useSnow = useSnow;
        return this;
    }

    public WaterUsePolicy setPlant(boolean usePlant) {
        this.usePlant = usePlant;
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

    /**
     * Sets all element usage flags to false where the provided {@link AbilityUser}
     * cannot actively use the element.
     */
    public void validate(AbilityUser user) {
        if (!user.canUseElement(CoreElement.WATER)) {
            setWater(false);   
        }
        if (!user.canUseElement(CoreElement.ICE)) {
            setIce(false);
            setSnow(false);
        }
        if (!user.canUseElement(CoreElement.PLANT)) {
            setPlant(false);
        }
    }

    public static WaterUsePolicy from(AbilityUser user) {
        WaterUsePolicy policy = new WaterUsePolicy();
        policy.validate(user);
        return policy;
    }
}
