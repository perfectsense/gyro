package beam.builder;

import beam.builder.util.gradle.JarPathModel;
import com.google.common.base.Charsets;
import com.psddev.dari.util.IoUtils;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

import java.io.File;
import java.io.FileWriter;

public class GradleBuilder extends ProviderBuilder {

    @Override
    public boolean validate(String path) {
        path = path + File.separator + "build.gradle";
        File buildFile = new File(path);
        if (buildFile.exists() && !buildFile.isDirectory()) {
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

        String jarName = null;
        try {
            String scriptContent = IoUtils.toString(getClass().getResourceAsStream("/init.gradle"), Charsets.UTF_8);
            File initScript = File.createTempFile("init", ".gradle");
            initScript.deleteOnExit();
            String initSrciptPath = initScript.getCanonicalPath();
            FileWriter fileWriter = new FileWriter(initScript);
            fileWriter.write(scriptContent);
            fileWriter.flush();
            fileWriter.close();

            ModelBuilder<JarPathModel> jarPathModelBuilder = connection.model(JarPathModel.class);
            jarPathModelBuilder.withArguments("--init-script", initSrciptPath);
            JarPathModel model = jarPathModelBuilder.get();
            jarName = model.getJarPath();

            BuildLauncher build = connection.newBuild();
            build.forTasks("shadowJar");
            build.run();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }

        return String.format("%s/build/libs/%s", path, jarName);
    }
}
