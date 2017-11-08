package beam.cli;

import beam.BeamRuntime;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beam.BeamCloud;
import beam.diff.Change;
import beam.diff.ChangeType;
import beam.diff.Diff;

@Command(name = "up", description = "Updates all assets to match the configuration.")
public class UpCommand extends AbstractCloudCommand implements AuditableCommand {

    @Option(name = { "-d", "--delete" })
    public boolean delete;

    @Option(name = { "-l", "--layer" }, description = "Launch only specific layers.")
    public String layersString;

    @Override
    protected boolean isConfigRequired() {
        return true;
    }

    @Override
    protected CloudHandler getCloudHandler() {
        return new CloudHandler() {

            private final Set<ChangeType> changeTypes = new HashSet<>();

            @Override
            public void last(Set<BeamCloud> clouds) throws Exception {
                out.println("Looking for changes...");
                out.flush();

                Set<String> includedLayerList = split(layersString);
                if (includedLayerList.size() > 0) {
                    includedLayerList.add("gateway");
                }

                for (BeamCloud cloud : clouds) {
                    cloud.setIncludedLayers(includedLayerList);

                    List<Diff<?, ?, ?>> diffs = cloud.findChanges(runtime);

                    changeTypes.clear();
                    writeDiffs(diffs, 0);

                    boolean hasChanges = false;
                    BufferedReader confirmReader = new BufferedReader(new InputStreamReader(System.in));

                    if (changeTypes.contains(ChangeType.CREATE) || changeTypes.contains(ChangeType.UPDATE)) {
                        hasChanges = true;

                        out.format("\nAre you sure you want to create and/or update resources in @|blue %s|@ cloud in account @|blue %s|@? (y/N) ",
                                cloud.getName(),
                                runtime.getAccount());
                        out.flush();

                        if ("y".equalsIgnoreCase(confirmReader.readLine())) {
                            setEverConfirmed(0);

                            out.println("");
                            out.flush();
                            setChangeable(diffs);
                            createOrUpdate(diffs);
                        }
                    }

                    if (changeTypes.contains(ChangeType.DELETE)) {
                        hasChanges = true;

                        if (delete) {
                            out.format("\nAre you sure you want to delete resources from @|blue %s|@ cloud in account @|blue %s|@? (y/N) ",
                                    cloud.getName(),
                                    runtime.getAccount());
                            out.flush();

                            if ("y".equalsIgnoreCase(confirmReader.readLine())) {
                                setEverConfirmed(0);

                                out.println("");
                                out.flush();
                                setChangeable(diffs);
                                delete(diffs);
                            }

                        } else {
                            out.format("\nSkipped deletes in @|blue %s|@ cloud. Run again with the --delete option to execute them.\n", cloud.getName());
                            out.flush();
                        }
                    }

                    if (!hasChanges) {
                        out.format("\nNo changes in @|blue %s|@ cloud in account @|blue %s|@\n", cloud.getName(), runtime.getAccount());
                        out.flush();
                    }
                }
            }

            private void writeDiffs(List<Diff<?, ?, ?>> diffs, int indent) {
                for (Diff<?, ?, ?> diff : diffs) {
                    if (!diff.hasChanges()) {
                        continue;
                    }

                    if (indent == 0) {
                        out.print('\n');
                        out.flush();
                    }

                    for (Change<?> change : diff.getChanges()) {
                        ChangeType type = change.getType();
                        List<Diff<?, ?, ?>> changeDiffs = change.getDiffs();

                        if (type == ChangeType.KEEP) {
                            boolean hasChanges = false;

                            for (Diff<?, ?, ?> changeDiff : changeDiffs) {
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
                        writeIndent(indent);
                        writeChange(change);
                        out.print('\n');
                        out.flush();

                        writeDiffs(changeDiffs, indent + 4);
                    }
                }
            }

            private void writeIndent(int indent) {
                for (int i = 0; i < indent; ++ i) {
                    out.print(' ');
                }
            }

            private void writeChange(Change<?> change) {
                switch (change.getType()) {
                    case CREATE :
                        out.format("@|green + %s|@", change);
                        break;

                    case UPDATE :
                        if (change.toString().contains("@|")) {
                            out.format(" * %s", change);
                        } else {
                            out.format("@|yellow * %s|@", change);
                        }
                        break;

                    case REPLACE :
                        out.format("@|blue * %s|@", change);
                        break;

                    case DELETE :
                        out.format("@|red - %s|@", change);
                        break;

                    default :
                        out.print(change);
                }
            }

            private void setChangeable(List<Diff<?, ?, ?>> diffs) {
                for (Diff<?, ?, ?> diff : diffs) {
                    for (Change<?> change : diff.getChanges()) {
                        change.setChangeable(true);
                        setChangeable(change.getDiffs());
                    }
                }
            }

            private void createOrUpdate(List<Diff<?, ?, ?>> diffs) {
                for (Diff<?, ?, ?> diff : diffs) {
                    for (Change<?> change : diff.getChanges()) {
                        ChangeType type = change.getType();

                        if (type == ChangeType.CREATE || type == ChangeType.UPDATE) {
                            execute(change);
                        }

                        createOrUpdate(change.getDiffs());
                    }
                }
            }

            private void delete(List<Diff<?, ?, ?>> diffs) {
                for (Diff<?, ?, ?> diff : diffs) {
                    for (Change<?> change : diff.getChanges()) {
                        delete(change.getDiffs());

                        if (change.getType() == ChangeType.DELETE) {
                            execute(change);
                        }
                    }
                }
            }

            private void execute(Change<?> change) {
                ChangeType type = change.getType();

                if (type == ChangeType.KEEP ||
                        type == ChangeType.REPLACE ||
                        change.isChanged()) {
                    return;
                }

                Set<Change<?>> dependencies = change.dependencies();

                if (dependencies != null && !dependencies.isEmpty()) {
                    for (Change<?> d : dependencies) {
                        execute(d);
                    }
                }

                out.write("Executing: ");
                writeChange(change);
                out.flush();

                change.getChangedAsset();
                out.write(" OK\n");
                out.flush();
            }
        };
    }
}
