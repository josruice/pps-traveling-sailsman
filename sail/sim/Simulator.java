package sail.sim;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.*;
import java.util.concurrent.*;

public class Simulator {
  private static String[] groups;
  private static int numgroups = -1;
  private static int duplicates = 1;
  private static String root = "sail";
  private static int FRAME_SKIP = 1; // = n means (n-1) frames are skipped for every n frames
  private static int t = -1;
  private static long seed=-1;
  private static int fps;
  private static long total_time = 1000;//ms
  private static long gui_refresh;
  private static boolean gui_enabled, log;
  private static double DT = 0.015; // test?

  public static void main(String[] args) throws Exception {
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      if(compiler == null) throw new IOException(":(");
    parseArgs(args);
    if(numgroups == -1 || t == -1) {
      throw new IllegalArgumentException("Missing groups or targets");
    }
    if(log) System.out.println("number of groups: "+numgroups +" and t = "+t);
    List<Class<Player>> player_class = new ArrayList<Class<Player>>();
    for(int i = 0; i < numgroups; ++i) {
      player_class.add(loadPlayer(groups[i]));
    }

    Timer[] timers = new Timer[numgroups];
    for(int i = 0; i < numgroups; ++ i) {
      timers[i] = new Timer();
      timers[i].start();
    }
    if(seed == -1)
      seed = System.currentTimeMillis();
    Player[] players = new Player[numgroups];
    for(int i = 0 ; i < numgroups; ++ i) {
      players[i] = player_class.get(i).newInstance();
    }
    new Simulator().play(numgroups, t, timers, players, seed);
    
    System.exit(0);
  }

  // move fasssstt
  public static double getSpeed(Point p, Point wind_direction) {
    if(Point.getNorm(p)==0) return 0;
    double angle = Point.angleBetweenVectors(p, wind_direction) + Math.PI;
    double x = 2.5 * Math.cos(angle) - 0.5;
    double y = 5 * Math.sin(angle);
    return Math.sqrt((x)*(x) + (y)*(y));
  }

  private static int custom_compare_to(
    int o1, 
    int o2, 
    List<Point> player_locations, 
    List<Point> target_locations, 
    // the endless war between camelCase and underscores does not end today.
    List<Point> newLocations, 
    int ii
  ) {
    Point intersection1 = getInterpolatedIntersectionPoint(
        target_locations.get(ii),
        player_locations.get(o1),
        newLocations.get(o1)
    );
    Point intersection2 = getInterpolatedIntersectionPoint(
        target_locations.get(ii),
        player_locations.get(o2),
        newLocations.get(o2)
    );

    double dist1 = Point.getDistance(player_locations.get(o1), intersection1);
    double dist3 = Point.getDistance(player_locations.get(o2), intersection2);
    double dist2 = Point.getDistance(newLocations.get(o1), player_locations.get(o1));
    double dist4 = Point.getDistance(newLocations.get(o2), player_locations.get(o2));
    double ratio1 = (dist1)/(dist2);
    double ratio2 = (dist3)/(dist4);
    return new Double(ratio1).compareTo(ratio2);
  }

  // a function should never be longer than ... never mind
  private void play(
    int numgroups, 
    int t, 
    Timer[] timers, 
    Player[] players, 
    Long seed
  ) throws Exception {
    HTTPServer server = null;
    Random gen = new Random(seed);
    if (gui_enabled) {
      server = new HTTPServer();
      if (!Desktop.isDesktopSupported())
        System.err.println("Desktop operations not supported");
      else if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
        System.err.println("Desktop browse operation not supported");
      else {
        try {
          Desktop.getDesktop().browse(new URI("http://localhost:" + server.port()));
        } catch (URISyntaxException exc) {
          exc.printStackTrace();
        }
      }
    }
    long[] time_remaining = new long[numgroups];
    for(int i = 0 ; i < numgroups; ++ i) {
      time_remaining[i] = total_time;
    }
    final Point wind_direction = Point.getRandomPoint(gen);
    List<Point> player_locations = new ArrayList<Point>();
    List<Point> initial_player_locations = new ArrayList<Point>();
    List<Point> player_locations_u = Collections.unmodifiableList(player_locations);

    for(int i = 0 ; i < numgroups; ++ i) {
      if(time_remaining[i] <= 0) {
        initial_player_locations.add(new Point(0,0));
        player_locations.add(new Point(0,0));
        continue;
      }
      if(log) System.out.println("choosing starting location of group: " + groups[i]);
      try {
        final int ii =i;
        Point p = timers[i].call(
          () -> players[ii].chooseStartingLocation(
            wind_direction, 
            gen.nextLong(),
            t
          ), 
          time_remaining[ii]
        );
        time_remaining[i] -= timers[i].getElapsedTime();
        player_locations.add(p);
        initial_player_locations.add(p);
      } catch(TimeoutException ex) {
        if(log) {
          System.out.println("Player "+groups[i] + " timed out" );
        }
        time_remaining[i] = 0;
        ex.printStackTrace();
        initial_player_locations.add(new Point(0,0));
        player_locations.add(new Point(0,0));

      } catch (Exception ex) {
        System.err.println(
          "Exception calling choose starting location of player: " + groups[i]
        );
        // the show must go on
        initial_player_locations.add(new Point(0,0));
        player_locations.add(new Point(0,0));
      }
    }

    List<Point> target_locations = new ArrayList<Point>();
    while(target_locations.size() < t) {
      Point poss = new Point(
        6 * DT + gen.nextDouble() * (10 - 12*DT), 
        6*DT + gen.nextDouble() * (10-12*DT)
      );
      boolean canput = true;
      for(int j = 0; j < numgroups ;++j) {
        if(Point.getDistance(initial_player_locations.get(j), poss) <= 0.01) {
          canput = false;
          break;
        }
      }
      if(canput)
        target_locations.add(poss);
    }
    List<Point> target_locations_u = Collections.unmodifiableList(target_locations);
    for(int i = 0 ; i < numgroups; ++ i) {
      if(time_remaining[i] <= 0) continue;
      if(log) System.out.println("Initializing group: " + groups[i]);
      try {
        final int ii =i;
        timers[i].call((Callable<Void>) () -> {
          players[ii].init(player_locations_u, target_locations_u, ii);
          return null;
        }, time_remaining[i]);
        time_remaining[i] -= timers[i].getElapsedTime();
      } catch(TimeoutException ex) {
        if(log) {
          System.out.println("Player "+groups[i] + " timed out" );
        }

        time_remaining[i] = 0;
      } catch (Exception ex) {
        System.err.println("Exception calling init of player: " + groups[i]);
        ex.printStackTrace();
      }
    }
    int[] vis_count = new int[t];
    for(int i= 0; i < t; ++i) {
      vis_count[i] = 0;
    }
    double[] scores = new double[numgroups];
    Map<Integer, Set<Integer>> visited_set = new HashMap<>();
    Map<Integer, Set<Integer>> visited_set_ut = new HashMap<>();
    for(int i = 0 ; i < numgroups; ++ i) {
      Set<Integer> putset = new HashSet<>();
      visited_set.put(i, putset);
      visited_set_ut.put(i, Collections.unmodifiableSet(putset));
    }
    Map<Integer, Set<Integer>> visited_set_u = Collections.unmodifiableMap(visited_set_ut);
    if(log) System.out.println("Starting game");
    int turn_counter = 0;
    while(true) {
      ++ turn_counter;
      List<Point> newLocations = new ArrayList<>();
      for(int i = 0 ; i < numgroups; ++i) {
        if(time_remaining[i] <= 0) {
          newLocations.add(player_locations.get(i));
          continue;
        }
        final int ii = i;
        // if(log) System.out.println("Moving group: " + groups[i]);
        try {
          Point newDirection = timers[i].call(
            () -> players[ii].move(player_locations_u, ii, DT, time_remaining[ii]), 
            time_remaining[i]
          );

          time_remaining[i] -= timers[i].getElapsedTime();
          if(newDirection.x == 0 && newDirection.y == 0) {
            newLocations.add(player_locations.get(i));
            continue;
          }
          if(true /*Point.getNorm(newDirection) > 1*/) {
            newDirection = Point.getUnitVector(newDirection);
          }
          double speed = getSpeed(newDirection, wind_direction);
          Point distanceMoved = new Point(
            newDirection.x * speed * DT, 
            newDirection.y * speed * DT
          );
          Point nextLocation = Point.sum(player_locations.get(i), distanceMoved);
          if (nextLocation.x < 0 || nextLocation.y > 10 || 
            nextLocation.y < 0 || nextLocation.x > 10) {
            System.err.println("location returned is out of bounds group " + groups[ii]);
            nextLocation = player_locations.get(i);
          }
          newLocations.add(nextLocation);
          //if(log) System.out.println(" from: ("+player_locations.get(i).x + ", " +
          //  player_locations.get(i).y+ ") to (" +newLocations.get(i).x +
          // ", "+newLocations.get(i).y+")");

        } catch(TimeoutException ex) {
          if(log) {
            System.out.println("Player " + i + ": "+groups[i] + " timed out" );
          }
          ex.printStackTrace();
          newLocations.add(player_locations.get(i));
          time_remaining[i] = 0;
        } catch (Exception ex) {
          System.err.println("Exception calling move of "+groups[ii]);
          ex.printStackTrace();
          newLocations.add(player_locations.get(i));
        }
      }

      for(int i = 0 ; i < t ;++ i) {
        List<Integer> groups_on_this = new ArrayList<>();
        for (int j = 0 ; j < numgroups; ++j) {
          if(!visited_set.get(j).contains(i) && 
            Point.getDistance(target_locations.get(i), player_locations.get(j)) < 0.01 + 6*DT && 
            checkCrossedInterpolation(
              target_locations.get(i), 
              player_locations.get(j), 
              newLocations.get(j)
            )
          ) {
            if(log) System.out.println("Group " + groups[j] +" visited target: "+i);
            visited_set.get(j).add(i);
            groups_on_this.add(j);
          }
        }
        final int ii = i;
        Collections.sort(
          groups_on_this, 
          (o1, o2) -> custom_compare_to(
            o1, 
            o2, 
            player_locations, 
            target_locations, 
            newLocations, 
            ii
          )
        );
        for(int x=0; x < groups_on_this.size();) {
          int y = x;
          int totp = 0;
          while(y < groups_on_this.size() && 
            custom_compare_to(
              groups_on_this.get(x),
              groups_on_this.get(y),
              player_locations,
              target_locations,
              newLocations, 
              ii
            ) == 0
          ) {
            totp += numgroups - vis_count[i] - (y-x);
            ++y;
          }
          int cnt = y - x;
          while(x < y) {
            scores[groups_on_this.get(x)] += totp/(double)cnt;
            ++x;
            vis_count[i]+=1;
          }
        }
      }

      boolean finished = false, all_timed_out = true;
      for(int i = 0 ; i < numgroups; ++ i) {
        if(time_remaining[i] > 0) all_timed_out = false;
        if(visited_set.get(i).size() == t &&
          checkCrossedInterpolation(
              initial_player_locations.get(i), 
              player_locations.get(i), 
              newLocations.get(i)
          )
        ) {
          System.out.println("Finisher was "+groups[i]);
          finished = true;
        }
      }
      if(all_timed_out) {
        finished = true;
      }


      player_locations.clear();
      for(int i = 0 ; i < numgroups; ++ i) {
        player_locations.add(newLocations.get(i));
      }
      newLocations.clear();
      for(int i = 0 ; i < numgroups ; ++ i) {
        if(time_remaining[i] <= 0) continue;
        try {
          final int ii =i;
          timers[i].call((Callable<Void>) () -> {
            players[ii].onMoveFinished(player_locations_u, visited_set_u);
            return null;
          }, time_remaining[i]);
          time_remaining[i] -= timers[i].getElapsedTime();

        } catch(TimeoutException ex) {
          if(log) {
            System.out.println("Player " + i + ": "+groups[i] + " timed out" );
          }
          ex.printStackTrace();
          time_remaining[i] = 0;
        } catch (Exception ex) {
          System.err.println("Exception calling onMoveFinished of player: " + groups[i]);
          ex.printStackTrace();
        }
      }
      // for(int i =0 ; i < numgroups; ++ i) {
      //   System.out.println("time left: " + time_remaining[i] + " for "+ groups[i]);
      // }
      if (finished) {
        if (gui_enabled) {
          gui(
            server,
            state(
              1,
              groups,
              scores,
              target_locations,
              player_locations,
              initial_player_locations,
              wind_direction,
              gui_refresh,
              visited_set
            )
          );
        }

        if(log) {
          for(int j = 0 ; j < numgroups; ++ j) {
            System.out.println(groups[j] + " scored " + scores[j]);
          }
        } 
        
        if(log) System.out.println("Ended!");
        finished = true;
        if(gui_enabled) for(;;);
      }
      
      if(gui_enabled && turn_counter % FRAME_SKIP == 0)
        gui(
          server, 
          state(
                  0,
            groups,
            scores,
            target_locations,
            player_locations,
            initial_player_locations,
            wind_direction,
            gui_refresh,
            visited_set
          )
        );
      if(finished)
        break;
    }
    for(int j = 0 ; j < numgroups; ++ j) {
      System.out.println(groups[j] + " scored " + scores[j]);
    }
    if(server != null) server.close();
  }


  // checks if (a --- > b) line segment intersects circle radius 0.01 around t
  private static boolean checkCrossedInterpolation(Point t, Point a, Point b) {
    Point p = getInterpolatedIntersectionPoint(t, a, b);
    if(p.x == -1 && p.y == -1) return false;
    else return true;
  }

  // Ooh O(1), math is cooool
  private static Point getInterpolatedIntersectionPoint(Point centre, Point e, Point l) {
    double r = 0.01;
    Point d = Point.getDirection(e, l);
    Point f = Point.getDirection(centre, e);
    double a = Point.dot(d,d);
    double b = 2 * Point.dot(f,d);
    double c = Point.dot(f,f) - r*r;
    double discr = b*b - 4*a*c;
    if(discr < 0) {
      return new Point(-1, -1);
    }
    discr = Math.sqrt(discr);
    double t1 = (-b - discr)/(2*a);
    double t2 = (-b + discr)/(2*a);


    if(t1 >=0 && t1 <= 1) {
      return Point.sum(e, Point.multiply(d,t1));
    } else if(t2 >= 0 && t2 <= 1) {
      System.err.println("console failure, this line should not"+
        "have been reached (exit interpolated point)");
      return new Point(-1, -1);
    } else {
      return new Point(-1, -1);
    }
  }

  // yawns
  private static void parseArgs(String[] args) {
    for(int i = 0; i < args.length; ++i) {

      if (args[i].equals("-g") || args[i].equals("--groups")) {
        if (i + 1 >= args.length) {
          throw new IllegalArgumentException("Missing number of groups");
        }
        numgroups = Integer.parseInt(args[++i]);
        if(numgroups <= 0 || numgroups >= 1000) {
          throw new IllegalArgumentException("numgroups <=0 or >= 1000");
        }
        groups = new String[numgroups];
        for(int j = 0 ; j < numgroups; ++j) {
          groups[j] = args[++i];
        }
      } else if (args[i].equals("-t") || args[i].equals("--targets")) {
        if (i+1 >= args.length) {
          throw new IllegalArgumentException("Missing number of targets");
        }
        t = Integer.parseInt(args[++i]);
      } else if (args[i].equals("-tl") || args[i].equals("--timelimit")) {
        if (i+1 >= args.length) {
          throw new IllegalArgumentException("Missing time limit");
        }
        total_time = Long.parseLong(args[++i]);
      } else if (args[i].equals("-dt") || args[i].equals("--timestep")) {
        if (i+1 >= args.length) {
          throw new IllegalArgumentException("Missing time step");
        }
        DT = Double.parseDouble(args[++i]);
      } else if (args[i].equals("-fs") || args[i].equals("--frameskip")) {
        if (i+1 >= args.length) {
          throw new IllegalArgumentException("Missing frame skip");
        }
        FRAME_SKIP = Integer.parseInt(args[++i]);
      } else if (args[i].equals("-S") || args[i].equals("--seed")) {
        if (i + 1 >= args.length) {
          throw new IllegalArgumentException("Missing seed");
        }
        seed = Long.parseLong(args[++i]);
      } else if (args[i].equals("-f") || args[i].equals("--fps")) {
        if (i+1 >= args.length) {
          throw new IllegalArgumentException("Missing fps");
        }
        double gui_fps = Double.parseDouble(args[++i]);
        gui_refresh = gui_fps > 0.0 ? (long) Math.round(1000.0 / gui_fps) : -1;
        gui_enabled = true;
      } else if (args[i].equals("--gui")) {
        gui_enabled = true;
      } else if (args[i].equals("--verbose")) {
        log = true;
      } else {
        throw new IllegalArgumentException("Unknown argument: " + args[i]);
      }
    }
  }


  private static long last_modified(Iterable <File> files) {
    long last_date = 0;
    for (File file : files) {
      long date = file.lastModified();
      if (last_date < date)
        last_date = date;
    }
    return last_date;
  }

  //ty Kevin shi
  private static Class <Player> loadPlayer(
      String group
    ) throws IOException, ReflectiveOperationException {
    String sep = File.separator;
    Set<File> player_files = directory(root + sep + group, ".java");
    File class_file = new File(root + sep + group + sep + "Player.class");
    long class_modified = class_file.exists() ? class_file.lastModified() : -1;
    if (class_modified < 0 || class_modified < last_modified(player_files) ||
            class_modified < last_modified(directory(root + sep + "sim", ".java"))) {
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      //System.out.println(System.getProperty( "java.home"));
      if (compiler == null)
        throw new IOException("Cannot find Java compiler");
      StandardJavaFileManager manager = compiler.
              getStandardFileManager(null, null, null);
      long files = player_files.size();
      if (log)
        System.err.print("Compiling " + files + " .java files ... ");
      if (!compiler.getTask(null, manager, null, null, null,
              manager.getJavaFileObjectsFromFiles(player_files)).call())
        throw new IOException("Compilation failed");
      //System.err.println("done!");
      class_file = new File(root + sep + group + sep + "Player.class");
      if (!class_file.exists())
        throw new FileNotFoundException("Missing class file");
    }
    ClassLoader loader = Simulator.class.getClassLoader();
    if (loader == null)
      throw new IOException("Cannot find Java class loader");
    @SuppressWarnings("rawtypes")
    Class raw_class = loader.loadClass(root + "." + group + ".Player");
    @SuppressWarnings("unchecked")
    Class <Player> player_class = raw_class;
    return player_class;
  }

  private static Set <File> directory(String path, String extension) {
    Set <File> files = new HashSet <File> ();
    Set <File> prev_dirs = new HashSet <File> ();
    prev_dirs.add(new File(path));
    do {
      Set <File> next_dirs = new HashSet<File>();
      for (File dir : prev_dirs)
        for (File file : dir.listFiles())
          if (!file.canRead()) ;
          else if (file.isDirectory())
            next_dirs.add(file);
          else if (file.getPath().endsWith(extension))
            files.add(file);
      prev_dirs = next_dirs;
    } while (!prev_dirs.isEmpty());
    return files;
  }

  public static class BoostString {
    double boost;
    String str;
    int i;

    public BoostString(double boost, String str, int i) {
        this.boost = boost;
        this.str = str;
        this.i = i;
    }
  }

  public static String state(
    int st,
    String[] groups,
    double[] scores,
    List<Point> targets,
    List<Point> players,
    List<Point> inits,
    Point wind_direction,
    long gui_refresh,
    Map<Integer, Set<Integer>> visited_set) {

    String buffer = "";
    buffer += st + ", ";
    buffer += players.size() + ", ";
    for(String pl: groups) {
      buffer += pl + ", ";
    }
    for(double sc : scores) {
      buffer += sc + ", ";
    }

    final Integer[] sorted = IntStream.range(0, numgroups)
        .mapToObj(i -> new BoostString(scores[i], groups[i], i)) // Create the instance
        .sorted(Comparator.comparingDouble(b -> b.boost))         // Sort using a Comparator
        .map(b -> b.i)                                       // Map it back to a string
        .toArray(Integer[]::new);  
    for(Integer inte : sorted) {
      buffer += (int)inte + ", ";
    } 
    for(Point x : players) {
      buffer += x.x + ", " + x.y + ", ";
    }
    for(Point x : inits) {
      buffer += x.x + ", " + x.y + ", ";
    }
    buffer += targets.size() +", ";
    for(Point x : targets) {
      buffer += x.x + ", " + x.y + ", ";
    }
    for(int i = 0 ; i < t; ++i) {
      int num = 0;
      for(int j = 0;j<numgroups; ++j) {
        num += (visited_set.get(j).contains(i)==true? 1:0);
      }
      buffer += num +", ";
    }
    buffer += wind_direction.x + ", " + wind_direction.y + ", ";
    buffer += gui_refresh;
    return buffer;
  }

  // icky gui wounds
  public static void gui(HTTPServer server, String content) {
    String path = null;
    for (;;) {
        // get request
      for (;;)
      try {
        path = server.request();
        break;
      } catch (IOException e) {
        System.err.println("HTTP request error: " + e.getMessage());
      }
        // dynamic content
      if (path.equals("data.txt")) {
        // send dynamic content
        try {
          server.reply(content);
          return;
        } catch (IOException e) {
          System.err.println("HTTP dynamic reply error: " + e.getMessage());
          continue;
        }
      }
      // static content
      if (path.equals("")) path = "webpage.html";
      else if (!path.equals("favicon.ico") &&
           !path.equals("apple-touch-icon.png") &&
           !path.equals("script.js")) break;
      // send file
      File file = new File(root + File.separator + "sim"
           + File.separator + path);
      try {
        server.reply(file);
      } catch (IOException e) {
        System.err.println("HTTP static reply error: " + e.getMessage());
      }
    }
  }
}