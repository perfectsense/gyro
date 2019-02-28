package gyro.plugin.ssh;

import gyro.core.BeamUI;
import com.psddev.dari.util.CompactMap;

import java.util.Iterator;
import java.util.Map;

public class Table {

    private final Map<String, Integer> columns = new CompactMap<String, Integer>();

    public Table addColumn(String header, int width) {
        columns.put(header, width);
        return this;
    }

    private void writeSeparator(BeamUI ui, char first, char fill, char between, char last) {
        ui.write("%c%c", first, fill);

        for (Iterator<Integer> i = columns.values().iterator(); i.hasNext();) {
            int width = i.next();

            for (; width > 0; -- width) {
                ui.write("%c", fill);
            }

            if (i.hasNext()) {
                ui.write("%c%c%c", fill, between, fill);
            }
        }

        ui.write("%c%c\n", fill, last);
    }

    public void writeHeader(BeamUI ui) {

        writeSeparator(ui, '+', '-', '+', '+');
        ui.write("| ");

        for (Iterator<Map.Entry<String, Integer>> i = columns.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, Integer> entry = i.next();
            String header = entry.getKey();
            int remainder = entry.getValue() - header.length();

            if (remainder < 0) {
                header = header.substring(header.length() + remainder);
                remainder = 0;
            }

            ui.write(header);

            for (; remainder > 0; -- remainder) {
                ui.write(" ");
            }

            if (i.hasNext()) {
                ui.write(" | ");
            }
        }

        ui.write(" |\n");
        writeSeparator(ui, '+', '-', '+', '+');
    }

    public void writeRow(BeamUI ui, Object... cells) {
        ui.write("| ");

        int cellsLength = cells != null ? cells.length : 0;
        int index = 0;

        for (Iterator<Integer> i = columns.values().iterator(); i.hasNext(); ++ index) {
            int width = i.next();
            Object cell = index < cellsLength ? cells[index] : null;
            String cellString = cell != null ? cell.toString() : "";
            int remainder = width - cellString.length();

            ui.write(cellString);

            for (; remainder > 0; -- remainder) {
                ui.write(" ");
            }

            if (i.hasNext()) {
                ui.write(" | ");
            }
        }

        ui.write(" |\n");
    }

    public void writeFooter(BeamUI ui) {
        writeSeparator(ui, '+', '-', '+', '+');
    }
}
