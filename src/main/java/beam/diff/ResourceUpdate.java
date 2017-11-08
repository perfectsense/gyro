package beam.diff;

import beam.BeamCloud;
import beam.BeamResource;
import com.google.common.base.Throwables;
import com.psddev.dari.util.IoUtils;
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

public class ResourceUpdate<B extends BeamCloud> extends ResourceChange<B> {

    private final BeamResource current;
    private final BeamResource pending;
    private final B cloud;
    private final Class<?> configClass;
    private final Set<String> updatedProperties = new HashSet<>();
    private final StringBuilder updatedPropertiesDisplay = new StringBuilder();
    private final Set<String> replacedProperties = new HashSet<>();
    private final StringBuilder replacedPropertiesDisplay = new StringBuilder();

    public ResourceUpdate(ResourceDiff diff, BeamResource<B, ?> current, BeamResource<B, ?> pending, B cloud, Class<?> configClass) {
        super(diff, ChangeType.UPDATE, current, pending);

        this.current = current;
        this.pending = pending;
        this.cloud = cloud;
        this.configClass = configClass;
    }

    public void tryToKeep() {
        try {
            for (PropertyDescriptor p : Introspector.getBeanInfo(configClass).getPropertyDescriptors()) {
                Method reader = p.getReadMethod();

                if (reader == null) {
                    continue;
                }

                Object currentValue = reader.invoke(current);
                Object pendingValue = reader.invoke(pending);

                if (((ObjectUtils.isBlank(pendingValue) && pendingValue instanceof NullArrayList) ||
                        (ObjectUtils.isBlank(pendingValue) && pendingValue instanceof NullSet)) ||
                        !ObjectUtils.isBlank(pendingValue)) {
                    ResourceDiffProperty propertyAnnotation = reader.getAnnotation(ResourceDiffProperty.class);

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

                        } else if (!pendingValue.equals(currentValue)) {
                            boolean showingDiff = false;

                            if (pendingValue instanceof Map) {
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

                            if (!showingDiff) {
                                changedProperties.add(p.getName());
                                changedPropertiesDisplay.append(p.getName());
                                changedPropertiesDisplay.append(": ");
                                changedPropertiesDisplay.append(currentValue);
                                changedPropertiesDisplay.append(" -> ");
                                changedPropertiesDisplay.append(pendingValue);
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

    @Override
    public ChangeType getType() {
        if (current.isVerifying()) {
            return ChangeType.UPDATE;

        } else if (updatedPropertiesDisplay.length() > 0) {
            return ChangeType.UPDATE;

        } else if (replacedPropertiesDisplay.length() > 0) {
            return ChangeType.REPLACE;

        } else {
            return ChangeType.KEEP;
        }
    }

    @Override
    protected BeamResource<B, ?> change() {
        ChangeType type = getType();

        if (type == ChangeType.UPDATE) {
            pending.update(cloud, current, updatedProperties);
            return pending;

        } else if (type == ChangeType.REPLACE) {
            return current;

        } else {
            return current;
        }
    }

    @Override
    public String toString() {
        ChangeType type = getType();

        if (current.isVerifying()) {
            return String.format(
                    "Verify %s",
                    current.toDisplayString());

        } else if (type == ChangeType.UPDATE) {
            return String.format(
                    "Update %s (%s)",
                    current.toDisplayString(),
                    updatedPropertiesDisplay);

        } else if (type == ChangeType.REPLACE) {
            return String.format(
                    "Replace %s (%s)",
                    current.toDisplayString(),
                    replacedPropertiesDisplay);

        } else {
            return current.toDisplayString();
        }
    }
}
