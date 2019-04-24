package gyro.core.scope;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
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
import gyro.parser.antlr4.GyroLexer;
import gyro.parser.antlr4.GyroParser;
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

    public RootScope(String file, Set<String> activeFiles) {
        this(file, null, activeFiles);
    }

    public RootScope(RootScope current) {
        this(current.getFile(), current, current.activeFiles);
    }

    private RootScope(String file, RootScope current, Set<String> activeFiles) {
        super(null, file);
        this.current = current;
        this.activeFiles.addAll(activeFiles);

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
        if (activeFiles.isEmpty()) {
            return findAllResources();
        }

        List<FileScope> activeFileScopes = new ArrayList<>();
        for (String path : activeFiles) {
            String activeFile = path;
            if (this.current == null) {
                path += ".state";
                Path rootDir = GyroCore.getRootInitFile().getParent().getParent();
                Path relative = rootDir.relativize(Paths.get(path).toAbsolutePath());
                Path state = Paths.get(rootDir.toString(), ".gyro", "state", relative.toString());
                if (Files.exists(state)) {
                    activeFile = state.toString();
                }
            }

            for (FileScope fileScope : getFileScopes()) {
                try {
                    if (Files.isSameFile(Paths.get(fileScope.getFile()), Paths.get(activeFile))) {
                        activeFileScopes.add(fileScope);
                    }
                } catch (IOException e) {
                    throw new GyroException(e.getMessage(), e);
                }
            }
        }

        return resources.values().stream()
            .filter(r -> activeFileScopes.contains(r.scope().getFileScope()))
            .collect(Collectors.toList());
    }

    public Resource findResource(String name) {
        return resources.get(name);
    }

    public void load(FileBackend backend) throws Exception {
        List<String> files = new ArrayList<>();

        try {
            Path gyroDir = GyroCore.getRootInitFile().getParent();
            try (Stream<Path> pathStream = this.current != null
                ? Files.find(gyroDir.getParent(), Integer.MAX_VALUE,
                (p, b) -> b.isRegularFile()
                    && p.toString().endsWith(".gyro")
                    && !p.toString().startsWith(gyroDir.toString()))

                : Files.find(gyroDir.resolve(Paths.get("state")), Integer.MAX_VALUE,
                (p, b) -> b.isRegularFile()
                    && p.toString().endsWith(".gyro.state"))) {

                for (Path path : (Iterable<Path>) pathStream::iterator) {
                    files.add(path.toString());
                }
            }

        } catch (IOException e) {
            throw new GyroException(e.getMessage(), e);
        }

        load(backend, files);
    }

    public void load(FileBackend backend, List<String> files) throws Exception {
        try (InputStream inputStream = backend.read(getFile())) {
            parse(inputStream, getFile()).evaluate(this);
        }

        if (files == null) {
            files = Collections.emptyList();
        }

        List<Node> nodes = new ArrayList<>();
        for (String file : files) {
            try (InputStream inputStream = backend.read(file)) {
                nodes.add(parse(inputStream, file));
            }
        }

        DeferError.evaluate(this, nodes);
        validate();
    }

    private FileNode parse(InputStream inputStream, String file) throws IOException {
        GyroLexer lexer = new GyroLexer(CharStreams.fromStream(inputStream));
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

        return (FileNode) Node.create(fileContext, file);
    }

    private void validate() {
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

    public void save(FileBackend backend) throws IOException {
        for (FileScope fileScope : getFileScopes()) {
            String file = fileScope.getFile();

            try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(
                    backend.write(file),
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
}
