package cn.kizzzy.qqt;

import java.util.HashMap;
import java.util.Map;

public enum QqtElementXyer {
    
    INS;
    
    public static class Point {
        public int x;
        public int y;
        
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    
    private static final Point ZERO = new Point(0, 0);
    
    private final Map<Integer, Point> pointKvs;
    
    QqtElementXyer() {
        pointKvs = new HashMap<>();
        pointKvs.put(14116, new Point(-7, -13));
        pointKvs.put(14117, new Point(+7, +4));
        pointKvs.put(14118, new Point(+7, -13));
        pointKvs.put(14119, new Point(-7, -12));
    }
    
    public Point GetXy(int value) {
        Point point = pointKvs.get(value);
        if (point == null) {
            point = ZERO;
        }
        return point;
    }
}
