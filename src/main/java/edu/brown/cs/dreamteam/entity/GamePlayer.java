package edu.brown.cs.dreamteam.entity;

import java.util.Collection;
import java.util.Set;

import edu.brown.cs.dreamteam.box.Box;
import edu.brown.cs.dreamteam.box.BoxSet;
import edu.brown.cs.dreamteam.box.HitBoxed;
import edu.brown.cs.dreamteam.datastructures.Vector;
import edu.brown.cs.dreamteam.event.ClientState;
import edu.brown.cs.dreamteam.game.Chunk;
import edu.brown.cs.dreamteam.game.ChunkMap;
import edu.brown.cs.dreamteam.game.Inventory;
import edu.brown.cs.dreamteam.item.Item;
import edu.brown.cs.dreamteam.utility.DreamMath;

/**
 * The internal representation of a player in the Game.
 * 
 * @author peter
 *
 */
public class GamePlayer extends DynamicEntity {

  private static final int SIZE = 5;
  private static final int MAX_HEALTH = 100;

  private static final int ITEM_PICK_RANGE = 3;

  private boolean itemPickedFlag;
  private boolean primaryActionFlag; // whether or not we should fire

  private boolean isAlive;
  private double health;

  private Inventory inventory;

  public static GamePlayer player(String sessionId, double xpos, double ypos) {
    return new GamePlayer(sessionId, xpos, ypos);
  }

  /**
   * Constructor for GamePlayer that initializes its origin position as well as
   * its id.
   * 
   * @param id
   *          the unique ID of the player
   * @param xPos
   *          the x position to start
   * @param yPos
   *          the y position to start
   */
  public GamePlayer(String id, double xPos, double yPos) {
    super(id, xPos, yPos, DynamicEntity.SIZE);
    this.setType("HUMAN");

    init();
  }

  private void init() {
    itemPickedFlag = false;
    primaryActionFlag = false;
    isAlive = true;
    inventory = new Inventory();
    health = MAX_HEALTH;
  }

  /**
   * Given a ClientState, updates the internal representations of the GamePlayer
   * to match the state.
   * 
   * @param state
   *          the ClientState to match
   */
  public void update(ClientState state) {
    int horzCoeff = state.retrieveHorzMultiplier();
    int vertCoeff = state.retrieveVertMultiplier();
    updatePlayer(state);
    updateDynamic(vertCoeff, horzCoeff);
  }

  /**
   * Initializes the player flags given the ClientState.
   * 
   * @param state
   *          The ClientState to match
   */
  private void updatePlayer(ClientState state) {
    itemPickedFlag = state.retrieveItemPicked();
    primaryActionFlag = state.retrievePrimaryAction();
  }

  /**
   * Returns if the player is alive at any given point.
   * 
   * @return true if the player is alive, false otherwise
   */
  public boolean isAlive() {
    return isAlive;
  }

  @Override
  public void kill() {
    isAlive = false;
  }

  @Override
  public void tick(ChunkMap chunkMap) {

    Collection<Chunk> chunksInRange = chunkMap.chunksInRange(this);
    for (Chunk c : chunksInRange) {
      c.removeInteractable(this);
    }

    updatePosition(chunkMap); // Calls movement in dynamic entity
    inventory.tick();

    if (primaryActionFlag) { // starts attack
      inventory.getActiveWeapon().fire();
    }
    if (itemPickedFlag) { // picks up items
      Collection<Item> items = chunkMap.itemsFromChunks(chunksInRange);
      Item closest = null;
      for (Item i : items) {
        if (closest == null) {
          closest = i;
        } else {
          closest = i.center().distance(center()) < closest.center()
              .distance(center()) ? i : closest;
        }
      }
      if (closest != null
          && closest.center().distance(center()) < ITEM_PICK_RANGE) {
        inventory.addItem(closest);
      }
    }

    // checks collision and hits them
    Set<Interactable> interactables = chunkMap
        .interactableFromChunks(chunksInRange);
    for (Interactable e : interactables) {
      if (hits(e)) {
        this.hit(e);
      }
    }

    if (health < 0) {
      this.kill();

    } else {
      Collection<Chunk> newChunks = chunkMap.chunksInRange(this);
      for (Chunk c : newChunks) {
        c.addDynamic(this);
      }
    }
  }

  @Override
  public BoxSet hitBox() {
    return inventory.getActiveWeapon().hitBox();

  }

  @Override
  public double reach() {
    double tmp = DreamMath.max(this.collisionBox().reach(),
        this.hitBox().reach(), SIZE);

    return tmp + speedCap();
  }

  @Override
  public BoxSet hurtBox() {
    return collisionBox();
  }

  @Override
  public void getHit(HitBoxed hitBoxed) {
    double damage = hitBoxed.baseDamage();
    health -= damage;
    if (health < 0) {
      kill();
    }

  }

  @Override
  public boolean hits(Interactable hurtBoxed) {

    for (Box b : hitBox().boxes()) {
      for (Box hb : hurtBoxed.hurtBox().boxes()) {
        Vector center = center().add(b.offset());
        Vector center2 = hurtBoxed.center().add(hb.offset());
        double diff = center.subtract(center2).magnitude();
        if (diff < b.radius() + hb.radius()) {
          return true;
        }

      }
    }
    return false;
  }

  @Override
  public double baseDamage() {
    return inventory.getActiveWeapon().baseDamage();
  }

  @Override
  public void hit(Interactable e) {

    e.getHit(this);
  }

}
