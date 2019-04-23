package gyro.core.scope;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gyro.core.Credentials;
import gyro.core.FileBackend;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.plugin.PluginLoader;
import gyro.core.resource.Resource;
import gyro.core.resource.ResourceFinder;
import gyro.core.workflow.Workflow;
import gyro.lang.GyroErrorListener;
import gyro.lang.GyroErrorStrategy;
import gyro.lang.GyroLanguageException;
import gyro.lang.ast.DeferError;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.FileNode;
import gyro.lang.ast.block.VirtualResourceNode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class RootScope extends FileScope {

    private final RootScope current;
    private final Map<String, Class<?>> resourceClasses = new HashMap<>();
    private final Map<String, Class<? extends ResourceFinder>> resourceFinderClasses = new HashMap<>();
    private final Map<String, VirtualResourceNode> virtualResourceNodes = new LinkedHashMap<>();
    private final List<Workflow> workflows = new ArrayList<>();
    private final List<FileScope> fileScopes = new ArrayList<>();
    private final Map<String, Resource> resources = new LinkedHashMap<>();
    private final Set<String> activeFiles = new HashSet<>();
    private final Map<String, Set<String>> duplicateResources = new HashMap<>();

    public RootScope(String file) {
        this(file, null, Collections.emptySet());
    }

    public RootScope(String file, Set<String> activePaths) {
        this(file, null, activePaths);
    }

    public RootScope(RootScope current, Set<String> activePaths) {
        this(current.getFile(), current, activePaths);
    }

    private RootScope(String file, RootScope current, Set<String> activePaths) {
        super(null, file);
        this.current = current;

        put("ENV", System.getenv());

        try {
            Path rootPath = Paths.get(file).toAbsolutePath().getParent();
            try (Stream<Path> pathStream = this.current != null
                ? Files.find(rootPath.getParent(), 100,
                    (p, b) -> b.isRegularFile()
                        && p.toString().endsWith(".gyro")
                        && !p.toString().startsWith(rootPath.toString()))

                : Files.find(rootPath, 100,
                    (p, b) -> b.isRegularFile()
                        && p.toString().endsWith(".gyro.state"))) {

                for (Path path : (Iterable<Path>) pathStream::iterator) {
                    FileScope fileScope = new FileScope(this, path.toString());
                    getFileScopes().add(fileScope);
                }
            }

            if (this.current == null) {
                for (String path : activePaths) {
                    path += ".state";
                    Path rootDir = GyroCore.getRootInitFile().getParent().getParent();
                    Path relative = rootDir.relativize(Paths.get(path).toAbsolutePath());
                    Path statePath = Paths.get(rootDir.toString(), ".gyro", "state", relative.toString());
                    Files.createDirectories(statePath.getParent());

                    this.activeFiles.add(statePath.toString());
                }
            } else {
                this.activeFiles.addAll(activePaths);
            }

        } catch (IOException e) {
            throw new GyroException(e.getMessage(), e);
        }

        put("ENV", System.getenv());
    }

    public RootScope getCurrent() {
        return current;
    }

    public Map<String, Class<?>> getResourceClasses() {
        return resourceClasses;
    }

    public Map<String, Class<? extends ResourceFinder>> getResourceFinderClasses() {
        return resourceFinderClasses;
    }

    public Map<String, VirtualResourceNode> getVirtualResourceNodes() {
        return virtualResourceNodes;
    }

    public List<Workflow> getWorkflows() {
        return workflows;
    }

    public List<FileScope> getFileScopes() {
        return fileScopes;
    }

    public void putResource(String name, Resource resource) {
        if (resources.containsKey(name)) {
            Resource old = resources.get(name);
            String oldPath = old.scope().getFileScope().getFile();
            String path = resource.scope().getFileScope().getFile();
            if (!oldPath.equals(path)) {
                duplicateResources.putIfAbsent(name, new HashSet<>());
                duplicateResources.get(name).add(oldPath);
                duplicateResources.get(name).add(path);
            }
        }

        resources.put(name, resource);
    }

    public Set<String> getActiveFiles() {
        return activeFiles;
    }

    public List<Resource> findAllResources() {
        return new ArrayList<>(resources.values());
    }

    public List<Resource> findAllActiveResources() {

        if (getActiveFiles().isEmpty()) {
            return findAllResources();
        }

        try {
            List<FileScope> activeFileScopes = new ArrayList<>();
            for (FileScope fileScope : getFileScopes()) {
                for (String path : activeFiles) {
                    if (Files.isSameFile(Paths.get(fileScope.getFile()), Paths.get(path))) {
                        activeFileScopes.add(fileScope);
                    }
                }
            }

            return resources.values().stream()
                .filter(r -> activeFileScopes.contains(r.scope().getFileScope()))
                .collect(Collectors.toList());

        } catch (IOException e) {
            throw new GyroException(e.getMessage(), e);
        }
    }

    public Resource findResource(String name) {
        return resources.get(name);
    }

    public void load(FileBackend backend) throws Exception {
        try (InputStream inputStream = backend.read(getFile())) {
            parse(inputStream).evaluate(this);
        }

        Map<Node, FileScope> map = new LinkedHashMap<>();
        for (FileScope fileScope : getFileScopes()) {
            try (InputStream inputStream = backend.read(fileScope.getFile())) {
                map.put(parse(inputStream), fileScope);
            }
        }

        while (true) {
            List<DeferError> errors = new ArrayList<>();
            Map<Node, FileScope> deferred = new HashMap<>();

            for (Map.Entry<Node, FileScope> entry : map.entrySet()) {
                try {
                    entry.getKey().evaluate(entry.getValue());

                } catch (DeferError error) {
                    errors.add(error);
                    deferred.put(entry.getKey(), entry.getValue());
                }
            }

            if (deferred.isEmpty()) {
                break;

            } else if (map.size() == deferred.size()) {
                StringBuilder sb = new StringBuilder();
                for (DeferError error : errors) {
                    sb.append(error.getMessage());
                }

                throw new GyroException(sb.toString());

            } else {
                map = deferred;
            }
        }

        validate();
    }

    public void save(FileBackend backend) throws IOException {
        for (FileScope fileScope : getFileScopes()) {
            String file = fileScope.getFile();
            OutputStream outputStream = backend.write(file);

            try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(
                    outputStream,
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
        }
    }

    private FileNode parse(InputStream inputStream) throws IOException {
        gyro.parser.antlr4.GyroLexer lexer = new gyro.parser.antlr4.GyroLexer(CharStreams.fromStream(inputStream));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        gyro.parser.antlr4.GyroParser parser = new gyro.parser.antlr4.GyroParser(stream);
        GyroErrorListener errorListener = new GyroErrorListener();

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        parser.setErrorHandler(new GyroErrorStrategy());

        gyro.parser.antlr4.GyroParser.FileContext fileContext = parser.file();

        int errorCount = errorListener.getSyntaxErrors();
        if (errorCount > 0) {
            throw new GyroLanguageException(String.format("%d %s found while parsing.", errorCount, errorCount == 1 ? "error" : "errors"));
        }

        return (FileNode) Node.create(fileContext);
    }

    public void validate() {
        StringBuilder sb = new StringBuilder();
        for (FileScope fileScope : getFileScopes()) {
            boolean hasCredentials = fileScope.values()
                .stream()
                .anyMatch(Credentials.class::isInstance);

            if (hasCredentials) {
                sb.append(String.format("Credentials are only allowed in '%s', found in '%s'%n", getFile(), fileScope.getFile()));
            }
        }

        boolean hasResources = this.values()
            .stream()
            .anyMatch(r -> r instanceof Resource && !(r instanceof Credentials));

        if (hasResources) {
            sb.append(String.format("Resources are not allowed in '%s'%n", getFile()));
        }

        for (Map.Entry<String, Set<String>> entry : duplicateResources.entrySet()) {
            sb.append(String.format("%nDuplicate resource %s defined in the following files:%n", entry.getKey()));
            entry.getValue().stream()
                .map(p -> p + "\n")
                .forEach(sb::append);
        }

        if (sb.length() != 0) {
            sb.insert(0, "Invalid configs\n");
            throw new GyroException(sb.toString());
        }
    }
}
