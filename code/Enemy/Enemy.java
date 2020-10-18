package deco2800.skyfall.entities.enemies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import deco2800.skyfall.Tickable;
import deco2800.skyfall.entities.Peon;
import deco2800.skyfall.animation.Direction;
import deco2800.skyfall.animation.Animatable;
import deco2800.skyfall.managers.GameManager;
import deco2800.skyfall.managers.SoundManager;
import deco2800.skyfall.entities.ICombatEntity;
import deco2800.skyfall.entities.MainCharacter;
import deco2800.skyfall.animation.AnimationRole;
import deco2800.skyfall.animation.AnimationLinker;

import java.util.Random;

/**
 * An instance to abstract the basic variables and methods of an enemy.
 */
public class Enemy extends Peon implements Animatable, ICombatEntity, Tickable {

    // Logger for tracking enemy information
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Basic enemy stats
    private int health;
    private int strength;
    private float attackRange;
    private float walkingSpeed;
    private float chasingSpeed;

    // Enemy target error
    private int targetError = -2 + new Random().nextInt(3);

    // Name of the biome the enemy is in.
    private String biome;

    // Enemy types
    public enum EnemyType {
        ABDUCTOR, HEAVY, SCOUT, MEDIUM
    }

    // type this enemy is
    private Enemy.EnemyType enemy;

    // Animation timings
    private long hurtTime = 0;
    private long deadTime = 0;
    // Booleans to check whether Enemy is in a state.
    private boolean isHurt = false;

    // sound effects
    protected String chasingSound;
    protected String hurtSound;
    protected String attackingSound;
    protected String diedSound;

    // Main character instance in the game.
    private MainCharacter mainCharacter;

    public Enemy(float col, float row, EnemyType enemyType, float speed, String biome,
                 String textureName) {
        super(row, col, speed, textureName, 10);

        // Sets the spawning location and all the collision
        this.setPosition(col, row);
        this.initialiseBox2D(col, row);
        this.setCollidable(true);

        // Sets the main character in the game.
        this.setMainCharacter(MainCharacter.getInstance());

        // Set type of enemy
        this.setType(enemyType);

        // Sets the type of the enemy, its name and the biome it is from
        this.setBiome(biome);
        this.setHealth(health);
        this.setMaxHealth(health);

        // Sets the texture for this enemy and the animations
        this.setTexture(textureName);
        this.setDirectionTextures();
        this.configureAnimations();
        this.configureSounds();
    }

    /**
     * This constructor is written for testing.
     *
     * @param col the x-coordinate of the enemy.
     * @param row the y-coordinate of the enemy.
     */
    public Enemy(float col, float row) {
        this.setRow(row);
        this.setCol(col);
    }

    /**
     * Enemy chase the player, if player position is in range, enemy attacks player.
     */
    public void attackAction() {
        if (!(mainCharacter.isDead() || mainCharacter.isRecovering() || mainCharacter.isHurt())) {
            this.setSpeed(getChasingSpeed());

            float targetShift = (float) targetError / 2;

            double xDestination;
            double yDestination;

            float xDirection;
            float yDirection;

            double playerAngle = Math.toRadians(-mainCharacter.getPlayerDirectionAngle() + 90);

            if (distance(mainCharacter) < targetShift) {
                xDirection = mainCharacter.getPosition().getCol() - getBody().getPosition().x;
                yDirection = mainCharacter.getPosition().getRow() - getBody().getPosition().y;
            } else {
                xDestination = mainCharacter.getPosition().getCol() + targetShift * Math.cos(playerAngle);
                yDestination = mainCharacter.getPosition().getRow() + targetShift * Math.sin(playerAngle);

                xDirection = (float) xDestination - getBody().getPosition().x;
                yDirection = (float) yDestination - getBody().getPosition().y;
            }

            getBody().setLinearVelocity(xDirection, yDirection);
            getBody().setLinearVelocity(getBody().getLinearVelocity().limit(chasingSpeed));

            this.position.set(getBody().getPosition().x, getBody().getPosition().y);

            // if the player in attack range then attack player
            if (distance(mainCharacter) < 0.98f) {
                setCurrentState(AnimationRole.ATTACK);
                mainCharacter.setCurrentState(AnimationRole.HURT);
                dealDamage(mainCharacter);
            }
        }
    }

    /**
     * under normal situation the enemy will random wandering in 100 radius circle
     */
    public void randomMoveAction() {
        setCurrentState(AnimationRole.MOVE);
        setSpeed(getWalkingSpeed());
        double moveAngle = new Random().nextDouble() * 2 * Math.PI;

        float xDirection = (float) Math.cos(moveAngle);
        float yDirection = (float) Math.sin(moveAngle);

        getBody().setLinearVelocity(xDirection, yDirection);
        getBody().setLinearVelocity(getBody().getLinearVelocity().limit(0.01f));

        this.position.set(getBody().getPosition().x, getBody().getPosition().y);
    }

    /**
     * Damage taken
     *
     * @param damage hero damage
     */
    @Override
    public void takeDamage(int damage) {
        hurtTime = 0;
        setHurt(true);
        health -= damage;

        // In Peon.class, when the health = 0,
        // isDead will be set true automatically.
        if (health <= 0) {
            die();
        }
    }

    /**
     * Deals damage to the main character by lowering their health
     *
     * @param mc The main character
     */
    @Override
    public void dealDamage(MainCharacter mc) {
        setCurrentState(AnimationRole.ATTACK);
        mc.playerHurt(this.getDamage());
        mc.setRecovering(true);
        SoundManager.playSound(attackingSound);
    }

    /**
     * Determines whether the enemy can deal damage
     *
     * @return True if they can deal damage, false otherwise
     */
    @Override
    public boolean canDealDamage() {
        return true;
    }

    /**
     * Get enemy's current strength
     *
     * @return enemy's current strength.
     */
    @Override
    public int getDamage() {
        return strength;
    }

    /**
     * Return a list of resistance attributes.
     *
     * @return A list of resistance attributes.
     */
    @Override
    public int[] getResistanceAttributes() {
        return new int[0];
    }

    /**
     * Set the type of enemy.
     *
     * @param enemyType the type of the enemy.
     */
    public void setType(EnemyType enemyType) {
        this.enemy = enemyType;
    }

    /**
     * If the animation is moving sets the animation state to be Move else NULL.
     * Also sets the direction
     */
    public void updateAnimation() {
        setTexture(getDefaultTexture());
        double angle = Math.atan2(getBody().getLinearVelocity().y, getBody().getLinearVelocity().x);

        setCurrentDirection(movementDirection(angle));

        /* Short Animations */
        if (getToBeRun() != null) {
            if (getToBeRun().getType() == AnimationRole.ATTACK) {
                return;
            } else if (getToBeRun().getType() == AnimationRole.DEAD) {
                setCurrentState(AnimationRole.STILL);
            }
        }

        if (isHurt) {
             setCurrentState(AnimationRole.HURT);
        }
    }

    /**
     * Handles the action of the enemy per time tick in game.
     *
     * @param tick number of second tin the game.
     */
    @Override
    public void onTick(long tick) {
        if (isDead()) {
            if (deadTime < 500) {
                deadTime += 20;
                setCurrentState(AnimationRole.DEAD);
            } else {
                die();
            }
        } else {
            if (this.distance(mainCharacter) < attackRange &&
                    !(mainCharacter.isDead() || mainCharacter.isRecovering() || mainCharacter.isHurt())) {
                attackAction();
                setCurrentState(AnimationRole.ATTACK);
            } else {
                setCurrentState(AnimationRole.MOVE);
                randomMoveAction();
            }
            if (isHurt) {
                setCurrentState(AnimationRole.HURT);
                checkIfHurtEnded();
            }
            this.updateAnimation();
        }
    }

    /**
     * To set enemy heal
     *
     * @param health set heal of enemy
     */
    public void setHealth(int health) {
        this.health = health;
    }

    /**
     * Return the health this enemy has.
     *
     * @return The health this enemy has.
     */
    public int getHealth() {
        return health;
    }

    void setValues(float scaling, int health, int damage, float attackRange, float walkingSpeed, float chasingSpeed) {
        this.setMaxHealth((int) (health * scaling));
        this.setHealth((int) (health * scaling));
        this.setStrength((int) ((float) damage * scaling));
        this.setAttackRange(attackRange * scaling);
        this.setWalkingSpeed(walkingSpeed * scaling);
        this.setChasingSpeed(chasingSpeed * scaling);
    }

    /**
     * Set default texture of the enemy in 8 different directions.
     */
    @Override
    public void setDirectionTextures() {
        String animationNameStart = "__ANIMATION_" + this.enemy.name();
        defaultDirectionTextures.put(Direction.EAST, animationNameStart + "MoveE:0");
        defaultDirectionTextures.put(Direction.WEST, animationNameStart + "MoveW:0");
        defaultDirectionTextures.put(Direction.SOUTH, animationNameStart + "MoveS:0");
        defaultDirectionTextures.put(Direction.NORTH, animationNameStart + "MoveN:0");
        defaultDirectionTextures.put(Direction.NORTH_EAST, animationNameStart + "MoveNE:0");
        defaultDirectionTextures.put(Direction.NORTH_WEST, animationNameStart + "MoveNW:0");
        defaultDirectionTextures.put(Direction.SOUTH_EAST, animationNameStart + "MoveSE:0");
        defaultDirectionTextures.put(Direction.SOUTH_WEST, animationNameStart + "MoveSW:0");
    }

    /**
     * Deploy the sound of the enemy into the game.
     */
    public void configureSounds() {
        this.chasingSound = "pick up";
        this.hurtSound = "be_hit";
        this.attackingSound = "fist_attack";
        this.diedSound = "died";
    }

    /**
     * Deploy the animation of the enemy in 8 different directions, and for moving,
     * hurting attacking and dead animations.
     */
    @Override
    public void configureAnimations() {
        String enemyName = this.enemy.name();

        // Move animations
        this.addAnimations(AnimationRole.MOVE, Direction.NORTH,
                new AnimationLinker(enemyName + "MoveN", AnimationRole.MOVE, Direction.NORTH, true, true));
        this.addAnimations(AnimationRole.MOVE, Direction.NORTH_EAST,
                new AnimationLinker(enemyName + "MoveNE", AnimationRole.MOVE, Direction.NORTH_EAST, true, true));
        this.addAnimations(AnimationRole.MOVE, Direction.NORTH_WEST,
                new AnimationLinker(enemyName + "MoveNW", AnimationRole.MOVE, Direction.NORTH_WEST, true, true));
        this.addAnimations(AnimationRole.MOVE, Direction.SOUTH,
                new AnimationLinker(enemyName + "MoveS", AnimationRole.MOVE, Direction.SOUTH, true, true));
        this.addAnimations(AnimationRole.MOVE, Direction.WEST,
                new AnimationLinker(enemyName + "MoveW", AnimationRole.MOVE, Direction.WEST, true, true));
        this.addAnimations(AnimationRole.MOVE, Direction.SOUTH_EAST,
                new AnimationLinker(enemyName + "MoveSE", AnimationRole.MOVE, Direction.SOUTH_EAST, true, true));
        this.addAnimations(AnimationRole.MOVE, Direction.SOUTH_WEST,
                new AnimationLinker(enemyName + "MoveSW", AnimationRole.MOVE, Direction.SOUTH_WEST, true, true));

        // Attack animations
        this.addAnimations(AnimationRole.ATTACK, Direction.EAST,
                new AnimationLinker(enemyName + "AttackE", AnimationRole.ATTACK, Direction.EAST, true, true));
        this.addAnimations(AnimationRole.ATTACK, Direction.NORTH,
                new AnimationLinker(enemyName + "AttackN", AnimationRole.ATTACK, Direction.NORTH, true, true));
        this.addAnimations(AnimationRole.ATTACK, Direction.SOUTH,
                new AnimationLinker(enemyName + "AttackS", AnimationRole.ATTACK, Direction.SOUTH, true, true));
        this.addAnimations(AnimationRole.ATTACK, Direction.SOUTH_EAST,
                new AnimationLinker(enemyName + "AttackSE", AnimationRole.ATTACK, Direction.SOUTH_EAST, true, true));
        this.addAnimations(AnimationRole.ATTACK, Direction.SOUTH_WEST,
                new AnimationLinker(enemyName + "AttackSW", AnimationRole.ATTACK, Direction.SOUTH_WEST, true, true));
        this.addAnimations(AnimationRole.ATTACK, Direction.WEST,
                new AnimationLinker(enemyName + "AttackW", AnimationRole.ATTACK, Direction.WEST, true, true));

        // Hurt animations
        this.addAnimations(AnimationRole.HURT, Direction.EAST,
                new AnimationLinker(enemyName + "DamageE", AnimationRole.HURT, Direction.EAST, true, true));
        this.addAnimations(AnimationRole.HURT, Direction.NORTH,
                new AnimationLinker(enemyName + "DamageN", AnimationRole.HURT, Direction.NORTH, true, true));
        this.addAnimations(AnimationRole.HURT, Direction.SOUTH,
                new AnimationLinker(enemyName + "DamageS", AnimationRole.HURT, Direction.SOUTH, true, true));
        this.addAnimations(AnimationRole.HURT, Direction.SOUTH_EAST,
                new AnimationLinker(enemyName + "DamageSE", AnimationRole.HURT, Direction.SOUTH_EAST, true, true));
        this.addAnimations(AnimationRole.HURT, Direction.SOUTH_WEST,
                new AnimationLinker(enemyName + "DamageSW", AnimationRole.HURT, Direction.SOUTH_WEST, true, true));
        this.addAnimations(AnimationRole.HURT, Direction.WEST,
                new AnimationLinker(enemyName + "DamageW", AnimationRole.HURT, Direction.WEST, true, true));

        // Dead animation
        this.addAnimations(AnimationRole.DEAD, Direction.DEFAULT,
                new AnimationLinker("enemyDie", AnimationRole.DEAD, Direction.DEFAULT, false, true));
    }

    /**
     * Setter of the enemy's speed when walking, will be used in
     * {@link #setValues(float, int, int, float, float, float)}.
     *
     * @param walkingSpeed the enemy's walking speed.
     */
    private void setWalkingSpeed(float walkingSpeed) {
        this.walkingSpeed = walkingSpeed;
    }

    /**
     * Getter of the enemy's speed when walking.
     *
     * @return the walking speed of this enemy.
     */
    private float getWalkingSpeed() {
        return this.walkingSpeed;
    }

    /**
     * Setter of the enemy's speed when chasing the player.
     *
     * @param chasingSpeed the enemy's chasing speed.
     */
    public void setChasingSpeed(float chasingSpeed) {
        this.chasingSpeed = chasingSpeed;
    }

    /**
     * Getter of the enemy's speed when chasing the player.
     *
     * @return the chasing speed of this enemy.
     */
    public float getChasingSpeed() {
        return this.chasingSpeed;
    }

    /**
     * Getter if the enemy's sound when chasing the player.
     *
     * @return the name of the chasing sound (defined in {@link SoundManager}) of
     *         this enemy.
     */
    public String getChaseSound() {
        return this.chasingSound;
    }

    /**
     * Getter of the enemy's sound when enemy is dying.
     *
     * @return the name of the dead sound (defined in {@link SoundManager}) of this
     *         enemy.
     */
    public String getDeadSound() {
        return this.diedSound;
    }

    /**
     * Getter of the enemy's sound when enemy is attacking.
     *
     * @return the name of the dead sound (defined in {@link SoundManager}) of this
     *         enemy.
     */
    public String getAttackSound() {
        return this.attackingSound;
    }

    /**
     * Set the amount of damage for this enemy.
     *
     * @param strength the new amount of damage to be set.
     */
    public void setStrength(int strength) {
        this.strength = strength;
    }

    /**
     * Set the biome the enemy is located at, will be used in
     * {@link #setValues(float, int, int, float, float, float)}.
     *
     * @param biome the name of the biome the enemy is located at.
     */
    public void setBiome(String biome) {
        this.biome = biome;
    }

    /**
     * Get the name of the biome this enemy is located at.
     *
     * @return the name of the biome this enemy is located.
     */
    public String getBiome() {
        return this.biome;
    }

    /**
     * Getter and setter of the main character object in the game.
     *
     * @param mainCharacter the main character in the game.
     */
    public void setMainCharacter(MainCharacter mainCharacter) {
        this.mainCharacter = mainCharacter;
    }

    /**
     * Get the main character instance in the game
     *
     * @return the main character object in the game.
     */
    public MainCharacter getMainCharacter() {
        return this.mainCharacter;
    }

    /**
     * Set the attack range for the enemy, will be called in
     * {@link #setValues(float, int, int, float, float, float)}.
     *
     * @param attackRange the attack range of the enemy.
     */
    public void setAttackRange(float attackRange) {
        this.attackRange = attackRange;
    }

    /**
     * Get the attack range of the enemy.
     *
     * @return the attack range of the enemy.
     */
    public float getAttackRange() {
        return this.attackRange;
    }

    /**
     * Get whether enemy is hurt.
     */
    public boolean getHurt() {
        return isHurt;
    }

    /**
     * Set whether enemy is hurt.
     *
     * @param isHurt the player's "hurt" status
     */
    public void setHurt(boolean isHurt) {
        this.isHurt = isHurt;
    }

    /**
     * Check whether the hurt time is within 2 seconds, therefore casting hurt
     * effects on enemy.
     */
    public void checkIfHurtEnded() {
        hurtTime += 20; // hurt for 1 second
        if (hurtTime > 340) {
            logger.info("Hurt ended");
            setHurt(false);
            hurtTime = 0;
        }
    }

    /**
     * Remove this enemy from the game world.
     */
    private void die() {
        if (isDead()) {
            if (getChaseSound() != null) {
                SoundManager.stopSound(getChaseSound());
            }
            if (getDeadSound() != null) {
                SoundManager.playSound(getDeadSound());

                this.setDead(true);
                logger.info("Enemy destroyed.");

                setCurrentState(AnimationRole.NULL);
                GameManager.get().getWorld().removeEntity(this);
            }
        }
    }

    /**
     * Get movement direction
     *
     * @param angle the angle between to tile
     */
    public Direction movementDirection(double angle) {
        angle = Math.toDegrees(angle - Math.PI);

        if (angle < 0) {
            angle += 360;
        }

        if (between(angle, 0, 45)) {
            return Direction.WEST;
        } else if (between(angle, 46, 90)) {
            return Direction.SOUTH_WEST;
        } else if (between(angle, 91, 135.5)) {
            return Direction.SOUTH;
        } else if (between(angle, 136, 180.5)) {
            return Direction.SOUTH_EAST;
        } else if (between(angle, 181, 225.5)) {
            return Direction.EAST;
        } else if (between(angle, 226, 270.9)) {
            return Direction.NORTH_EAST;
        } else if (between(angle, 271, 315.9)) {
            return Direction.NORTH;
        } else if (between(angle, 316, 360)) {
            return Direction.NORTH_WEST;
        }
        return null;
    }

    /**
     * Get the duration main character hurts.
     *
     * @return the duration main character hurts.
     */
    public long getHurtTime() {
        return hurtTime;
    }

    public void setHurtTime(long hurtTime) {
        this.hurtTime = hurtTime;
    }

    /**
     * @return string representation of this class including its enemy type, biome
     *         and x,y coordinates
     */
    @Override
    public String toString() {
        return String.format("%s at (%d, %d) %s biome", enemy, (int) getCol(), (int) getRow(), getBiome());
    }

    /**
     * Check whether the object equals to this enemy instance.
     *
     * @param obj the object to be checked
     * @return true if the objects equals, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Enemy && this.hashCode() == obj.hashCode();
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
