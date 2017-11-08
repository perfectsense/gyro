package beam.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AutoScaleConfig extends Config {

    private String loadBalancer;
    private List<String> loadBalancers;
    private int minPerSubnet;
    private int maxPerSubnet;
    private int cooldown;
    private List<AutoScalePolicyConfig> policies;
    private List<AutoScaleScheduleConfig> schedules;

    public String getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(String loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public List<String> getLoadBalancers() {
        if (loadBalancers == null) {
            loadBalancers = new ArrayList<>();

            if (loadBalancer != null) {
                loadBalancers.add(loadBalancer);
            }
        }
        return loadBalancers;
    }

    public void setLoadBalancers(List<String> loadBalancers) {
        this.loadBalancers = loadBalancers;
    }

    public int getMinPerSubnet() {
        return minPerSubnet;
    }

    public void setMinPerSubnet(int minPerSubnet) {
        this.minPerSubnet = minPerSubnet;
    }

    public int getMaxPerSubnet() {
        return maxPerSubnet;
    }

    public void setMaxPerSubnet(int maxPerSubnet) {
        this.maxPerSubnet = maxPerSubnet;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public List<AutoScalePolicyConfig> getPolicies() {
        if (policies == null) {
            policies = new ArrayList<>();
        }

        return policies;
    }

    public void setPolicies(List<AutoScalePolicyConfig> policies) {
        this.policies = policies;
    }

    public List<AutoScaleScheduleConfig> getSchedules() {
        if (schedules == null) {
            schedules = new ArrayList<>();
        }

        Collections.sort(schedules, new Comparator<AutoScaleScheduleConfig>() {
            @Override
            public int compare(AutoScaleScheduleConfig o1, AutoScaleScheduleConfig o2) {
                if (o1.getInterval().isBefore(o2.getInterval())) {
                    return -1;
                }

                if (o1.getInterval().isAfter(o2.getInterval())) {
                    return 1;
                }

                return 0;
            }
        });

        return schedules;
    }

    public void setSchedules(List<AutoScaleScheduleConfig> schedules) {
        this.schedules = schedules;
    }

}