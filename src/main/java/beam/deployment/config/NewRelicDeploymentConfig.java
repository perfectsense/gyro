package beam.deployment.config;

import beam.BeamConfig;
import beam.BeamException;
import beam.BeamRuntime;
import beam.config.ConfigValue;
import beam.enterprise.EnterpriseApi;
import com.psddev.dari.util.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.*;

@ConfigValue("newrelic")
public class NewRelicDeploymentConfig extends PsdDeploymentConfig {

    private String getRevision() {
        if (getExtraAttributes().containsKey("metadata")) {
            Map<String, String> metadata = (Map<String, String>) getExtraAttributes().get("metadata");
            String buildNumber = metadata.get("buildNumber");
            String jenkinsBuildPath = metadata.get("jenkinsBuildPath");
            String jenkinsWarFile = metadata.get("jenkinsWarFile");
            return String.format("%s/%s/%s", jenkinsBuildPath, buildNumber, jenkinsWarFile);

        } else {
            throw new BeamException("Unable to determine deployment revision!");
        }
    }

    private String getDescription(String action, String revision) {
        StringBuilder target = new StringBuilder();
        if (getExtraAttributes().containsKey("loadbalancers")) {
            for (String loadbalancerName : (List<String>) getExtraAttributes().get("loadbalancers")) {
                target.append(" ");
                target.append(loadbalancerName);
            }

            return String.format("%s instances for revision %s to production loadbalancer%s", action, revision, target);

        } else {
            throw new BeamException("Unable to determine deployment loadbalancer!");
        }
    }

    private void createMarker(String action) {
        String applicationId = null;
        String apiKey = null;
        String user = EnterpriseApi.getEnterpriseUser();
        String revision = getRevision();
        String description = getDescription(action, revision);

        List<Map<String, Object>> projects = BeamConfig.get(List.class, "projects", new ArrayList<>());
        for (Map<String, Object> project : projects) {
            if (BeamRuntime.getCurrentRuntime().getProject().equals(project.get("name"))) {
                if (project.containsKey("newrelic")) {
                    Map<String, Object> newrelicMap = (Map<String, Object>) project.get("newrelic");
                    applicationId = newrelicMap.containsKey("applicationId") ? newrelicMap.get("applicationId").toString() : null;
                    apiKey = newrelicMap.containsKey("apiKey") ? newrelicMap.get("apiKey").toString() : null;
                }
            }
        }

        if (applicationId == null || apiKey == null) {
            throw new BeamException("Newrelic applicationId or apiKey is not configured.");
        }

        String endpoint = "https://api.newrelic.com/v2/applications/" + applicationId + "/deployments.json";
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Api-Key", apiKey);
        headers.put("Content-Type", "application/json");

        Map<String, String> parameters = new HashMap<>();
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("user", user);
        dataMap.put("revision", revision);
        dataMap.put("description", description);
        dataMap.put("timestamp", new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));

        Map<String, Object> deploymentMap = new HashMap<>();
        deploymentMap.put("deployment", dataMap);
        String payload = ObjectUtils.toJson(deploymentMap);


        try {
            Map<String, Object> response = BeamRuntime.callApi("post", endpoint, headers, parameters, payload);
            if (response.containsKey("error")) {
                throw new BeamException(response.get("error").toString());
            }
        } catch (Exception e) {
            throw new BeamException(e.getMessage());
        }
    }

    @Override
    public void afterPush() {
        createMarker("Pushing");
    }

    @Override
    public void afterRevert() {
        createMarker("Reverting");
    }

    @Override
    public void afterCommit() {
        createMarker("Committing");
    }
}
