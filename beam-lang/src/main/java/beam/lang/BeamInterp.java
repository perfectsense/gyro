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

    private BeamConfig root;

    private final Map<String, Class<? extends BeamConfig>> extensions = new HashMap<>();

    private static final Formatter ui = new Formatter();

    public static Formatter ui() {
        return ui;
    }

    public void addExtension(String key, Class<? extends BeamConfig> extension) {
        extensions.put(key, extension);
    }

    public boolean hasExtension(String key) {
        return extensions.containsKey(key);
    }

    public Class<? extends BeamConfig> getExtension(String key) {
        return extensions.get(key);
    }

    public BeamConfig createConfig(String extensionType) {
        Class klass = extensions.get(extensionType);

        if (klass != null) {
            try {
                BeamConfig config = (BeamConfig) klass.newInstance();
                config.setType(extensionType);

                if (config instanceof BeamExtension) {
                    ((BeamExtension) config).setInterp(this);
                }

                return config;
            } catch (InstantiationException | IllegalAccessException ie) {
                throw new BeamLangException("Unable to instantiate " + klass.getClass().getSimpleName());
            }
        }

        BeamConfig config = new BeamConfig();
        config.setType(extensionType);

        return config;
    }

    public void init() {
        extensions.clear();
    }

    public BeamConfig parse(String filename) throws IOException {
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

    private void resolve(BeamConfig config) {
        boolean progress = true;
        while (progress) {
            progress = config.resolve(config);
        }
    }

    private void applyExtensions(BeamConfig parent) {
        List<BeamConfig> appliedConfigs = new ArrayList<>();

        Iterator<BeamConfig> iterator = parent.getSubConfigs().iterator();
        while (iterator.hasNext()) {
            BeamConfig child = iterator.next();

            if (child.getClass() != getExtension(child.getType())) {
                // Replace `child` config block with the equivalent extension
                // config block.

                BeamConfig config = createConfig(child.getType());
                config.setCtx(child.getCtx());
                config.setType(child.getType());
                config.setParams(child.getParams());
                config.setSubConfigs(child.getSubConfigs());
                config.importContext(child);
                config.applyExtension(this);

                appliedConfigs.add(config);

                iterator.remove();
            } else {
                child.applyExtension(this);
            }
        }

        parent.getSubConfigs().addAll(appliedConfigs);
    }

    private void calculateDependencies(BeamConfig config) {
        config.getDependencies(config);
    }
}
