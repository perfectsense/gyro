package beam.deployment.config;

import beam.BeamCloud;
import beam.config.ConfigValue;
import beam.config.DeploymentConfig;
import com.psddev.dari.util.ObjectUtils;

import java.util.*;

@ConfigValue("url")
public class UrlDeploymentConfig extends DeploymentConfig {

    private Integer buildNumber;
    private String objectUrl;

    public Integer getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(Integer buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getObjectUrl() {
        return objectUrl;
    }

    public void setObjectUrl(String objectUrl) {
        this.objectUrl = objectUrl;
    }

    @Override
    public Map<String, String> prepare(BeamCloud cloud, Object pending) {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("type", this.getClass().getName());
        dataMap.put("war_file", ObjectUtils.firstNonNull(getObjectUrl(), ""));

        return dataMap;
    }

    @Override
    public Map<String, String> getGroupHashItems() {
        Map<String, String> groupHashItems = new LinkedHashMap<>();
        groupHashItems.put("buildNumber", getBuildNumber().toString());
        groupHashItems.put("objectUrl", getObjectUrl());

        return groupHashItems;
    }

    @Override
    public List<String> getGroupHashKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("buildNumber");
        keys.add("war_file");

        return keys;
    }

    @Override
    public String toDisplayString() {
        return String.format("buildNumber: %d, objectUrl: %s", getBuildNumber(), getObjectUrl());
    }
}
