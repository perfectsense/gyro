package gyro.core.scope;

import java.io.IOException;
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
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.resource.Resource;
import gyro.core.resource.ResourceFinder;
import gyro.core.workflow.Workflow;
import gyro.lang.ast.block.VirtualResourceNode;

public class RootScope extends Scope {

    private final RootScope current;
    private final Map<String, Class<?>> resourceClasses = new HashMap<>();
    private final Map<String, Class<? extends ResourceFinder>> resourceFinderClasses = new HashMap<>();
    private final Map<String, VirtualResourceNode> virtualResourceNodes = new LinkedHashMap<>();
    private final List<Workflow> workflows = new ArrayList<>();
    private final List<FileScope> fileScopes = new ArrayList<>();
    private final FileScope initScope;
    private final Map<String, Resource> resources = new LinkedHashMap<>();
    private final Set<String> activeScopePaths = new HashSet<>();

    public RootScope() {
        this(null, Collections.emptySet());
    }

    public RootScope(Set<String> activePaths) {
        this(null, activePaths);
    }

    public RootScope(RootScope current) {
        this(current, Collections.emptySet());
    }

    public RootScope(RootScope current, Set<String> activePaths) {
        super(null);
        this.current = current;

        try {
            initScope = new FileScope(this, GyroCore.findPluginPath().toString());

            Path rootPath = GyroCore.findRootDirectory(Paths.get("").toAbsolutePath());
            try (Stream<Path> pathStream = getCurrent() != null
                ? Files.find(rootPath.getParent(), 100,
                    (p, b) -> b.isRegularFile()
                        && p.toString().endsWith(".gyro")
                        && !p.toString().startsWith(rootPath.toString()))

                : Files.find(rootPath, 100,
                    (p, b) -> b.isRegularFile()
                        && p.toString().endsWith(".gyro.state"))) {

                for (Path path : pathStream.collect(Collectors.toSet())) {
                    FileScope fileScope = new FileScope(this, path.toString());
                    getFileScopes().add(fileScope);
                }
            }

            if (getCurrent() == null) {
                for (String path : activePaths) {
                    path += ".state";
                    Path rootDir = GyroCore.findPluginPath().getParent().getParent();
                    Path relative = rootDir.relativize(Paths.get(path).toAbsolutePath());
                    Path statePath = Paths.get(rootDir.toString(), ".gyro", "state", relative.toString());
                    Files.createDirectories(statePath.getParent());

                    this.activeScopePaths.add(statePath.toString());
                }
            } else {
                this.activeScopePaths.addAll(activePaths);
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

    public FileScope getInitScope() {
        return initScope;
    }

    public Map<String, Resource> getResources() {
        return resources;
    }

    public Set<String> getActiveScopePaths() {
        return activeScopePaths;
    }

    public List<Resource> findAllResources() {
        return new ArrayList<>(resources.values());
    }

    public List<Resource> findAllActiveResources() {

        if (getActiveScopePaths().isEmpty()) {
            return findAllResources();
        }

        try {
            List<FileScope> activeFileScopes = new ArrayList<>();
            for (FileScope fileScope : getFileScopes()) {
                for (String path : activeScopePaths) {
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

    public void validate() {
        StringBuilder sb = new StringBuilder();
        for (FileScope fileScope : getFileScopes()) {
            boolean hasCredentials = fileScope.values()
                .stream()
                .anyMatch(Credentials.class::isInstance);

            if (hasCredentials) {
                sb.append(String.format("Credentials are only allowed in '%s', found in '%s'%n", getInitScope().getFile(), fileScope.getFile()));
            }
        }

        boolean hasResources = getInitScope().values()
            .stream()
            .anyMatch(r -> r instanceof Resource && !(r instanceof Credentials));

        if (hasResources) {
            sb.append(String.format("Resources are not allowed in '%s'%n", getInitScope().getFile()));
        }

        if (sb.length() != 0) {
            sb.insert(0, "Invalid configs\n");
            throw new GyroException(sb.toString());
        }
    }
}
