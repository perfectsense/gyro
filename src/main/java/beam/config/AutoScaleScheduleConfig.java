package beam.config;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class AutoScaleScheduleConfig extends Config {

    private String name;
    private Integer scaleUpPerSubnet;
    private Integer scaleDownPerSubnet;
    private Integer desiredPerSubnet;
    private String startRecurrence;
    private String endRecurrence;
    private String duration;
    private String startTime;
    private String endTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getScaleUpPerSubnet() {
        return scaleUpPerSubnet;
    }

    public void setScaleUpPerSubnet(Integer scaleUpPerSubnet) {
        this.scaleUpPerSubnet = scaleUpPerSubnet;
    }

    public Integer getScaleDownPerSubnet() {
        return scaleDownPerSubnet;
    }

    public void setScaleDownPerSubnet(Integer scaleDownPerSubnet) {
        this.scaleDownPerSubnet = scaleDownPerSubnet;
    }

    public Integer getDesiredPerSubnet() {
        return desiredPerSubnet;
    }

    public void setDesiredPerSubnet(Integer desiredPerSubnet) {
        this.desiredPerSubnet = desiredPerSubnet;
    }

    public String getStartRecurrence() {
        return startRecurrence;
    }

    public void setStartRecurrence(String startRecurrence) {
        this.startRecurrence = startRecurrence;
    }

    public String getEndRecurrence() {
        return endRecurrence;
    }

    public void setEndRecurrence(String endRecurrence) {
        this.endRecurrence = endRecurrence;
    }

    public Interval getInterval() {
        DateTime startTime = getStartTime() == null ?
                DateTime.now() : DateTime.parse(getStartTime());

        return new Interval(startTime, getDurationPeriod());
    }

    public Period getDurationPeriod() {
        if (duration == null) {
            return Period.ZERO;
        }

        PeriodFormatter formatter = new PeriodFormatterBuilder()
                .appendDays().appendSuffix("d")
                .appendHours().appendSuffix("h")
                .toFormatter();

        return Period.parse(duration, formatter);
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public DateTime getStartDateTime() {
        if (getStartTime() == null) {
            return null;
        }

        return DateTime.parse(getStartTime());
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public DateTime getEndDateTime() {
        if (getEndTime() != null) {
            return DateTime.parse(getEndTime());
        }

        if (getDurationPeriod() != Period.ZERO) {
            return getStartDateTime().plus(getDurationPeriod());
        }

        return null;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public boolean isRecurring() {
        if (getStartRecurrence() != null && getEndRecurrence() != null) {
            return true;
        }

        return false;
    }
}