package gyro.core;

public interface BeamInstance {

    public String getInstanceId();

    public String getState();

    public String getPrivateIpAddress();

    public String getPublicIpAddress();

    public String getHostname();

    public String getName();

    public String getLaunchDate();

}
