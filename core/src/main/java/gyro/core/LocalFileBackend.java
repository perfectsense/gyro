package gyro.core;

import gyro.core.scope.RootScope;
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
import java.util.Set;
import java.util.stream.Collectors;

public class LocalFileBackend extends FileBackend {

    @Override
    public String name() {
        return "local";
    }

    @Override
    public boolean load(RootScope scope) throws Exception {

        if (scope.getInitScope() != null) {
            Path file = Paths.get(scope.getInitScope().getFile());
            GyroCore.verifyConfig(file);

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

            // defer error
            Node.create(fileContext).evaluate(scope.getInitScope());
        }

        Path rootPath = GyroCore.findRootDirectory(Paths.get("").toAbsolutePath());
        Set<Path> paths;
        if (scope.getCurrent() != null) {
            paths = Files.find(rootPath.getParent(), 100,
                (p, b) -> b.isRegularFile()
                    && p.toString().endsWith(".gyro")
                    && !p.toString().startsWith(rootPath.toString()))
                .collect(Collectors.toSet());
        } else {
            paths = Files.find(rootPath, 100,
                (p, b) -> b.isRegularFile()
                    && p.toString().endsWith(".gyro.state"))
                .collect(Collectors.toSet());
        }

        for (Path path : paths) {
            FileScope fileScope = new FileScope(scope, path.toString());
            scope.getFileScopes().add(fileScope);
        }

        for (FileScope fileScope : scope.getFileScopes()) {
            Path file = Paths.get(fileScope.getFile());
            GyroCore.verifyConfig(file);

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

            // defer error
            Node.create(fileContext).evaluate(fileScope);
        }

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
}
