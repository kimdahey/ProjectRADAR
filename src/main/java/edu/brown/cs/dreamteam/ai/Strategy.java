package edu.brown.cs.dreamteam.ai;

import java.util.Collection;

import edu.brown.cs.dreamteam.board.Board;
import edu.brown.cs.dreamteam.board.Position;
import edu.brown.cs.dreamteam.datastructures.Vector;
import edu.brown.cs.dreamteam.game.Chunk;

/**
 * An interface defining a game mode's strategy to control the AI player.
 *
 * @author efu2
 */
public abstract class Strategy {
  protected Board board;
  protected AiPlayer player;

  /**
   * Constructs the strategy class.
   * 
   * @param board
   *          A graph representation of the game board.
   * @param player
   *          The AiPlayer this strategy is for.
   */
  public Strategy(Board board, AiPlayer player) {
    this.board = board;
    this.player = player;
  }

  /**
   * Given the goal Position, updates the AiPlayer's movement coefficients to
   * move towards the goal.
   * 
   * @param goal
   *          The Position that the AI is aiming to move towards.
   */
  protected void moveTo(Position goal) {
    // Get the next node to run to for the shortest path to the goal
    Position curr = getCurrentPosition();
    Position next = board.getMoveTo(curr, goal);

    // Update player to go in direction of next
    double x = next.x - curr.x;
    int horzCoeff = x < 0 ? (x > 0 ? 1 : 0) : -1;
    double y = next.y - curr.y;
    int vertCoeff = y < 0 ? (y > 0 ? 1 : 0) : -1;
    player.updateDynamic(vertCoeff, horzCoeff);
  }

  protected Position getCurrentPosition() {
    Vector position = player.center();
    return new Position(position.x, position.y);
  }

  /**
   * Makes the next move based on the specific strategy's goals.
   *
   * @param chunks
   *          Information about all visible entities.
   */
  abstract void makeNextMove(Collection<Chunk> chunks);
}