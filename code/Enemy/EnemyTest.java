package deco2800.skyfall.entities.enemies;

import deco2800.skyfall.animation.AnimationRole;
import deco2800.skyfall.animation.Direction;
import deco2800.skyfall.entities.MainCharacter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test enemy.
 */
public class EnemyTest {

    // An instance of a main character
    private MainCharacter testCharacter;

    // Enemies
    private Enemy testEnemy;
    private Enemy testDummyEnemy;

    // Strings
    private String biomeName = "Forest";

    /**
     * Set up for enemy tests.
     */
    @Before
    public void setUp() {
        // set up enemy, need the longer constructor to test toString(), equals() and hashcode()
        testEnemy = new Enemy(30f, 30f, Enemy.EnemyType.HEAVY,
            0.06f, biomeName, "enemyTexture");
        testEnemy.setHealth(10);
        testEnemy.getMainCharacter().setHurt(false);
        testEnemy.getMainCharacter().setRecovering(false);
        testEnemy.getMainCharacter().setDead(false);
	testEnemy.setStrength(3);
        testEnemy.setChasingSpeed(3f);
        testEnemy.setAttackRange(100);

	// set up main character
        testCharacter = new MainCharacter(30f, 30f, 0.05f, "Main Piece", 50);
	testEnemy.getMainCharacter().setHurt(false);
        testEnemy.getMainCharacter().setRecovering(false);
        testEnemy.getMainCharacter().setDead(false);
        testEnemy.setMainCharacter(testCharacter);
	
	// for testting equals()
        testDummyEnemy = new Enemy(0f, 0f, Enemy.EnemyType.SCOUT,
                0.06f, biomeName, "dummyTexture");
    }

    @Test
    public void setterAndGetterTests() {

        testEnemy.setHealth(10);
        Assert.assertEquals(10, testEnemy.getHealth());

        testEnemy.setBiome("testBiome");
        Assert.assertEquals("testBiome", testEnemy.getBiome());

        testEnemy.setMainCharacter(testCharacter);
        Assert.assertEquals(testCharacter, testEnemy.getMainCharacter());

        testEnemy.setHurt(true);
        Assert.assertTrue(testEnemy.getHurt());

        Assert.assertTrue(testEnemy.canDealDamage());
        Assert.assertArrayEquals(new int[0], testEnemy.getResistanceAttributes());

        // Test movementDirections, will test other directions later
        Assert.assertEquals(Direction.NORTH, testEnemy.movementDirection(2f));
    }

    /**
     * Test enemy's ability to be hurt.
     * Related methods include:
     *  > takeDamage()
     *  > checkIfHurtEnded()
     */
    @Test
    public void enemyHurtTest() {
        testEnemy.takeDamage(3);
        // set hurt time to 0.
        Assert.assertEquals(0, testEnemy.getHurtTime());
        // set enemy's "isHurt" status to true.
        Assert.assertTrue(testEnemy.getHurt());
        // reduce enemy's health
        Assert.assertEquals(7, testEnemy.getHealth());
        // if hurt equals TRUE, and updateAnimation() is called in onTick(),
        // then AnimationRole changed from NULL to HURT.
        testEnemy.updateAnimation();
        Assert.assertEquals(AnimationRole.HURT, testEnemy.getCurrentState());

        // when hurt time is less than 340 (less than 2 seconds in the game)...
        testEnemy.checkIfHurtEnded();
        Assert.assertEquals(20, testEnemy.getHurtTime());
        Assert.assertTrue(testEnemy.getHurt());
        // After hurt animations (around 2 seconds)...
        testEnemy.setHurtTime(360);
        testEnemy.checkIfHurtEnded();
        // reset hurt time to 0.
        Assert.assertEquals(0, testEnemy.getHurtTime());
        // enemy recovered
        Assert.assertFalse(testEnemy.getHurt());
    }

    /**
     * Test attackAction() 
     */
    @Test
    public void enemyAttackTest() {
	// test if attackAction() can deal damage to main character 
        testEnemy.attackAction();
        Assert.assertEquals(AnimationRole.ATTACK, testEnemy.getCurrentState());
        Assert.assertTrue(testEnemy.getMainCharacter().isHurt());
        Assert.assertEquals(47, testEnemy.getMainCharacter().getHealth());
	// if enemy is close to main character, speed increases 
        Assert.assertEquals(3f, testEnemy.getChasingSpeed(), 0);
        // reset hurt status
        testEnemy.setHurtTime(400);
        testEnemy.checkIfHurtEnded();
        // reset enemy keeps moving
        testEnemy.randomMoveAction();

        // if main character is recovering, enemy cannot attack
        testEnemy.getMainCharacter().setHurt(false);
        testEnemy.getMainCharacter().setRecovering(true);
        testEnemy.getMainCharacter().setDead(false);
        testEnemy.attackAction();
        Assert.assertEquals(AnimationRole.MOVE, testEnemy.getCurrentState());
        Assert.assertFalse(testEnemy.getMainCharacter().isHurt());

	// if enemy deals damage after main character recovered, trigger main character's hurt function again 
        testEnemy.getMainCharacter().setHurt(true);
        testEnemy.getMainCharacter().setRecovering(false);
        testEnemy.getMainCharacter().setDead(false);
        testEnemy.attackAction();
        Assert.assertEquals(AnimationRole.MOVE, testEnemy.getCurrentState());
        Assert.assertTrue(testEnemy.getMainCharacter().isHurt());
        testEnemy.setHurtTime(400);
        testEnemy.checkIfHurtEnded();

	// if main character is dead, enemy cannot attack 
	enemy.getMainCharacter().setHurt(false);
        testEnemy.getMainCharacter().setRecovering(false);
        testEnemy.getMainCharacter().setDead(true);
        testEnemy.attackAction();
        Assert.assertEquals(AnimationRole.ATTACK, testEnemy.getCurrentState());
        Assert.assertTrue(testEnemy.getMainCharacter().isHurt());
        testCharacter = new MainCharacter(0f, 0f, 0.05f, "Main Piece", 50);
        testEnemy.setMainCharacter(testCharacter);
        testEnemy.randomMoveAction();
    }

    /**
     * Test all animations for enemy updateAnimation() 
     */
    @Test
    public void setAndGetEnemyAnimationTest() {
        // If enemy dies, set animation state to DEAD.
        testEnemy.setCurrentState(AnimationRole.DEAD);
        Assert.assertEquals(AnimationRole.DEAD, testEnemy.getCurrentState());

        // If enemy hurts, set animation state to HURT.
        testEnemy.setCurrentState(AnimationRole.HURT);
        Assert.assertEquals(AnimationRole.HURT, testEnemy.getCurrentState());

        // If enemy moves, set animation state to MOVE.
        testEnemy.setCurrentState(AnimationRole.MOVE);
        Assert.assertEquals(AnimationRole.MOVE, testEnemy.getCurrentState());

        // If enemy attacks, set animation state to ATTACK.
        testEnemy.setCurrentState(AnimationRole.HURT);
        Assert.assertEquals(AnimationRole.HURT, testEnemy.getCurrentState());
    }

    /**
     * Test whether the sound files for the enemies are correct.
     */
    @Test
    public void setAndGetSoundTest() {
        testEnemy.configureSounds();
        Assert.assertEquals("pick up", testEnemy.getChaseSound());
        Assert.assertEquals("fist_attack", testEnemy.getAttackSound());
        Assert.assertEquals("died", testEnemy.getDeadSound());
    }

    @Test
    public void testToString() {
        String testString = "HEAVY at (30, 30) Forest biome";
        Assert.assertEquals(testString, testEnemy.toString());
    }

    @Test
    public void testEquals() {
        // Not equal due to different hashcode.
        Assert.assertFalse(testEnemy.equals(testDummyEnemy));

        // Not equal due to different instance
        Assert.assertFalse(testEnemy.equals(testCharacter));

        // Equals due to same instance and same hashcode
        testDummyEnemy =  new Enemy(30f, 30f, Enemy.EnemyType.HEAVY,
                0.06f, biomeName, "enemyTexture");
        Assert.assertTrue(testEnemy.equals(testDummyEnemy));
    }
}