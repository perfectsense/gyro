package beam.config;

public class AutoScalePolicyConfig extends Config {

    private String name;
    private int instancesPerSubnet;
    private int cooldown;
    private AutoScalePolicyAlarmConfig alarm;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getInstancesPerSubnet() {
        return instancesPerSubnet;
    }

    public void setInstancesPerSubnet(int instancesPerSubnet) {
        this.instancesPerSubnet = instancesPerSubnet;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public AutoScalePolicyAlarmConfig getAlarm() {
        return alarm;
    }

    public void setAlarm(AutoScalePolicyAlarmConfig alarm) {
        this.alarm = alarm;
    }
}
