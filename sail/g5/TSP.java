package sail.g5;


import sail.sim.Point;
import sail.sim.Simulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class PointHelper {
    public static double distance(Point first, Point second, Point windDirection) {
        double distance = Point.getDistance(first, second);
        Point direction = Point.getDirection(first, second);
        double speed = Simulator.getSpeed(direction, windDirection);
        return distance/speed;
    }
}

public class TSP {
    private class Travel {
        private ArrayList<Point> travel = new ArrayList<>();
        private ArrayList<Point> previousTravel = new ArrayList<>();

        public Travel(List<Point> points) {
            for (int i = 0; i < points.size(); i++) {
                travel.add(new Point(points.get(i).x, points.get(i).y));
            }
        }

        public List<Point> getPoints() {
            return new ArrayList<>(this.travel);
        }

        public void generateInitialTravel() {
            Collections.shuffle(travel);
        }

        public void swapPoints() {
            int a = generateRandomIndex();
            int b = a;
            while (b == a) b = generateRandomIndex();
            previousTravel = travel;
            Point x = travel.get(a);
            Point y = travel.get(b);
            travel.set(a, y);
            travel.set(b, x);
        }

        public void revertSwap() {
            travel = previousTravel;
        }

        private int generateRandomIndex() {
            return (int) (Math.random() * (travel.size()-1)) + 1;
        }

        public Point getPoint(int index) {
            return travel.get(index);
        }

        public double getDistance(Point windDirection) {
            double distance = 0;
            for (int index = 0; index < travel.size(); index++) {
                Point starting = getPoint(index);
                Point destination;
                if (index + 1 < travel.size()) {
                    destination = getPoint(index + 1);
                } else {
                    destination = getPoint(0);
                }
                distance += PointHelper.distance(starting, destination, windDirection);
            }
            return distance;
        }
    }

    private Travel travel;
    private Point windDirection;

    public TSP(List<Point> points, Point windDirection) {
        this.windDirection = windDirection;
        this.travel = new Travel(points);
    }

    public List<Point> simulateAnnealing(double startingTemperature, int numberOfIterations, double coolingRate) {
        System.out.println("Starting SA with temperature: " + startingTemperature + ", # of iterations: " + numberOfIterations + " and colling rate: " + coolingRate);
        double t = startingTemperature;

        double bestDistance = this.travel.getDistance(this.windDirection);
        System.out.println("Initial distance of travel: " + bestDistance);
        List<Point> bestSolution = this.travel.getPoints();
        Travel currentSolution = this.travel;

        for (int i = 0; i < numberOfIterations; i++) {
            if (t <= 0.1) break;
            currentSolution.swapPoints();
            double currentDistance = currentSolution.getDistance(this.windDirection);
            if (currentDistance < bestDistance) {
                System.out.println(bestDistance);
                bestDistance = currentDistance;
                bestSolution = currentSolution.getPoints();
            }
            else if (Math.exp((bestDistance - currentDistance) / t) < Math.random()) {
                currentSolution.revertSwap();
            }
            t *= coolingRate;
            if (i % 100 == 0) {
                System.out.println("Iteration #" + i);
            }
        }
        return bestSolution;
    }
}
