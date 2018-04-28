package edu.brown.cs.dreamteam.main;

import java.util.Map;

import edu.brown.cs.dreamteam.event.ClientState;
import edu.brown.cs.dreamteam.event.GameEventListener;
import edu.brown.cs.dreamteam.game.ChunkMap;

/**
 * Architect is an interface holding architects, available to work with both
 * Dummy and Real Architects
 * 
 * @author peter
 *
 */
public abstract class Architect implements Runnable, GameEventListener {

  /**
   * Returns a Map of client ids to clientstates that exist within the
   * Architecture
   * 
   * @return a map of client ids to clientstates that exist within the
   *         Architecture
   */
  public abstract Map<String, ClientState> retrieveClientStates();

  // TODO: pray that put replaces, not adds another if there is a repeat.
  public abstract void putClientState(String name, ClientState state);

  public abstract void initSpark();

  public void onGameChange(ChunkMap chunks, int id) {
    // TODO Auto-generated method stub

  }
}
