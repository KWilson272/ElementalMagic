package me.kwilson272.elementalmagic.api.effect;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ability.Ability;

/**
 * Manages PvP and PvE related ability effects.
 */
public interface EffectHandler {

    void enable();
    void disable(boolean shutDown);

    /**
     * Clears the velocity priorities from the previous tick.
     */
    void clearVelocityPriorities();

    /**
     * Checks if an Entity can be affected by Abilities.
     *
     * @param entity the Entity being checked
     * @return boolean true if the Entity can be affected, false otherwise
     */
    boolean canAffect(Entity entity);

    /**
     * Damages the provided Entity with the specified amount of damage. This
     * should be called any time an Ability is directly responsible for damaging
     * an entity.
     *
     * @param entity the Entity being damaged
     * @param cause  the Ability cause of the damage
     * @param damage the double amount of damage
     * @return boolean true if the Entity was damaged, false otherwise
     */
    boolean damageEntity(Entity entity, Ability cause, double damage);
    
    /**
     * Checks if the provided Entity is currently damaging another Entity with
     * an Ability. This method is meant to determine if an EntityDamageByEntityEvent
     * was caused as a result of an Ability calling {@link #damageEntity(Entity, Ability, double)}
     *
     * @param entity the Entity being checked
     * @return true if the Entity is damaging another Entity, false otherwise
     */
    boolean isDamaging(Entity entity);

    /**
     * Checks if the provided Entity is currently being damaged by an Ability.
     * This method is meant to determine if an EntityDamageByEntityEvent was
     * called as a result of an Ability calling {@link #damageEntity(Entity, Ability, double)}
     *
     * @param entity the Entity being checked
     * @return true if the Entity is being damaged, false otherwise
     */
    boolean isBeingDamaged(Entity entity);

    /**
     * Converts vanilla damage to ability-caused damage. Examples include
     * but are not limited to: ability-created blocks dealing damage
     * (cactus, magma, etc...) and ability-created entities dealing damage.
     * This method may alter the amount of damage that should be done based
     * on the result of called events or various checks.
     *
     * @param entity the Entity being damaged
     * @param cause the Ability responsible for the damage
     * @param damage the damage done in the event
     *
     * @return the modified amount of damage that should be dealt
     */
    double convertDamage(Entity entity, Ability cause, double damage);

    /**
     * Sets an Entity's velocity to the provided Vector.
     *
     * <p> This method should be called whenever an Ability affects the velocity
     * of an Entity as part of its intended PvE or PvP effects. EX: an Ability
     * deals knockback upon damaging an entity.
     *
     * <p> This method does not need to be called when an Ability is setting
     * the velocity of an Entity that is used for its visual effects. EX: an
     * Ability which uses FallingBlocks directly in its animation.
     *
     * <p> Note: for abilities that need more of a final say in the velocity
     * an entity is set to at the end of the tick, see
     * {@link #setVelocity(Entity, Ability, Vector, int)}.
     *
     * @param entity the Entity whose velocity is being set
     * @param cause the Ability causing the velocity change
     * @param velocity the Velocity being applied to the Entity
     *
     * @return boolean true if the Velocity was applied, false otherwise
     */
    boolean setVelocity(Entity entity, Ability cause, Vector velocity);

    /**
     * Sets an Entity's velocity to the provided Vector, with the given priority.
     *
     * <p> This method should be called whenever an Ability affects the velocity
     * of an Entity as part of its intended PvE or PvP effects. EX: an Ability
     * deals knockback upon damaging an entity.
     *
     * <p> This method does not need to be called when an Ability is setting
     * the velocity of an Entity that is used for its visual effects. EX: an
     * Ability which uses FallingBlocks directly in its animation.
     *
     * @param entity the Entity whose velocity is being set
     * @param cause the Ability causing the velocity change
     * @param velocity the Velocity being applied to the Entity
     * @param priority the Integer priority the velocity is applied at
     *
     * @return boolean true if the Velocity was applied, false otherwise
     */
    boolean setVelocity(Entity entity, Ability cause, Vector velocity, int priority);

    /**
     * Sets the provided Entity on fire for the specified duration. If a value
     * of <= 0 is passed, the Entity will be extinguished. This method will
     * overwrite the Entity's current FireTicks.
     *
     * @param entity the Entity being burned
     * @param cause the Ability directly responsible
     * @param durationMillis the duration of the burn in milliseconds
     * @return true if the Entity was burned, false otherwise.
     */
    boolean setFireDuration(Entity entity, Ability cause, long durationMillis);

    /**
     * Converts vanilla firetick alteration to be ability-caused. This should
     * only be called when an ability is indirectly responsible for changing
     * an entity's burn duration. EX: an ability-made TempBlock sets an Entity
     * on fire.
     *
     * <p> This method may modify the amount of time the entity should burn for.
     *
     * @param entity the Entity being burned
     * @param cause the Ability cause of the burning
     * @param durationMillis the duration of the burn in milliseconds
     *
     * @return the modified time the entity should burn for, in milliseconds
     */
    long convertFire(Entity entity, Ability cause, long durationMillis);

    /**
     * Adds a PotionEffect to the provided entity.
     *
     * @param entity the LivingEntity having the PotionEffect added
     * @param potionEffect the PotionEffect being added
     * @param cause the Ability instance causing the PotionEffect to be added
     *
     * @return boolean true if the PotionEffect was added, false otherwise
     */
    boolean addPotionEffect(LivingEntity entity, Ability cause, PotionEffect effect);

    /**
     * Removes a PotionEffect from the entity with the provided PotionEffectType.
     *
     * @param entity the LivingEntity whose PotionEffect is being removed
     * @param cause the Ability instance causing the removal
     * @param potionEffectType the PotionEffectType being removed
     *
     * @return boolean true if the PotionEffect was removed, false otherwise
     */
    boolean removePotionEffect(LivingEntity entity, Ability cause, PotionEffectType type);

    /**
     * Stops a {@link LivingEntity} from moving for the provided duration.
     *
     * @param entity the {@code LivingEntity} being immobilized
     * @param cause the {@link Ability} responsible
     * @param durationMillis the Long duration of immobilization in milliseconds.
     * @return true if the entity was immobilized, false otherwise
     */
    boolean stopMovement(LivingEntity entity, Ability cause, long durationMillis);

    /**
     * Checks if a {@link LivingEntity} is currently immobilized.
     *
     * @param entity the [@code LivingEntity} being checked.
     * @return true if the entity is immobilized, false otherwise.
     */
    boolean isImmobilized(LivingEntity entity);

    /**
     * Allows an immobilized {@link LivingEntity} to move again. This function
     * should do nothing if {@link #isImmobilized} returns false for the given
     * entity.
     *
     * @param entity the {@link LivingEntity} whose movement is allowed.
     */
    void allowMovement(LivingEntity entity);
}
