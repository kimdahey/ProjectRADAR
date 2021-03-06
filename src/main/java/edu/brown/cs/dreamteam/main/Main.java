package edu.brown.cs.dreamteam.main;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import spark.Spark;

public class Main {

  private static final int DEFAULT_PORT = 4567;
  private String[] args;
  private Architect a;

  private DebugMode debugMode;

  private Main(String[] args) {
    this.args = args;
    init();
    run();
  }

  private void init() {
    debugMode = DebugMode.DEFAULT;
    a = debugMode.architect();
  }

  public static void main(String[] args) {
    new Main(args);
  }

  private void run() {
    // Parse command line arguments
    OptionParser parser = new OptionParser();
    parser.accepts("gui");
    parser.accepts("port").withRequiredArg().ofType(Integer.class)
        .defaultsTo(DEFAULT_PORT);
    OptionSet options = parser.parse(args);

    runSparkServer((int) options.valueOf("port"));
    a.initSpark();
    new Thread(debugMode.architect()).run();
  }

  private void runSparkServer(int port) {
    Spark.port(port);
  }

}
