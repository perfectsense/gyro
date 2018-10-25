package beam.core;

import java.util.ArrayList;
import java.util.List;

public class BeamScalar implements BeamReferable {

    private List<BeamLiteral> elements;

    private boolean resolved;

    private Object value;

    public List<BeamLiteral> getElements() {
        if (elements == null) {
            elements = new ArrayList<>();
        }

        return elements;
    }

    public void setElements(List<BeamLiteral> elements) {
        this.elements = elements;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean resolve(BeamContext context) {
        if (getElements().isEmpty()) {
            throw new BeamException("Unable to resolve scalar with zero elements!");
        }

        if (resolved) {
            return false;
        }

        boolean progress = false;
        if (getElements().size() == 1) {
            BeamLiteral literal = getElements().get(0);
            progress = literal.resolve(context);

            if (literal.getValue() != null) {
                value = literal.getValue();
                resolved = true;
                progress = true;
            }

        } else {
            StringBuilder sb = new StringBuilder();
            for (BeamLiteral literal : getElements()) {
                progress = literal.resolve(context) || progress;

                if (literal.getValue() != null) {
                    Object resolvedLiteral = literal.getValue();

                    // Enforce string concat
                    if (!(resolvedLiteral instanceof String)) {
                        throw new BeamException(String.format("Illegal placement of %s in %s, expect a String", literal.getLiteral(), this));
                    } else {
                        sb.append(resolvedLiteral);
                    }
                } else {
                    return progress;
                }
            }

            value = sb.toString();
            resolved = true;
            progress = true;
        }

        return progress;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Object element : getElements()) {
            sb.append(element);
        }

        return sb.toString();
    }
}
