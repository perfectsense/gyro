package beam.lang;

public class VirtualResourceControl extends Control {

    private VirtualResourceDefinition definition;
    private Container container;
    private String resourceIdentifier;

    public VirtualResourceControl(VirtualResourceDefinition definition, Container container) {
        this.definition = definition;
        this.container = container;
    }

    public String resourceIdentifier() {
        return resourceIdentifier;
    }

    public void resourceIdentifier(String resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    @Override
    public boolean resolve() {
        evaluate();

        return true;
    }

    @Override
    public void evaluate() {
        Container parent = (Container) parent();
        Frame frame = new Frame();
        frame.parent(parent);
        parent.frames().add(frame);

        frame.keyValues.putAll(container.keyValues);
        definition.evaluate(resourceIdentifier, frame);
    }

}
