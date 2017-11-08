package beam.deployment.config;

import beam.BeamCloud;
import beam.config.ConfigValue;
import beam.config.DeploymentConfig;
import com.psddev.dari.util.ObjectUtils;

import java.util.*;

@ConfigValue("default")
public class PsdDeploymentConfig extends DeploymentConfig {

    private Integer buildNumber;
    private String jenkinsBucket;
    private String jenkinsBucketRegion;
    private String jenkinsBuildPath;
    private String jenkinsWarfile;

    public Integer getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(Integer buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getJenkinsBucket() {
        return jenkinsBucket;
    }

    public void setJenkinsBucket(String jenkinsBucket) {
        this.jenkinsBucket = jenkinsBucket;
    }

    public String getJenkinsBucketRegion() {
        return jenkinsBucketRegion;
    }

    public void setJenkinsBucketRegion(String jenkinsBucketRegion) {
        this.jenkinsBucketRegion = jenkinsBucketRegion;
    }

    public String getJenkinsBuildPath() {
        return jenkinsBuildPath;
    }

    public void setJenkinsBuildPath(String jenkinsBuildPath) {
        this.jenkinsBuildPath = jenkinsBuildPath;
    }

    public String getJenkinsWarfile() {
        return jenkinsWarfile;
    }

    public void setJenkinsWarfile(String jenkinsWarfile) {
        this.jenkinsWarfile = jenkinsWarfile;
    }

    @Override
    public Map<String, String> prepare(BeamCloud cloud, Object pending) {
        String bucketName = getJenkinsBucket();
        String buildsKey = String.format("production-builds/%s/%s/%s",
                getJenkinsBuildPath(), getBuildNumber(),
                getJenkinsWarfile());

        String oldBuildsKey = String.format("builds/%s/%s/%s", getJenkinsBuildPath(),
                getBuildNumber(), getJenkinsWarfile());

        String commonKey = String.format("%s/%d/%s", getJenkinsBuildPath(), getBuildNumber(), getJenkinsWarfile());
        String warUrl = cloud.copyDeploymentFile(bucketName, getJenkinsBucketRegion(), buildsKey, oldBuildsKey, commonKey, pending);

        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("war_file", ObjectUtils.firstNonNull(warUrl, ""));

        return dataMap;
    }

    @Override
    public Map<String, String> getGroupHashItems() {
        Map<String, String> groupHashItems = new LinkedHashMap<>();
        groupHashItems.put("buildNumber", getBuildNumber().toString());
        groupHashItems.put("jenkinsBucket", getJenkinsBucket());
        groupHashItems.put("jenkinsBuildPath", getJenkinsBuildPath());
        groupHashItems.put("jenkinsWarFile", getJenkinsWarfile());

        return groupHashItems;
    }

    @Override
    public List<String> getGroupHashKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("buildNumber");
        keys.add("jenkinsBucket");
        keys.add("jenkinsBuildPath");
        keys.add("jenkinsWarfile");

        return keys;
    }

    @Override
    public String toDisplayString() {
        return String.format("buildNumber: %d, jenkinsBucket: %s, jenkinsBuildPath: %s, jenkinsWarFile: %s",
                getBuildNumber(),
                getJenkinsBucket(),
                getJenkinsBuildPath(),
                getJenkinsWarfile());
    }
}
