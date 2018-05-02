package edu.brown.cs.dreamteam.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.brown.cs.dreamteam.datastructures.Vector;
import edu.brown.cs.dreamteam.entity.Obstacle;
import edu.brown.cs.dreamteam.game.GameMap;
import edu.brown.cs.dreamteam.item.Item;
import edu.brown.cs.dreamteam.item.Type;
import edu.brown.cs.dreamteam.weapon.EnergyBlast;

// note: radius of obstacles hs to be at least 15 to be valid,
// unless we want the pick-up items to be literal specks
public class DummyGameMap implements GameMap {

  @Override
  public Collection<Obstacle> getObstacles() {
    Obstacle x = new Obstacle("x", new Vector(15, 15), 5);
    Obstacle y = new Obstacle("y", new Vector(40, 50), 10);
    List<Obstacle> z = new ArrayList<>();
    z.add(x);
    z.add(y);
    return z;
  }

  @Override
  public Collection<Item> getItems() {
    Item a = new Item("Item1", new Vector(3, 60), Type.WEAPON,
        new EnergyBlast());
    List<Item> z = new ArrayList<Item>();
    z.add(a);

    return z;
  }

}
