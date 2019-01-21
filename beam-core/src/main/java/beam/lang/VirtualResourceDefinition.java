package beam.lang;

import beam.parser.antlr4.BeamParser.VirtualResourceContext;

import java.util.ArrayList;
import java.util.List;

public class VirtualResourceDefinition {

    private String name;
    private List<String> parameters = new ArrayList<>();
    private BeamVisitor visitor;
    private VirtualResourceContext context;

    public VirtualResourceDefinition(BeamVisitor visitor, VirtualResourceContext context) {
        this.visitor = visitor;
        this.context = context;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public List<String> parameters() {
        return parameters;
    }

}
