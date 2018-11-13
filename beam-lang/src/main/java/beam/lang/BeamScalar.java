package beam.lang;

import java.util.ArrayList;
import java.util.List;

public class BeamScalar implements BeamResolvable {

    private List<BeamLiteral> elements;

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
    public boolean resolve(BeamConfig config) {
        if (getElements().isEmpty()) {
            throw new BeamLangException("Unable to resolve scalar with zero elements!");
        }

        if (value != null) {
            return false;
        }

        boolean progress = false;
        if (getElements().size() == 1) {
            BeamLiteral literal = getElements().get(0);
            progress = literal.resolve(config);

            if (literal.getValue() != null) {
                value = literal.getValue();
                progress = true;
            }

        } else {
            StringBuilder sb = new StringBuilder();
            for (BeamLiteral literal : getElements()) {
                progress = literal.resolve(config) || progress;

                if (literal.getValue() != null) {
                    Object resolvedLiteral = literal.getValue();

                    // Enforce string concat
                    if (!(resolvedLiteral instanceof String)) {
                        throw new BeamLangException(String.format("Illegal placement of %s in %s, expect a String", literal.getLiteral(), this));
                    } else {
                        sb.append(resolvedLiteral);
                    }
                } else {
                    return progress;
                }
            }

            value = sb.toString();
            progress = true;
        }

        return progress;
    }

    @Override
    public String toString() {
        if (value != null) {
            return value.toString();
        }

        StringBuilder sb = new StringBuilder();
        for (Object element : getElements()) {
            sb.append(element);
        }

        return sb.toString();
    }
}
