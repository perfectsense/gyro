package beam.openstack;

import beam.BeamCloud;
import beam.BeamRuntime;
import beam.BeamStorage;
import beam.BeamStorageCreator;

public class SwiftStorageCreator implements BeamStorageCreator {

    @Override
    public BeamStorage createStorage(BeamRuntime runtime) throws Exception {
        for (BeamCloud cloud : runtime.getClouds()) {
            if (cloud instanceof OpenStackCloud) {
                String container = System.getenv("BEAM_S3_BUCKET");

                if (container == null) {
                    container = ("beam.project-" +
                            runtime.getProject() + ".environment-" +
                            runtime.getEnvironment()).
                            replaceAll("_", "-");
                }

                return new SwiftStorage((OpenStackCloud) cloud, container);
            }
        }

        return null;
    }

}