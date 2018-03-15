package beam.core.diff;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

public class DiffUtil {

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
