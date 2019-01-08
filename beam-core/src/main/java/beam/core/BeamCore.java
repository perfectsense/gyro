package beam.core;

import beam.core.diff.ChangeType;
import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiff;
import beam.core.diff.ResourceName;
import beam.lang.BeamErrorListener;
import beam.lang.BeamLanguageException;
import beam.lang.BeamVisitor;
import beam.lang.Node;
import beam.lang.ResourceNode;
import beam.lang.RootNode;
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

public class BeamCore {

    private final Map<String, Class<? extends ResourceNode>> resourceTypes = new HashMap<>();

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

    public void addResourceType(String key, Class<? extends ResourceNode> extension) {
        resourceTypes.put(key, extension);
    }

    public boolean hasResourceType(String key) {
        return resourceTypes.containsKey(key);
    }

    public Class<? extends Node> getResourceType(String key) {
        return resourceTypes.get(key);
    }

    public void init() {
        resourceTypes.clear();
    }

    public RootNode parse(String path) throws IOException {
        init();
        addResourceType("state", BeamLocalState.class);
        addResourceType("provider", BeamProvider.class);

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

        // Load configuration
        BeamVisitor visitor = new BeamVisitor(this, path);
        visitor.visitBeam_root(context);        // First pass ensures resourceTypes are loaded and executed
        RootNode rootNode = visitor.visitBeam_root(context);

        if (!rootNode.resolve()) {
            System.out.println("Unable to resolve config.");
        }

        // Load state, assuming this isn't a state file itself.
        if (!path.endsWith(".state")) {
            BeamState backend = getState(rootNode);
            try {
                RootNode stateNode = backend.load(rootNode, this);
                stateNode.copyNonResourceState(rootNode);

                rootNode.setState(stateNode);
            } catch (Exception ex) {
                throw new BeamLanguageException("Unable to load state.", ex);
            }
        }

        return rootNode;
    }

    public RootNode parseImport(String path) throws IOException {
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

        // Load configuration
        BeamVisitor visitor = new BeamVisitor(this, path);

        RootNode rootNode = visitor.visitBeam_root(context);

        if (!rootNode.resolve()) {
            System.out.println("Unable to resolve config.");
        }

        // Load state, assuming this isn't a state file itself.
        if (!path.endsWith(".state")) {
            BeamState backend = getState(rootNode);
            try {
                RootNode stateNode = backend.load(rootNode, this);
                stateNode.copyNonResourceState(rootNode);

                rootNode.setState(stateNode);
            } catch (Exception ex) {
                throw new BeamLanguageException("Unable to load state.", ex);
            }
        }

        return rootNode;
    }

    public BeamState getState(RootNode block) {
        BeamState backend = new BeamLocalState();

        for (ResourceNode resourceBlock : block.resources()) {
            if (resourceBlock instanceof BeamState) {
                backend = (BeamState) resourceBlock;
            }
        }

        return backend;
    }

    public List<ResourceDiff> diff(RootNode pending, boolean refresh) throws Exception {
        ResourceDiff diff = new ResourceDiff(pending.state(), pending);
        diff.setRefresh(refresh);
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

    public void setChangeable(List<ResourceDiff> diffs) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                change.setChangeable(true);
                setChangeable(change.getDiffs());
            }
        }
    }

    public void execute(ResourceChange change) {
        ChangeType type = change.getType();

        if (type == ChangeType.KEEP || type == ChangeType.REPLACE || change.isChanged()) {
            return;
        }

        Set<ResourceChange> dependencies = change.dependencies();

        if (dependencies != null && !dependencies.isEmpty()) {
            for (ResourceChange d : dependencies) {
                execute(d);
            }
        }

        BeamCore.ui().write("Executing: ");
        writeChange(change);
        BeamResource resource = change.executeChange();

        RootNode stateNode = resource.rootNode().state();
        BeamState backend = getState(resource.rootNode());

        if (type == ChangeType.DELETE) {
            stateNode.removeResource(resource);
        } else {
            ResourceName nameAnnotation = resource.getClass().getAnnotation(ResourceName.class);
            if (nameAnnotation != null && !nameAnnotation.parent().equals("")) {
                // Save parent resource when current resource is a subresource.
                Node parent = resource.parentNode();
                if (parent instanceof BeamResource) {
                    stateNode.putResource(((BeamResource) parent).copy());
                }
            } else {
                stateNode.putResource(resource.copy());
            }
        }

        BeamCore.ui().write(" OK\n");
        backend.save(stateNode);

        for (RootNode importNode : stateNode.imports().values()) {
            backend.save(importNode);
        }
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

    /*
    private void initState(RootNode source, RootNode state) {
        for (ResourceNode block : source.resources()) {
            if (block instanceof BeamResource) {
                continue;
            }

            state.putResource(block);
        }
    }
    */

}
