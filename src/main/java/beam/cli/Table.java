package beam.cli;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import com.psddev.dari.util.CompactMap;

public class Table {

    private final Map<String, Integer> columns = new CompactMap<String, Integer>();

    public Table addColumn(String header, int width) {
        columns.put(header, width);
        return this;
    }

    private void writeSeparator(PrintWriter out, char first, char fill, char between, char last) {
        out.write(first);
        out.write(fill);

        for (Iterator<Integer> i = columns.values().iterator(); i.hasNext();) {
            int width = i.next();

            for (; width > 0; -- width) {
                out.write(fill);
            }

            if (i.hasNext()) {
                out.write(fill);
                out.write(between);
                out.write(fill);
            }
        }

        out.write(fill);
        out.write(last);
        out.write('\n');
    }

    public void writeHeader(PrintWriter out) {

        writeSeparator(out, '+', '-', '+', '+');
        out.write("| ");

        for (Iterator<Map.Entry<String, Integer>> i = columns.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, Integer> entry = i.next();
            String header = entry.getKey();
            int remainder = entry.getValue() - header.length();

            if (remainder < 0) {
                header = header.substring(header.length() + remainder);
                remainder = 0;
            }

            out.write(header);

            for (; remainder > 0; -- remainder) {
                out.write(' ');
            }

            if (i.hasNext()) {
                out.write(" | ");
            }
        }

        out.write(" |\n");
        writeSeparator(out, '+', '-', '+', '+');
        out.flush();
    }

    public void writeRow(PrintWriter out, Object... cells) {
        out.write("| ");

        int cellsLength = cells != null ? cells.length : 0;
        int index = 0;

        for (Iterator<Integer> i = columns.values().iterator(); i.hasNext(); ++ index) {
            int width = i.next();
            Object cell = index < cellsLength ? cells[index] : null;
            String cellString = cell != null ? cell.toString() : "";
            int remainder = width - cellString.length();

            out.write(cellString);

            for (; remainder > 0; -- remainder) {
                out.write(' ');
            }

            if (i.hasNext()) {
                out.write(" | ");
            }
        }

        out.write(" |\n");
        out.flush();
    }

    public void writeFooter(PrintWriter out) {
        writeSeparator(out, '+', '-', '+', '+');
        out.flush();
    }
}
