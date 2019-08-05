package gyro.core.scope;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.psddev.dari.util.Converter;
import com.psddev.dari.util.StringUtils;
import gyro.core.FileBackend;
import gyro.core.GyroException;
import gyro.core.GyroInputStream;
import gyro.core.GyroOutputStream;
import gyro.core.LogDirectiveProcessor;
import gyro.core.PrintDirectiveProcessor;
import gyro.core.auth.CredentialsDirectiveProcessor;
import gyro.core.auth.CredentialsPlugin;
import gyro.core.auth.UsesCredentialsDirectiveProcessor;
import gyro.core.command.HighlanderDirectiveProcessor;
import gyro.core.control.ForDirectiveProcessor;
import gyro.core.control.IfDirectiveProcessor;
import gyro.core.diff.ChangePlugin;
import gyro.core.directive.DirectivePlugin;
import gyro.core.directive.DirectiveSettings;
import gyro.core.finder.FinderPlugin;
import gyro.core.plugin.PluginDirectiveProcessor;
import gyro.core.plugin.PluginSettings;
import gyro.core.reference.FinderReferenceResolver;
import gyro.core.reference.ReferencePlugin;
import gyro.core.reference.ReferenceSettings;
import gyro.core.repo.RepositoryDirectiveProcessor;
import gyro.core.resource.DescriptionDirectiveProcessor;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.ExtendsDirectiveProcessor;
import gyro.core.resource.Resource;
import gyro.core.resource.ResourcePlugin;
import gyro.core.resource.TypeDescriptionDirectiveProcessor;
import gyro.core.scope.converter.DiffableScopeToDiffable;
import gyro.core.scope.converter.IdObjectToResource;
import gyro.core.scope.converter.IterableToOne;
import gyro.core.scope.converter.ResourceToIdObject;
import gyro.core.validation.ValidationError;
import gyro.core.validation.ValidationErrorException;
import gyro.core.virtual.VirtualDirectiveProcessor;
import gyro.core.workflow.CreateDirectiveProcessor;
import gyro.core.workflow.DeleteDirectiveProcessor;
import gyro.core.workflow.ReplaceDirectiveProcessor;
import gyro.core.workflow.UpdateDirectiveProcessor;
import gyro.core.workflow.WorkflowDirectiveProcessor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import gyro.util.Bug;

public class RootScope extends FileScope {

    private final Converter converter;
    private final NodeEvaluator evaluator;
    private final FileBackend backend;
    private final RootScope current;
    private final Set<String> loadFiles;
    private final List<FileScope> fileScopes = new ArrayList<>();
    private final Node initNode;
    private final List<Node> bodyNodes = new ArrayList<>();

    public RootScope(String file, FileBackend backend, RootScope current, Set<String> loadFiles) {
        super(null, file);

        converter = new Converter();

        converter.setThrowError(true);
        converter.putAllStandardFunctions();
        converter.putInheritableFunction(DiffableScope.class, Diffable.class, new DiffableScopeToDiffable());
        converter.putInheritableFunction(Iterable.class, Object.class, new IterableToOne());
        converter.putInheritableFunction(Object.class, Resource.class, new IdObjectToResource(this));
        converter.putInheritableFunction(Resource.class, Object.class, new ResourceToIdObject());

        this.evaluator = new NodeEvaluator();
        this.backend = backend;
        this.current = current;

        try (Stream<String> s = list()) {
            this.loadFiles = (loadFiles != null ? s.filter(loadFiles::contains) : s).collect(Collectors.toSet());
        }

        Stream.of(
            new ChangePlugin(),
            new CredentialsPlugin(),
            new DirectivePlugin(),
            new FinderPlugin(),
            new ReferencePlugin(),
            new ResourcePlugin())
            .forEach(p -> getSettings(PluginSettings.class).getPlugins().add(p));

        Stream.of(
            new CreateDirectiveProcessor(),
            new CredentialsDirectiveProcessor(),
            new DeleteDirectiveProcessor(),
            new DescriptionDirectiveProcessor(),
            new ExtendsDirectiveProcessor(),
            new ForDirectiveProcessor(),
            new IfDirectiveProcessor(),
            new HighlanderDirectiveProcessor(),
            new ReplaceDirectiveProcessor(),
            new RepositoryDirectiveProcessor(),
            new PluginDirectiveProcessor(),
            new TypeDescriptionDirectiveProcessor(),
            new UpdateDirectiveProcessor(),
            new UsesCredentialsDirectiveProcessor(),
            new VirtualDirectiveProcessor(),
            new WorkflowDirectiveProcessor(),
            new PrintDirectiveProcessor(),
            new LogDirectiveProcessor())
            .forEach(p -> getSettings(DirectiveSettings.class).getProcessors().put(p.getName(), p));

        Stream.of(
            new FinderReferenceResolver())
            .forEach(r -> getSettings(ReferenceSettings.class).getResolvers().put(r.getName(), r));

        put("ENV", System.getenv());

        if (!StringUtils.isBlank(getFile())) {
            try (GyroInputStream input = openInput(getFile())) {
                initNode = Node.parse(input, getFile(), GyroParser::file);

            } catch (IOException error) {
                throw new Bug(error);
            }
        } else {
            initNode = null;
        }

        for (String loadFile : this.loadFiles) {
            try (GyroInputStream input = openInput(loadFile)) {
                bodyNodes.add(Node.parse(input, loadFile, GyroParser::file));

            } catch (IOException error) {
                throw new Bug(error);

            } catch (Exception error) {
                throw new GyroException(
                    String.format("Can't parse @|bold %s|@ in @|bold %s|@!", loadFile, this.backend),
                    error);
            }
        }
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

    public List<FileScope> getFileScopes() {
        return fileScopes;
    }

    public Stream<String> list() {
        try {
            return backend.list();

        } catch (Exception error) {
            throw new GyroException(
                String.format("Can't list files in @|bold %s|@!", backend),
                error);
        }
    }

    public GyroInputStream openInput(String file) {
        return new GyroInputStream(backend, file);
    }

    public OutputStream openOutput(String file) {
        return new GyroOutputStream(backend, file);
    }

    public void delete(String file) {
        try {
            backend.delete(file);

        } catch (Exception error) {
            throw new GyroException(
                String.format("Can't delete @|bold %s|@ in @|bold %s|@!", file, backend),
                error);
        }
    }

    public Object convertValue(Type returnType, Object object) {
        return converter.convert(returnType, object);
    }

    public List<Resource> findResources() {
        return findResourcesIn(null);
    }

    public List<Resource> findResourcesIn(Set<String> diffFiles) {
        Stream<Resource> stream = Stream.concat(Stream.of(this), getFileScopes().stream())
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .filter(e -> e.getValue() instanceof Resource)
            .filter(e -> ((Resource) e.getValue()).primaryKey().equals(e.getKey()))
            .map(Entry::getValue)
            .map(Resource.class::cast);

        if (diffFiles != null && !diffFiles.isEmpty()) {
            stream = stream.filter(r -> diffFiles.contains(DiffableInternals.getScope(r).getFileScope().getFile()));
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
        if (id == null) {
            return null;
        }

        DiffableType<T> type = DiffableType.getInstance(resourceClass);
        DiffableField idField = type.getIdField();

        return findResourcesByClass(resourceClass)
            .filter(r -> id.equals(idField.getValue(r)))
            .findFirst()
            .orElseGet(() -> {
                T r = type.newInstance(new DiffableScope(this, null));
                DiffableInternals.setExternal(r, true);
                idField.setValue(r, id);
                return r;
            });
    }

    public void evaluate() {
        evaluator.visit(initNode, this);
        evaluator.visitBody(bodyNodes, this);

        for (Resource resource : findResources()) {
            DiffableType<?> type = DiffableType.getInstance(resource.getClass());
            DiffableScope scope = DiffableInternals.getScope(resource);
            Map<String, Node> nodes = scope.getValueNodes();

            for (DiffableField field : type.getFields()) {
                Node node = nodes.get(field.getName());

                if (node != null) {
                    field.setValue(resource, evaluator.visit(node, scope));
                }
            }
        }
    }

    public void validate() {
        StringBuilder sb = new StringBuilder();

        if (values().stream().anyMatch(Resource.class::isInstance)) {
            sb.append(String.format("Resources are not allowed in '%s'%n", getFile()));
        }

        if (sb.length() != 0) {
            sb.insert(0, "Invalid configs\n");
            throw new GyroException(sb.toString());
        }

        List<ValidationError> errors = findResources().stream()
            .map(r -> DiffableType.getInstance(r.getClass()).validate(r))
            .flatMap(List::stream)
            .collect(Collectors.toList());

        if (!errors.isEmpty()) {
            throw new ValidationErrorException(errors);
        }
    }

}
