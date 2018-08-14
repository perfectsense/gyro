package beam.commands;

import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.BeamRuntime;
import beam.core.ConfigKey;
import beam.core.diff.*;
import beam.parser.ASTListener;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import beam.parser.ast.ASTBeamRoot;
import beam.parser.ast.Node;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import io.airlift.airline.OptionType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.fusesource.jansi.AnsiRenderWriter;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

@Command(name = "sync", description = "Sync all existing assets and generate legacy configuration.")
public class SyncCommand implements Runnable {

    @Option(type = OptionType.GLOBAL, name = "--debug", description = "Debug mode")
    public boolean debug;

    public void run() {
        try {
            File configFile = new File("test2.beam");
            ASTBeamRoot root = parse(configFile.getCanonicalPath());
            if (root == null) {
                return;
            }

            ResourceDiff resourceDiff = root.getDiff();

            List<Change<?>> changes = new ArrayList<>();
            Map<String, List<BeamResource>> resourceByType = new HashMap<>();
            DiffUtil.findChangesByResourceClass(Arrays.asList(resourceDiff), null, changes);
            DiffUtil.findChangesByType(changes, ChangeType.DELETE);

            for (Change<?> change : changes) {
                BeamResource resource = (BeamResource) change.getCurrentAsset();
                Class<?> resourceClass = resource.getClass();
                ConfigKey configKey = resourceClass.getAnnotation(ConfigKey.class);
                if (configKey == null) {
                    continue;
                }
                String packageName = resourceClass.getName().split("\\.")[1];
                String key = String.format("%s-%s", packageName, configKey.value());

                if (!resourceByType.containsKey(key)) {
                    resourceByType.put(key, new ArrayList<>());
                }

                resourceByType.get(key).add(resource);
            }

            for (String key : resourceByType.keySet()) {
                StringBuilder sb = new StringBuilder();
                for (BeamResource resource : resourceByType.get(key)) {
                    sb.append(BeamRuntime.getBeamConfigTranslator().generate(resource));
                    sb.append("\n");
                }

                File legacyConfig = Paths.get(String.format("legacy/%s.beam", key)).toFile();
                if (!legacyConfig.exists()) {
                    legacyConfig.getParentFile().mkdirs();
                    legacyConfig.createNewFile();
                }

                PrintWriter configOut = new PrintWriter(legacyConfig);
                configOut.println(sb.toString() + "\n");
                configOut.close();
                System.out.println(String.format("%s %s synced.", resourceByType.get(key).size(), key));
            }

            try {
                // 5 resource will be syncd, are you sure you want to continue?
            } catch (Exception e) {
                throw new BeamException(e.getMessage(), e.getCause());
            }

            for (Node n : root.getNodes()) {
                System.out.println(n);
            }
        } catch (IOException ioe) {

        }
    }

    public ASTBeamRoot parse(String filename) {
        try {
            BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(filename));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            BeamParser parser = new BeamParser(tokens);
            BeamParser.BeamRootContext context = parser.beamRoot();

            ASTListener listener = new ASTListener();
            ParseTreeWalker.DEFAULT.walk(listener, context);

            return listener.getRoot();

        } catch (Exception error) {
            Throwable cause = null;

            PrintWriter out = new AnsiRenderWriter(System.out, true);
            if (error instanceof BeamException) {
                out.write("\n@|red Error: " + error.getMessage() + "|@\n");
                out.flush();

                if (debug) {
                    cause = error.getCause();
                }

            } else {
                out.write("\n@|red Unexpected error! Stack trace follows:|@\n");
                out.flush();

                cause = error;
            }

            if (cause != null) {
                out.write(cause.getClass().getName());
                out.write(": ");
                cause.printStackTrace(out);
                out.flush();
            }
        }

        return null;
    }
}
