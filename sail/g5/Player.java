package sail.g5;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player extends sail.sim.Player {
    List<Point> targets;
    double[] avgTargetDistances;
    double[] computedTargetToTargetsWeights;
    List<Point> prevGroupLocations;
    Map<Integer, Set<Integer>> visitedTargets; // For every player, store which targets he has visited.
    Map<Integer, Set<Integer>> unvisitedTargets; // For every player, store which target he has NOT visited.
    Map<Integer, Set<Integer>> playerVisitsByTarget; // For every target, store which players have visited it (been received).
    Set<Point> ourVisitedTargets;
    Set<Integer> ourUnvisitedTargets;

    List<Point> shortestPath;
    double shortestTime;

    Random gen;
    int id;
    int numTargets;
    int numPlayers;
    int movesCounter;
    Long initialTimeRemaining;
    Point initialLocation;
    Point currentLocation;
    Point windDirection;

    MST mst;
    double[][] graph;
    Tree tree;
    ArrayList<Integer> path;

    // One of: random, middle, windMiddleToEdges, randomAroundWindMiddleToEdges
    final String INITIAL_POINT = "middle";
    // One of: greedy, weightedGreedy, mst, clusteringMst, clusteringWeightedGreedy, optimalPath (extremely unefficient)
    final String INITIAL_STRATEGY = "weightedGreedy";
    private String strategy;

    // Enable or disable different Weighted Greedy params, to compare results.
    final boolean WG_SCORE_ENABLED = true;
    final boolean WG_SCORE_1_PENALTY = false;
    final boolean WG_TIME_ENABLED = true;
    final boolean WG_PLAYERS_DISTANCES_ENABLED = false;

    final boolean WG_CHECK_FOLLOWING_ENABLED = true;

    // This one times out for t=100 and tl=1000, in the first few turns, so it is disabled automatically.
    final boolean WG_TARGETS_DISTANCES_ENABLED = true;


    ArrayList<ClusterInfo> clusters;
    ClusterInfo currentCluster;

    @Override
    public Point chooseStartingLocation(Point windDirection, Long seed, int t) {
        // you don't have to use seed unless you want it to
        // be deterministic (wrt input randomness)
        gen = new Random(seed);
        this.numTargets = t;
        this.windDirection = windDirection;

        switch (INITIAL_POINT) {
            case "random":
                initialLocation = new Point(gen.nextDouble()*10, gen.nextDouble()*10);
                break;
            case "middle":
                initialLocation = new Point(5.0, 5.0);
                break;
            case "windMiddleToEdges":
                initialLocation = getWindMiddleToEdges(this.windDirection);
                break;
            case "randomAroundWindMiddleToEdges":
                Point windMiddleToEdges = getWindMiddleToEdges(this.windDirection);
                initialLocation = new Point(
                        windMiddleToEdges.x + gen.nextDouble(),
                        windMiddleToEdges.y + gen.nextDouble()
                );
                break;
            case "windMiddleFromEdges":
                break;
            default:
                System.err.println("Invalid initial point "+INITIAL_POINT+" chosen");
                initialLocation = new Point(0, 0);
        }
        // initialLocation = new Point(5.0, 5.0); // Use the middle point as initial location.
        currentLocation = new Point(initialLocation.x, initialLocation.y);
        return initialLocation;
    }

    private Point getWindMiddleToEdges(Point windDirection) {
        // The middle point wrt the wind lies in the line that follows the wind vector, upwind direction,
        // at 1/3 of the distance from the center (5,5) to the edges of the board.
        // To compute it, first we need the unit vector that represents the wind direction.
        Point unitWind = Point.getUnitVector(windDirection);

        // Now, we need the distance of the edges of the board in this direction. If the angle of the wind
        // wrt to the x-axis is multiple of 90º, then the solution is trivial, since the distance is 5 km.
        // For the rest of the cases, we need some trigonometry.
        Point xAxisVector = new Point(1,0);

        // Rotate the wind so that it is a vector in the first quadrant. This works because of symmetry and
        // it helps to simplify things.
        Point absUnitWind = new Point(Math.abs(unitWind.x), Math.abs(unitWind.y));

        double alpha = Point.angleBetweenVectors(xAxisVector, absUnitWind);
        double distanceToEdge = 5.0;
        if (alpha <= Math.PI / 4) { // alpha <= 45º
            distanceToEdge /= Math.cos(alpha);
        } else { // 45º < alpha <= 90º
            distanceToEdge /= Math.sin(alpha);
        }

        // Now we have everything we need.
        return new Point(
                5 + (1./3 * distanceToEdge) * unitWind.x,
                5 + (1./3 * distanceToEdge) * unitWind.y
        );
    }

    @Override
    public void init(List<Point> groupLocations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
        this.movesCounter = 0;
        this.numPlayers = groupLocations.size();
        this.initialTimeRemaining = null;

        this.strategy = INITIAL_STRATEGY;
//        if (this.numTargets >= 500){
//            this.strategy = "greedy";
//        }

        // Initialize unvisited targets by player map.
        this.unvisitedTargets = new HashMap<Integer, Set<Integer>>();
        for (int playerId = 0; playerId < this.numPlayers; ++playerId) {
            Set<Integer> playerUnvisited = new HashSet<Integer>();
            for (int i = 0; i < this.numTargets; ++i) {
                playerUnvisited.add(i);
            }
            this.unvisitedTargets.put(playerId, playerUnvisited);
        }
        this.ourUnvisitedTargets = this.unvisitedTargets.get(this.id);
        this.ourVisitedTargets = new HashSet<Point>();

        // Initialize player visits by target map and averaged distance from one target to the rest.
        this.playerVisitsByTarget = new HashMap<Integer, Set<Integer>>();
        this.avgTargetDistances = new double[this.numTargets];
        this.computedTargetToTargetsWeights = new double[this.numTargets];
        for (int targetId = 0; targetId < this.numTargets; ++targetId) {
            Set<Integer> playerVisits = new HashSet<Integer>();
            this.playerVisitsByTarget.put(targetId, playerVisits);

            for (int targetId2 = 0; targetId2 < this.numTargets; ++targetId2) {
                if (targetId == targetId2) continue;
                this.avgTargetDistances[targetId] += computeEstimatedTimeToTarget(
                        this.targets.get(targetId), this.targets.get(targetId2));
            }
            this.avgTargetDistances[targetId] /= this.numTargets;
        }

        if(this.strategy== "mst"){
            ArrayList<Point> targetsClone = new ArrayList<Point>();
            for(Point target: targets){
                targetsClone.add(target);
            }
            targetsClone.add(initialLocation);

            graph = new double[numTargets + 1][numTargets + 1];
            for(int i = 0; i < numTargets + 1; i++){
                for(int j = 0; j < numTargets + 1; j++){
                    graph[i][j] = computeEstimatedTimeToTarget(targetsClone.get(i), targetsClone.get(j));
                }
            }

            mst = new MST();
            int[] parents = mst.primMST(graph);
            buildTree(parents);
            path = new ArrayList<Integer>();
            tree.preorder(path);

            path.remove(0);
        }

        else if(this.strategy== "clusteringMst"){
            graph = new double[numTargets][numTargets];
            for(int i = 0; i < numTargets; i++){
                for(int j = 0; j < numTargets; j++){
                    graph[i][j] = computeEstimatedTimeToTarget(targets.get(i), targets.get(j));
                }
            }
            cluster(10,2);
            currentCluster = findNextUnvisitedCluster(groupLocations);
            computePathInCluster();
        }

        else if(this.strategy== "clusteringWeightedGreedy"){
            graph = new double[numTargets][numTargets];
            for(int i = 0; i < numTargets; i++){
                for(int j = 0; j < numTargets; j++){
                    graph[i][j] = computeEstimatedTimeToTarget(targets.get(i), targets.get(j));
                }
            }
            cluster(10,2);
            currentCluster = findNextUnvisitedCluster(groupLocations);
            path = currentCluster.clusterPoints;
        }
    }

    public void computePathInCluster(){
        ArrayList<Integer> clusterPoints = currentCluster.clusterPoints;
        int numPoints = clusterPoints.size();

        graph = new double[numPoints][numPoints];
        for(int i = 0; i < numPoints; i++){
            for(int j = 0; j < numPoints; j++){
                int index1 = clusterPoints.get(i);
                int index2 = clusterPoints.get(j);
                graph[i][j] = computeEstimatedTimeToTarget(targets.get(index1), targets.get(index2));
            }
        }

        mst = new MST();
        int[] parents = mst.primMST(graph);
        buildTree(parents);
        path = new ArrayList<Integer>();
        tree.preorder(path);

        for(int i = 0; i < path.size(); i++){
            int clusterPointIndex = path.get(i);
            int targetIndex = clusterPoints.get(clusterPointIndex);
            path.set(i, targetIndex);
        }
    }

    public void cluster(int minNumElements, double maxDistance){
        clusters = new ArrayList<ClusterInfo>();
        DBSCANClusterer clusterer = new DBSCANClusterer<Point>(targets,minNumElements,maxDistance, graph);
        ArrayList<ArrayList<Integer>> clustersList = clusterer.performClustering();

        for(ArrayList<Integer> clusterPoints: clustersList){
            Point mean = new Point(0,0);
            for(int targetId: clusterPoints){
                mean = Point.sum(mean, targets.get(targetId));
            }
            mean = Point.multiply(mean, 1/clusterPoints.size());
            clusters.add(new ClusterInfo(clusterPoints, mean));
        }
    }

    @Override
    public Point move(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs) {
        if (this.initialTimeRemaining == null) this.initialTimeRemaining = timeRemainingMs;
        this.currentLocation = groupLocations.get(id);
//        if(unvisitedTargets.size() < 100){
//          this.strategy= "weightedGreedy";
//        }
        Point move;

        switch (this.strategy) {
            case "greedy":
                move = greedyMove(groupLocations, id, timeStep, timeRemainingMs);
                break;
            case "weightedGreedy":
                move = weightedGreedyMove(groupLocations, id, timeStep, timeRemainingMs);
                break;
            case "mst":
                move = mstMove(groupLocations, id, timeStep, timeRemainingMs);
                break;
            case "clusteringMst":
                move = clusteringMstMove(groupLocations, id, timeStep, timeRemainingMs);
                break;
            case "clusteringWeightedGreedy":
                move = clusteringWeightedGreedyMove(groupLocations, id, timeStep, timeRemainingMs);
                break;
            case "optimalPath":
                move = optimalPathMove(groupLocations, id, timeStep, timeRemainingMs);
                break;
            default:
                System.err.println("Invalid strategy "+this.strategy+" chosen");
                move = new Point(0,0);
                break;
        }
        this.movesCounter++;
        this.prevGroupLocations = groupLocations;
        return move;
    }

    // Backtracking search. Pass the bestValue as a Double so that it is passed by reference.
    public void optimalSearchRecursive (List<Point> unusedPoints, List<Point> currentPath, double currentTime) {
        Point lastPoint = currentPath.get(currentPath.size()-1);
        if (unusedPoints.size() <= 0) {
            Point firstPoint = currentPath.get(0);
            double finalTime = currentTime + computeEstimatedTimeToTarget(lastPoint, firstPoint);
            if (finalTime < shortestTime) {
                this.shortestTime = finalTime;
                this.shortestPath = new ArrayList<Point>(currentPath);
            }
        } else {
            double newValue;
            for (int i = 0; i < unusedPoints.size(); ++i) {
                Point newPoint = unusedPoints.remove(i);
                currentPath.add(newPoint);
                newValue = currentTime + computeEstimatedTimeToTarget(lastPoint, newPoint);
                optimalSearchRecursive(unusedPoints, currentPath, newValue);
                currentPath.remove(currentPath.size()-1);
                unusedPoints.add(i, newPoint);
            }
        }
    }

    public Point optimalPathMove (List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        if (this.shortestPath == null) {
            ArrayList<Point> unusedPoints = new ArrayList<Point>(this.targets);
            ArrayList<Point> currentPath = new ArrayList<Point>();
            currentPath.add(this.initialLocation);
            this.shortestTime = Double.POSITIVE_INFINITY;
            optimalSearchRecursive(unusedPoints, currentPath, 0.0);
            this.shortestPath.remove(0); // Remove the initial location.
        }

        Point nextToVisit = this.initialLocation;
        for (Point targetInPath : this.shortestPath) {
            if (!this.ourVisitedTargets.contains(targetInPath)) {
                nextToVisit = targetInPath;
                break;
            }
        }
        return computeNextDirection(nextToVisit, timeStep);
    }

    public Point clusteringWeightedGreedyMove(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        while(path.size() > 0 && !ourUnvisitedTargets.contains(path.get(0)))
            path.remove(0);

        if(path.size() == 0){
            if(clusters.size() == 0){
                Point direction = Point.getDirection(currentLocation,initialLocation);
                Point unitDirection = Point.getUnitVector(direction);
                return unitDirection;
            }
            else{
                currentCluster = findNextUnvisitedCluster(groupLocations);
                path = currentCluster.clusterPoints;

                Point direction = Point.getDirection(currentLocation,currentCluster.closestClusterPoint);
                Point unitDirection = Point.getUnitVector(direction);
                return unitDirection;
            }
        }

        double maxWeight = Double.NEGATIVE_INFINITY;
        Point maxWeightTarget = this.initialLocation;  // If no unvisited targets, initial location will be our next target.
        for (int targetId : path) {
            if(!ourUnvisitedTargets.contains(targetId)){
                path.remove(Integer.valueOf(targetId));
                continue;
            }
            double ourTime = computeEstimatedTimeToTarget(targets.get(targetId));
            int score = computeRemainingScore(targetId);
            double othersTime = Utils.sum(computeUnvisitedPlayersTimeTo(groupLocations, targetId));

            double weight = (score * othersTime) / ourTime;
            if (weight > maxWeight) {
                maxWeight = weight;
                maxWeightTarget = targets.get(targetId);
            }
        }


        return computeNextDirection(maxWeightTarget, timeStep);
    }

    public Point clusteringMstMove(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        while(path.size() > 0 && !ourUnvisitedTargets.contains(path.get(0)))
            path.remove(0);

        if(path.size() == 0){
            if(clusters.size() == 0){
                Point direction = Point.getDirection(currentLocation,initialLocation);
                Point unitDirection = Point.getUnitVector(direction);
                return unitDirection;
            }
            else{
                currentCluster = findNextUnvisitedCluster(groupLocations);

                computePathInCluster();

                Point direction = Point.getDirection(currentLocation,currentCluster.closestClusterPoint);
                Point unitDirection = Point.getUnitVector(direction);
                return unitDirection;
            }
        }

        int targetIndex = path.get(0);
        Point target = targets.get(targetIndex);
        Point nextTarget = initialLocation;
        if(path.size() >= 2){
            int nextTargetIndex = path.get(1);
            nextTarget = targets.get(nextTargetIndex);
        }

        Point directionBetweenTargets = Point.getDirection(target,nextTarget);
        Point unitDirectionBetweenTargets = Point.getUnitVector(directionBetweenTargets);
        Point pointWithin10MetersDirection = Point.sum(target,Point.multiply(unitDirectionBetweenTargets,0.01));

        Point direction = Point.getDirection(currentLocation,pointWithin10MetersDirection);
        Point unitDirection = Point.getUnitVector(direction);
        return unitDirection;
    }

    public ClusterInfo findNextUnvisitedCluster(List<Point> groupLocations){
        ClusterInfo closestCluster = currentCluster;
        double maxHeuristic = 0;
        for(ClusterInfo clustering: clusters){
            clustering = computeClosestClusterPoint(clustering);
            clustering.clusterHeuristic = computeClusterHeuristic(clustering, groupLocations);

            if(clustering.clusterHeuristic > maxHeuristic){
                closestCluster = clustering;
                maxHeuristic = clustering.clusterHeuristic;
            }
        }

        clusters.remove(closestCluster);
        return closestCluster;
    }

    public double computeClusterHeuristic(ClusterInfo clustering, List<Point> groupLocations){
        double time = clustering.timeToCluster;
        ArrayList<Integer> clusterPoints = clustering.clusterPoints;
        Point mean = clustering.clusterMean;
        double distanceFromMid = Point.getDistance(mean, new Point(5,5));
        double heuristic = 0;

        for(int targetId: clusterPoints){
            int score = computeRemainingScore(targetId);
            double othersTime = Utils.sum(computeUnvisitedPlayersTimeTo(groupLocations, targetId));

            heuristic += score * othersTime;
        }

        heuristic = heuristic + 15*time + 50/distanceFromMid;
        return heuristic;
    }

    public ClusterInfo computeClosestClusterPoint(ClusterInfo clustering){
        ArrayList<Integer> clusterPoints = clustering.clusterPoints;

        Point closestClusterPoint = targets.get(clusterPoints.get(0));
        double minTime = Double.MAX_VALUE;
        for (int clusterPoint : clusterPoints) {
            double time = computeEstimatedTimeToTarget(targets.get(clusterPoint));
            if (time < minTime) {
                minTime = time;
                closestClusterPoint = targets.get(clusterPoint);
            }
        }

        clustering.closestClusterPoint = closestClusterPoint;
        clustering.timeToCluster = minTime;

        return clustering;
    }

    public Point mstMove(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        while(path.size() > 0 && !ourUnvisitedTargets.contains(path.get(0)))
            path.remove(0);

        if(path.size() == 0){
            Point direction = Point.getDirection(currentLocation,initialLocation);
            Point unitDirection = Point.getUnitVector(direction);
            return unitDirection;
        }

        int targetIndex = path.get(0);
        Point target = targets.get(targetIndex);
        Point nextTarget = initialLocation;
        if(path.size() >= 2){
            int nextTargetIndex = path.get(1);
            nextTarget = targets.get(nextTargetIndex);
        }

        Point directionBetweenTargets = Point.getDirection(target,nextTarget);
        Point unitDirectionBetweenTargets = Point.getUnitVector(directionBetweenTargets);
        Point pointWithin10MetersDirection = Point.sum(target,Point.multiply(unitDirectionBetweenTargets,0.01));

        return computeNextDirection(pointWithin10MetersDirection,timeStep);
    }

    public Point weightedGreedyMove(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        // Let's get the maximum weight unvisited target according to the following formula (if all params are enabled):
        //  targetWeight =
        //      targetRemainingScore * distanceFromNonVisitedPlayers * max(1, diffOfOurTimeAndClosestPlayerTime)
        //     —————————————————————————————————————————————————————————————————————————————————————————————————————
        //               timeToTarget * (weightedDistanceToOtherTargets || avgDistanceToOtherTargets)

        double maxWeight = Double.NEGATIVE_INFINITY;
        Point maxWeightTarget = this.initialLocation;  // If no unvisited targets, initial location will be our next target.
        for (int targetId : this.ourUnvisitedTargets) {
            double weight = 1.0;
            int score = -1;
            double ourTime = -1.0;

            if (WG_SCORE_ENABLED) {
                score = computeRemainingScore(targetId);
                weight *= score;
            }

            if (WG_SCORE_1_PENALTY) {
                if (score == -1) {
                    // Score hasn't been previous computed.
                    score = computeRemainingScore(targetId);
                }
                if (score == 1) {
                    // We are the only one that hasn't visited it. Penalize its weight so that this are the last one visited.
                    weight /= 1000;
                }
            }

            if (WG_TIME_ENABLED) {
                ourTime = computeEstimatedTimeToTarget(targets.get(targetId));
                weight /= ourTime;
            }

            if (WG_PLAYERS_DISTANCES_ENABLED) {
                List<Double> othersTime = computeUnvisitedPlayersTimeTo(groupLocations, targetId);
                double minOthersTime = Utils.min(othersTime);

                if (ourTime < minOthersTime) {
                    // We are closer than anyone, so increment the weight inversely proportional to the difference between
                    // the closest one distance and our distance.
                    weight *= 2 - (minOthersTime - ourTime)/minOthersTime;
                }

                double avgPlayersTime = Utils.average(othersTime);
                weight *= avgPlayersTime;
            }

            if (WG_TARGETS_DISTANCES_ENABLED && this.numTargets >= 500) {
                int updateFrequency = this.numTargets <= 100? 4 : 40;
                boolean shouldUpdateWeights = this.movesCounter % updateFrequency == 0;
                if (shouldUpdateWeights) {
                    computeWeightedTimeToOtherTargets(targetId, this.targets);
                }
                weight /= this.computedTargetToTargetsWeights[targetId];
            }

            if (WG_CHECK_FOLLOWING_ENABLED && this.numTargets >= 500) {
                for (int playerId = 0; playerId < this.numPlayers; ++playerId) {
                    if (playerId == this.id) continue;
                    if (!this.playerVisitsByTarget.get(targetId).contains(playerId)) {
                        Point playerLocation = groupLocations.get(playerId);
                        double playerTime = computeEstimatedTimeToTarget(playerLocation, targets.get(targetId));
                        if (ourTime == -1.0) ourTime = computeEstimatedTimeToTarget(targets.get(targetId));
                        if (playerTime < ourTime && this.prevGroupLocations != null) {
                            Point prevPlayerLocation = this.prevGroupLocations.get(playerId);
                            Point playerDirection = Point.getDirection(prevPlayerLocation, playerLocation);
                            Point directionToTarget = Point.getDirection(this.currentLocation, playerLocation);
                            double tenDegrees = Math.PI/18;
                            if (Point.angleBetweenVectors(playerDirection, directionToTarget) < tenDegrees) {
                                // At this point, we know that there is a player moving in the direction of this target,
                                // which haven't been visited by this player, and that is closer than us.
                                weight = 0;
                            }
                        }


                    }
                }
            }

            if (weight > maxWeight) {
                maxWeight = weight;
                maxWeightTarget = targets.get(targetId);
            }
        }

        return computeNextDirection(maxWeightTarget, timeStep);
    }

    // Applies the Greedy Strategy to choose the next target, only considering the time to reach it.
    public Point greedyMove(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        Point nextTarget = initialLocation; // If no unvisited targets, initial location will be our next target.

        double minTime = Double.MAX_VALUE;
        for (int unvisitedTargetPoint : this.ourUnvisitedTargets) {
            double time = computeEstimatedTimeToTarget(targets.get(unvisitedTargetPoint));
            if (time < minTime) {
                minTime = time;
                nextTarget = targets.get(unvisitedTargetPoint);
            }
        }

        return computeNextDirection(nextTarget, timeStep);
    }

    public void buildTree(int[] parents){
        tree = new Tree();
        int rootIndex = findRootIndexOfMST(parents);
        tree.root = new Node(rootIndex);
        tree.root.children = findChildren(rootIndex, parents);
    }

    public int findRootIndexOfMST(int[] parents){
        for(int i = 0; i < numTargets; i++){
            if(parents[i] == -1){
                return i;
            }
        }
        return -1;
    }

    public ArrayList<Node> findChildren(int rootIndex, int[] parents){
        ArrayList<Node> children = null;

        for(int i = 0; i < parents.length; i++){
            if(parents[i] == rootIndex){
                Node child = new Node(i);
                child.children = findChildren(i, parents);

                if(children == null)
                    children = new ArrayList<Node>();
                children.add(child);
            }
        }

        return children;
    }

    private double computeWeightedTimeToOtherTargets(int targetId, List<Point> targets) {
        // It is weighted because it also takes into account the remaining score of those targets.
        double weightedTime = 1.0;
        Point thisTarget = targets.get(targetId);
        for (int i : this.ourUnvisitedTargets) {
            if (i == targetId) continue;  // Skip itself.
            Point otherTarget = targets.get(i);
            int remainingScore = computeRemainingScore(i);
            weightedTime += computeEstimatedTimeToTarget(thisTarget, otherTarget) / remainingScore;
        }
        this.computedTargetToTargetsWeights[targetId] = weightedTime;
        return weightedTime;
    }


    private List<Double> computeUnvisitedPlayersTimeTo(List<Point> groupLocations, int targetId) {
        ArrayList<Double> distances = new ArrayList<Double>();
        Point target = this.targets.get(targetId);
        for (int playerId = 0; playerId < this.numPlayers; ++playerId) {
            if (playerId == this.id) continue; // Skip our own.
            if (!this.playerVisitsByTarget.get(targetId).contains(playerId)) {
                // This means that this player hasn't visited this target yet, so
                // compute her time to target.
                Point player = groupLocations.get(playerId);
                distances.add(computeEstimatedTimeToTarget(player, target));
            }
        }
        return distances;
    }

    private int computeRemainingScore(int targetId) {
        return this.numPlayers - this.playerVisitsByTarget.get(targetId).size();
    }

    private double computeEstimatedTimeToTarget(Point target) {
        return computeEstimatedTimeToTarget(this.currentLocation, target);
    }

    private double computeEstimatedTimeToTarget(Point player, Point target) {
        double distance = Point.getDistance(player, target);
        Point direction = Point.getDirection(player, target);
        double speed = Simulator.getSpeed(direction, windDirection);
        return distance/speed;
    }

    private Point computeNextDirection(Point target, double timeStep) {
        Point directionToTarget = Point.getDirection(this.currentLocation, target);
        Point perpendicularLeftDirection = Point.rotateCounterClockwise(directionToTarget, Math.PI/2.0);
        Point perpendicularRightDirection = Point.rotateCounterClockwise(directionToTarget, -Math.PI/2.0);
        return findBestDirection(perpendicularLeftDirection, perpendicularRightDirection, target, 100, timeStep);
    }

    private Point findBestDirection(Point leftDirection, Point rightDirection, Point target, int numSteps,
                                    double timeStep) {
        // First, check if the target is reachable in one time step from our current position.
        double currentDistanceToTarget = Point.getDistance(this.currentLocation, target);
        Point directionToTarget = Point.getDirection(this.currentLocation, target);
        double speedToTarget = Simulator.getSpeed(directionToTarget, this.windDirection);
        double distanceWeCanTraverse = speedToTarget * timeStep;
        double distanceTo10MeterAroundTarget = currentDistanceToTarget-0.01;
        if (distanceWeCanTraverse > distanceTo10MeterAroundTarget) {
            return directionToTarget;
        }

        // If that is not the case, choose the direction that will get us to a point where the time to reach
        // the target is minimal, if going directly to the target.
        double minTimeToTarget = Double.POSITIVE_INFINITY;
        Point minTimeToTargetDirection = null;

        double totalRadians = Point.angleBetweenVectors(leftDirection, rightDirection);
        double radiansStep = totalRadians / (double) numSteps;
        for (double i = 0.0; i < totalRadians; i+=radiansStep) {
            Point direction = Point.rotateCounterClockwise(rightDirection, i);
            Point expectedPosition = computeExpectedPosition(direction, timeStep);
            Point nextDirection = Point.getDirection(expectedPosition, target);
            double distance = Point.getDistance(expectedPosition, target);
            double speed = Simulator.getSpeed(nextDirection, this.windDirection);
            double timeToTarget = distance / speed;

            if (timeToTarget < minTimeToTarget) {
                minTimeToTargetDirection = direction;
                minTimeToTarget = timeToTarget;
            }
        }

        return minTimeToTargetDirection;
    }

    private Point computeExpectedPosition(Point moveDirection, double timeStep) {
        Point unitMoveDirection = Point.getUnitVector(moveDirection);
        double speed = Simulator.getSpeed(unitMoveDirection, this.windDirection);
        Point distanceMoved = new Point(
                unitMoveDirection.x * speed * timeStep,
                unitMoveDirection.y * speed * timeStep
        );
        Point nextLocation = Point.sum(this.currentLocation, distanceMoved);
        if (nextLocation.x < 0 || nextLocation.y > 10 || nextLocation.y < 0 || nextLocation.x > 10) {
            return this.currentLocation;
        }
        return nextLocation;
    }

    /**
     * visitedTargets.get(i) is a set of targets that the ith player has visited.
     */
    @Override
    public void onMoveFinished(List<Point> groupLocations, Map<Integer, Set<Integer>> visitedTargets) {
        // Let's use this information to prepare some convenient data structures:
        //  - For every player, unvisited targets, with a special variable pointing to ours.
        //  - For every target, players that have visited it. This will help compute potential score easily.
        this.visitedTargets = visitedTargets;
        this.ourVisitedTargets = new HashSet<Point>();

        for (Integer playerId : visitedTargets.keySet()) {
            Set<Integer> playerVisited = this.visitedTargets.get(playerId);
            Set<Integer> playerUnvisited = this.unvisitedTargets.get(playerId);
            playerUnvisited.removeAll(playerVisited);

            for (Integer target : playerVisited) {
                this.playerVisitsByTarget.get(target).add(playerId);
                if (playerId == this.id) {
                    ourVisitedTargets.add(this.targets.get(target));
                }
            }
        }
    }
}
