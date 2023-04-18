package my.edu.utar.fyp;

public class ReferencePoint {
    private float x, y;
    private int rssi1, rss2, rss3i3, pointNum;
    private String timestamp;

    public ReferencePoint(float x, float y, int rssi1, int rss2, int rss3i3, int pointNum, String timestamp) {
        this.x = x;
        this.y = y;
        this.rssi1 = rssi1;
        this.rss2 = rss2;
        this.rss3i3 = rss3i3;
        this.pointNum = pointNum;
        this.timestamp = timestamp;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public int getRssi1() {
        return rssi1;
    }

    public void setRssi1(int rssi1) {
        this.rssi1 = rssi1;
    }

    public int getRss2() {
        return rss2;
    }

    public void setRss2(int rss2) {
        this.rss2 = rss2;
    }

    public int getRss3i3() {
        return rss3i3;
    }

    public void setRss3i3(int rss3i3) {
        this.rss3i3 = rss3i3;
    }

    public int getPointNum() {
        return pointNum;
    }

    public void setPointNum(int pointNum) {
        this.pointNum = pointNum;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
