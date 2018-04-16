package edu.brown.cs.dreamteam.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

import edu.brown.cs.dreamteam.debug.DummyGameMap;
import edu.brown.cs.dreamteam.entity.GamePlayer;
import edu.brown.cs.dreamteam.event.ClientState;
import edu.brown.cs.dreamteam.game.Chunk;
import edu.brown.cs.dreamteam.game.ChunkMap;
import edu.brown.cs.dreamteam.game.GameEngine;
import edu.brown.cs.dreamteam.utility.Logger;
import freemarker.template.Configuration;
import spark.ExceptionHandler;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.TemplateViewRoute;
import spark.template.freemarker.FreeMarkerEngine;

/**
 * The Main Architect for the project, where all functionality integration the
 * many components exist
 *
 * @author peter
 *
 */
public class SystemArchitect extends Architect {
  private static final Gson GSON = new Gson();

  private GameEngine game;
  private Rooms rooms = new Rooms();
  private Map<String, ClientState> clientStates;

  public SystemArchitect() {
    init();
  }

  private void init() {
    clientStates = Maps.newConcurrentMap();
  }

  public void initSpark() {
    System.out.println("a");
    Spark.externalStaticFileLocation("src/main/resources/static");
    Spark.exception(Exception.class, new ExceptionPrinter());
    FreeMarkerEngine freeMarker = createEngine();
    Spark.webSocket("/xx/websocket", new GameWebSocketHandler(this));
    // Setup Spark Routes
    Spark.get("/", new HomeHandler(), freeMarker);
    Spark.get("/game/:roomID", new GameHandler(), freeMarker);
    Spark.post("/giveStatus", new SendStatusHandler(this));
    Spark.exception(Exception.class, (e, r, er) -> {
      e.printStackTrace();
    });
  }

  @Override
  public void run() {
    Logger.logMessage("Architect is now running");
    Map<String, Thread> threads = threads();

    for (Thread t : threads.values()) {
      t.start();
    }

  }

  /**
   * Returns a thread safe set of ClientStates.
   *
   * @return
   */
  public Map<String, ClientState> retrieveClientStates() {

    return clientStates;
  }

  private Map<String, Thread> threads() {
    Map<String, Thread> threads = new HashMap<>();
    threads.put("game", new Thread(game, "game"));

    return threads;
  }

  @Override
  public void onGameChange(ChunkMap chunks) {
    Collection<GamePlayer> movingThings = chunks.getPlayers();
    Double radius = 5.0;
    System.out.println("here");
    for (GamePlayer p : movingThings) {
      System.out.println("PLAYERRRR");
      Collection<Chunk> chunksNeeded = chunks.getChunksNearPlayer(p, radius);
      Map<String, Object> variables = new ImmutableMap.Builder<String, Object>()
          .put("player", p)
          .put("dynamics", chunks.dynamicFromChunks(chunksNeeded))
          .put("statics", chunks.staticFromChunks(chunksNeeded)).build();
      Messenger.broadcastIndividualMessage(p.getId(),
          "INDIVIDUAL MESSAGE: " + GSON.toJson(variables));
    }
  }

  @Override
  public void putClientState(String name, ClientState state) {
    clientStates.put(name, state);
  }

  @WebSocket
  public class GameWebSocketHandler {
    private String sender, msg;
    private Architect a;

    public GameWebSocketHandler(Architect a) {
      this.a = a;
    }

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
      Messenger.addUserUserID(user);
      Messenger.broadcastMessage(sender = "Server",
          msg = ("Someone joined the chat!"));

    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
      String username = Messenger.sessionUserMap.get(user);
      Messenger.sessionUserMap.remove(user);
      Messenger.userSessionMap.remove(username);
      Messenger.broadcastMessage(sender = "Server",
          msg = (username + " left the chat"));
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
      System.out.println(Messenger.sessionUserMap.get(user));
      System.out.println(message);
      if (message.equals("start")) {
        putClientState(Messenger.sessionUserMap.get(user),
            new ClientState(Messenger.sessionUserMap.get(user)));
        GameEngine engine = GameBuilder.create(a)
            .addHumanPlayer(
                new PlayerSession(Messenger.sessionUserMap.get(user), user))
            .generateMap(new DummyGameMap()).complete();
        System.out.println("Ready to start...");
        new Thread(engine).start();
      } else if (message.equals("left")) {
        ClientState c = clientStates.get(Messenger.sessionUserMap.get(user));
        if (c != null) {
          c.leftHeld(true);
        }
      } else if (message.equals("right")) {
        ClientState c = clientStates.get(Messenger.sessionUserMap.get(user));
        if (c != null) {
          c.rightHeld(true);
        }
      } else if (message.equals("up")) {
        ClientState c = clientStates.get(Messenger.sessionUserMap.get(user));
        if (c != null) {
          c.forwardHeld(true);
        }
      } else if (message.equals("right")) {
        ClientState c = clientStates.get(Messenger.sessionUserMap.get(user));
        if (c != null) {
          c.backwardHeld(true);
        }
      } else if (message.equals("space")) {
        ClientState c = clientStates.get(Messenger.sessionUserMap.get(user));
        if (c != null) {
          c.primaryAction(true);
        }
      } else if (message.equals("f")) {
        ClientState c = clientStates.get(Messenger.sessionUserMap.get(user));
        if (c != null) {
          c.itemPicked(true);
        }
      }

      Messenger.broadcastMessage(sender = Messenger.sessionUserMap.get(user),
          msg = message);
    }
  }

  /**
   * the handler for on start of the game page.
   *
   * @author anina
   */
  private class GameHandler implements TemplateViewRoute {

    @Override
    public ModelAndView handle(Request arg0, Response arg1) throws Exception {
      String room = arg0.params(":roomID");
      Map<String, Object> variables = new ImmutableMap.Builder<String, Object>()
          .put("title", "Game R.A.D.A.R.").put("roomID", room).build();
      return new ModelAndView(variables, "game.ftl");
    }
  }
  //
  // /**
  // * the handler for on start of the homepage.
  // *
  // * @author anina
  // */
  // private class JoinHandler implements TemplateViewRoute {
  //
  // @Override
  // public ModelAndView handle(Request arg0, Response arg1) throws Exception {
  // QueryParamsMap qm = arg0.queryMap();
  // List<String> room = new ArrayList<>(rooms.getNotPlayingYetRoomIDs());
  // String codename = qm.value("codename");
  // if (codename == null || codename.equals("")) {
  // codename = "Guest";
  // }
  // Map<String, Object> variables = new ImmutableMap.Builder<String, Object>()
  // .put("title", "Join R.A.D.A.R.").put("codename", codename)
  // .put("roomIDs", room).build();
  // return new ModelAndView(variables, "join.ftl");
  // }
  // }
  //
  // /**
  // * the handler for on start of the homepage.
  // *
  // * @author anina
  // */
  // private class CreateHandler implements TemplateViewRoute {
  //
  // @Override
  // public ModelAndView handle(Request arg0, Response arg1) throws Exception {
  // String newRoomID = rooms.generateNewRoom();
  // QueryParamsMap qm = arg0.queryMap();
  // String codename = qm.value("codename");
  // if (codename == null || codename.equals("")) {
  // codename = "Guest";
  // }
  // Map<String, Object> variables = new ImmutableMap.Builder<String, Object>()
  // .put("title", "Create R.A.D.A.R.").put("codename", codename)
  // .put("newRoomID", newRoomID).build();
  // return new ModelAndView(variables, "create.ftl");
  // }
  // }

  /**
   * the handler for on start of the homepage.
   *
   * @author anina
   */
  private class HomeHandler implements TemplateViewRoute {

    @Override
    public ModelAndView handle(Request arg0, Response arg1) throws Exception {
      String newRoomID = rooms.generateNewRoom();
      Map<String, Object> variables = new ImmutableMap.Builder<String, Object>()
          .put("title", "R.A.D.A.R.").put("roomID", newRoomID).build();
      return new ModelAndView(variables, "home.ftl");
    }

  }

  private static FreeMarkerEngine createEngine() {
    Configuration config = new Configuration();
    File templates = new File("src/main/resources/spark/template/freemarker");
    try {
      config.setDirectoryForTemplateLoading(templates);
    } catch (IOException ioe) {
      System.out.printf("ERROR: Unable use %s for template loading.%n",
          templates);
      System.exit(1);
    }
    return new FreeMarkerEngine(config);
  }

  /**
   * Display an error page when an exception occurs in the server.
   *
   * @author jj
   */
  private static class ExceptionPrinter implements ExceptionHandler {
    @Override
    public void handle(Exception e, Request req, Response res) {
      res.status(500);
      StringWriter stacktrace = new StringWriter();
      try (PrintWriter pw = new PrintWriter(stacktrace)) {
        pw.println("<pre>");
        e.printStackTrace(pw);
        pw.println("</pre>");
      }
      res.body(stacktrace.toString());
    }
  }

}
