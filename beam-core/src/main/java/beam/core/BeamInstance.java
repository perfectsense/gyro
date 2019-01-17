package beam.core;

public interface BeamInstance {

    public String getInstanceId();

    public String getState();

    public String getPrivateIpAddress();

    public String getPublicIpAddress();

    public String getHostname();

    public String getLocation();

    public String getLaunchDate();

}
