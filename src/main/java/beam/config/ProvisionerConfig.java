package beam.config;

import java.io.PrintWriter;

import beam.BeamInstance;

@ConfigKey("type")
public abstract class ProvisionerConfig extends Config {

    private String type;
    private String user;
    private String keyfile;
    private String name;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getKeyfile() {
        return keyfile;
    }

    public void setKeyfile(String keyfile) {
        this.keyfile = keyfile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract void provision(BeamInstance instance, RootConfig config, boolean prepare, PrintWriter out) throws Exception;
}
