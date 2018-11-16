package beam.core.diff;

import beam.core.BeamCredentials;
import beam.core.BeamResource;
import com.google.common.base.Throwables;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.Lazy;
import com.psddev.dari.util.ObjectUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;

public class ResourceChange {

    private final BeamResource currentResource;
    private final BeamResource pendingResource;

    private final ResourceDiff diff;
    private final List<ResourceDiff> diffs = new ArrayList<>();
    private boolean changeable;
    private boolean changed;

    private final Set<String> updatedProperties = new HashSet<>();
    private final StringBuilder updatedPropertiesDisplay = new StringBuilder();
    private final Set<String> replacedProperties = new HashSet<>();
    private final StringBuilder replacedPropertiesDisplay = new StringBuilder();

    private final Lazy<BeamResource> changedResource = new Lazy<BeamResource>() {

        @Override
        public final BeamResource create() throws Exception {
            BeamResource resource = change();
            changed = true;

            return resource;
        }
    };

    public ResourceChange(ResourceDiff diff, BeamResource currentResource, BeamResource pendingResource) {
        this.currentResource = currentResource;
        this.pendingResource = pendingResource;
        this.diff = diff;
    }

    public BeamResource getChangedResource() {
        if (isChangeable()) {
            return changedResource.get();

        } else {
            throw new IllegalStateException("Can't change yet!");
        }
    }

    public void create(Collection<? extends BeamResource> pendingResources) throws Exception {
        diff.create(this, pendingResources);
    }

    public <R extends BeamResource> void update(Collection<R> currentResources, Collection<R> pendingResources) throws Exception {
        diff.update(this, currentResources, pendingResources);
    }

    public void delete(Collection<? extends BeamResource> pendingResources) throws Exception {
        diff.delete(this, pendingResources);
    }

    public Set<ResourceChange> dependencies() {
        Set<ResourceChange> dependencies = new HashSet<>();

        BeamResource resource = pendingResource != null ? pendingResource : currentResource;
        for (BeamResource r : (getType() == ChangeType.DELETE ? resource.dependents() : resource.dependencies())) {
            ResourceChange c = r.getChange();

            if (c != null) {
                dependencies.add(c);
            }
        }

        return dependencies;
    }

    public ChangeType getType() {
        if (pendingResource == null) {
            return ChangeType.DELETE;
        } else if (currentResource == null) {
            return ChangeType.CREATE;
        } else if (updatedPropertiesDisplay.length() > 0) {
            return ChangeType.UPDATE;
        } else if (replacedPropertiesDisplay.length() > 0) {
            return ChangeType.REPLACE;
        } else {
            return ChangeType.KEEP;
        }
    }

    public BeamResource getCurrentResource() {
        return currentResource;
    }

    public List<ResourceDiff> getDiffs() {
        return diffs;
    }

    public boolean isChangeable() {
        return changeable;
    }

    public void setChangeable(boolean changeable) {
        this.changeable = changeable;
    }

    public boolean isChanged() {
        return changed;
    }

    public void tryToKeep() {
        try {
            if (currentResource == null || pendingResource == null) {
                return;
            }

            Class klass = pendingResource != null ? pendingResource.getClass() : null;
            if (klass == null && currentResource != null) {
                klass = currentResource.getClass();
            }

            if (klass == null) {
                return;
            }

            for (PropertyDescriptor p : Introspector.getBeanInfo(klass).getPropertyDescriptors()) {
                Method reader = p.getReadMethod();

                if (reader == null) {
                    continue;
                }

                Object currentValue = reader.invoke(currentResource);
                Object pendingValue = reader.invoke(pendingResource);

                ResourceDiffProperty propertyAnnotation = reader.getAnnotation(ResourceDiffProperty.class);
                boolean nullable = propertyAnnotation != null && propertyAnnotation.nullable();

                if (((ObjectUtils.isBlank(pendingValue) && pendingValue instanceof NullArrayList) ||
                        (ObjectUtils.isBlank(pendingValue) && pendingValue instanceof NullSet)) ||
                        (!ObjectUtils.isBlank(pendingValue) || nullable)) {

                    if (propertyAnnotation != null) {
                        Set<String> changedProperties;
                        StringBuilder changedPropertiesDisplay;

                        if (propertyAnnotation.updatable()) {
                            changedProperties = updatedProperties;
                            changedPropertiesDisplay = updatedPropertiesDisplay;
                        } else {
                            changedProperties = replacedProperties;
                            changedPropertiesDisplay = replacedPropertiesDisplay;
                        }

                        if ("tags".equals(p.getName())) {
                            Map<?, ?> currentMap = (Map<?, ?>) currentValue;

                            for (Map.Entry<?, ?> entry : ((Map<?, ?>) pendingValue).entrySet()) {
                                Object key = entry.getKey();
                                Object pendingMapValue = entry.getValue();
                                Object currentMapValue = currentMap.get(key);

                                if (currentMapValue == null ||
                                        !currentMapValue.equals(pendingMapValue)) {

                                    changedProperties.add(p.getName());
                                    changedPropertiesDisplay.append("tag: ");
                                    changedPropertiesDisplay.append(key);
                                    changedPropertiesDisplay.append(" -> ");
                                    changedPropertiesDisplay.append(pendingMapValue);
                                    changedPropertiesDisplay.append(", ");
                                }
                            }
                        } else if (pendingValue == null) {
                            if (!ObjectUtils.isBlank(currentValue)) {
                                changedProperties.add(p.getName());
                                changedPropertiesDisplay.append("unsetting ");
                                changedPropertiesDisplay.append(p.getName());
                                changedPropertiesDisplay.append("[" + currentValue + "]");
                                changedPropertiesDisplay.append(", ");
                            }
                        } else if (!pendingValue.equals(currentValue)) {
                            boolean showingDiff = false;

                            if (pendingValue instanceof Map) {
                                if (propertyAnnotation.mapSummary()) {
                                    changedProperties.add(p.getName());
                                    changedPropertiesDisplay.append(p.getName());
                                    changedPropertiesDisplay.append(": ");
                                    changedPropertiesDisplay.append(mapSummaryDiff((Map) currentValue, (Map) pendingValue));

                                    showingDiff = true;

                                } else {
                                    File currentFile = null;
                                    File pendingFile = null;
                                    File diffFile = null;

                                    try {
                                        currentFile = File.createTempFile("beam", ".json");
                                        FileWriter currentWriter = new FileWriter(currentFile);
                                        currentWriter.write(ObjectUtils.toJson(currentValue, true));
                                        currentWriter.close();

                                        pendingFile = File.createTempFile("beam", ".json");
                                        FileWriter pendingWriter = new FileWriter(pendingFile);
                                        pendingWriter.write(ObjectUtils.toJson(pendingValue, true));
                                        pendingWriter.close();

                                        List<String> arguments = new ArrayList<>();
                                        arguments.add("wdiff");

                                        arguments.add("-w"); // start delete
                                        arguments.add("@|red -");
                                        arguments.add("-x"); // end delete
                                        arguments.add("|@");


                                        arguments.add("-y"); // start insert
                                        arguments.add("@|yellow +");
                                        arguments.add("-z"); // end insert
                                        arguments.add("|@");

                                        arguments.add(currentFile.getCanonicalPath());
                                        arguments.add(pendingFile.getCanonicalPath());

                                        diffFile = File.createTempFile("beam", "diff");

                                        ProcessBuilder diffProcess = new ProcessBuilder(arguments);
                                        diffProcess.redirectOutput(diffFile);
                                        diffProcess.redirectError(diffFile);
                                        int exitCode = diffProcess.start().waitFor();

                                        if (exitCode == 1) {
                                            String diffOutput = IoUtils.toString(diffFile, Charset.forName("UTF-8"));

                                            changedProperties.add(p.getName());
                                            changedPropertiesDisplay.append(p.getName());
                                            changedPropertiesDisplay.append(": ");
                                            changedPropertiesDisplay.append(diffOutput);

                                            showingDiff = true;
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    } finally {
                                        if (currentFile != null) {
                                            currentFile.delete();
                                        }

                                        if (pendingFile != null) {
                                            pendingFile.delete();
                                        }

                                        if (diffFile != null) {
                                            diffFile.delete();
                                        }
                                    }
                                }
                            }

                            if (!showingDiff) {
                                changedProperties.add(p.getName());
                                changedPropertiesDisplay.append(p.getName());
                                changedPropertiesDisplay.append(": ");
                                if (ObjectUtils.isBlank(currentValue)) {
                                    changedPropertiesDisplay.append(pendingValue);
                                } else {
                                    changedPropertiesDisplay.append(currentValue);
                                    changedPropertiesDisplay.append(" -> ");
                                    changedPropertiesDisplay.append(pendingValue);
                                }
                                changedPropertiesDisplay.append(", ");
                            }
                        }
                    }
                }
            }

            if (updatedPropertiesDisplay.length() > 0) {
                updatedPropertiesDisplay.setLength(updatedPropertiesDisplay.length() - 2);
            }

            if (replacedPropertiesDisplay.length() > 0) {
                replacedPropertiesDisplay.setLength(replacedPropertiesDisplay.length() - 2);
            }

        } catch (IllegalAccessException |
                IntrospectionException error) {
            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            throw Throwables.propagate(error);
        }
    }

    protected BeamResource change() {
        ChangeType type = getType();

        if (type == ChangeType.UPDATE) {
            pendingResource.update(currentResource, updatedProperties);
            return pendingResource;

        } else if (type == ChangeType.REPLACE) {
            return currentResource;

        } else {
            return currentResource;
        }
    }

    @Override
    public String toString() {
        ChangeType type = getType();

        if (type == ChangeType.UPDATE) {
            return String.format(
                    "Update %s (%s)",
                    currentResource.toDisplayString(),
                    updatedPropertiesDisplay);

        } else if (type == ChangeType.REPLACE) {
            return String.format(
                    "Replace %s (%s)",
                    currentResource.toDisplayString(),
                    replacedPropertiesDisplay);

        } else {
            return currentResource.toDisplayString();
        }
    }

    private String mapSummaryDiff(Map current, Map pending) {
        StringBuilder diffResult = new StringBuilder();
        if (current == null) {
            current = new HashMap();
        }

        final MapDifference diff = Maps.difference(current, pending);
        for (Object key : diff.entriesOnlyOnRight().keySet()) {
            diffResult.append(String.format("+[%s => %s], ", key, pending.get(key)));
        }

        for (Object key : diff.entriesOnlyOnLeft().keySet()) {
            diffResult.append(String.format("-[%s => %s], ", key, current.get(key)));
        }

        if (diffResult.lastIndexOf(",") == diffResult.length()) {
            diffResult.setLength(diffResult.length() - 2);
        }

        for (Object key : diff.entriesDiffering().keySet()) {
            diffResult.append(String.format("*%s ", key));
            MapDifference.ValueDifference value = (MapDifference.ValueDifference)diff.entriesDiffering().get(key);
            Object currentValue = value.leftValue();
            Object pendingValue = value.rightValue();

            if (currentValue instanceof Map && pendingValue instanceof Map) {
                diffResult.append("(");
                diffResult.append(mapSummaryDiff((Map) currentValue, (Map) pendingValue));
                diffResult.append(") ");
            } else {
                diffResult.append("(");
                diffResult.append(String.format("[%s => %s]", currentValue, pendingValue));
                diffResult.append(") ");
            }
        }

        return diffResult.toString();
    }
}
