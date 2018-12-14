package beam.lang;

import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BeamInterp {

    private BeamBlock root;

    private final Map<String, Class<? extends BeamBlock>> extensions = new HashMap<>();

    private static final Formatter ui = new Formatter();

    public static Formatter ui() {
        return ui;
    }

    public void addExtension(String key, Class<? extends BeamBlock> extension) {
        extensions.put(key, extension);
    }

    public boolean hasExtension(String key) {
        return extensions.containsKey(key);
    }

    public Class<? extends BeamBlock> getExtension(String key) {
        return extensions.get(key);
    }

    public BeamBlock createConfig(String extensionType) {
        return createConfig(extensionType, null);
    }

    public BeamBlock createConfig(String extensionType, BeamBlock original) {
        Class klass = extensions.get(extensionType);

        if (klass != null) {
            try {
                BeamBlock config = (BeamBlock) klass.newInstance();
                if (original != null) {
                    config.setType(original.getType());
                    config.setParameters(original.getParameters());
                    config.setChildren(original.getChildren());

                    for (BeamContextKey key : original.keys()) {
                        config.add(key, original.get(key));
                    }
                }

                config.setType(extensionType);

                if (config instanceof BeamBlockMethod) {
                    ((BeamBlockMethod) config).setInterp(this);
                }

                return config;
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new BeamLangException("Unable to instantiate " + klass.getClass().getSimpleName());
            }
        }

        BeamBlock config = new BeamBlock();
        config.setType(extensionType);

        return config;
    }

    public void init() {
        extensions.clear();
    }

    public BeamBlock parse(String filename) throws IOException {
        BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(filename));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        BeamParser parser = new BeamParser(tokens);
        BeamParser.BeamRootContext context = parser.beamRoot();

        File configFile = new File(filename);
        BeamListener listener = new BeamListener(this, configFile.getCanonicalPath());
        ParseTreeWalker.DEFAULT.walk(listener, context);

        root = listener.getConfig();
        resolve(root);
        applyExtensions(root);
        resolve(root);
        calculateDependencies(root);

        return root;
    }

    private void resolve(BeamBlock config) {
        boolean progress = true;
        while (progress) {
            progress = config.resolve(config);
        }
    }

    private void applyExtensions(BeamBlock parent) {
        List<BeamBlock> appliedConfigs = new ArrayList<>();

        Iterator<BeamBlock> iterator = parent.getChildren().iterator();
        while (iterator.hasNext()) {
            BeamBlock child = iterator.next();

            if (child.getClass() != getExtension(child.getType())) {
                // Replace `child` config block with the equivalent extension
                // config block.

                BeamBlock config = createConfig(child.getType(), child);
                config.applyExtension(this);

                appliedConfigs.add(config);

                iterator.remove();
            } else {
                child.applyExtension(this);
            }
        }

        parent.getChildren().addAll(appliedConfigs);
    }

    private void calculateDependencies(BeamBlock config) {
        config.getDependencies(config);
    }
}
