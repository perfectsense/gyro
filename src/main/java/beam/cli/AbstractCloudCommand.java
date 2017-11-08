package beam.cli;

import com.psddev.dari.util.ObjectUtils;
import io.airlift.command.Arguments;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.*;

import beam.BeamCloud;
import beam.BeamException;
import beam.BeamRuntime;
import org.fusesource.jansi.AnsiRenderWriter;
import org.yaml.snakeyaml.Yaml;

/**
 * Basic {@link AbstractCommand} implementation that can iterate over every
 * cloud in an environment.
 *
 * @see AbstractCloudCommand.CloudHandler
 */
public abstract class AbstractCloudCommand extends AbstractCommand {

    @Arguments(title = "environment", usage = "<environment>", description = "Name of environment to use.")
    public List<String> arguments;

    public String environmentName;

    protected transient PrintWriter out;
    protected transient BeamRuntime runtime;

    private boolean everConfirmed;

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

    public boolean isEverConfirmed() {
        return everConfirmed;
    }

    public void setEverConfirmed(long delay) {
        if (everConfirmed) {
            return;
        }

        everConfirmed = true;

        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {

        }
    }

    @Override
    protected final void doExecute() throws Exception {
        runtime = null;

        if (arguments != null) {
            if (arguments.size() != 1) {
                arguments.remove(arguments.size() - 1);
                throw new BeamException(String.format("Invalid arguments: %s", String.join(" ", arguments)));
            } else {
                environmentName = arguments.get(0);
            }
        }

        if (environmentName != null) {
            runtime = BeamRuntime.setCurrentRuntime(environmentName);
        }

        if (runtime == null) {
            runtime = BeamRuntime.getCurrentRuntime();

            if (environmentName == null) {
                environmentName = runtime.getEnvironment();
            }
        } else {
            environmentName = runtime.getEnvironment();
        }

        if (isConfigRequired() && runtime.getConfig() == null) {
            throw new BeamException("Configuration file required!");
        }

        if (this instanceof AuditableCommand && ((AuditableCommand) this).shouldAudit()) {
            BeamRuntime.startAudit(this);
        }

        this.out = new AnsiRenderWriter(System.out, true);

        Set<BeamCloud> clouds = runtime.getClouds();
        boolean hasCloud = !clouds.isEmpty();
        CloudHandler handler = getCloudHandler();

        boolean success = true;
        try {
            if (hasCloud) {
                handler.first();

                for (BeamCloud cloud : clouds) {
                    handler.each(cloud);
                }

                handler.last(clouds);

            } else {
                handler.empty();
            }
        } catch (Exception ex) {
            success = false;
            Throwable cause = null;

            PrintWriter out = new AnsiRenderWriter(System.out, true);
            if (ex instanceof BeamException) {
                out.write("@|red Error: " + ex.getMessage() + "|@\n");
                out.flush();

                cause = ex.getCause();

            } else {
                out.write("@|red Unexpected error! Stack trace follows:|@\n");
                out.flush();

                cause = ex;
            }

            if (cause != null) {
                out.write(cause.getClass().getName());
                out.write(": ");
                cause.printStackTrace(out);
                out.flush();
            }
        } finally {
            if (this instanceof AuditableCommand && ((AuditableCommand) this).shouldAudit()) {
                BeamRuntime.getCurrentRuntime().finishAudit(success);
            }
        }
    }

    /**
     * Callbacks from {@link AbstractCloudCommand}.
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

    public static void getCommandConfig(String command, Class commandClass, Object obj, String path) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = null;

        if (ObjectUtils.isBlank(path)) {
            path = "/etc/beam/config.yml";

            File configFile = new File(path);
            if(!configFile.exists()) {
               path = System.getProperty("user.home") + File.separator +
                       ".beam" + File.separator + "config.yml";
            }
        }

        try {
            Map<String, Object> json = (Map<String, Object>) yaml.load(new FileInputStream(path));
            map = (Map<String, Object>) json.get(command);

        } catch (Exception error) {
        }

        if (map == null) {
            map = new HashMap<>();
        }

        for (Field field : commandClass.getDeclaredFields()) {
            Object current = null;
            try {
                current = field.get(obj);

            } catch (Exception error) {
                continue;
            }

            if (current == null) {
                try {
                    field.set(obj, map.get(field.getName()));
                } catch (Exception error) {
                }
            }
        }
    }
}
