package beam.config;

public class AutoScalePolicyAlarmConfig extends Config {

    private String metric;
    private double greater;
    private double less;
    private int period;

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public double getGreater() {
        return greater;
    }

    public void setGreater(double greater) {
        this.greater = greater;
    }

    public double getLess() {
        return less;
    }

    public void setLess(double less) {
        this.less = less;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
}
