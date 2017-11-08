package beam.cli;

import beam.Beam;
import beam.BeamCloud;
import beam.BeamException;
import beam.BeamRuntime;
import io.airlift.command.Arguments;
import org.fusesource.jansi.AnsiRenderWriter;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Basic {@link AbstractCommand} implementation that can iterate over every
 * cloud in an environment.
 *
 * @see AbstractGlobalCommand.CloudHandler
 */
public abstract class AbstractGlobalCommand extends AbstractCommand {

    @Arguments(title = "account project", usage = "<account> <project>", description = "Name of the account and project.")
    public List<String> arguments;

    protected transient PrintWriter out;
    protected transient BeamRuntime runtime;

    /**
     * Returns {@code true} if this command requires access to the
     * configuration file.
     */
    protected boolean isConfigRequired() {
        return false;
    }

    /**
     * @return Never {@code null}.
     */
    protected abstract CloudHandler getCloudHandler();

    /**
     * @return Never {@code null}.
     */

    @Override
    protected final void doExecute() throws Exception {
        this.out = new AnsiRenderWriter(System.out, true);
        runtime = null;

        File configFile = new File("network.yml");

        try {
            createRuntime(configFile);

        } catch (Exception error) {
            throw error;
        }

        Set<BeamCloud> clouds = runtime.getClouds();
        boolean hasCloud = !clouds.isEmpty();
        CloudHandler handler = getCloudHandler();

        if (hasCloud) {
            handler.first();

            for (BeamCloud cloud : clouds) {
                handler.each(cloud);
            }

            handler.last(clouds);

        } else {
            handler.empty();
        }
    }

    protected void createRuntime(File configFile) throws Exception {
        if (!configFile.isFile()) {

            if (arguments == null) {
               throw new BeamException("The account and project must be provided.");
            }

            if (arguments.size() != 2) {
                throw new BeamException("Invalid number of arguments. The account and project must be provided.");
            }

            String accountName = arguments.get(0);
            String projectName = arguments.get(1);

            if (projectName == null) {
                throw new BeamException("Can't determine the project!");
            }

            if (accountName == null) {
                throw new BeamException("Can't determine the account!");
            }

            Set<BeamCloud> clouds = new HashSet<>();
            Map<String, BeamCloud> cloudsByName = new HashMap<String, BeamCloud>();

            BeamRuntime tempRuntime = new BeamRuntime("network", projectName, accountName, "1", "", clouds);

            for (Class<? extends BeamCloud> cloudClass : Beam.getReflections().getSubTypesOf(BeamCloud.class)) {
                try {
                    BeamCloud cloud = cloudClass.getConstructor(BeamRuntime.class).newInstance(tempRuntime);

                    cloudsByName.put(cloud.getName(), cloud);

                } catch (IllegalAccessException |
                        InstantiationException |
                        InvocationTargetException |
                        NoSuchMethodException error) {

                    out.format("Can't create an instance of [%s]!", cloudClass);
                    out.flush();
                }
            }

            for (String name : cloudsByName.keySet()) {
                BeamCloud cloud = cloudsByName.get(name);

                if (cloud != null) {
                    clouds.add(cloud);

                } else {
                    out.format("Can't find a subclass of [%s] associated with [%s]!", BeamCloud.class.getName(), name);
                }
            }

            runtime = new BeamRuntime("network", projectName, accountName, "1", "", clouds);
            BeamRuntime.setCurrentRuntime(runtime);

        } else {

            runtime = BeamRuntime.setCurrentRuntime("network");

            if (runtime == null) {
                runtime = BeamRuntime.getCurrentRuntime();
            }
        }
    }

    /**
     * Callbacks from {@link AbstractGlobalCommand}.
     */
    protected abstract class CloudHandler {

        /**
         * Called before the first {@link #each} call if there are any
         * clouds in this environment.
         *
         * <p>Default implementation does nothing.</p>
         */
        public void first() throws Exception {
        }

        /**
         * Called for each cloud in this environment.
         *
         * <p>Default implementation does nothing.</p>
         *
         * @param cloud Can't be {@code null}.
         */
        public void each(BeamCloud cloud) throws Exception {
        }

        /**
         * Called after every {@link #each} call if there are any
         * clouds in this environment.
         *
         * <p>Default implementation does nothing.</p>
         *
         * @param clouds Can't be {@code null}.
         */
        public void last(Set<BeamCloud> clouds) throws Exception {
        }

        /**
         * Called if there aren't any clouds in this environment.
         *
         * <p>Default implementation outputs a message.</p>
         */
        public void empty() throws Exception {
            out.write("@|red No clouds found.|@\n");
            out.flush();
        }

        public Set<String> split(String argument) {
            Set<String> split = new HashSet<>();

            if (argument!= null) {
                split.addAll(Arrays.asList(argument.trim().split("\\s*,\\s*")));
            }

            return split;
        }
    }
}


