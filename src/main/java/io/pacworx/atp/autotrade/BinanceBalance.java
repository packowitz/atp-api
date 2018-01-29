package io.pacworx.atp.autotrade;

public class BinanceBalance {
    private String asset;
    private double free;
    private double locked;

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public double getFree() {
        return free;
    }

    public void setFree(String free) {
        this.free = Double.parseDouble(free);
    }

    public double getLocked() {
        return locked;
    }

    public void setLocked(String locked) {
        this.locked = Double.parseDouble(locked);
    }
}
