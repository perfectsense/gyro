package beam.docs;

import java.util.Arrays;
import java.util.List;

public abstract class BeamDocletOption implements BeamDoclet.Option {

    private String name;
    private int arguments;
    private String description;
    private String parameters;

    public BeamDocletOption(String name, int arguments, String description, String parameters) {
        this.name = name;
        this.arguments = arguments;
        this.description = description;
        this.parameters = parameters;
    }

    @Override
    public int getArgumentCount() {
        return arguments;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Kind getKind() {
        return Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return Arrays.asList(name);
    }

    @Override
    public String getParameters() {
        return parameters;
    }

}
