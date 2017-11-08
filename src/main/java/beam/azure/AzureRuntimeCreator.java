package beam.azure;

import beam.BeamCloud;
import beam.BeamRuntime;
import beam.BeamRuntimeCreator;
import com.psddev.dari.util.ObjectUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

public class AzureRuntimeCreator implements BeamRuntimeCreator {
    @Override
    public BeamRuntime createRuntime(String env) throws Exception {
        String userDataString = FileUtils.readFileToString(new File("/etc/beam/customdata.json"));
        Map userData = (Map) ObjectUtils.fromJson(userDataString);

        String project = (String) userData.get("project");
        String serial = (String) userData.get("serial");

        AzureCloud cloud = new AzureCloud(project, serial);

        return new BeamRuntime(
                env != null ? env : (String) userData.get("environment"),
                project,
                null,
                serial,
                (String) userData.get("internalDomain"),
                new HashSet<BeamCloud>(Arrays.asList(cloud)));
    }

    @Override
    public BeamRuntime createRuntime() throws Exception {
        return createRuntime(null);
    }
}
