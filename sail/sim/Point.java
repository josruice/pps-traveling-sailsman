package sail.sim;
import java.io.Serializable;
import java.util.Random;

public class Point implements Serializable {
    public final double x;
    public final double y;
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object other) {
    	if(!(other instanceof Point)) return false;
    	Point o = (Point) other;
    	return x == o.x && y == o.y;
    }

    @Override
    public int hashCode() {
    	return new Double(x * 10000 + y).hashCode();
    }

    public static double angleBetweenVectors(Point p1, Point p2) {
        double x1 = p1.x, x2 = p2.x, y1 = p1.y, y2 = p2.y;
        double dot = x1*x2 + y1*y2;    
        double det = x1*y2 - y1*x2; 
        double angle = Math.atan2(det, dot);
        if(angle < 0) angle += 2*Math.PI;
        return angle;
    }

    /*
     * Angle in Radian
     */
    public static Point rotateCounterClockwise(Point vector, double angle) {
        double newx, newy,x,y;
        x = vector.x;
        y = vector.y;
        newx = x*Math.cos(angle) - y*Math.sin(angle);
        newy = y*Math.cos(angle) + x*Math.sin(angle);
        return new Point(newx, newy);
    }

    public static double getDistance(Point first, Point second) {
        double dist_square = (first.x - second.x)*(first.x - second.x) + (first.y - second.y)*(first.y - second.y);
        double dist = Math.sqrt(dist_square);
        return dist;
    }

    public static Point getUnitVector(Point point) {
        double x = point.x, y = point.y;
        double norm = Math.hypot(x, y);
        x /= norm;
        y /= norm;
        
        return new Point(x, y);
    }
    public static double getNorm(Point p) {
        return Math.hypot(p.x, p.y);
    }
    public static Point getRandomPoint(Random gen) {
        double angle = gen.nextDouble() * 2 * Math.PI;
        return getUnitVector(new Point(Math.cos(angle),Math.sin(angle)));
    }
    public static Point getDirection(Point p1, Point p2) {
        return sum(p2, new Point(-p1.x, -p1.y));
    }
    public static Point sum(Point p1, Point p2 ) {
        return new Point(p1.x + p2.x, p1.y + p2.y);
    }
    public static double dot(Point a, Point b) {
        return a.x*b.x + a.y *b.y;
    }
    public static Point multiply(Point a, double b) {
        return new Point(a.x*b, a.y*b);
    }
}
