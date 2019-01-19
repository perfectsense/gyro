package beam.plugins.enterprise;

import beam.core.diff.ResourceName;
import beam.lang.ConfigResource;

@ResourceName("project")
public class EnterpriseProject extends ConfigResource {

    private String login;
    private String duoPush;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getDuoPush() {
        return duoPush;
    }

    public void setDuoPush(String duoPush) {
        this.duoPush = duoPush;
    }

}
