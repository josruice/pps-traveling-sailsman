package sail.g5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import sail.sim.Point;

public class ClusterInfo {
	ArrayList<Integer> clusterPoints;
	public Point closestClusterPoint;
	public double timeToCluster;
	public double clusterHeuristic;
	
	public ClusterInfo(ArrayList<Integer> clusterPoints){
		this.clusterPoints = clusterPoints;
	}
}
