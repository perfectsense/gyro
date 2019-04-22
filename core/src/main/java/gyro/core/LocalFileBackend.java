package gyro.core;

import gyro.core.scope.RootScope;
import gyro.lang.GyroErrorStrategy;
import gyro.lang.GyroLanguageException;
import gyro.core.resource.Resource;
import gyro.lang.ast.Node;
import gyro.core.scope.FileScope;
import gyro.lang.GyroErrorListener;
import gyro.core.plugin.PluginLoader;
import gyro.lang.ast.block.FileNode;
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
    public boolean load(RootScope scope) throws Exception {

        FileNode initNode = parseFile(Paths.get(scope.getFile()));
        initNode.evaluate(scope);
        for (FileScope fileScope : scope.getFileScopes()) {
            FileNode fileNode = parseFile(Paths.get(fileScope.getFile()));
            fileNode.evaluate(fileScope);
        }

        scope.validate();
        return true;
    }

    @Override
    public void save(RootScope scope) throws IOException {
        for (FileScope fileScope : scope.getFileScopes()) {
            String file = fileScope.getFile();

            Path newFile = Files.createTempFile("local-file-backend-", ".gyro.state");

            try {
                try (BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(
                        Files.newOutputStream(newFile),
                        StandardCharsets.UTF_8))) {

                    for (PluginLoader pluginLoader : fileScope.getPluginLoaders()) {
                        out.write(pluginLoader.toString());
                    }

                    for (Object value : fileScope.values()) {
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
    }

    @Override
    public void delete(String path) {

    }

    private FileNode parseFile(Path file) throws IOException {
        GyroCore.verifyConfig(file);

        if (!Files.exists(file) || Files.isDirectory(file)) {
            throw new GyroException(file + " is not a valid gyro config.");
        }

        GyroLexer lexer = new GyroLexer(CharStreams.fromFileName(file.toString()));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        GyroParser parser = new GyroParser(stream);
        GyroErrorListener errorListener = new GyroErrorListener();

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        parser.setErrorHandler(new GyroErrorStrategy());

        GyroParser.FileContext fileContext = parser.file();

        int errorCount = errorListener.getSyntaxErrors();
        if (errorCount > 0) {
            throw new GyroLanguageException(String.format("%d %s found while parsing.", errorCount, errorCount == 1 ? "error" : "errors"));
        }

        return (FileNode) Node.create(fileContext);
    }

}
