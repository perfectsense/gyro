package beam.core;

import java.util.ArrayList;
import java.util.List;

public class BeamScalar {

    private List<Object> elements;

    public List<Object> getElements() {
        if (elements == null) {
            elements = new ArrayList<>();
        }

        return elements;
    }

    public void setElements(List<Object> elements) {
        this.elements = elements;
    }

    public Object resolve(BeamContext context) {
        if (getElements().isEmpty()) {
            throw new BeamException("Unable to resolve scalar with zero elements!");
        }

        if (getElements().size() == 1) {
            Object element = getElements().get(0);
            if (element instanceof BeamReference) {
                return ((BeamReference) element).resolve(context);
            } else {
                return element;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Object element : getElements()) {
            if (element instanceof BeamReference) {
                Object resolvedElement = ((BeamReference) element).resolve(context);
                if (!(resolvedElement instanceof String)) {
                    sb.append(element);
                } else {
                    sb.append(resolvedElement);
                }
            } else {
                sb.append(element);
            }
        }

        return sb.toString();
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
