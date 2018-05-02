package edu.brown.cs.dreamteam.game;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.brown.cs.dreamteam.datastructures.Vector;
import edu.brown.cs.dreamteam.entity.Entity;
import edu.brown.cs.dreamteam.entity.GamePlayer;
import edu.brown.cs.dreamteam.entity.Interactable;
import edu.brown.cs.dreamteam.event.ClientState;
import edu.brown.cs.dreamteam.item.Item;

/**
 * Chunk Map is the primary location of our storage of entity information,
 * including which areas they affect.
 * 
 * @author peter
 *
 */
public class ChunkMap {

  private final int height;
  private final int width;
  private final int chunkSize;

  private final int totalWidth;
  private final int totalHeight;

  private Chunk[][] chunks;

  private Map<String, GamePlayer> players;
  private Map<String, Entity> entities;

  private int tickCount;

  /**
   * Constructor for ChunkMap.
   * 
   * @param width
   *          The number of chunks wide
   * @param height
   *          the number of chunks high
   * @param chunkSize
   *          the size of each chunk
   */
  public ChunkMap(int width, int height, int chunkSize) {
    this.height = height;
    this.width = width;
    this.chunkSize = chunkSize;
    totalWidth = width * chunkSize;
    totalHeight = height * chunkSize;
    init();
  }

  private void init() {
    chunks = new Chunk[height][width];
    entities = new HashMap<String, Entity>();
    players = new HashMap<String, GamePlayer>();
    initChunks();

  }

  /**
   * Initializes each chunk.
   */
  private void initChunks() {

    for (int r = 0; r < height; r++) {
      for (int c = 0; c < width; c++) {
        chunks[r][c] = new Chunk(r, c);

      }
    }
  }

  /**
   * Given clientStates, updates each client to fit the client state.
   * 
   * @param clientStates
   *          A Map of ID to client state that holds information about the state
   *          of all clients
   */
  public void updateClients(Map<String, ClientState> clientStates) {

    for (Entry<String, ClientState> entry : clientStates.entrySet()) {
      String clientId = entry.getKey();
      GamePlayer player = players.get(clientId);
      player.update(entry.getValue());

    }
  }

  /**
   * Ticks all entities in the Chunk Map.
   */
  public void tick() {
    for (Entity e : entities.values()) {
      e.tick(this);
    }
    tickCount++;
  }

  public int getChunkRow(double ypos) {
    int rpos = (totalHeight - (int) ypos - 1);
    return rpos / chunkSize;
  }

  public int getChunkCol(double xpos) {
    int cpos = (int) xpos;
    return cpos / chunkSize;
  }

  /**
   * Adds a player to the game.
   * 
   * @param player
   *          the game player to be added
   */
  public void addPlayer(GamePlayer player) {
    players.put(player.getId(), player);
    entities.put(player.getId(), player);

  }

  public void addInteractable(Interactable e) {
    Collection<Chunk> chunks = chunksInRange(e);
    for (Chunk c : chunks) {
      c.addInteractable(e);
    }
  }

  public void addItem(Item item) {
    Chunk c = chunkFromPosition(item.center());
    c.addItem(item);
  }

  public Collection<Chunk> chunksInRange(Interactable e) {
    return chunksInRange(e, e.reach());

  }

  public Collection<Chunk> chunksInRange(Entity e, double radius) {
    double left = e.center().x - radius;
    double right = e.center().x + radius;
    double top = e.center().y + radius;
    double bottom = e.center().y - radius;

    int fromRow = getChunkRow(top);
    int toRow = getChunkRow(bottom);
    int fromCol = getChunkCol(left);
    int toCol = getChunkCol(right);

    return chunksInRange(fromRow, toRow, fromCol, toCol);
  }

  /**
   * Returns the chunks in the range of the bounds.
   * 
   * @param fromRow
   *          the row to start
   * @param toRow
   *          the row to end
   * @param fromCol
   *          the col to start
   * @param toCol
   *          the col to end
   * @return A collection of Chunks within the range of the bounds
   */
  public Collection<Chunk> chunksInRange(int fromRow, int toRow, int fromCol,
      int toCol) {
    Collection<Chunk> res = new LinkedList<Chunk>();
    for (int r = Math.max(fromRow, 0); r <= toRow && r < height; r++) {
      for (int c = Math.max(fromCol, 0); c <= toCol && c < width; c++) {
        res.add(chunks[r][c]);
      }
    }

    return res;

  }

  public Chunk chunkFromPosition(Vector position) {
    int row = getChunkRow(position.y);
    int col = getChunkCol(position.x);

    return chunks[row][col];
  }

  public Set<Interactable> entitiesFromChunks(Collection<Chunk> chunks) {
    Set<Interactable> res = new HashSet<Interactable>();
    for (Chunk c : chunks) {
      res.addAll(c.getInteractable());
    }

    return res;
  }

  public Set<Item> itemsFromChunks(Collection<Chunk> chunks) {
    Set<Item> res = new HashSet<>();
    for (Chunk c : chunks) {
      res.addAll(c.getItems());
    }
    return res;
  }

  public Collection<GamePlayer> getPlayers() {
    return players.values();
  }

  public int tickCount() {
    return tickCount;
  }

}
