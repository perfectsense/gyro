package beam.cli;

import beam.core.BeamCredentials;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.diff.ChangeType;
import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiff;
import beam.core.extensions.ProviderExtension;
import beam.lang.BCL;
import beam.lang.BeamConfig;
import beam.lang.BeamConfigKey;
import beam.lang.BeamResolvable;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Command(name = "up", description = "Updates all resources to match the configuration.")
public class UpCommand extends AbstractCommand {

    @Arguments
    private List<String> arguments;

    private final Set<ChangeType> changeTypes = new HashSet<>();

    @Override
    public void doExecute() throws Exception {

        if (getArguments().size() < 1) {
            throw new BeamException("Beam configuration file required.");
        }

        try {
            BCL.init();
            BCL.addExtension(new ProviderExtension());

            BeamConfig root = BCL.parse(getArguments().get(0));
            BCL.resolve(root);

            Set<BeamResource> resources = new TreeSet<>();
            for (BeamConfigKey key : root.getContext().keySet()) {
                BeamResolvable resolvable = root.getContext().get(key);
                Object value = resolvable.getValue();

                if (value instanceof BeamCredentials) {
                    BeamCredentials credentials = (BeamCredentials) value;
                    for (BeamResource resource : credentials.dependents()) {
                        BeamResource resourceRoot = resource.findTop();
                        if (resourceRoot != null) {
                            resources.add(resourceRoot);
                        }
                    }
                }
            }

            ResourceDiff diff = new ResourceDiff(new ArrayList<>(), resources);
            diff.diff();

            changeTypes.clear();

            List<ResourceDiff> diffs = new ArrayList<>();
            diffs.add(diff);

            writeDiffs(diffs);
        } finally {
            BCL.shutdown();
        }

    }

    public List<String> getArguments() {
        return arguments;
    }

    private void writeDiffs(List<ResourceDiff> diffs) {
        for (ResourceDiff diff : diffs) {
            if (!diff.hasChanges()) {
                continue;
            }

            for (ResourceChange change : diff.getChanges()) {
                ChangeType type = change.getType();
                List<ResourceDiff> changeDiffs = change.getDiffs();

                if (type == ChangeType.KEEP) {
                    boolean hasChanges = false;

                    for (ResourceDiff changeDiff : changeDiffs) {
                        if (changeDiff.hasChanges()) {
                            hasChanges = true;
                            break;
                        }
                    }

                    if (!hasChanges) {
                        continue;
                    }
                }

                changeTypes.add(type);
                writeChange(change);
                Beam.ui().write("\n");
                Beam.ui().indented(() -> writeDiffs(changeDiffs));
            }
        }
    }

    private void writeChange(ResourceChange change) {
        switch (change.getType()) {
            case CREATE :
                Beam.ui().write("@|green + %s|@", change);
                break;

            case UPDATE :
                if (change.toString().contains("@|")) {
                    Beam.ui().write(" * %s", change);
                } else {
                    Beam.ui().write("@|yellow * %s|@", change);
                }
                break;

            case REPLACE :
                Beam.ui().write("@|blue * %s|@", change);
                break;

            case DELETE :
                Beam.ui().write("@|red - %s|@", change);
                break;

            default :
                Beam.ui().write(change.toString());
        }
    }

}
