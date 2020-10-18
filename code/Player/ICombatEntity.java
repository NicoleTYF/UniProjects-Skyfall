package deco2800.skyfall.entities;

/**
 * Defines an entity that participates in combat.
 * Created by chris-poli on 26/7/17.
 */
public interface ICombatEntity extends HasHealth {

    /**
     * Deal damage to this entity.
     */
    void takeDamage(int damage);

    /**
     * Deal damage to another ICombatEntity so damage calculation can be applied.
     */
    void dealDamage(MainCharacter mc);

    /**
     * Some combat entities will only be able to be attacked.
     */
    boolean canDealDamage();

    /**
     * Get the amount of damage this entity deals.
     *
     * @return The damage this entity deals.
     */
    int getDamage();

    /**
     * Get an array of the resistance attributes.
     *
     * @return A array containing the resistance attributes of the combat entity.
     */
    int[] getResistanceAttributes();

}

