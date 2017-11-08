package beam.config;

import java.util.ArrayList;
import java.util.List;

public class SecurityRuleConfig extends Config {

    private String name;
    boolean excludeSelf;
    private List<AccessPermissionConfig> permissions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isExcludeSelf() {
        return excludeSelf;
    }

    public void setExcludeSelf(boolean excludeSelf) {
        this.excludeSelf = excludeSelf;
    }

    public List<AccessPermissionConfig> getPermissions() {
        if (permissions == null) {
            permissions = new ArrayList<>();
        }
        return permissions;
    }

    public void setPermissions(List<AccessPermissionConfig> permissions) {
        this.permissions = permissions;
    }
}
