package beam.aws;

import beam.BeamCloud;
import beam.BeamRuntime;
import beam.BeamStorage;
import beam.BeamStorageCreator;

public class S3StorageCreator implements BeamStorageCreator {

    @Override
    public BeamStorage createStorage(BeamRuntime runtime) throws Exception {
        for (BeamCloud cloud : runtime.getClouds()) {
            if (cloud instanceof AWSCloud) {
                String bucket = System.getenv("BEAM_S3_BUCKET");

                if (bucket == null) {
                    bucket = ("beam.project-" +
                            runtime.getProject() + ".environment-" +
                            runtime.getEnvironment()).
                            replaceAll("_", "-");
                }

                return new S3Storage((AWSCloud) cloud, bucket);
            }
        }

        return null;
    }
}
