package beam.core;

import beam.core.diff.ChangeType;
import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiff;
import beam.lang.BeamErrorListener;
import beam.lang.BeamLanguageException;
import beam.lang.BeamLanguageExtension;
import beam.lang.BeamVisitor;
import beam.lang.types.ContainerBlock;
import beam.lang.types.Node;
import beam.lang.types.ResourceBlock;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import com.psddev.dari.util.ThreadLocalStack;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class BeamCore {

    private ContainerBlock root;

    private final Map<String, Class<? extends BeamLanguageExtension>> extensions = new HashMap<>();

    private static final ThreadLocalStack<BeamUI> UI = new ThreadLocalStack<>();

    public static BeamUI ui() {
        return UI.get();
    }

    public static void pushUi(BeamUI ui) {
        UI.push(ui);
    }

    public static BeamUI popUi() {
        return UI.pop();
    }

    public void addExtension(String key, Class<? extends BeamLanguageExtension> extension) {
        extensions.put(key, extension);
    }

    public boolean hasExtension(String key) {
        return extensions.containsKey(key);
    }

    public Class<? extends Node> getExtension(String key) {
        return extensions.get(key);
    }

    public void init() {
        extensions.clear();
    }

    public ContainerBlock parse(String path) throws IOException {
        init();
        addExtension("state", BeamLocalState.class);
        addExtension("provider", BeamProvider.class);

        BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(path));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        BeamParser parser = new BeamParser(tokens);
        BeamErrorListener errorListener = new BeamErrorListener();
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        BeamParser.Beam_rootContext context = parser.beam_root();

        if (errorListener.getSyntaxErrors() > 0) {
            throw new BeamLanguageException(errorListener.getSyntaxErrors() + " errors while parsing.");
        }

        BeamVisitor visitor = new BeamVisitor(this);
        visitor.visitBeam_root(context);        // First pass ensures extensions are loaded and executed
        root = visitor.visitBeam_root(context);

        if (!root.resolve()) {
            System.out.println("Unable to resolve config.");
        }

        return root;
    }

    public BeamState getState(ContainerBlock block) {
        BeamState backend = new BeamLocalState();

        for (ResourceBlock resourceBlock : block.resources()) {
            if (resourceBlock instanceof BeamState) {
                backend = (BeamState) resourceBlock;
            }
        }

        return backend;
    }

    public void copyNonResourceState(ContainerBlock source, ContainerBlock state) {
        state.copyNonResourceState(source);

        for (ResourceBlock block : source.resources()) {
            if (block instanceof BeamResource) {
                continue;
            }

            state.putResource(block);
        }
    }

    public Set<BeamResource> findBeamResources(ContainerBlock block) {
        return findBeamResources(block, false);
    }

    public Set<BeamResource> findBeamResources(ContainerBlock block, boolean refresh) {
        Set<BeamResource> resources = new TreeSet<>();

        for (ResourceBlock resource : block.resources()) {
            if (resource instanceof BeamResource) {
                if (refresh) {
                    BeamCore.ui().write("@|bold,blue Refreshing|@: @|yellow %s|@ -> %s...",
                        resource.getResourceType(), resource.getResourceIdentifier());
                }

                if (refresh && ((BeamResource) resource).refresh()) {
                    resources.add((BeamResource) resource);

                    BeamCore.ui().write("\n");
                } else if (refresh) {
                    BeamCore.ui().write("\n");
                } else if (!refresh) {
                    resources.add((BeamResource) resource);
                }
            }

            resources.addAll(findBeamResources(resource, refresh));
        }

        return resources;
    }

    public List<ResourceDiff> diff(Set<BeamResource> current, Set<BeamResource> pending) throws Exception {
        ResourceDiff diff = new ResourceDiff(current, pending);
        diff.diff();

        List<ResourceDiff> diffs = new ArrayList<>();
        diffs.add(diff);

        return diffs;
    }

    public Set<ChangeType> writeDiffs(List<ResourceDiff> diffs) {
        Set<ChangeType> changeTypes = new HashSet<>();
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
                BeamCore.ui().write("\n");
                BeamCore.ui().indented(() -> changeTypes.addAll(writeDiffs(changeDiffs)));
            }
        }

        return changeTypes;
    }

    private void writeChange(ResourceChange change) {
        switch (change.getType()) {
            case CREATE :
                BeamCore.ui().write("@|green + %s|@", change);
                break;

            case UPDATE :
                if (change.toString().contains("@|")) {
                    BeamCore.ui().write(" * %s", change);
                } else {
                    BeamCore.ui().write("@|yellow * %s|@", change);
                }
                break;

            case REPLACE :
                BeamCore.ui().write("@|blue * %s|@", change);
                break;

            case DELETE :
                BeamCore.ui().write("@|red - %s|@", change);
                break;

            default :
                BeamCore.ui().write(change.toString());
        }
    }

    public void setChangeable(List<ResourceDiff> diffs) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                change.setChangeable(true);
                setChangeable(change.getDiffs());
            }
        }
    }

    public void execute(ResourceChange change, ContainerBlock state, BeamState backend, String path) {
        ChangeType type = change.getType();

        if (type == ChangeType.KEEP || type == ChangeType.REPLACE || change.isChanged()) {
            return;
        }

        Set<ResourceChange> dependencies = change.dependencies();

        if (dependencies != null && !dependencies.isEmpty()) {
            for (ResourceChange d : dependencies) {
                execute(d, state, backend, path);
            }
        }

        BeamCore.ui().write("Executing: ");
        writeChange(change);
        BeamResource resource = change.executeChange();

        if (type == ChangeType.DELETE) {
            state.removeResource(resource);
        } else {
            state.putResource(resource);

        }

        BeamCore.ui().write(" OK\n");
        backend.save(path, state);
    }
}
