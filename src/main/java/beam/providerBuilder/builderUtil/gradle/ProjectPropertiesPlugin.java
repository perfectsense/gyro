package beam.providerBuilder.builderUtil.gradle;

import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

public class ProjectPropertiesPlugin implements Plugin<Project> {
    private final ToolingModelBuilderRegistry registry;

    @Inject
    public ProjectPropertiesPlugin(ToolingModelBuilderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void apply(Project project) {
        registry.register(new JarPathModelBuilder());
    }

    private static class JarPathModelBuilder implements ToolingModelBuilder {
        @Override
        public boolean canBuild(String modelName) {
            return modelName.equals(JarPathModel.class.getName());
        }

        @Override
        public Object buildAll(String modelName, Project project) {
            String name = project.getName();
            String version = project.getVersion().toString();

            return new DefaultJarPathModel(name, version);
        }
    }
}
