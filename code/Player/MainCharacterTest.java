package deco2800.skyfall.entities;

import com.badlogic.gdx.Input;
import deco2800.skyfall.entities.enemies.Scout;
import deco2800.skyfall.entities.spells.Spell;
import deco2800.skyfall.entities.spells.SpellType;
import deco2800.skyfall.entities.weapons.EmptyItem;
import deco2800.skyfall.animation.AnimationLinker;
import deco2800.skyfall.animation.AnimationRole;
import deco2800.skyfall.animation.Direction;
import deco2800.skyfall.entities.weapons.Sword;
import deco2800.skyfall.managers.*;
import deco2800.skyfall.managers.database.DataBaseConnector;
import deco2800.skyfall.resources.GoldPiece;
import deco2800.skyfall.resources.Item;
import deco2800.skyfall.resources.items.Stone;
import deco2800.skyfall.resources.items.*;
import deco2800.skyfall.util.HexVector;
import deco2800.skyfall.worlds.Tile;
import deco2800.skyfall.worlds.world.Chunk;
import deco2800.skyfall.worlds.world.World;
import deco2800.skyfall.worlds.world.WorldBuilder;
import deco2800.skyfall.worlds.world.WorldDirector;

import org.junit.*;
import org.lwjgl.Sys;
import org.mockito.Mock;

import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ WorldBuilder.class, WorldDirector.class, DatabaseManager.class, DataBaseConnector.class,
        GameManager.class })
public class MainCharacterTest {

    private MainCharacter testCharacter;
    private Tile testTile;
    private GoldPiece testGoldPiece;
    private InventoryManager inventoryManager;
    private World w = null;

    @Mock
    private GameManager mockGM;

    // A hashmap for testing player's animations
    private HashMap testMap = new HashMap();

    @Before
    /**
     * Sets up all variables to be used for testing
     */
    public void setup() throws Exception {
	// for giving random numbers for stats
        Random random = new Random(0);
        whenNew(Random.class).withAnyArguments().thenReturn(random);

	// reset and instantiate new character stats after each test
        MainCharacter.resetInstance();
        testCharacter = MainCharacter.getInstance();

	// set character position to x=0 y=0
        testTile = new Tile(null, 0f, 0f);

	// build the world for character to walk on
        WorldBuilder builder = new WorldBuilder();
        WorldDirector.constructTestWorld(builder, 0);
        w = builder.getWorld();

	// mock the entire class instead of calling it directly 
        mockGM = mock(GameManager.class);
        mockStatic(GameManager.class);

        physics = new PhysicsManager();
        when(mockGM.getManager(PhysicsManager.class)).thenReturn(physics);

        when(GameManager.get()).thenReturn(mockGM);
        when(mockGM.getWorld()).thenReturn(w);
    }

    @After
    /**
     * Sets up all variables to be null after esting
     */
    public void tearDown() {
        // testCharacter = null;
    }

    /**
     * Test getters and setters from Peon super Character class
     */
    @Test
    public void setterGetterTest() {
	// test set/get hurt status 
        testCharacter.setTexChanging(true);
        Assert.assertTrue(testCharacter.isTexChanging());
        testCharacter.setHurt(true);
        Assert.assertTrue(testCharacter.isHurt());
        testCharacter.setHurt(true);
        Assert.assertTrue(testCharacter.isHurt());

	// test set/get texture image  
        testCharacter.changeTexture("mainCharacter");
        assertEquals("mainCharacter", testCharacter.getTexture());

	// test set/get character name  
        Assert.assertEquals(testCharacter.getName(),"Player1"); 
	testCharacter.setName("Side Piece");
        Assert.assertEquals(testCharacter.getName(), "John Smith");

	// test set/get character health and death stats
        Assert.assertFalse(testCharacter.isDead());
        Assert.assertEquals(testCharacter.getHealth(), 50);
        testCharacter.changeHealth(5);
        Assert.assertEquals(testCharacter.getHealth(), 55);
        testCharacter.changeHealth(-55);
        Assert.assertEquals(testCharacter.getHealth(), 0);
        Assert.assertEquals(testCharacter.getDeaths(), 1);
        Assert.assertTrue(testCharacter.isDead());

	// test set/get animations
	testCharacter.addAnimations(AnimationRole.MOVE_EAST, "right");
        testCharacter.getAnimationName(AnimationRole.MOVE_EAST);

	// test set/get character directions
	testCharacter.setCurrentDirection(Direction.EAST);
        Assert.assertEquals(testCharacter.getCurrentDirection(), Direction.EAST);
	testCharacter.setCurrentDirection(Direction.WEST);
        Assert.assertEquals(testCharacter.getCurrentDirection(), Direction.WEST);

	// test set/get character state 
	testCharacter.setCurrentState(AnimationRole.MOVE);
        Assert.assertEquals(testCharacter.getCurrentState(), AnimationRole.MOVE);
        testCharacter.setCurrentState(AnimationRole.NULL);
        Assert.assertEquals(testCharacter.getCurrentState(), AnimationRole.NULL);
    }

    /**
     * Test level up of main character
     */
    @Test
    public void levelTest() {
        testCharacter.changeLevel(4);
        Assert.assertEquals(5, testCharacter.getLevel());

        testCharacter.changeLevel(-5);
        Assert.assertEquals(5, testCharacter.getLevel());

        testCharacter.changeLevel(-4);
        Assert.assertEquals(1, testCharacter.getLevel());
    }

    /**
     * Test health changing of main character
     */
    @Test
    public void healthChangeTest() { 
	// test if character can be hurt 
        testCharacter.changeHealth(50);
        Assert.assertEquals(testCharacter.getHealth(), 50);
        testCharacter.changeHealth(-2);

	// test if character can die 
        Assert.assertEquals(testCharacter.getHealth(), 48);
        testCharacter.changeHealth(-48);
        Assert.assertEquals(testCharacter.getHealth(), 0);
        Assert.assertEquals(testCharacter.getDeaths(), 2);
    }

    /**
     * Test playerHurt effect
     */
    @Test
    public void playerHurtTest() {
        // Set isHurt to true.
        testCharacter.playerHurt(3);
        Assert.assertTrue(testCharacter.isHurt());
        // Health decreases
        Assert.assertEquals(47, testCharacter.getHealth());
        // set current animation to hurt
        Assert.assertEquals(AnimationRole.HURT, testCharacter.getCurrentState());
        // set hurt time and recover time to 0.
        Assert.assertEquals(0, testCharacter.getHurtTime());
        Assert.assertEquals(0, testCharacter.getRecoverTime());

        // test checkIfHurtEnded()
        testCharacter.checkIfHurtEnded();
        // hurt time increases by 20.
        Assert.assertEquals(20, testCharacter.getHurtTime());
        // after hurt animation finished (around 2 seconds),
        // finish hurting, start recovering.
        testCharacter.setHurtTime(500);
        testCharacter.checkIfHurtEnded();
        // set animation status to "not hurt" and is recovering.
        Assert.assertFalse(testCharacter.isHurt());
        Assert.assertTrue(testCharacter.isRecovering());
        // reset hurt time.
        Assert.assertEquals(0, testCharacter.getHurtTime());
    }

    /**
     * Test recover effect
     */
    @Test
    public void playerRecoverTest() {
        // Set the health status of player from hurt back to normal
        // so that the effect (e.g. sprite flashing) will disappear
        // after recovering.
        testCharacter.checkIfRecovered();
        testCharacter.checkIfRecovered();
        // recover time increased by 20.
        Assert.assertEquals(20, testCharacter.getRecoverTime());
        // main character unable to be touched by other objects.
        Assert.assertFalse(testCharacter.getCollidable());

        // After recovered (around 3 seconds)...
        testCharacter.setRecoverTime(3000);
        testCharacter.checkIfRecovered();
        // reset recover time.
        Assert.assertEquals(0, testCharacter.getRecoverTime());
        // main character able to be touched by other objects again.
        Assert.assertTrue(testCharacter.getCollidable());
        // set animation/sprite status to "not recovering".
        Assert.assertFalse(testCharacter.isRecovering());
    }

    /**
     * Test kill effect and method
     */
    @Test
    public void playerKillTest() {
        // call kill() when character's health is 0.
        testCharacter.playerHurt(100);
        // set animation status to DEAD.
        Assert.assertEquals(AnimationRole.DEAD, testCharacter.getCurrentState());
        // reset dead time to 0.
        Assert.assertEquals(0, testCharacter.getDeadTime());
        // main character's number of death increased by 1.
        Assert.assertEquals(1, testCharacter.getDeaths());
    }

    /**
     * Test whether the animation role is updated when a 
     * method is called.
     */
    @Test
    public void updateAnimationTest() {
        // test hurt animation state
        testCharacter.playerHurt(2);
        test Character.updateAnimation();
        assertEquals(AnimationRole.HURT, testCharacter.getCurrentState());
        testCharacter.setHurt(false);
    }

    /**
     * Test whether the right animation(state, texture, direction, anim name) is running 
     */
    @Test
    public void movementAnimationsExist() {
        testCharacter.setCurrentState(AnimationRole.MOVE);
        testCharacter.setCurrentDirection(Direction.EAST);

        AnimationLinker al = testCharacter.getToBeRun();
        Assert.assertEquals(al.getAnimationName(), "MainCharacterE_Anim");
        Assert.assertEquals(al.getType(), AnimationRole.MOVE);
    }

    /**
     * Test whether the right animation texture is applied on the main character  
     */
    @Test
    public void directionTexturesExist() {
        testCharacter.setCurrentDirection(Direction.EAST);
        String s = testCharacter.getDefaultTexture();
        Assert.assertEquals(s, "__ANIMATION_MainCharacterE_Anim:0");

        testCharacter.setCurrentDirection(Direction.WEST);
        s = testCharacter.getDefaultTexture();
        Assert.assertEquals(s, "__ANIMATION_MainCharacterW_Anim:0");

        testCharacter.setCurrentDirection(Direction.NORTH);
        s = testCharacter.getDefaultTexture();
        Assert.assertEquals(s, "__ANIMATION_MainCharacterN_Anim:0");
    }
}
