package sail.g5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Implementation of density-based clustering algorithm DBSCAN.
 * 
 * Publication: 
 * Ester, Martin; Kriegel, Hans-Peter; Sander, Jörg; Xu, Xiaowei (1996). 
 * Simoudis, Evangelos; Han, Jiawei; Fayyad, Usama M., eds. 
 * A density-based algorithm for discovering clusters in large spatial 
 * databases with noise. Proceedings of the Second International Conference 
 * on Knowledge Discovery and Data Mining (KDD-96). AAAI Press. pp. 226–231
 * 
 * Usage:
 * - Identify type of input values.
 * - Implement metric for input value type using DistanceMetric interface.
 * - Instantiate using {@link #DBSCANClusterer(Collection, int, double, DistanceMetric)}.
 * - Invoke {@link #performClustering()}.
 * 
 * See tests and metrics for example implementation and use.
 * 
 * @author <a href="mailto:cf@christopherfrantz.org>Christopher Frantz</a>
 *
 * @param <V> Input value type
 */
public class DBSCANClusterer<V> {

	/** maximum distance of values to be considered as cluster */
	private double epsilon = 1f;

	/** minimum number of members to consider cluster */
	private int minimumNumberOfClusterMembers = 2;

	/** distance metric applied for clustering **/
//	private DistanceMetric<V> metric = null;

	/** internal list of input values to be clustered */
	private ArrayList<V> inputValues = null;

	/** index maintaining visited points */
	private HashSet<V> visitedPoints = new HashSet<V>();
	
	private double[][] distances;

	/**
	 * Creates a DBSCAN clusterer instance. 
	 * Upon instantiation, call {@link #performClustering()} 
	 * to perform the actual clustering.
	 * 
	 * @param inputValues Input values to be clustered
	 * @param minNumElements Minimum number of elements to constitute cluster
	 * @param maxDistance Maximum distance of elements to consider clustered
	 * @param metric Metric implementation to determine distance 
	 */
	public DBSCANClusterer(final Collection<V> inputValues, int minNumElements, double maxDistance, double[][] distances){
		setInputValues(inputValues);
		setMinimalNumberOfMembersForCluster(minNumElements);
		setMaximalDistanceOfClusterMembers(maxDistance);
		setDistances(distances);
	}

	
	/**
	 * Sets the distances
	 *  
	 * @param distances
	 */
	public void setDistances(double[][] distances){
//		if (metric == null) {
//			throw new DBSCANClusteringException("DBSCAN: Distance metric has not been specified (null).");
//		}
		this.distances = new double[distances.length][distances[0].length];
		for(int i = 0; i < distances.length; i++){
			for(int j = 0; j < distances[0].length; j++){
				this.distances[i][j] = distances[i][j];
			}
		}
	}

	/**
	 * Sets the distance metric
	 * 
	 * @param metric
	 */
//	public void setDistanceMetric(final DistanceMetric<V> metric){
////		if (metric == null) {
////			throw new DBSCANClusteringException("DBSCAN: Distance metric has not been specified (null).");
////		}
//		this.metric = metric;
//	}

	/**
	 * Sets a collection of input values to be clustered.
	 * Repeated call overwrite the original input values.
	 * 
	 * @param collection
	 */
	public void setInputValues(final Collection<V> collection){
//		if (collection == null) {
//			throw new DBSCANClusteringException("DBSCAN: List of input values is null.");
//		}
		this.inputValues = new ArrayList<V>(collection);
	}

	/**
	 * Sets the minimal number of members to consider points of close proximity
	 * clustered.
	 * 
	 * @param minimalNumberOfMembers
	 */
	public void setMinimalNumberOfMembersForCluster(final int minimalNumberOfMembers) {
		this.minimumNumberOfClusterMembers = minimalNumberOfMembers;
	}

	/**
	 * Sets the maximal distance members of the same cluster can have while
	 * still be considered in the same cluster.
	 * 
	 * @param maximalDistance
	 */
	public void setMaximalDistanceOfClusterMembers(final double maximalDistance) {
		this.epsilon = maximalDistance;
	}

	/**
	 * Determines the neighbours of a given input value.
	 * 
	 * @param inputValue Input value for which neighbours are to be determined
	 * @return list of neighbours 
	 */
//	private ArrayList<V> getNeighbours(final V inputValue){
//		ArrayList<V> neighbours = new ArrayList<V>();
//		for(int i=0; i<inputValues.size(); i++) {
//			V candidate = inputValues.get(i);
//			if (metric.calculateDistance(inputValue, candidate) <= epsilon) {
//				neighbours.add(candidate);
//			}
//		}
//		return neighbours;
//	}
	
	/**
	 * Determines the neighbours of a given input value.
	 * 
	 * @param inputValue Input value for which neighbours are to be determined
	 * @return list of neighbours 
	 */
	private ArrayList<Integer> getNeighboursFromIndex(int index){
		ArrayList<Integer> neighbours = new ArrayList<Integer>();
		for(int i=0; i<inputValues.size(); i++) {
//			V candidate = inputValues.get(i);
			if (distances[index][i] <= epsilon) {
				neighbours.add(i);
			}
		}
		return neighbours;
	}


	/**
	 * Merges the elements of the right collection to the left one and returns
	 * the combination.
	 * 
	 * @param neighbours1 left collection
	 * @param neighbours2 right collection
	 * @return Modified left collection
	 */
	private ArrayList<Integer> mergeRightToLeftCollection(final ArrayList<Integer> neighbours1,
			final ArrayList<Integer> neighbours2) {
		for (int i = 0; i < neighbours2.size(); i++) {
			int tempPt = neighbours2.get(i);
			if (!neighbours1.contains(tempPt)) {
				neighbours1.add(tempPt);
			}
		}
		return neighbours1;
	}

	/**
	 * Applies the clustering and returns a collection of clusters (i.e. a list
	 * of lists of the respective cluster members).
	 * 
	 * @return
	 */
	public ArrayList<ArrayList<Integer>> performClustering() {

//		if (inputValues == null) {
//			throw new DBSCANClusteringException("DBSCAN: List of input values is null.");
//		}
//
//		if (inputValues.isEmpty()) {
//			throw new DBSCANClusteringException("DBSCAN: List of input values is empty.");
//		}
//
//		if (inputValues.size() < 2) {
//			throw new DBSCANClusteringException("DBSCAN: Less than two input values cannot be clustered. Number of input values: " + inputValues.size());
//		}
//
//		if (epsilon < 0) {
//			throw new DBSCANClusteringException("DBSCAN: Maximum distance of input values cannot be negative. Current value: " + epsilon);
//		}
//
//		if (minimumNumberOfClusterMembers < 2) {
//			throw new DBSCANClusteringException("DBSCAN: Clusters with less than 2 members don't make sense. Current value: " + minimumNumberOfClusterMembers);
//		}

		ArrayList<ArrayList<Integer>> resultList = new ArrayList<ArrayList<Integer>>();
		visitedPoints.clear();

		ArrayList<Integer> neighbours;
		int index = 0;

		while (inputValues.size() > index) {
			V p = inputValues.get(index);
			if (!visitedPoints.contains(p)) {
				visitedPoints.add(p);
//				neighbours = getNeighbours(p);
				neighbours = getNeighboursFromIndex(index);

				if (neighbours.size() >= minimumNumberOfClusterMembers) {
					int ind = 0;
					while (neighbours.size() > ind) {
						V r = inputValues.get(neighbours.get(ind));
						if (!visitedPoints.contains(r)) {
							visitedPoints.add(r);
//							ArrayList<V> individualNeighbours = getNeighbours(r);
							ArrayList<Integer> individualNeighbours = getNeighboursFromIndex(ind);
							if (individualNeighbours.size() >= minimumNumberOfClusterMembers) {
								neighbours = mergeRightToLeftCollection(
										neighbours,
										individualNeighbours);
							}
						}
						ind++;
					}
					//TODO: Take this outside this block
					resultList.add(neighbours);
				}
//				resultList.add(neighbours);
			}
			index++;
		}
		return resultList;
	}

}
