package deco2800.skyfall.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import deco2800.skyfall.GameScreen;
import deco2800.skyfall.Tickable;
import deco2800.skyfall.animation.Animatable;
import deco2800.skyfall.animation.AnimationLinker;
import deco2800.skyfall.animation.AnimationRole;
import deco2800.skyfall.animation.Direction;
import deco2800.skyfall.buildings.BuildingType;
import deco2800.skyfall.buildings.DesertPortal;
import deco2800.skyfall.buildings.ForestPortal;
import deco2800.skyfall.buildings.MountainPortal;
import deco2800.skyfall.entities.spells.Spell;
import deco2800.skyfall.entities.spells.SpellCaster;
import deco2800.skyfall.entities.spells.SpellFactory;
import deco2800.skyfall.entities.spells.SpellType;
import deco2800.skyfall.entities.vehicle.AbstractVehicle;
import deco2800.skyfall.entities.vehicle.Bike;
import deco2800.skyfall.entities.vehicle.SandCar;
import deco2800.skyfall.entities.weapons.*;
import deco2800.skyfall.gamemenu.HealthCircle;
import deco2800.skyfall.gamemenu.popupmenu.ConstructionTable;
import deco2800.skyfall.managers.*;
import deco2800.skyfall.observers.KeyDownObserver;
import deco2800.skyfall.observers.KeyUpObserver;
import deco2800.skyfall.observers.TouchDownObserver;
import deco2800.skyfall.resources.Blueprint;
import deco2800.skyfall.resources.GoldPiece;
import deco2800.skyfall.resources.Item;
import deco2800.skyfall.resources.ManufacturedResources;
import deco2800.skyfall.resources.items.Hatchet;
import deco2800.skyfall.resources.items.PickAxe;
import deco2800.skyfall.saving.AbstractMemento;
import deco2800.skyfall.saving.Save;
import deco2800.skyfall.util.HexVector;
import deco2800.skyfall.util.WorldUtil;
import deco2800.skyfall.worlds.Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import static deco2800.skyfall.buildings.BuildingType.*;

/**
 * Main character in the game
 */
public class MainCharacter extends Peon
        implements KeyDownObserver, KeyUpObserver, TouchDownObserver, Tickable, Animatable {

    public static final String ANIMATION_MAIN_CHARACTER_E_ANIM_0 = "__ANIMATION_MainCharacterE_Anim:0";
    public static final String CONSTRUCTION_TABLE = "constructionTable";
    public static final String SAND_CAR = "sand_car";
    public static final String SWORD = "sword";
    public static final String BOW = "bow";
    public static final String AXE = "axe";
    public static final String PICK_AXE = "Pick Axe";
    public static final String HATCHET = "Hatchet";
    private static MainCharacter mainCharacterInstance = null;
    private boolean residualFromPopUp = false;

    /**
     * Removes the stored main character instance so that the next call to any of
     * the {@code getInstance} methods will create a new {@code MainCharacter}.
     */
    public static void resetInstance() {
        mainCharacterInstance = null;
    }

    public static MainCharacter getInstance(float col, float row, float speed, String name, int health,
            String[] textures) {
        if (mainCharacterInstance == null) {
            mainCharacterInstance = new MainCharacter(col, row, speed, name, health, textures);
        }
        return mainCharacterInstance;
    }

    public static MainCharacter getInstance(float col, float row, float speed, String name, int health) {
        if (mainCharacterInstance == null) {
            mainCharacterInstance = new MainCharacter(col, row, speed, name, health);
        }
        return mainCharacterInstance;
    }

    public static MainCharacter getInstance() {
        if (mainCharacterInstance == null) {
            mainCharacterInstance = new MainCharacter(0, 0, 0.05f, "Main Piece", 50);
        }
        return mainCharacterInstance;
    }

    public static void loadMainCharacter(MainCharacterMemento memento, Save save) {
        if (mainCharacterInstance == null) {
            mainCharacterInstance = new MainCharacter(memento, save);
        } else {
            mainCharacterInstance.load(memento);
        }
    }

    // The id of the character for storing in a database
    private long id;

    /**
     * Sets the save of the main character
     *
     * @param save the save of the main character
     */
    public void setSave(Save save) {
        this.save = save;
    }

    // The save file of this character
    private Save save;

    // Logger to show messages
    private final Logger logger = LoggerFactory.getLogger(MainCharacter.class);

    // Manager for all of MainCharacter's inventories
    private InventoryManager inventories;

    /**
     * Gets toBuild
     *
     * @return The toBuild value
     */
    public boolean isToBuild() {
        return toBuild;
    }

    private boolean toBuild;
    // List of blueprints that the player has learned.

    private List<Blueprint> blueprintsLearned;
    private PetsManager petsManager;


    /**
     * Gets the crafted buildings
     *
     * @return The crafted buildings
     */
    public List<BuildingType> getCraftedBuildings() {
        return craftedBuildings;
    }

    private List<BuildingType> craftedBuildings;

    /**
     * Gets the constructed builds
     *
     * @return The constructored buildings
     */
    public List<BuildingType> getConstructedBuildings() {
        return constructedBuildings;
    }

    private List<BuildingType> constructedBuildings;

    // Variables to sound effects
    private static final String WALK_NORMAL = "walk_D";
    public static final String HURT_SOUND_NAME = "be_hit";
    public static final String DIED_SOUND_NAME = "died";

    public static final String BOWATTACK = "bow_standard";
    public static final String AXEATTACK = "axe_standard";
    public static final String SWORDATTACK = "sword_standard";
    public static final String SPEAR = "spear";

    // Level/point system for the Main Character to be recorded as game goes on
    private int level;

    // Amount of time reviving to update health
    private int revive;

    /*
     * Food is from 100 to 0 and goes down as the Player does actions such as: -
     * Walking - Combat - Resource Collecting Once the food level reaches 0, the
     * Player begins to starve, and starts to lose health points. Still unsure if I
     * should implement time based starvation where as time goes on, the Player
     * loses hunger.
     */
    private int foodLevel;

    // The accumulated food tick to tick
    private float foodAccum;

    // Textures for all 6 directions to correspond to movement of character

    // A goldPouch to store the character's gold pieces.
    private HashMap<Integer, Integer> goldPouch;

    /**
     * The direction and speed of the MainCharacter
     */
    protected Vector2 direction;
    protected float currentSpeed;

    /*
     * Helper vars to tell which direction the player intends to move
     */
    private int xInput;
    private int yInput;
    private float acceleration;
    private float maxSpeed;
    private double vel;
    private ArrayList<Integer> velHistoryX;
    private ArrayList<Integer> velHistoryY;
    private boolean isMoving;
    private boolean canSwim;
    private boolean isSprinting;

    // Is the camera locked onto the main character
    private boolean cameraLock = true;

    /*
     * Used for combat testing melee/range weapons. What number item slot the player
     * has pressed. e.g. 1 = test range weapon 2 = test melee weapon
     */
    private int itemSlotSelected = 1;

    /**
     * How long does MainCharacter playerHurt status lasts,
     */
    private long hurtTime = 0;

    /**
     * How long does MainCharacter take to recover,
     */
    private long recoverTime = 3000;

    /**
     * How long does MainCharacter take to dead before game over screen shows,
     */
    private long deadTime = 500;

    /**
     * Check whether MainCharacter is in a certain state.
     */
    private boolean isHurt = false;
    private boolean isRecovering = false;
    private boolean isTexChanging = false;

    /**
     * Item player is currently equipped with/holding.
     */
    protected Item equippedItem;

    /**
     * The spell the user currently has selected to cast.
     */
    protected SpellType spellSelected = SpellType.NONE;

    /**
     * Used to cast spells.
     */
    protected SpellCaster spellCaster = null;

    /**
     * How much mana the character has available for spellcasting.
     */
    protected int mana = 100;

    // Current time in interval to restore mana.
    protected int manaCD = 0;

    // Tick interval to restore mana.
    protected int totalManaCooldown = 10;

    /**
     * The GUI health bar for the character.
     */
    private HealthCircle healthBar;

    /**
     * Loads a main character from a memento
     *
     * @param memento the memento to load the character.
     * @param save    The save the character is in
     */
    private MainCharacter(MainCharacterMemento memento, Save save) {
        super(memento.row, memento.col, 0, "Main Character", memento.health, "MainCharacter");
        this.load(memento);
        this.save = save;
        this.setTexture(ANIMATION_MAIN_CHARACTER_E_ANIM_0);
        this.setHeight(1);
        this.setObjectName("MainPiece");
        this.setRecovering(false);
        GameManager.getManagerFromInstance(InputManager.class).addKeyDownListener(this);
        GameManager.getManagerFromInstance(InputManager.class).addKeyUpListener(this);
        GameManager.getManagerFromInstance(InputManager.class).addTouchDownListener(this);
        this.petsManager = GameManager.getManagerFromInstance(PetsManager.class);
        this.inventories = GameManager.getManagerFromInstance(InventoryManager.class);
        setUpCharacter();
    }

    private void setUpCharacter() {
        this.goldPouch = new HashMap<>();

        xInput = 0;
        yInput = 0;
        setAcceleration(10.f);
        setMaxSpeed(5.f);
        vel = 0;
        velHistoryX = new ArrayList<>();
        velHistoryY = new ArrayList<>();

        blueprintsLearned = new ArrayList<>();

        this.equippedItem = new EmptyItem();
        isMoving = false;

        // Sets the filters so that MainCharacter doesn't collide with projectile.
        for (Fixture fix : getBody().getFixtureList()) {
            Filter filter = fix.getFilterData();
            filter.categoryBits = (short) 0x2; // Set filter category to 2
            filter.maskBits = (short) (0xFFFF ^ 0x4); // remove mask category 4 (projectiles)
            fix.setFilterData(filter);
        }

        isSprinting = false;

        canSwim = false;
        this.scale = 0.4f;
        setDirectionTextures();
        configureAnimations();

        spellCaster = new SpellCaster(this);
    }

    /**
     * Base Main Character constructor
     */
    public MainCharacter(float col, float row, float speed, String name, int health) {
        super(row, col, speed, name, health, "MainCharacter");
        toBuild = false;
        this.id = System.nanoTime();
        this.setTexture(ANIMATION_MAIN_CHARACTER_E_ANIM_0);
        this.setHeight(1);
        this.setObjectName("MainPiece");
        this.setMaxHealth(health);
        constructedBuildings = new ArrayList<>();
        craftedBuildings = new ArrayList<>();

        InputManager inputManager = GameManager.getManagerFromInstance(InputManager.class);
        inputManager.addKeyDownListener(this);
        inputManager.addKeyUpListener(this);
        inputManager.addTouchDownListener(this);

        this.petsManager = GameManager.getManagerFromInstance(PetsManager.class);

        this.inventories = GameManager.getManagerFromInstance(InventoryManager.class);

        this.level = 1;

        // create a new goldPouch object
        setUpCharacter();
    }

    /**
     * Constructor with various textures
     *
     * @param textures A array of length 6 with string names corresponding to
     *                 different orientation 0 = North 1 = North-East 2 = South-East
     *                 3 = South 4 = South-West 5 = North-West
     */
    private MainCharacter(float col, float row, float speed, String name, int health, String[] textures) {
        this(col, row, speed, name, health);
        this.setTexture(textures[2]);
    }

    /**
     * Setup the character specific gui elements.
     */
    public void setUpGUI() {
        this.setupHealthBar();
        this.setupGameOverScreen();
    }

    /**
     * Set up the health bar.
     */
    private void setupHealthBar() {
        // If the health bar does not equal null, create it
        if (this.healthBar != null) {
            this.healthBar = (HealthCircle) GameManager.getManagerFromInstance(GameMenuManager.class)
                    .getUIElement("healthCircle");
        }
    }

    /**
     * Set up the game over screen.
     */
    private void setupGameOverScreen() {
        // If the gameMenuManager does not equal null, create the game over screen
        // The GUI PopUp for the character
        GameMenuManager gameMenuManager = GameManager.getManagerFromInstance(GameMenuManager.class);
        if (gameMenuManager != null) {
            gameMenuManager.hideOpened();
            gameMenuManager.setPopUp("gameOverTable");
            gameMenuManager.getPopUp("gameOverTable");
            logger.info("Game Over");
        }
    }

    /**
     * Switch the item the MainCharacter has equip.
     *
     * @param keyCode Keycode the player has pressed.
     */
    protected void switchItem(int keyCode) {
        // If key is in range of 1-9, accept the input.
        if (keyCode >= 8 && keyCode <= 16) {
            int keyNumber = Integer.parseInt(Input.Keys.toString(keyCode));
            this.itemSlotSelected = keyNumber;
            logger.info("Switched to item: {}", keyNumber);
        }
    }

    /**
     * Sets the player's equipped item
     *
     * @param item the item to equip
     */
    public boolean setEquippedItem(Item item) {
        if (item.isEquippable()) {
            if (!this.equippedItem.getName().equals(new EmptyItem().getName())) {
                this.inventories.add(this.equippedItem);
            }
            this.equippedItem = item;
            return true;
        } else {
            logger.warn("You can't equip {}.", item.getName());
            return false;
        }
    }

    /**
     * Sets the equipped item to be null when it runs out of durability
     */
    public void unEquip() {
        // Return item to a tile in the world
        if (equippedItem instanceof Weapon) {
            Tile returnTile = GameManager.get().getWorld().getTile(((Weapon) equippedItem).getPosition());
            GameManager.get().getWorld().addEntity(((Weapon) equippedItem).newInstance(returnTile));
        }

        this.equippedItem = new EmptyItem();

    }

    /**
     * Returns the players equipped item
     *
     * @return Item object that player is equipped with
     */
    public Item getEquippedItem() {
        return equippedItem;
    }

    /**
     * Gets pestManager
     *
     * @return
     */
    public PetsManager getPetsManager() {
        return this.petsManager;
    }

    /**
     * Returns string of players equipped item, or "No item equipped" if
     * equippedItem == null
     *
     * @return String of equipped item
     */
    public String displayEquippedItem() {
        if (equippedItem != null) {
            return equippedItem.toString();
        } else {
            return "No item equipped";
        }
    }

    /**
     * Use the function of equipped item
     */
    public void useEquipped() {
        if ((equippedItem instanceof Weapon && !((Weapon) equippedItem).isUsable())
                || (equippedItem instanceof ManufacturedResources
                        && !((ManufacturedResources) equippedItem).isUsable())) {
            this.unEquip();
            return;
        }

        equippedItem.use(this.getPosition());
    }

    /**
     * Return the currently selected item slot.
     *
     * @return The item slot the MainCharacter has equip.
     */
    public int getItemSlotSelected() {
        return this.itemSlotSelected;
    }

    /**
     * Attack with the weapon the character has equip.
     */
    public void attack(HexVector mousePosition) {
        // Animation control
        logger.debug("Attacking");

        setCurrentState(AnimationRole.ATTACK);

        // If there is a spell selected, spawn the spell.
        // else, just fire off a normal projectile.
        if (this.spellSelected != SpellType.NONE) {
            this.castSpell(mousePosition, spellSelected);
        } else {
            this.fireProjectile(mousePosition);
        }
    }

    protected Projectile defaultProjectile = null;

    /**
     * Fire a projectile in the position that the mouse is in.
     *
     * @param mousePosition The position of the user's mouse.
     */
    protected Projectile currentProjectile = null;
    protected boolean currentAttackIsMelee = false;

    protected void fireProjectile(HexVector mousePosition) {
        HexVector unitDirection = mousePosition.subtract(this.getPosition()).normalized();

        setCurrentState(AnimationRole.ATTACK);

        // Make projectile move toward the angle
        // Spawn projectile in front of character

        int weaponRange = equippedItem.getSubtype().equals("range") ? 10 : 0;
        int range = this.itemSlotSelected == 1 ? weaponRange : 0;

        if (!(equippedItem instanceof Weapon)) {
            return;
        }

        if (currentProjectile != null && !(currentProjectile.beenDestroyed)) {
            return;
        }

        currentAttackIsMelee = !(((Weapon) equippedItem).getWeaponType().equals("range"));

        // If there is a default projectile selected to fire, use that.
        if (defaultProjectile == null) {
            currentProjectile = new Projectile(mousePosition, ((Weapon) equippedItem).getTexture("attack"), "hitbox",
                    new HexVector(
                            position.getCol() + 0.5f
                                    + 1.5f * (currentAttackIsMelee ? unitDirection.getRow() : unitDirection.getCol()),
                            position.getRow() + 0.5f
                                    + 1.5f * (currentAttackIsMelee ? -unitDirection.getCol() : unitDirection.getRow())),
                    ((Weapon) equippedItem).getDamage(), ((Weapon) equippedItem).getAttackRate(), range);
        } else {
            currentProjectile = defaultProjectile;
        }

        currentProjectile.setAngle((currentAttackIsMelee ? -90.f : 0.f) + 180.f
                + (float) Math.toDegrees(Math.atan2(unitDirection.getRow(), unitDirection.getCol())));

        // Add the projectile entity to the game world.
        GameManager.get().getWorld().addEntity(currentProjectile);

        // Play weapon attackEntity sound
        switch ((equippedItem).getName()) {
        case SWORD:
            SoundManager.playSound(SWORD);
            break;
        case SPEAR:
            SoundManager.playSound(SPEAR);
            break;
        case BOW:
            SoundManager.playSound(BOWATTACK);
            break;
        case AXE:
            SoundManager.playSound(AXEATTACK);
            break;
        default:
            SoundManager.playSound(HURT_SOUND_NAME);
            break;
        }

    }

    /**
     * Cast the spell in the position that the mouse is in.
     *
     * @param mousePosition The position of the user's mouse.
     */
    private void castSpell(HexVector mousePosition, SpellType spellType) {

        // Unselect the spell.
        this.spellSelected = SpellType.NONE;

        // Create the spell using the factory.
        Spell spell = SpellFactory.createSpell(spellType, mousePosition);

        String spellName = spellType.name();
        logger.info("Spell Case: {}", spellName);

        int manaCost = spell.getManaCost();

        // Check if there is enough mana to attack.
        if (mana < manaCost) {
            return;
        }

        // Subtract some mana, and update the GUI.
        this.mana -= manaCost;

        GameManager.get().getWorld().addEntity(spell);
    }

    /**
     * Set the mana the character has available.
     *
     * @param mana The mana to set for the character.
     */
    public void setMana(int mana) {
        this.mana = mana;
    }

    /**
     * Get the mana the character currently has available.
     *
     * @return The mana the character has available.
     */
    public int getMana() {
        return this.mana;
    }

    /**
     * Lets the player enter a vehicle, by changing there speed and there sprite
     *
     * @param vehicle The vehicle they are entering
     */
    public void enterVehicle(String vehicle) {
        // Determine the vehicle they are entering and set their new speed and
        // texture
        if (vehicle.equals("Camel")) {
            setAcceleration(0.1f);
            setMaxSpeed(0.8f);
        } else if (vehicle.equals("Dragon")) {
            setAcceleration(0.125f);
            setMaxSpeed(1f);
        } else if (vehicle.equals("Boat")) {
            setAcceleration(0.01f);
            setMaxSpeed(0.5f);
        } else {
            setAcceleration(0.03f);
            setMaxSpeed(0.6f);
        }
    }

    /**
     * Lets the player exit the vehicle by setting their speed back to default and
     * changing the texture. Also changing swimming to false in case they were in a
     * boat
     */
    public void exitVehicle() {
        setAcceleration(0.01f);
        setMaxSpeed(0.4f);
    }

    public void pickUpInventory(Item item) {
        this.inventories.add(item);
    }

    /**
     * Attempts to drop given item from inventory
     *
     * @param item item to be dropped from inventory
     */
    public void dropInventory(String item) {
        this.inventories.drop(item);
    }

    /**
     * Player takes damage from other entities/ by starving.
     *
     * @param damage the damage deal to the player.
     */
    public void playerHurt(int damage) {
        // If the player isn't recovering, set hurt
        // and change health/animations
        if (!isRecovering) {
            setHurt(true);
            changeHealth(-damage);
            updateHealth();
            setCurrentState(AnimationRole.HURT);

            // Check if player died and run kill method
            if (this.getHealth() < 1) {
                logger.info("Player died.");
                kill();
            } else {
                hurtTime = 0;
                recoverTime = 0;
                SoundManager.playSound(HURT_SOUND_NAME);

                if (hurtTime >= 400) {
                    setRecovering(true);
                }
            }
        }
    }

    /**
     * Checks if the players hurt is over
     */
    public void checkIfHurtEnded() {
        hurtTime += 20; // playerHurt for 1 second

        if (hurtTime > 400) {
            logger.info("Hurt ended");
            setHurt(false);
            setRecovering(true);
            setTexChanging(true);
            hurtTime = 0;
        }
    }

    /**
     * Helper function to update healthBar outside of class.
     */
    public void updateHealth() {
        if (this.healthBar != null) {
            this.healthBar.update();
        }
    }

    /**
     * Player recovers from being attacked. It removes player 's playerHurt effect
     * (e.g. sprite flashing in red), in playerHurt().
     */
    public boolean isRecovering() {
        return isRecovering;
    }

    /*
     * Set players to recovering if hurt
     */
    public void setRecovering(boolean isRecovering) {
        this.isRecovering = isRecovering;
    }

    public boolean isTexChanging() {
        return isTexChanging;
    }

    public void setTexChanging(boolean isTexChanging) {
        this.isTexChanging = isTexChanging;
    }

    /**
     * Check if player has recovered
     */
    public void checkIfRecovered() {
        recoverTime += 20;
        this.changeCollideability(false);

        if (recoverTime > 1000) {
            logger.info("Recovered");
            setRecovering(false);
            setTexChanging(false);
            changeCollideability(true);
            recoverTime = 0;
        }
    }

    /**
     * Kills the playerr and notifys the game that the player has died and cannot do
     * any actions in game anymore. Once game is retried, quests are reset.
     */
    public void kill() {
        SoundManager.playSound(DIED_SOUND_NAME);
        setCurrentState(AnimationRole.DEAD);
        deadTime = 0;
        setDead(true);

        // Show game over screen
        setupGameOverScreen();
    }

    /**
     * @return if player is in the state of "playerHurt".
     */
    public boolean isHurt() {
        return isHurt;
    }

    /**
     * @param isHurt the player's "playerHurt" status
     */
    @SuppressWarnings("WeakerAccess")
    public void setHurt(boolean isHurt) {
        this.isHurt = isHurt;
    }

    /**
     * Set the players inventory to a predefined inventory e.g for loading player
     * saves
     *
     * @param inventoryContents the save for the inventory
     */
    public void setInventory(Map<String, List<Item>> inventoryContents, List<String> quickAccessContent) {
        this.inventories = new InventoryManager(inventoryContents, quickAccessContent);
    }

    /**
     * Gets the inventory manager of the character, so it can only be modified this
     * way, prevents having it being a public variable
     *
     * @return the inventory manager of character
     */
    public InventoryManager getInventoryManager() {
        return this.inventories;
    }

    /**
     * Change current level of character and increases health by 10
     *
     * @param change amount being added or subtracted
     */
    public void changeLevel(int change) {
        if (level + change >= 1) {
            this.level += change;
        }
    }

    /**
     * Gets the current level of character
     *
     * @return level of character
     */
    public int getLevel() {
        return this.level;
    }

    /**
     * Change the player's appearance to the set texture
     *
     * @param texture the texture to set
     */
    public void changeTexture(String texture) {
        this.setTexture(texture);
    }

    /**
     * Handles mouse click events
     *
     * @param screenX the x position the mouse was pressed at
     * @param screenY the y position the mouse was pressed at
     * @param pointer mouse pointer
     * @param button  the button which was pressed
     */
    public void notifyTouchDown(int screenX, int screenY, int pointer, int button) {
        // only allow left clicks to move player

        if (GameScreen.getIsPaused()) {
            return;
        }

        // Check if player wants to place a building
        if (button == 0) {

            float[] mouse = WorldUtil.screenToWorldCoordinates(Gdx.input.getX(), Gdx.input.getY());
            float[] clickedPosition = WorldUtil.worldCoordinatesToColRow(mouse[0], mouse[1]);

            // Check we have permission to build
            if (toBuild) {
                GameMenuManager gmm = GameManager.getManagerFromInstance(GameMenuManager.class);
                QuestManager qm = GameManager.getManagerFromInstance(QuestManager.class);

                ConstructionTable bs = (ConstructionTable) gmm.getPopUp(CONSTRUCTION_TABLE);
                bs.build(GameManager.get().getWorld(), (int) clickedPosition[0], (int) clickedPosition[1]);
                qm.addBuilding(bs.selectBuilding(bs.getBuildingID(), 0, 0).getBuildingType());
                toBuild = false;
            }

        }

    }

    /**
     * Reset the mana cooldown period and restore 1 mana to the MainCharacter.
     */
    protected void restoreMana() {

        // Reset the cooldown period.
        this.manaCD = 0;

        // Time interval has passed so restore some mana.
        if (this.mana < 100) {
            this.mana++;
        }
    }

    /**
     * Adds a crafted building
     *
     * @param building The BuildingType to add to crafted buildings
     */
    public void addCraftedBuilding(BuildingType building) {
        craftedBuildings.add(building);
    }

    /**
     * Adds a constructed building
     *
     * @param building The building to add to constructed buildings
     */
    public void addConstructedBuilding(BuildingType building) {
        constructedBuildings.add(building);
    }

    /**
     * Removes a crafted building
     *
     * @param building The crated building to remove
     */
    public void removeCraftedBuilding(BuildingType building) {
        craftedBuildings.remove(building);
    }

    /**
     * Removes a constructed building
     *
     * @param building The constructed building to remove
     */
    public void removeConstructedBuilding(BuildingType building) {
        constructedBuildings.remove(building);
    }

    /**
     * Sets the toBuild value
     *
     * @param toBuild The value to toBuild value will be set to
     */
    public void setToBuild(Boolean toBuild) {
        this.toBuild = toBuild;
    }

    /**
     * Handles tick based stuff, e.g. movement
     */
    @Override
    public void onTick(long i) {
        onTickNotPaused();
        this.movementSound();
        this.centreCameraAuto();

        // Mana restoration.
        this.manaCD++;
        if (this.manaCD > totalManaCooldown) {
            this.restoreMana();
        }

        if (currentProjectile != null && !(currentProjectile.beenDestroyed) && currentAttackIsMelee) {
            final float radius = 1.5f;
            float currentAngle = currentProjectile.getAngle();
            currentProjectile.setAngle(currentAngle + 12.f);

            currentProjectile.setPosition((currentProjectile.getPosition()
                    .add(new HexVector((float) (radius * Math.cos(Math.toRadians(currentAngle))),
                            (float) (radius * Math.sin(Math.toRadians(currentAngle)))))).subtract(
                                    new HexVector((float) (radius * Math.cos(Math.toRadians(currentAngle + 12.f))),
                                            (float) (radius * Math.sin(Math.toRadians(currentAngle + 12.f))))));
        }

        if (isHurt) {
            checkIfHurtEnded();
        } else if (isRecovering) {
            checkIfRecovered();
        }
        this.updateAnimation();

        if (isDead()) {
            if (deadTime < 500) {
                deadTime += 20;
            } else {
                GameManager.setPaused(true);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            GameMenuManager gmm = GameManager.getManagerFromInstance(GameMenuManager.class);
            ConstructionTable bs = (ConstructionTable) gmm.getPopUp(CONSTRUCTION_TABLE);
            bs.updateBlueprintShopPanel();
            gmm.setPopUp(CONSTRUCTION_TABLE);
        }

        // After death, check if health is restored after restart
        if (getHealth() < 1) {
            updateHealth();
        }

        // Add to revive if character is not dead
        if (!this.isDead()) {
            revive += 1;
        }

        // Revive health if character has revived for 100 ticks
        if (revive == 100) {
            changeHealth(1);
            updateHealth();
            revive = 0;
        }

    }

    private void onTickNotPaused() {
        if (!GameScreen.getIsPaused()) {
            if (residualFromPopUp) {
                residualInputsFromPopUp();
            }
            this.updatePosition();
        } else {
            SoundManager.stopSound(WALK_NORMAL);
            getBody().setLinearVelocity(0f, 0f);
            residualFromPopUp = true;
        }
    }

    @Override
    public void handleCollision(Object other) {
        // Put specific collision logic here
    }

    private Map<Direction, String> vehicleDirection = new EnumMap<>(Direction.class);

    private Map<Direction, String> vehicleDirection2 = new EnumMap<>(Direction.class);

    private String vehicleType = null;

    private void vehicleTexture(String vehicleName) {
        if (vehicleName.equals("bike")) {
            defaultDirectionTextures = vehicleDirection;
        }
        if (vehicleName.equals(SAND_CAR)) {
            defaultDirectionTextures = vehicleDirection2;
        }

    }

    public void resetVelocity() {
        xInput = 0;
        yInput = 0;
    }

    boolean petout = false;

    /**
     * Sets the appropriate movement flags to true on keyDown
     *
     * @param keycode the key being pressed
     */
    @Override
    public void notifyKeyDown(int keycode) {
        // player cant move when paused
        if (GameManager.getPaused()) {
            xInput = 0;
            yInput = 0;
            return;
        }
        switch (keycode) {
        case Input.Keys.W:
            yInput += 1;
            break;
        case Input.Keys.A:
            xInput += -1;
            break;
        case Input.Keys.S:
            yInput += -1;
            break;
        case Input.Keys.D:
            xInput += 1;
            break;
        case Input.Keys.V:
            petsManager.replacePet(this);
            break;
        case Input.Keys.F:
            vehicleToUse();
            break;
        case Input.Keys.SHIFT_LEFT:
            isSprinting = true;
            maxSpeed *= 2.f;
            break;
        case Input.Keys.SPACE:
            float[] mouse = WorldUtil.screenToWorldCoordinates(Gdx.input.getX(), Gdx.input.getY());
            float[] clickedPosition = WorldUtil.worldCoordinatesToSubColRow(mouse[0], mouse[1]);
            HexVector mousePosition = new HexVector(clickedPosition[0], clickedPosition[1]);

            if (spellSelected != SpellType.NONE) {
                this.attack(mousePosition);
            } else {
                useEquipped();
                fireProjectile(mousePosition);
            }

            break;
        case Input.Keys.ALT_LEFT:
            // Attack moved to SPACE
            break;
        case Input.Keys.Z:
            selectSpell(SpellType.FLAME_WALL);
            break;
        case Input.Keys.X:
            selectSpell(SpellType.SHIELD);
            break;
        case Input.Keys.C:
            selectSpell(SpellType.TORNADO);
            break;
        case Input.Keys.L:
            toggleCameraLock();
            break;
        case Input.Keys.K:
            centreCameraManual();
            break;
        default:
            switchItem(keycode);
            break;
        }

        // Let the SpellCaster know a key was pressed.
        spellCaster.onKeyPressed(keycode);
    }

    public void vehicleToUse() {
        if (!isOnVehicle) {
            notOnVehicle();
        } else {
            if (vehicleType.equals("bike")) {
                defaultDirectionTextures = defaultMainCharacterTextureMap;
                isOnVehicle = false;
                GameManager.get().getWorld().addEntity(new Bike(this.getCol(), this.getRow(), this));
            }
            if (vehicleType.equals(SAND_CAR)) {
                defaultDirectionTextures = defaultMainCharacterTextureMap;
                isOnVehicle = false;
                GameManager.get().getWorld().addEntity(new SandCar(this.getCol(), this.getRow(), this));
            }
        }
    }

    /**
     * When the character is not on a vehicle
     */
    private void notOnVehicle() {
        AbstractVehicle vehicle = null;
        for (AbstractEntity ve : GameManager.get().getWorld().getEntities()) {
            if (ve instanceof Bike && ve.distance(this) < 3) {
                vehicle = (Bike) ve;
                ((Bike) vehicle).removeBike();
                isOnVehicle = true;
                setCurrentState(AnimationRole.NULL);
                vehicleTexture("bike");
                maxSpeed = 10f;
                vehicleType = "bike";
            }
            if (ve instanceof SandCar && ve.distance(this) < 3) {
                vehicle = (SandCar) ve;
                ((SandCar) vehicle).removeSandCar();
                isOnVehicle = true;
                setCurrentState(AnimationRole.NULL);
                vehicleTexture(SAND_CAR);
                maxSpeed = 10f;
                vehicleType = SAND_CAR;
            }
        }
    }

    /**
     * Select the spell that the character is ready to cast. When they next click
     * attack, this spell will cast.
     *
     * @param type The SpellType to cast.
     */
    public void selectSpell(SpellType type) {
        this.spellSelected = type;
    }

    public void residualInputsFromPopUp() {
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            yInput += 1;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            xInput += -1;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            yInput += -1;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            xInput += 1;
        }

        residualFromPopUp = false;

    }

    /**
     * Sets the appropriate movement flags to false on keyUp
     *
     * @param keycode the key being released
     */
    @Override
    public void notifyKeyUp(int keycode) {
        // Player cant move when paused
        if (GameManager.getPaused()) {
            xInput = 0;
            yInput = 0;
            return;
        }

        switch (keycode) {
        case Input.Keys.W:
            yInput -= 1;
            break;
        case Input.Keys.A:
            xInput -= -1;
            break;
        case Input.Keys.S:
            yInput -= -1;
            break;
        case Input.Keys.D:
            xInput -= 1;
            break;
        case Input.Keys.SHIFT_LEFT:
            isSprinting = false;
            maxSpeed /= 2.f;
            break;
        case Input.Keys.SPACE:
            SoundManager.stopSound(WALK_NORMAL);
            break;
        default:
            break;
        }
    }

    /**
     * Adds a piece of gold to the Gold Pouch
     *
     * @param gold  The piece of gold to be added to the pouch
     * @param count How many of that piece of gold should be added
     */
    public void addGold(GoldPiece gold, Integer count) {
        // store the gold's value (5G, 10G etc) as a variable
        Integer goldValue = gold.getValue();

        // if this gold value already exists in the pouch
        if (goldPouch.containsKey(goldValue)) {
            // add this piece to the already existing list of pieces
            goldPouch.put(goldValue, goldPouch.get(goldValue) + count);
        } else {
            goldPouch.put(goldValue, count);
        }
    }

    /**
     * Removes one instance of a gold piece in the pouch with a specific value.
     *
     * @param goldValue The value of the gold piece to be removed from the pouch.
     */
    public void removeGold(Integer goldValue) {
        // if this gold value does not exist in the pouch
        if (goldPouch.containsKey(goldValue)) {
            if (goldPouch.get(goldValue) > 1) {
                goldPouch.put(goldValue, goldPouch.get(goldValue) - 1);
            } else {
                goldPouch.remove(goldValue);
            }
        }
    }

    /**
     * Removes all instances of a gold piece in the pouch with a specific value.
     */
    public void removeAllGold() {
        goldPouch.clear();
    }

    /**
     * Gets the tile at a position.
     *
     * @param xPos The x position
     * @param yPos The y position
     * @return The Tile at that position
     */
    public Tile getTile(float xPos, float yPos) {
        // Returns tile at left arm (our perspective) of the player
        float tileCol = (float) Math.round(xPos);
        float tileRow = (float) Math.round(yPos);
        if (tileCol % 2 != 0) {
            tileRow += 0.5f;
        }
        return GameManager.get().getWorld().getTile(tileCol, tileRow);
    }

    /**
     * Returns the types of GoldPieces in the pouch and how many of each type exist
     *
     * @return The contents of the Main Character's gold pouch
     */
    public Map<Integer, Integer> getGoldPouch() {
        return new HashMap<>(goldPouch);
    }

    /**
     * Returns the sum of the gold piece values in the Gold Pouch
     *
     * @return The total value of the Gold Pouch
     */
    public Integer getGoldPouchTotalValue() {
        int totalValue = 0;

        for (Entry<Integer, Integer> entry : goldPouch.entrySet()) {
            totalValue += entry.getKey() * entry.getValue();
        }
        return totalValue;
    }

    /**
     * If the player is within 1m of a gold piece, it will be added to their Gold
     * Pouch.
     */
    public void addClosestGoldPiece() {
        for (AbstractEntity entity : GameManager.get().getWorld().getEntities()) {
            if (entity instanceof GoldPiece && this.getPosition().distance(entity.getPosition()) <= 1) {
                this.addGold((GoldPiece) entity, 1);
            }
        }
    }

    /**
     * Moves the player based on current key inputs Called in onTick method
     */
    private void updatePosition() {
        // Gets the players current position
        float xPos = position.getCol();
        float yPos = position.getRow();

        // Gets the tile the player is standing on
        Tile currentTile = getTile(xPos, yPos);

        // Determined friction scaling factor to apply based on current tile
        float friction;
        if (currentTile != null && currentTile.getTexture() != null) {
            // Tile specific friction
            friction = Tile.getFriction(currentTile.getTextureName());
        } else {
            // Default friction
            friction = 1f;
        }

        // Applies friction to the body
        getBody().setLinearDamping(friction);

        // If the player can move to the next tile process the movement
        if (checkTileMovement()) {
            this.processMovement();
        }

        // Updates the players position based on where their body is located
        Vector2 bodyPos = getBody().getPosition();
        this.position.set(bodyPos.x, bodyPos.y);
    }

    /**
     * Checks if the player can move onto the tile they are trying to move onto
     *
     * @return boolean: true if can move, false if can't move
     */
    private boolean checkTileMovement() {
        // Gets the next tile
        Tile tile = getTile(position.getCol() + xInput, position.getRow() + yInput);

        boolean valid = true;

        if (tile == null) {
            valid = false;
        }

        if (tile != null && (tile.getTextureName().contains("water") || tile.getTextureName().contains("lake")
                || tile.getTextureName().contains("ocean")) && !canSwim) {
            valid = false;
        }

        return valid;
    }

    /**
     * Process the movement of the player Only called if the player can move onto
     * the next tile
     */
    private void processMovement() {
        // Gets the players current position
        float xVel = getBody().getLinearVelocity().x;
        float yVel = getBody().getLinearVelocity().y;
        recordVelHistory(xVel, yVel);

        preventSliding(xVel, yVel);

        getBody().applyForceToCenter(new Vector2(xInput * getAcceleration(), yInput * getAcceleration()), true);

        getBody().setLinearVelocity(getBody().getLinearVelocity().limit(maxSpeed));

        updateVel();
    }

    /**
     * Updates the players velocity to prevent the player from sliding around the
     * map
     *
     * @param xVel the player's velocity in the x direction
     * @param yVel the player's velocity in the y direction
     */
    private void preventSliding(float xVel, float yVel) {

        if ((!checkDirection(xInput, xVel) && !checkDirection(yInput, yVel)) || (xInput == 0 && yInput == 0)) {
            getBody().setLinearVelocity(0, 0);
        } else {
            if (!checkDirection(xInput, xVel) || xInput == 0) {
                getBody().setLinearVelocity(0, yVel);
            }

            if (!checkDirection(yInput, yVel) || yInput == 0) {
                getBody().setLinearVelocity(xVel, 0);
            }
        }
    }

    /**
     * Checks if the player is travelling in the same direction as intended
     *
     * @param mainInput the input direction being checked
     * @param vel       the player's velocity in the direction being checked
     * @return true if the player is travelling in the right direction
     */
    private boolean checkDirection(int mainInput, float vel) {
        boolean characterDirection = true;

        if (mainInput != 0 && vel / Math.abs(vel) != mainInput && vel != 0) {
            characterDirection = false;
        }

        return characterDirection;
    }

    /**
     * Records the player velocity history
     *
     * @param xVel The x velocity
     * @param yVel The y velocity
     */
    private void recordVelHistory(float xVel, float yVel) {
        if (velHistoryX.size() < 2 || velHistoryY.size() < 2) {
            velHistoryX.add((int) (xVel * 100));
            velHistoryY.add((int) (yVel * 100));
        } else if (velHistoryX.get(1) != (int) (xVel * 100) || velHistoryY.get(1) != (int) (yVel * 100)) {
            velHistoryX.set(0, velHistoryX.get(1));
            velHistoryX.set(1, (int) (xVel * 100));

            velHistoryY.set(0, velHistoryY.get(1));
            velHistoryY.set(1, (int) (yVel * 100));
        }
    }

    private void updateVel() {
        vel = getBody().getLinearVelocity().len();
    }

    /**
     * Gets the direction the player is currently facing North: 0 deg East: 90 deg
     * South: 180 deg West: 270 deg
     *
     * @return the player direction (units: degrees)
     */
    public double getPlayerDirectionAngle() {
        double val;
        if (xInput != 0 || yInput != 0) {
            val = Math.atan2(yInput, xInput);
        } else if (velHistoryX != null && velHistoryY != null && velHistoryX.size() > 1 && velHistoryY.size() > 1) {
            val = Math.atan2(velHistoryY.get(0), velHistoryX.get(0));
        } else {
            val = 0;
        }
        return -Math.toDegrees(val);
    }

    /**
     * Converts the current players direction into a cardinal direction North,
     * South-West, etc.
     *
     * @return new texture to use
     */
    private String getPlayerDirectionCardinal() {
        double playerDirectionAngle = getPlayerDirectionAngle();
        int playerDirectionIndex = Math.floorMod((int) Math.floor((playerDirectionAngle + 90.0) / 45), 8);

        switch (playerDirectionIndex) {
        case 0:
            setCurrentDirection(Direction.NORTH);
            return "North";
        case 1:
            setCurrentDirection(Direction.NORTH_EAST);
            return "North-East";
        case 2:
            setCurrentDirection(Direction.EAST);
            return "East";
        case 3:
            setCurrentDirection(Direction.SOUTH_EAST);
            return "South-East";
        case 4:
            setCurrentDirection(Direction.SOUTH);
            return "South";
        case 5:
            setCurrentDirection(Direction.SOUTH_WEST);
            return "South-West";
        case 6:
            setCurrentDirection(Direction.WEST);
            return "West";
        case 7:
            setCurrentDirection(Direction.NORTH_WEST);
            return "North-West";
        default:
            return "Invalid";
        }
    }

    /**
     * Sets the players acceleration
     *
     * @param newAcceleration: the new acceleration for the player
     */
    private void setAcceleration(float newAcceleration) {
        this.acceleration = newAcceleration;
    }

    /**
     * Sets the players max speed
     *
     * @param newMaxSpeed: the new max speed of the player
     */
    private void setMaxSpeed(float newMaxSpeed) {
        this.maxSpeed = newMaxSpeed;
    }

    /**
     * Gets a list of the players current velocity 0: x velocity 1: y velocity 2:
     * net velocity
     *
     * @return list of players velocity properties
     */
    public List<Float> getVelocity() {
        ArrayList<Float> velocity = new ArrayList<>();
        velocity.add(getBody().getLinearVelocity().x);
        velocity.add(getBody().getLinearVelocity().y);
        velocity.add((float) vel);
        return velocity;
    }

    /**
     * Gets the players current acceleration
     *
     * @return the players acceleration
     */
    public float getAcceleration() {
        return this.acceleration;
    }

    private void movementSound() {
        if (!isMoving && vel != 0) {
            // Runs when the player starts moving
            isMoving = true;
            SoundManager.loopSound(WALK_NORMAL);
        }

        if (isMoving && vel == 0) {
            // Runs when the player stops moving
            isMoving = false;
            SoundManager.stopSound(WALK_NORMAL);
        }
    }

    public void addBlueprint(Blueprint blueprint) {
        if (blueprint != null) {
            this.blueprintsLearned.add(blueprint);
        }
    }

    public List<Blueprint> getUnlockedBlueprints() {
        List<Blueprint> unlocked = new ArrayList<>();

        // For items and general storage
        QuestManager qm = GameManager.get().getManager(QuestManager.class);
        switch (save.getGameStage()) {
        case 3:
            unlocked.add(SAFEHOUSE);
            break;
        case 2:
            unlocked.add(WATCHTOWER);
            break;
        case 1:
            unlocked.add(CABIN);
            break;
        case 0:
            unlocked.add(new Hatchet());
            unlocked.add(new PickAxe());
            unlocked.add(new Sword());
            unlocked.add(new Bow());
            unlocked.add(new Spear());
            unlocked.add(CASTLE);
            break;
        default:
            break;
        }

        // for portals
        switch (save.getGameStage()) {
        case 3:
            if (qm.questFinished()) {
                unlocked.add(new ForestPortal(0, 0, 0));
            }
            break;
        case 2:
            if (qm.questFinished()) {
                unlocked.add(new MountainPortal(0, 0, 0));
            }
            break;
        case 1:
            if (qm.questFinished()) {
                unlocked.add(new DesertPortal(0, 0, 0));
            }
            break;
        case 0:
            if (qm.questFinished()) {
                unlocked.add(new ForestPortal(0, 0, 0));
            }
            break;
        default:
            break;
        }
        return unlocked;
    }

    /***
     * A getter method for the blueprints that the player has learned.
     *
     * @return the learned blueprints list
     */
    public List<Blueprint> getBlueprintsLearned() {
        return this.blueprintsLearned;
    }

    /***
     * Checks if the player has sufficient resources , used in building a new item, returns true if the player
     * has sufficient resources , false otherwise.
     * @param newItem the new Item that the player wants to create
     */
    public boolean checkRequiredResources(Blueprint newItem){

        if (newItem.getRequiredWood() > this.getInventoryManager().getAmount("Wood")) {
            logger.info("You don't haven enough wood");
            return false;

        } else if (newItem.getRequiredStone() > this.getInventoryManager().getAmount("Stone")) {
            logger.info("You don't haven enough stone");
            return false;

        } else if (newItem.getRequiredMetal() > this.getInventoryManager().getAmount("Metal")) {
            logger.info("You don't haven enough metal");
            return false;

        } else {
            return true;
        }
    }

    /***
     * Reduces the required resources form the player , used in item building
     * @param newItem the new Item that the player wants to create
     */
    public void deductRequiredResources(Blueprint newItem){

        this.getInventoryManager().dropMultiple("Metal", newItem.getRequiredMetal());
        this.getInventoryManager().dropMultiple("Stone", newItem.getRequiredStone());
        this.getInventoryManager().dropMultiple("Wood", newItem.getRequiredWood());
    }

    /***
     * Creates an item if the player has the blueprint. Checks if required resources
     * are in the inventory. if yes, creates the item, adds it to the player's
     * inventory and deducts the required resource from inventory
     */
    public void createItem (Blueprint newItem) {
        if (checkRequiredResources(newItem)){
            switch (newItem.getName()) {
                case HATCHET:
                    this.getInventoryManager().add(new Hatchet());
                    break;

                case PICK_AXE:
                    this.getInventoryManager().add(new PickAxe());
                    break;

                case SWORD:
                    this.getInventoryManager().add(new Sword());
                    break;
                case SPEAR:
                    this.getInventoryManager().add(new Spear());
                    break;

                case BOW:
                    this.getInventoryManager().add(new Bow());
                    break;

                case "Cabin":
                    craftedBuildings.add(CABIN);
                    break;

                case "StorageUnit":
                    craftedBuildings.add(STORAGE_UNIT);
                    break;

                case "TownCentre":
                    craftedBuildings.add(TOWNCENTRE);
                    break;

                case "Fence":
                    craftedBuildings.add(FENCE);
                    break;

                case "SafeHouse":
                    craftedBuildings.add(SAFEHOUSE);
                    break;

                case "WatchTower":
                    craftedBuildings.add(WATCHTOWER);
                    break;

                case "Castle":
                    craftedBuildings.add(CASTLE);
                    break;

                case "forestPortal":
                    craftedBuildings.add(FORESTPORTAL);
                    break;
                case "desertPortal":
                    craftedBuildings.add(DESERTPORTAL);
                    break;
                case "mountainPortal":
                    craftedBuildings.add(MOUNTAINPORTAL);
                    break;
                case "volcanoPortal":
                    craftedBuildings.add(VOLCANOPORTAL);
                    break;

                default:
                    logger.info("Invalid Item");
                    break;
                }
                deductRequiredResources(newItem);
        }
    }

    /**
     * Sets the animations.
     */
    @Override
    public void configureAnimations() {

        // Walk animation
        addAnimations(AnimationRole.MOVE, Direction.NORTH_WEST,
                new AnimationLinker("MainCharacterNW_Anim",
                        AnimationRole.MOVE, Direction.NORTH_WEST, true, true));

        addAnimations(AnimationRole.MOVE, Direction.NORTH_EAST,
                new AnimationLinker("MainCharacterNE_Anim",
                        AnimationRole.MOVE, Direction.NORTH_WEST, true, true));

        addAnimations(AnimationRole.MOVE, Direction.SOUTH_WEST,
                new AnimationLinker("MainCharacterSW_Anim",
                        AnimationRole.MOVE, Direction.SOUTH_WEST, true, true));

        addAnimations(AnimationRole.MOVE, Direction.SOUTH_EAST,
                new AnimationLinker("MainCharacterSE_Anim",
                        AnimationRole.MOVE, Direction.SOUTH_EAST, true, true));

        addAnimations(AnimationRole.MOVE, Direction.EAST,
                new AnimationLinker("MainCharacterE_Anim",
                        AnimationRole.MOVE, Direction.EAST, true, true));

        addAnimations(AnimationRole.MOVE, Direction.NORTH,
                new AnimationLinker("MainCharacterN_Anim",
                        AnimationRole.MOVE, Direction.NORTH, true, true));

        addAnimations(AnimationRole.MOVE, Direction.WEST,
                new AnimationLinker("MainCharacterW_Anim",
                        AnimationRole.MOVE, Direction.WEST, true, true));

        addAnimations(AnimationRole.MOVE, Direction.SOUTH,
                new AnimationLinker("MainCharacterS_Anim",
                        AnimationRole.MOVE, Direction.SOUTH, true, true));

        // Attack animation
        addAnimations(AnimationRole.ATTACK, Direction.DEFAULT,
                new AnimationLinker("MainCharacter_Attack_E_Anim",
                AnimationRole.ATTACK, Direction.DEFAULT, false, true));

        // Hurt animation
        addAnimations(AnimationRole.HURT, Direction.EAST,
                new AnimationLinker("MainCharacter_Hurt_E_Anim",
                        AnimationRole.HURT, Direction.EAST, true, true));

        addAnimations(AnimationRole.HURT, Direction.WEST,
                new AnimationLinker("MainCharacter_Hurt_W_Anim",
                        AnimationRole.HURT, Direction.EAST, true, true));

        // Dead animation
        addAnimations(AnimationRole.DEAD, Direction.DEFAULT,
                new AnimationLinker("MainCharacter_Dead_E_Anim",
                        AnimationRole.DEAD, Direction.DEFAULT, false, true));

        // Dead animation
        addAnimations(AnimationRole.STILL, Direction.DEFAULT,
                new AnimationLinker("MainCharacter_Dead_E_Still",
                        AnimationRole.STILL, Direction.DEFAULT, false, true));
    }

    private Map<Direction, String> defaultMainCharacterTextureMap = new EnumMap<>(Direction.class);

    /**
     * Sets default direction textures uses the get index for Animation feature as
     * described in the animation documentation section 4.
     */
    @Override
    public void setDirectionTextures() {

        defaultMainCharacterTextureMap.put(Direction.EAST, ANIMATION_MAIN_CHARACTER_E_ANIM_0);
        defaultMainCharacterTextureMap.put(Direction.NORTH, "__ANIMATION_MainCharacterN_Anim:0");
        defaultMainCharacterTextureMap.put(Direction.WEST, "__ANIMATION_MainCharacterW_Anim:0");
        defaultMainCharacterTextureMap.put(Direction.SOUTH, "__ANIMATION_MainCharacterS_Anim:0");
        defaultMainCharacterTextureMap.put(Direction.NORTH_EAST, "__ANIMATION_MainCharacterNE_Anim:0");
        defaultMainCharacterTextureMap.put(Direction.NORTH_WEST, "__ANIMATION_MainCharacterNW_Anim:0");
        defaultMainCharacterTextureMap.put(Direction.SOUTH_EAST, "__ANIMATION_MainCharacterSE_Anim:0");
        defaultMainCharacterTextureMap.put(Direction.SOUTH_WEST, "__ANIMATION_MainCharacterSW_Anim:0");
        // Bike
        vehicleDirection.put(Direction.SOUTH, "bikeSOUTH");
        vehicleDirection.put(Direction.EAST, "bikeEAST");
        vehicleDirection.put(Direction.NORTH, "bikeNORTH");
        vehicleDirection.put(Direction.WEST, "bikeWEST");
        vehicleDirection.put(Direction.NORTH_EAST, "__ANIMATION_MainCharacterNE_Anim:0");
        vehicleDirection.put(Direction.NORTH_WEST, "__ANIMATION_MainCharacterNW_Anim:0");
        vehicleDirection.put(Direction.SOUTH_EAST, "__ANIMATION_MainCharacterSE_Anim:0");
        vehicleDirection.put(Direction.SOUTH_WEST, "__ANIMATION_MainCharacterSW_Anim:0");
        // Sand Car
        vehicleDirection2.put(Direction.NORTH, "sand_car_NORTH");
        vehicleDirection2.put(Direction.SOUTH, "sand_car_SOUTH");
        vehicleDirection2.put(Direction.EAST, "sand_car_EAST");
        vehicleDirection2.put(Direction.WEST, "sand_car_WEST");
        vehicleDirection2.put(Direction.NORTH_EAST, "sand_car_NORTHEAST");
        vehicleDirection2.put(Direction.NORTH_WEST, "sand_car_NORTHWEST");
        vehicleDirection2.put(Direction.SOUTH_EAST, "sand_car_SOUTHEAST");
        vehicleDirection2.put(Direction.SOUTH_WEST, "sand_car_SOUTHWEST");
        defaultDirectionTextures = defaultMainCharacterTextureMap;
    }

    private boolean isOnVehicle = false;

    /**
     * If the animation is moving sets the animation state to be Move else NULL.
     * Also sets the direction
     */
    public void updateAnimation() {
        getPlayerDirectionCardinal();

        /* Short Animations */
        if (!isOnVehicle) {
            if (getToBeRun() != null && getToBeRun().getType() == AnimationRole.ATTACK) {
                return;
            }

            if (isDead()) {
                setCurrentState(AnimationRole.STILL);
            } else if (isHurt) {
                setCurrentState(AnimationRole.HURT);
            } else {
                if (getVelocity().get(2) == 0f) {
                    setCurrentState(AnimationRole.NULL);
                } else {
                    setCurrentState(AnimationRole.MOVE);
                }
            }
        } else if (vehicleType.equals("bike")) {
            setCurrentState(AnimationRole.VEHICLE_BIKE_MOVE);
        }
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
     * Get the duration for main character to recover.
     *
     * @return the duration for main character to recover.
     */
    public long getRecoverTime() {
        return recoverTime;
    }

    public void setRecoverTime(long recoverTime) {
        this.recoverTime = recoverTime;
    }

    /**
     * Get the duration for main character to die.
     *
     * @return the duration for main character to die.
     */
    public long getDeadTime() {
        return deadTime;
    }

    /**
     * Get the x-coordinate input of the player.
     *
     * @return the x-coordinate input of the player.
     */
    public int getXInput() {
        return xInput;
    }

    /**
     * Get the y-coordinate input of the player.
     *
     * @return the y-coordinate input of the player.
     */
    public int getYInput() {
        return yInput;
    }

    /**
     * Get whether main character is sprinting.
     *
     * @return whether main character is sprinting.
     */
    public boolean getIsSprinting() {
        return isSprinting;
    }

    /**
     * Toggles if the camera should follow the player
     */
    private void toggleCameraLock() {
        if (!cameraLock) {
            cameraLock = true;
            centreCameraManual();
        } else {
            cameraLock = false;
        }
    }

    /**
     * Centres the camera onto the player Designed to called on a loop
     */
    private void centreCameraAuto() {
        if (cameraLock) {
            float[] coords = WorldUtil.colRowToWorldCords(this.getCol(), this.getRow());
            GameManager.get().getCamera().position.set(coords[0], coords[1], 0);

        }
    }

    /**
     * Centres the camera onto the player Not supposed to be called on a loop
     */
    private void centreCameraManual() {
        float[] coords = WorldUtil.colRowToWorldCords(this.getCol(), this.getRow());
        GameManager.get().getCamera().position.set(coords[0], coords[1], 0);
    }

    /**
     * Returns the id of this character
     *
     * @return the id of this character
     */
    public long getID() {
        return this.id;
    }

    /**
     * Sets the id of the main character
     *
     * @param id The id that the main character will be set to
     */
    public void setID(long id) {
        this.id = id;

    }

    /**
     * Returns the save this character is for
     *
     * @return the save this character is for
     */
    public Save getSave() {
        return save;
    }

    public int getGameStage() {
        return this.save.getGameStage();
    }

    public int getFoodLevel() {
        return this.foodLevel;
    }

    public MainCharacterMemento save() {
        return new MainCharacterMemento(this);
    }

    public void load(MainCharacterMemento memento) {
        this.id = memento.mainCharacterID;
        this.level = memento.level;
        this.foodLevel = memento.foodLevel;
        this.foodAccum = memento.foodAccum;
        this.goldPouch = memento.goldPouch;
    }

    public static class MainCharacterMemento extends AbstractMemento implements Serializable {
        private long mainCharacterID;
        private int level;
        private int foodLevel;
        private float foodAccum;
        private float col;
        private float row;
        private int health;
        private HashMap<Integer, Integer> goldPouch;

        public MainCharacterMemento(MainCharacter character) {
            this.col = character.getCol();
            this.row = character.getRow();
            this.mainCharacterID = character.id;
            this.level = character.level;
            this.health = character.getHealth();
            this.foodLevel = character.foodLevel;
            this.foodAccum = character.foodAccum;
            this.goldPouch = character.goldPouch;
        }
    }
}
