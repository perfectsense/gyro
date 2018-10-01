package beam.core.diff;

import beam.core.*;
import org.reflections.Reflections;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DiffUtil {

    public static Iterable<? extends BeamResource> loadCurrent(BeamProvider provider, BeamResourceFilter filter) {
        Reflections reflections = new Reflections("beam");
        Set<BeamResource> current = new HashSet<>();
        for (Class<? extends BeamResource> resourceClass : reflections.getSubTypesOf(BeamResource.class)) {
            try {
                ConfigKey configKey = resourceClass.getAnnotation(ConfigKey.class);
                if (configKey == null) {
                    continue;
                }

                BeamResource resource = resourceClass.newInstance();
                Set<BeamResource> resourceSet = new HashSet<>();
                resource.init(provider, filter, resourceSet);
                current.addAll(resourceSet);
            } catch (IllegalAccessException | InstantiationException error) {
                throw new BeamException(String.format("Unable to load resource from %s", resourceClass), error);
            }
        }

        return current;
    }

    public static ResourceDiff<?> generateDiff(BeamProvider provider, BeamResourceFilter filter, Iterable<? extends BeamResource> current, Iterable<? extends BeamResource> pending) throws Exception {
        ResourceDiff resourceDiff = new ResourceDiff(provider, filter, current, pending);
        resourceDiff.diff();
        return resourceDiff;
    }

    public static void findChangesByResourceClass(List<Diff<?, ?, ?>> diffs, Class<?> resourceClass, List<Change<?>> changes) {
        for (Diff<?, ?, ?> diff : diffs) {
            for (Change<?> change : diff.getChanges()) {
                BeamResource resource = ((ResourceChange)change).getResource();
                if (resourceClass == null || resource.getClass().equals(resourceClass)) {
                    changes.add(change);
                } else {
                    findChangesByResourceClass(change.getDiffs(), resourceClass, changes);
                }
            }
        }
    }

    public static void findChangesByType(List<Change<?>> changes, ChangeType type) {
        Iterator<Change<?>> iter = changes.iterator();
        while (iter.hasNext()) {
            Change<?> change = iter.next();
            if (change.getType() != type) {
                iter.remove();
            }
        }
    }

    public static Object getPropertyValue(Object reference, String resourceClass, String resourceProperty) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        if (resourceClass == null || reference.getClass().getName().endsWith(resourceClass)) {
            for (PropertyDescriptor property : Introspector.getBeanInfo(reference.getClass()).getPropertyDescriptors()) {
                if (property.getName().equals(resourceProperty)) {
                    Method readMethod = property.getReadMethod();
                    if (readMethod != null) {
                        return readMethod.invoke(reference);
                    }
                }
            }
        }

        return null;
    }

    public static void writeDiffs(List<Diff<?, ?, ?>> diffs, int indent, PrintWriter out, Set<ChangeType> changeTypes) {
        for (Diff<?, ?, ?> diff : diffs) {
            if (!diff.hasChanges()) {
                continue;
            }

            if (indent == 0) {
                out.print('\n');
                out.flush();
            }

            for (Change<?> change : diff.getChanges()) {
                ChangeType type = change.getType();
                List<Diff<?, ?, ?>> changeDiffs = change.getDiffs();

                if (type == ChangeType.KEEP) {
                    boolean hasChanges = false;

                    for (Diff<?, ?, ?> changeDiff : changeDiffs) {
                        if (changeDiff.hasChanges()) {
                            hasChanges = true;
                            break;
                        }
                    }

                    if (!hasChanges) {
                        continue;
                    }
                }

                changeTypes.add(type);
                writeIndent(indent, out);
                writeChange(change, out);
                out.print('\n');
                out.flush();

                writeDiffs(changeDiffs, indent + 4, out, changeTypes);
            }
        }
    }

    private static void writeIndent(int indent, PrintWriter out) {
        for (int i = 0; i < indent; ++ i) {
            out.print(' ');
        }
    }

    private static void writeChange(Change<?> change, PrintWriter out) {
        switch (change.getType()) {
            case CREATE :
                out.format("@|green + %s|@", change);
                break;

            case UPDATE :
                if (change.toString().contains("@|")) {
                    out.format(" * %s", change);
                } else {
                    out.format("@|yellow * %s|@", change);
                }
                break;

            case REPLACE :
                out.format("@|blue * %s|@", change);
                break;

            case DELETE :
                out.format("@|red - %s|@", change);
                break;

            default :
                out.print(change);
        }
    }

    public static void setChangeable(List<Diff<?, ?, ?>> diffs) {
        for (Diff<?, ?, ?> diff : diffs) {
            for (Change<?> change : diff.getChanges()) {
                change.setChangeable(true);
                setChangeable(change.getDiffs());
            }
        }
    }

    public static void createOrUpdate(List<Diff<?, ?, ?>> diffs, PrintWriter out) {
        for (Diff<?, ?, ?> diff : diffs) {
            for (Change<?> change : diff.getChanges()) {
                ChangeType type = change.getType();

                if (type == ChangeType.CREATE || type == ChangeType.UPDATE) {
                    execute(change, out);
                }

                createOrUpdate(change.getDiffs(), out);
            }
        }
    }

    public static void delete(List<Diff<?, ?, ?>> diffs, PrintWriter out) {
        for (Diff<?, ?, ?> diff : diffs) {
            for (Change<?> change : diff.getChanges()) {
                delete(change.getDiffs(), out);

                if (change.getType() == ChangeType.DELETE) {
                    execute(change, out);
                }
            }
        }
    }

    private static void execute(Change<?> change, PrintWriter out) {
        ChangeType type = change.getType();

        if (type == ChangeType.KEEP ||
                type == ChangeType.REPLACE ||
                change.isChanged()) {
            return;
        }

        Set<Change<?>> dependencies = change.dependencies();

        if (dependencies != null && !dependencies.isEmpty()) {
            for (Change<?> d : dependencies) {
                execute(d, out);
            }
        }

        out.write("Executing: ");
        writeChange(change, out);
        out.flush();

        change.getChangedAsset();
        out.write(" OK\n");
        out.flush();
    }
}
