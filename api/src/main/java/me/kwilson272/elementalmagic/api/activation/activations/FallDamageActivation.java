package me.kwilson272.elementalmagic.api.activation.activations;

import me.kwilson272.elementalmagic.api.activation.Activation;

public class FallDamageActivation implements Activation {
    
    private final double originalDamage;
    private double damage;

    public FallDamageActivation(double damage) {
        this.originalDamage = damage; 
        this.damage = damage;
    }

    public double getOriginalDamage() {
        return originalDamage;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }
}
