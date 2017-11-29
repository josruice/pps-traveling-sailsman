package sail.g5;

import java.util.List;

public class Utils {
    public static double average (List<Double> list) {
        return sum(list) / Math.max(1, list.size());
    }

    public static double sum (List<Double> list) {
        double result = 0.0;
        for (Double n : list) result += n;
        return result;
    }

    public static double min (List<Double> list) {
        double result = Double.POSITIVE_INFINITY;
        for (Double n : list)
            if (n < result) result = n;
        return result;
    }
}
