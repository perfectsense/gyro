package gyro.core;

import gyro.lang.BeamLanguageException;
import gyro.lang.FileBackend;
import gyro.lang.Resource;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.FileScope;
import gyro.lang.listeners.ErrorListener;
import gyro.lang.plugins.PluginLoader;
import gyro.parser.antlr4.BeamLexer;
import gyro.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalFileBackend extends FileBackend {

    @Override
    public String name() {
        return "local";
    }

    @Override
    public boolean load(FileScope scope) throws Exception {
        Path file = Paths.get(scope.getFile());

        if (!Files.exists(file) || Files.isDirectory(file)) {
            return false;
        }

        BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(file.toString()));
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

        return true;
    }

    @Override
    public void save(FileScope scope) throws IOException {
        String file = scope.getFile();

        if (!file.endsWith(".state")) {
            file += ".state";
        }

        Path newFile = Files.createTempFile("local-file-backend-", ".bcl.state");

        try {
            try (BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(
                            Files.newOutputStream(newFile),
                            StandardCharsets.UTF_8))) {

                Path dir = Paths.get(file).getParent();

                for (FileScope i : scope.getImports()) {
                    String importFile = i.getFile();

                    if (!importFile.endsWith(".state")) {
                        importFile += ".state";
                    }

                    out.write("import ");
                    out.write(dir.relativize(Paths.get(importFile)).toString());
                    out.write('\n');
                }

                for (PluginLoader pluginLoader : scope.getPluginLoaders()) {
                    out.write(pluginLoader.toString());
                }

                for (Object value : scope.values()) {
                    if (value instanceof Resource) {
                        out.write(((Resource) value).toNode().toString());
                    }
                }
            }

            Files.move(
                    newFile,
                    Paths.get(file),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException error) {
            Files.deleteIfExists(newFile);
            throw error;
        }
    }

    @Override
    public void delete(String path) {

    }

}
