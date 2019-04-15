package gyro.core;

import gyro.lang.GyroLanguageException;
import gyro.core.resource.Resource;
import gyro.lang.ast.Node;
import gyro.core.scope.FileScope;
import gyro.lang.GyroErrorListener;
import gyro.core.plugin.PluginLoader;
import gyro.parser.antlr4.GyroLexer;
import gyro.parser.antlr4.GyroParser;
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
        GyroCore.verifyConfig(file);

        if (file.toString().endsWith(".state")) {
            file = getStatePath(file);
        }

        if (!Files.exists(file) || Files.isDirectory(file)) {
            return false;
        }

        GyroLexer lexer = new GyroLexer(CharStreams.fromFileName(file.toString()));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        GyroParser parser = new GyroParser(stream);
        GyroErrorListener errorListener = new GyroErrorListener();

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        GyroParser.FileContext fileContext = parser.file();

        if (errorListener.getSyntaxErrors() > 0) {
            throw new GyroLanguageException(errorListener.getSyntaxErrors() + " errors while parsing.");
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

        Path newFile = Files.createTempFile("local-file-backend-", ".gyro.state");

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

                    out.write("@import ");
                    out.write(dir != null ? dir.relativize(Paths.get(importFile)).toString() : importFile);
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
                    getStatePath(Paths.get(file)),
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

    private Path getStatePath(Path file) throws IOException {
        Path rootDir = GyroCore.findPluginPath().getParent().getParent();
        Path relative = rootDir.relativize(file.toAbsolutePath());
        Path statePath = Paths.get(rootDir.toString(), ".gyro", "state", relative.toString());

        if (statePath.toFile().getParentFile() != null && !statePath.toFile().getParentFile().exists()) {
            statePath.toFile().getParentFile().mkdirs();
        }

        return statePath;
    }
}
