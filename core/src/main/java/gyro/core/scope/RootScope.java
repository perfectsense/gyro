/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.core.scope;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.psddev.dari.util.Converter;
import gyro.core.FileBackend;
import gyro.core.GyroException;
import gyro.core.GyroInputStream;
import gyro.core.GyroOutputStream;
import gyro.core.LogDirectiveProcessor;
import gyro.core.PrintDirectiveProcessor;
import gyro.core.audit.AuditorDirectiveProcessor;
import gyro.core.audit.AuditorPlugin;
import gyro.core.audit.MetadataDirectiveProcessor;
import gyro.core.auth.CredentialsDirectiveProcessor;
import gyro.core.auth.CredentialsPlugin;
import gyro.core.auth.UsesCredentialsDirectiveProcessor;
import gyro.core.backend.FileBackendDirectiveProcessor;
import gyro.core.backend.FileBackendPlugin;
import gyro.core.command.HighlanderDirectiveProcessor;
import gyro.core.command.HighlanderSettings;
import gyro.core.control.ForDirectiveProcessor;
import gyro.core.control.IfDirectiveProcessor;
import gyro.core.diff.ChangeSettings;
import gyro.core.diff.ConfiguredFieldsChangeProcessor;
import gyro.core.diff.GlobalChangePlugin;
import gyro.core.directive.DirectivePlugin;
import gyro.core.directive.DirectiveSettings;
import gyro.core.finder.FinderPlugin;
import gyro.core.plugin.PluginDirectiveProcessor;
import gyro.core.plugin.PluginSettings;
import gyro.core.preprocessor.Preprocessor;
import gyro.core.preprocessor.PreprocessorSettings;
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
import gyro.core.resource.ModificationChangeProcessor;
import gyro.core.resource.ModificationPlugin;
import gyro.core.resource.Resource;
import gyro.core.resource.ResourcePlugin;
import gyro.core.resource.TypeDescriptionDirectiveProcessor;
import gyro.core.resource.WaitDirectiveProcessor;
import gyro.core.scope.converter.DiffableScopeToDiffable;
import gyro.core.scope.converter.IdObjectToResource;
import gyro.core.scope.converter.IterableToOne;
import gyro.core.scope.converter.ResourceToIdObject;
import gyro.core.validation.ValidationError;
import gyro.core.validation.ValidationErrorException;
import gyro.core.virtual.VirtualDirectiveProcessor;
import gyro.core.workflow.CreateDirectiveProcessor;
import gyro.core.workflow.DefineDirectiveProcessor;
import gyro.core.workflow.DeleteDirectiveProcessor;
import gyro.core.workflow.ReplaceDirectiveProcessor;
import gyro.core.workflow.RestoreRootProcessor;
import gyro.core.workflow.UpdateDirectiveProcessor;
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
    private final Set<String> loadFiles;
    private final List<FileScope> fileScopes = new ArrayList<>();

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
        this.loadFiles = loadFiles != null ? ImmutableSet.copyOf(loadFiles) : ImmutableSet.of();

        Stream.of(
            new AuditorPlugin(),
            new CredentialsPlugin(),
            new DirectivePlugin(),
            new FileBackendPlugin(),
            new FinderPlugin(),
            new GlobalChangePlugin(),
            new ModificationPlugin(),
            new ReferencePlugin(),
            new ResourcePlugin(),
            new RootPlugin())
            .forEach(p -> getSettings(PluginSettings.class).getPlugins().add(p));

        Stream.of(
            new ModificationChangeProcessor(),
            new ConfiguredFieldsChangeProcessor())
            .forEach(p -> getSettings(ChangeSettings.class).getProcessors().add(p));

        Stream.of(
            AuditorDirectiveProcessor.class,
            CreateDirectiveProcessor.class,
            CredentialsDirectiveProcessor.class,
            DefineDirectiveProcessor.class,
            DeleteDirectiveProcessor.class,
            DescriptionDirectiveProcessor.class,
            ExtendsDirectiveProcessor.class,
            FileBackendDirectiveProcessor.class,
            ForDirectiveProcessor.class,
            HighlanderDirectiveProcessor.class,
            IfDirectiveProcessor.class,
            LogDirectiveProcessor.class,
            MetadataDirectiveProcessor.class,
            PluginDirectiveProcessor.class,
            PrintDirectiveProcessor.class,
            ReplaceDirectiveProcessor.class,
            RepositoryDirectiveProcessor.class,
            TypeDescriptionDirectiveProcessor.class,
            UpdateDirectiveProcessor.class,
            UsesCredentialsDirectiveProcessor.class,
            VirtualDirectiveProcessor.class,
            WaitDirectiveProcessor.class)
            .forEach(p -> getSettings(DirectiveSettings.class).addProcessor(p));

        Stream.of(
            FinderReferenceResolver.class)
            .forEach(r -> getSettings(ReferenceSettings.class).addResolver(r));

        Stream.of(
            new RestoreRootProcessor())
            .forEach(p -> getSettings(RootSettings.class).getProcessors().add(p));

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

    public GyroOutputStream openOutput(String file) {
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

        if (idField == null) {
            throw new GyroException(String.format(
                "Unable to find @Id on a getter in %s",
                resourceClass.getSimpleName()));
        }

        return findResourcesByClass(resourceClass)
            .filter(r -> id.equals(idField.getValue(r)))
            .findFirst()
            .orElseGet(() -> type.newExternal(this, id));
    }

    public List<Node> load() {
        List<Node> nodes = new ArrayList<>();

        evaluateFile(getFile(), node -> nodes.addAll(node.getBody()));

        List<Node> finalNodes = nodes;
        try {
            for (Preprocessor pp : getSettings(PreprocessorSettings.class).getPreprocessors()) {
                finalNodes = pp.preprocess(finalNodes, this);
            }
            evaluator.evaluateBody(finalNodes, this);

        } catch (Defer error) {
            // Ignore for now since this is reevaluated later.
        }

        return finalNodes;
    }

    public void evaluate() {
        List<Node> nodes = load();
        Set<String> existingFiles;

        try (Stream<String> s = list()) {
            existingFiles = s.collect(Collectors.toCollection(LinkedHashSet::new));
        }

        if (getSettings(HighlanderSettings.class).isHighlander()) {
            int s = loadFiles.size();

            if (s == 0) {
                throw new GyroException("Must specify a file in highlander mode!");

            } else if (s != 1) {
                throw new GyroException("Can't specify more than one file in highlander mode!");

            } else {
                Optional.of(loadFiles.iterator().next())
                    .filter(existingFiles::contains)
                    .ifPresent(f -> evaluateFile(f, nodes::add));
            }

        } else {
            existingFiles.forEach(f -> evaluateFile(f, nodes::add));
        }

        evaluator.evaluate(this, nodes);

        getSettings(RootSettings.class).getProcessors().forEach(p -> {
            try {
                p.process(this);

            } catch (Exception error) {
                throw new GyroException(
                    String.format("Can't process root using @|bold %s|@!", p),
                    error);
            }
        });
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
