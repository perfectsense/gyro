package gyro.plugin.enterprise;

import gyro.core.diff.ResourceName;
import gyro.lang.Resource;

import java.util.Set;

@ResourceName("project")
public class EnterpriseProject extends Resource {

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

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create() {

    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {

    }

    @Override
    public String toDisplayString() {
        return null;
    }

    @Override
    public Class resourceCredentialsClass() {
        return null;
    }

}
