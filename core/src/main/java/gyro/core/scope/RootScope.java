package gyro.core.scope;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.psddev.dari.util.Converter;
import gyro.core.FileBackend;
import gyro.core.GyroException;
import gyro.core.GyroInputStream;
import gyro.core.GyroOutputStream;
import gyro.core.LogDirectiveProcessor;
import gyro.core.PrintDirectiveProcessor;
import gyro.core.auth.CredentialsDirectiveProcessor;
import gyro.core.auth.CredentialsPlugin;
import gyro.core.auth.UsesCredentialsDirectiveProcessor;
import gyro.core.backend.FileBackendDirectiveProcessor;
import gyro.core.backend.FileBackendPlugin;
import gyro.core.command.HighlanderDirectiveProcessor;
import gyro.core.command.HighlanderSettings;
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
import gyro.lang.ast.block.FileNode;
import gyro.parser.antlr4.GyroParser;
import gyro.util.Bug;
import org.apache.commons.lang3.StringUtils;

public class RootScope extends FileScope {

    private final Converter converter;
    private final NodeEvaluator evaluator;
    private final FileBackend backend;
    private final RootScope current;
    private final List<FileScope> fileScopes = new ArrayList<>();

    public RootScope(String file, FileBackend backend, RootScope current) {
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

        Stream.of(
            new ChangePlugin(),
            new CredentialsPlugin(),
            new DirectivePlugin(),
            new FileBackendPlugin(),
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
            new FileBackendDirectiveProcessor(),
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

    public Set<String> getFiles() {
        return fileScopes.stream()
            .map(FileScope::getFile)
            .collect(Collectors.toCollection(LinkedHashSet::new));
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

    public void evaluate(Set<String> files) {
        List<Node> nodes = new ArrayList<>();

        evaluateFile(getFile(), node -> nodes.addAll(node.getBody()));

        try {
            evaluator.visitBody(nodes, this);

        } catch (Defer error) {
            // Ignore for now since this is reevaluated later.
        }

        if (getSettings(HighlanderSettings.class).isHighlander()) {
            if (files != null) {
                if (files.size() == 1) {
                    evaluateFile(files.iterator().next(), nodes::add);

                } else {
                    throw new GyroException("Can't specify more than one file in highlander mode!");
                }

            } else {
                throw new GyroException("Must specify a file in highlander mode!");
            }

        } else {
            try (Stream<String> s = list()) {
                s.forEach(f -> {
                    if (files == null || files.contains(f)) {
                        evaluateFile(f, nodes::add);
                    }
                });
            }
        }

        evaluator.visitBody(nodes, this);

        for (Resource resource : findResources()) {
            DiffableType<?> type = DiffableType.getInstance(resource.getClass());
            DiffableScope scope = DiffableInternals.getScope(resource);
            Map<String, Node> valueNodes = scope.getValueNodes();

            for (DiffableField field : type.getFields()) {
                Node node = valueNodes.get(field.getName());

                if (node != null) {
                    field.setValue(resource, evaluator.visit(node, scope));
                }
            }
        }
    }

    private void evaluateFile(String file, Consumer<FileNode> consumer) {
        if (StringUtils.isBlank(file)) {
            return;
        }

        try (GyroInputStream input = openInput(file)) {
            consumer.accept((FileNode) Node.parse(input, file, GyroParser::file));

        } catch (IOException error) {
            throw new Bug(error);

        } catch (Exception error) {
            throw new GyroException(
                String.format("Can't parse @|bold %s|@ in @|bold %s|@!", file, this.backend),
                error);
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
            .map(r -> DiffableType.getInstance(r).validate(r))
            .flatMap(List::stream)
            .collect(Collectors.toList());

        if (!errors.isEmpty()) {
            throw new ValidationErrorException(errors);
        }
    }

}
