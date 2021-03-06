package edu.brown.cs.dreamteam.entity;

import java.util.Objects;

import edu.brown.cs.dreamteam.datastructures.Vector;
import edu.brown.cs.dreamteam.game.ChunkMap;

/**
 * The base class for many elements of our game.
 * 
 * @author peter
 *
 */
public abstract class Entity {

  private final String id;

  /**
   * Each entity must have a unique ID. If two entities are created with the
   * same ID, they are considered the same entity
   * 
   * @param id
   *          The ID of the Entity
   */
  public Entity(String id) {
    this.id = id;

  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return id;
  }

  @Override
  /**
   * Equality and hashcode based on id
   */
  public boolean equals(Object o) {
    if (o instanceof Entity) {
      Entity e = (Entity) o;
      return e.getId().equals(this.getId());
    }
    return false;
  }

  public abstract void tick(ChunkMap chunkMap);

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public abstract Vector center();

}
