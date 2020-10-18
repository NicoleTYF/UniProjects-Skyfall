package deco2800.skyfall.entities.enemies;

import deco2800.skyfall.util.HexVector;

/**
 * An instance of a Heavy (hard level) enemy.
 */
public class Heavy extends Enemy implements Spawnable {

    public Heavy(float col, float row, float scaling, String biome) {
        super(col, row, EnemyType.HEAVY, 0.06f, biome, "enemyHeavy");

        this.setType(EnemyType.HEAVY);

        // Assign values, includes default values
        this.setValues(scaling, 100, 5, 6, 3f, 4f);
    }

    /**
     * Constructor of Heavy enemy, used for testing
     *
     * @param col     the x-coordinate of the enemy.
     * @param row     the y-coordinate of the enemy.
     * @param scaling the scaling factor of the enemy's stats.
     */
    public Heavy(float col, float row, float scaling) {
        super(col, row);

        this.setValues(scaling, 100, 15, 1, 0.08f, 0.1f);
    }

    @Override
    public Enemy newInstance(float row, float col) {
        return new Heavy(col, row, getScale(), getBiome());
    }

    @Override
    public Enemy newInstance(HexVector spawnPos) {
        return newInstance(spawnPos.getRow(), spawnPos.getCol());
    }

    /**
     * Check whether the object equals to this enemy instance.
     *
     * @param obj the object to be checked
     * @return true if the objects equals, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Heavy && this.hashCode() == obj.hashCode();
    }

    /**
     * The hashcode of the enemy based on {@link #toString()}. It will be used in
     * {@link #equals(Object)} for comparing objects.
     *
     * @return the hashcode of the enemy instance.
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
