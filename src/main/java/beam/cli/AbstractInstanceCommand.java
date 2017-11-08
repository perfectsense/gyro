package beam.cli;

import io.airlift.command.Option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import beam.BeamCloud;
import beam.BeamInstance;

/**
 * Basic {@link AbstractCloudCommand} implementation that can iterate over every
 * instance in every cloud in an environment.
 *
 * @see AbstractInstanceCommand.InstanceHandler
 */
public abstract class AbstractInstanceCommand extends AbstractCloudCommand {

    @Option(name = { "-l", "--layer" }, description = "Filter the instances by the layers.")
    public String layersString;

    @Option(name = { "-i", "--instance-id" }, description = "Instance ID(s) to log into or execute command on.")
    public String instanceIdsString;

    /**
     * Returns {@code true} if the list of instances for each cloud in this
     * environment can be cached.
     */
    protected boolean isCacheOk() {
        return true;
    }

    /**
     * @return Never {@code null}.
     */
    protected abstract InstanceHandler getInstanceHandler();

    @Override
    protected final CloudHandler getCloudHandler() {
        return new CloudHandler() {

            private final InstanceHandler handler = getInstanceHandler();
            private final Set<String> layers = split(layersString);
            private final Set<String> instanceIds = split(instanceIdsString);

            @Override
            public void first() throws Exception {
                handler.cloudFirst();
            }

            @Override
            public void each(BeamCloud cloud) throws Exception {

                boolean found = false;
                List<BeamInstance> instances = new ArrayList<BeamInstance>();

                for (BeamInstance instance : cloud.getInstances(isCacheOk())) {
                    if ((environmentName == null || environmentName.equals(instance.getEnvironment())) &&
                            (layers.isEmpty() || layers.contains(instance.getLayer())) &&
                            (instanceIds.isEmpty() || instanceIds.contains(instance.getId()))) {
                        if (!found) {
                            found = true;
                            handler.first(cloud);
                        }

                        instances.add(instance);
                        handler.each(cloud, instance);
                    }
                }

                if (found) {
                    handler.last(cloud, instances);

                } else {
                    handler.empty(cloud);
                }
            }

            @Override
            public void last(Set<BeamCloud> clouds) throws Exception {
                handler.cloudLast(clouds);
            }

            @Override
            public void empty() throws Exception {
                handler.cloudEmpty();
            }
        };
    }

    /**
     * Callbacks from {@link AbstractInstanceCommand}.
     */
    protected abstract class InstanceHandler {

        /**
         * @see AbstractCloudCommand.CloudHandler#first
         */
        public void cloudFirst() throws Exception {
        }

        /**
         * @see AbstractCloudCommand.CloudHandler#last
         */
        public void cloudLast(Set<BeamCloud> clouds) throws Exception {
        }

        /**
         * @see AbstractCloudCommand.CloudHandler#empty
         */
        public void cloudEmpty() throws Exception {
        }

        /**
         * Called before the first {@link #each} call for each cloud
         * if there are any instances within in this environment.
         *
         * <p>Default implementation does nothing.</p>
         */
        public void first(BeamCloud cloud) throws Exception {
        }

        /**
         * Called for each instance for each cloud in this environment.
         *
         * <p>Default implementation does nothing.</p>
         */
        public void each(BeamCloud cloud, BeamInstance instance) throws Exception {
        }

        /**
         * Called after every {@link #each} call for each cloud
         * if there are any instances within in this environment.
         *
         * <p>Default implementation does nothing.</p>
         */
        public void last(BeamCloud cloud, List<BeamInstance> instances) throws Exception {
        }

        /**
         * Called if there aren't any instances for each cloud within
         * in this environment.
         *
         * <p>Default implementation outputs a message.</p>
         */
        public void empty(BeamCloud cloud) throws Exception {
            out.write("@|red No instances found.|@\n");
            out.flush();
        }
    }
}
