package edu.brown.cs.dreamteam.main;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.brown.cs.dreamteam.event.ClientState;
import edu.brown.cs.dreamteam.game.GameEngine;
import edu.brown.cs.dreamteam.game.GameMap;
import edu.brown.cs.dreamteam.map.ItemGameMap;
import edu.brown.cs.dreamteam.map.MainGameMap;
import edu.brown.cs.dreamteam.networking.Messenger;
import edu.brown.cs.dreamteam.networking.PlayerSession;
import edu.brown.cs.dreamteam.utility.Logger;
import freemarker.template.Configuration;
import freemarker.template.Version;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import spark.*;
import spark.template.freemarker.FreeMarkerEngine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * The Main Architect for the project,where all functionality integration
 * the*many components exist**
 *
 * @author peter
 *
 */
public class SystemArchitect extends Architect {
    private static final Gson GSON = new Gson();

    private static final int VERSION_MAJOR = 2;
    private static final int VERSION_MINOR = 3;
    private static final int VERSION_MICRO = 23;

    private GameEngine game;
    private Rooms rooms;
    private AtomicInteger userID;

    private GameMap map;

    public SystemArchitect() {
        map = new MainGameMap();
        rooms = new Rooms();
        userID = new AtomicInteger(6);
    }

    public void initSpark() {
        Spark.externalStaticFileLocation("src/main/resources/static");
        Spark.exception(Exception.class, new ExceptionPrinter());
        FreeMarkerEngine freeMarker = createEngine();
        Spark.webSocket("/websocket", new GameWebSocketHandler(this));
        // Setup Spark Routes
        Spark.get("/", new HomeHandler(), freeMarker);
        Spark.get("/debug", new DebugHandler(), freeMarker);
        Spark.post("/change-map", new ChangeMapHandler());
        Spark.get("/game/:roomID", new GameHandler(), freeMarker);
        // Spark.post("/giveStatus", new SendStatusHandler(this));
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

    private Map<String, Thread> threads() {
        Map<String, Thread> threads = new HashMap<>();
        threads.put("game", new Thread(game, "game"));

        return threads;
    }

    @WebSocket
    public class GameWebSocketHandler {
        private String sender;
        private String msg;
        private Architect a;

        public GameWebSocketHandler(Architect a) {
            this.a = a;
        }

        @OnWebSocketConnect
        public void onConnect(Session user) throws Exception {
            Map<String, List<String>> params = user.getUpgradeRequest()
                    .getParameterMap();
            if (params != null && params.containsKey("roomID")) {
                // As the parameter's value is a List, we use 'get(0)'
                String roomID = params.get("roomID").get(0);
                if (roomID != null) {
                    Room r = rooms.getNotPlayingRoom(roomID);
                    if (r != null) {
                        PlayerSession p = new PlayerSession(
                                new AtomicInteger(userID.getAndIncrement()).toString(), user);
                        r.addPlayer(p);
                        Messenger.broadcastMessage("Someone has joined!", r);
                    }
                }
            }

        }

        @OnWebSocketClose
        public void onClose(Session user, int statusCode, String reason) {
            Map<String, List<String>> params = user.getUpgradeRequest()
                    .getParameterMap();
            if (params != null && params.containsKey("roomID")) {
                // As the parameter's value is a List, we use 'get(0)'
                String roomID = params.get("roomID").get(0);
                Room r = rooms.getNotPlayingRoom(roomID);
                if (r == null) {
                    r = rooms.getPlayingRoom(roomID);
                }
                if (r != null) {
                    r.removePlayer(user);
                }

                if (r != null) {
                    if (r.getPlayers().isEmpty()) {
                        rooms.stopPlaying(roomID);
                    } else {
                        Messenger.broadcastMessage("Someone has left!", r);
                    }
                }
            }

        }

        @OnWebSocketMessage
        public void onMessage(Session user, String message) {
            JsonObject received = GSON.fromJson(message, JsonObject.class);
            ClientState c = null;
            Map<String, List<String>> params = user.getUpgradeRequest()
                    .getParameterMap();
            if (params != null && params.containsKey("roomID")) {
                String roomID = params.get("roomID").get(0);
                Room r = rooms.getPlayingRoom(roomID);
                switch (received.get("type").getAsString()) {
                    case "name":
                        r = rooms.getNotPlayingRoom(roomID);
                        if (r != null) {
                            String username = received.get("status").getAsString();
                            for (PlayerSession p : r.getPlayers()) {
                                if (p.getSession().equals(user)) {
                                    p.setUserName(username);
                                }
                            }
                        }
                        Messenger.broadcastMessage("new name", r);
                        break;
                    case "game":
                        r = rooms.getNotPlayingRoom(roomID);
                        if (r != null) {
                            rooms.startRoom(roomID, r);
                            Logger.logMessage("Creating a new Game");

                            GameEngine engine = getEngine(r, user);
                            Thread x = new Thread(engine);
                            x.start();
                            Messenger.broadcastMessage("start", r);
                        } else {
                            throw new IllegalArgumentException("BAD ROOM??");
                        }
                        break;
                    case "key":
                        c = r.getClient(user);
                        if (c != null) {
                            switch (received.get("status").getAsString()) {
                                case "left":
                                    c.leftHeld(received.get("held").getAsBoolean());
                                    break;
                                case "right":
                                    c.rightHeld(received.get("held").getAsBoolean());
                                    break;
                                case "up":
                                    c.forwardHeld(received.get("held").getAsBoolean());
                                    break;
                                case "down":
                                    c.backwardHeld(received.get("held").getAsBoolean());
                                    break;
                                case "space":
                                    c.primaryAction(true);
                                    break;
                                case "f":
                                    c.itemPicked(true);
                                    break;
                                case "r":
                                    c.placeRadar(true);
                                    break;
                                default:
                                    System.out
                                            .println("EWWOW: key sent that isn't an option...wtf");
                                    break;
                            }
                            r.putOldClient(user, c);
                        }

                        break;
                    default:
                        System.out.println("hewwo");
                        break;
                }
            }
        }

    }

    public GameEngine getEngine(Room r, Session user) {
        GameBuilder builder = GameBuilder.create(getGameMap(), r);
        Collection<PlayerSession> hewwo = r.getPlayers();
        for (PlayerSession player : hewwo) {
            builder.addHumanPlayer(player.getId());
            r.putNewClient(player.getId(), user);
        }
        return builder.complete();
    }

    public synchronized GameMap getGameMap() {
        return map;
    }

    public synchronized void setGameMap(GameMap gameMap) {
        this.map = gameMap;
    }

    /**
     * the handler for on start of the game page.
     *
     * @author anina
     */
    private class GameHandler implements TemplateViewRoute {

        @Override
        public ModelAndView handle(Request arg0, Response arg1) throws Exception {
            String roomID = arg0.params(":roomID");

            // if there's already a game going on, don't let this guy in.
            if (rooms.alreadyPlaying(roomID)) {

                Map<String, Object> variables = new ImmutableMap.Builder<String, Object>()
                        .put("title", "Game R.A.D.A.R.").build();
                return new ModelAndView(variables, "error.ftl");
            }
            // if there's already four players, don't let this guy in.
            if (rooms.isNotPlaying(roomID)) {
                Room r = rooms.getNotPlayingRoom(roomID);
                if (r.numPlayers() == 4) {
                    Map<String, Object> variables = new ImmutableMap.Builder<String, Object>()
                            .put("title", "Game R.A.D.A.R.").build();
                    return new ModelAndView(variables, "error.ftl");
                }
            } else {
                Room r = new Room(roomID);
                rooms.addNotPlayingRoom(roomID, r);
            }

            Map<String, Object> variables = new ImmutableMap.Builder<String, Object>()
                    .put("title", "Game R.A.D.A.R.").put("roomID", roomID).build();
            return new ModelAndView(variables, "game.ftl");
        }
    }

    private class DebugHandler implements TemplateViewRoute {

        @Override
        public ModelAndView handle(Request req, Response res) throws Exception {
            Map<String, Object> variables = new ImmutableMap.Builder<String, Object>()
                    .put("title", "Debug Page").build();
            return new ModelAndView(variables, "debug.ftl");
        }

    }

    private class ChangeMapHandler implements Route {

        @Override
        public Object handle(Request req, Response res) throws Exception {
            QueryParamsMap qm = req.queryMap();
            String map = qm.value("map");
            System.out.println(map);
            switch (map) {
                case "main":
                    setGameMap(new MainGameMap());
                    break;
                case "items":
                    setGameMap(new ItemGameMap());
                    break;
                default:
                    break;
            }
            return null;
        }

    }

    /**
     * the handler for on start of the homepage.
     *
     * @author anina
     */
    private class HomeHandler implements TemplateViewRoute {

        @Override
        public ModelAndView handle(Request arg0, Response arg1) throws Exception {
            String newRoomId = rooms.generateNewRoom();
            Room r = new Room(newRoomId);
            rooms.addNotPlayingRoom(newRoomId, r);
            Map<String, Object> variables = new ImmutableMap.Builder<String, Object>()
                    .put("title", "R.A.D.A.R.").put("roomID", newRoomId).build();
            return new ModelAndView(variables, "home.ftl");
        }

    }

    private static FreeMarkerEngine createEngine() {
        Configuration config = new Configuration(new Version(VERSION_MAJOR, VERSION_MINOR, VERSION_MICRO));
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
