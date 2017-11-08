package beam.aws;

import java.util.Arrays;
import java.util.HashSet;

import beam.utils.RestyReadTimeout;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

import beam.BeamCloud;
import beam.BeamRuntime;
import beam.BeamRuntimeCreator;

public class AWSRuntimeCreator implements BeamRuntimeCreator {

    @Override
    public BeamRuntime createRuntime(String env) throws Exception {
        JSONObject userData = new Resty(Resty.Option.timeout(500), new RestyReadTimeout(500)).
                json("http://169.254.169.254/latest/user-data").
                object();

        String project = userData.getString("project");
        String serial = userData.getString("serial");
        AWSCloud cloud = new AWSCloud(project, serial);

        String zone = new Resty(Resty.Option.timeout(500), new RestyReadTimeout(500)).
                text("http://169.254.169.254/latest/meta-data/placement/availability-zone").
                toString();
        cloud.getActiveRegions().add(zone.substring(0, zone.length() - 1));

        return new BeamRuntime(
                env != null ? env : userData.getString("environment"),
                project,
                null,
                serial,
                userData.getString("internalDomain"),
                new HashSet<BeamCloud>(Arrays.asList(cloud)));
    }

    @Override
    public BeamRuntime createRuntime() throws Exception {
        return createRuntime(null);
    }
}