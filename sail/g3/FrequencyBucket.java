package sail.g3;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class FrequencyBucket {
  HashMap<Integer, Set<Integer>> contained_points;
  ArrayList<Point> targets;
  int numPlayers;
  int playerId;
  int total_score = 0;
  private double x_0;
  private double x_1;
  private double y_0;
  private double y_1;

  public FrequencyBucket(int numPlayers,int playerId, double x_0, double x_1, double y_0, double y_1){
    this.numPlayers = numPlayers;
    this.playerId = playerId;
    this.x_0 = x_0;
    this.x_1 = x_1;
    this.y_0 = y_0;
    this.y_1 = y_1;
    contained_points = new HashMap();
    targets = new ArrayList<Point>();
  }

  public void initBucket(List<Point> targets){
    for ( int i = 0; i < targets.size(); i++){
      Point target = targets.get(i);
      if(target.x >= this.x_0 && target.x < this.x_1 && target.y >= this.y_0 && target.y < this.y_1){
        this.contained_points.put(i, new HashSet());
        this.targets.add(target);
      }
    }
  }


  public void updateBucket(Map<Integer, Set<Integer>> visited_set){
    for (Map.Entry<Integer, Set<Integer>> entry : visited_set.entrySet()) {
      int player = entry.getKey();
      for (int target : entry.getValue()) {
        if(contained_points.containsKey(target)){
          contained_points.get(target).add(player);
        }
     }
    }
    updateScore();
  }

  public void updateScore(){
    this.total_score = 0;
    for (Map.Entry<Integer, Set<Integer>> entry : this.contained_points.entrySet()) {
      HashSet<Integer> visited_player = (HashSet) entry.getValue();
      if (visited_player.contains(this.playerId)) continue;
      this.total_score += this.numPlayers - visited_player.size();
    }
  }

  public String toString(){
    return "Boundary: ("+this.x_0+","+this.y_0+") and ("+this.x_1+","+this.y_1+")\nContains: "+this.contained_points + "\nPlayer ID: " + this.playerId+" Score: "+this.total_score+"\n\n";
  }
}