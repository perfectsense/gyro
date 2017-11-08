package beam.pkg;

import java.io.IOException;
import java.nio.file.Path;

public interface BeamPackageFetcher {

    public boolean canFetchPackage(String packageUrl);

    public Path fetchPackage(String packageUrl, String destinationPath) throws IOException;

}
