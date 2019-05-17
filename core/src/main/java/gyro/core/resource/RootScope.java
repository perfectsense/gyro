package gyro.core.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.psddev.dari.util.Converter;
import com.psddev.dari.util.TypeDefinition;
import gyro.core.Credentials;
import gyro.core.FileBackend;
import gyro.core.GyroException;
import gyro.core.plugin.PluginDirectiveProcessor;
import gyro.core.plugin.RepositoryDirectiveProcessor;
import gyro.lang.GyroErrorListener;
import gyro.lang.GyroErrorStrategy;
import gyro.lang.GyroLanguageException;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.VirtualResourceNode;
import gyro.parser.antlr4.GyroLexer;
import gyro.parser.antlr4.GyroParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class RootScope extends FileScope {

    private final Converter converter;
    private final NodeEvaluator evaluator;
    private final FileBackend backend;
    private final RootScope current;
    private final Set<String> loadFiles;
    private final Map<String, DirectiveProcessor> directiveProcessors = new HashMap<>();
    private final Map<String, Class<?>> resourceClasses = new HashMap<>();
    private final Map<String, Class<? extends ResourceFinder>> resourceFinderClasses = new HashMap<>();
    private final Map<String, VirtualResourceNode> virtualResourceNodes = new LinkedHashMap<>();
    private final List<Workflow> workflows = new ArrayList<>();
    private final List<FileScope> fileScopes = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public RootScope(String file, FileBackend backend, RootScope current, Set<String> loadFiles) {
        super(null, file);

        converter = new Converter();

        converter.setThrowError(true);
        converter.putAllStandardFunctions();

        converter.putInheritableFunction(
            Object.class,
            Resource.class,
            (c, returnType, id) -> c.convert(
                returnType,
                findResourceById((Class<? extends Resource>) returnType, id)));

        converter.putInheritableFunction(
            Resource.class,
            Object.class,
            (c, returnType, resource) -> c.convert(
                returnType,
                DiffableType.getInstance(resource.getClass())
                    .getIdField()
                    .getValue(resource)));

        this.evaluator = new NodeEvaluator();
        this.backend = backend;
        this.current = current;

        try (Stream<String> s = backend.list()) {
            this.loadFiles = (loadFiles != null ? s.filter(loadFiles::contains) : s).collect(Collectors.toSet());
        }

        Stream.of(new RepositoryDirectiveProcessor(), new PluginDirectiveProcessor())
            .forEach(p -> directiveProcessors.put(p.getName(), p));

        put("ENV", System.getenv());
    }

    public NodeEvaluator getEvaluator() {
        return evaluator;
    }

    public FileBackend getBackend() {
        return backend;
    }

    public RootScope getCurrent() {
        return current;
    }

    public Set<String> getLoadFiles() {
        return loadFiles;
    }

    public Map<String, DirectiveProcessor> getDirectiveProcessors() {
        return directiveProcessors;
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

    public Object convertValue(Type returnType, Object object) {
        return converter.convert(returnType, object);
    }

    public List<Resource> findResources() {
        return findResourcesIn(null);
    }

    public List<Resource> findResourcesIn(Set<String> diffFiles) {
        Stream<Resource> stream = Stream.concat(Stream.of(this), getFileScopes().stream())
            .map(Map::values)
            .flatMap(Collection::stream)
            .filter(Resource.class::isInstance)
            .map(Resource.class::cast);

        if (diffFiles != null && diffFiles.isEmpty()) {
            stream = stream.filter(r -> diffFiles.contains(r.scope.getFileScope().getFile()));
        }

        return stream.collect(Collectors.toList());
    }

    public <T extends Resource> Stream<T> findResourcesByClass(Class<T> resourceClass) {
        return findResources()
            .stream()
            .filter(resourceClass::isInstance)
            .map(resourceClass::cast);
    }

    public Resource findResource(String name) {
        return Stream.concat(Stream.of(this), getFileScopes().stream())
            .map(s -> s.get(name))
            .filter(Resource.class::isInstance)
            .map(Resource.class::cast)
            .findFirst()
            .orElse(null);
    }

    public <T extends Resource> T findResourceById(Class<T> resourceClass, Object id) {
        DiffableField idField = DiffableType.getInstance(resourceClass).getIdField();

        return findResourcesByClass(resourceClass)
            .filter(r -> id.equals(idField.getValue(r)))
            .findFirst()
            .orElseGet(() -> {
                T r = TypeDefinition.getInstance(resourceClass).newInstance();
                r.external = true;
                r.scope = new DiffableScope(this);
                idField.setValue(r, id);
                return r;
            });
    }

    public void load() throws Exception {
        try (InputStream input = backend.openInput(getFile())) {
            evaluator.visit(parse(input, getFile()), this);
        }

        List<Node> nodes = new ArrayList<>();

        for (String file : loadFiles) {
            try (InputStream inputStream = backend.openInput(file)) {
                nodes.add(parse(inputStream, file));
            }
        }

        try {
            evaluator.visitBody(nodes, this);

        } catch (DeferError e) {
            throw new GyroException(e.getMessage());
        }

        validate();
    }

    private Node parse(InputStream inputStream, String file) throws IOException {
        GyroLexer lexer = new GyroLexer(CharStreams.fromReader(new InputStreamReader(inputStream), file));
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

        return Node.create(fileContext);
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

        Map<String, List<String>> duplicateResources = new HashMap<>();
        for (Resource resource : findResources()) {
            String fullName = resource.primaryKey();
            duplicateResources.putIfAbsent(fullName, new ArrayList<>());
            duplicateResources.get(fullName).add(resource.scope.getFileScope().getFile());
        }

        for (Map.Entry<String, List<String>> entry : duplicateResources.entrySet()) {
            if (entry.getValue().size() > 1) {
                sb.append(String.format("%nDuplicate resource %s defined in the following files:%n", entry.getKey()));
                entry.getValue().stream()
                    .map(p -> p + "\n")
                    .forEach(sb::append);
            }
        }

        if (sb.length() != 0) {
            sb.insert(0, "Invalid configs\n");
            throw new GyroException(sb.toString());
        }
    }

}
