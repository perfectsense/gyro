package beam.pkg;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class FilePackageFetcher implements BeamPackageFetcher {

    @Override
    public boolean canFetchPackage(String packageUrl) {
        return packageUrl.startsWith("/") || packageUrl.startsWith("./");
    }

    @Override
    public Path fetchPackage(String packageUrl, String destinationPath) {
        String basename = new File(packageUrl).getName();

        Path packagePath = FileSystems.getDefault().getPath(packageUrl);
        packagePath.toFile().mkdirs();

        return packagePath;
    }

}
