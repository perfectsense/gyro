package beam.providerBuilder;

import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.File;

public class GradleBuilder extends ProviderBuilder {

    @Override
    public boolean validate(String path) {
        path = path + File.separator + "build.gradle";
        File buildFile = new File(path);
        if(buildFile.exists() && !buildFile.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String build(String path) {
        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(new File(path))
                .connect();
        try {
            BuildLauncher build = connection.newBuild();
            build.forTasks("dist");
            build.run();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }

        return path + "/build/libs/beam.git-1.0.jar";
    }
}
