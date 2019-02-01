package beam.core;

import beam.lang.BeamLanguageException;
import beam.lang.Resource;
import beam.lang.FileBackend;
import beam.lang.ast.Node;
import beam.lang.ast.scope.FileScope;
import beam.lang.listeners.ErrorListener;
import beam.lang.plugins.PluginLoader;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalFileBackend extends FileBackend {

    @Override
    public String name() {
        return "local";
    }

    @Override
    public void load(FileScope scope) throws Exception {
        BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(scope.getFile()));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        BeamParser parser = new BeamParser(stream);
        ErrorListener errorListener = new ErrorListener();

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        BeamParser.BeamFileContext fileContext = parser.beamFile();

        if (errorListener.getSyntaxErrors() > 0) {
            throw new BeamLanguageException(errorListener.getSyntaxErrors() + " errors while parsing.");
        }

        Node.create(fileContext).evaluate(scope);
    }

    @Override
    public void save(FileScope scope) throws IOException {
        String file = scope.getFile();

        if (!file.endsWith(".state")) {
            file += ".state";
        }

    @Override
    public void save(FileScope state) {
        try {
            String path = state.getFileScope().getFile().endsWith(".state") ? state.getFile() : state.getFile() + ".state";

            File temp = File.createTempFile("local-state",".bcl");

            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            for (PluginLoader pluginLoader : state.getFileScope().getPluginLoaders()) {
                out.write(pluginLoader.toString());
            }

            for (Resource resource : state.getFileScope().getResources().values()) {
                out.write(resource.serialize(0));
            }
            out.close();

            File stateFile = new File(path);
            temp.renameTo(stateFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String path) {

    }

}
