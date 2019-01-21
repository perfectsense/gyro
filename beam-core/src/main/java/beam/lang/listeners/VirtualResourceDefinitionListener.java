package beam.lang.listeners;

import beam.lang.BeamVisitor;
import beam.lang.VirtualResourceDefinition;
import beam.parser.antlr4.BeamParser.VirtualResourceContext;
import beam.parser.antlr4.BeamParserBaseListener;

import java.util.HashMap;
import java.util.Map;

public class VirtualResourceDefinitionListener extends BeamParserBaseListener {

    private BeamVisitor visitor;
    private Map<String, VirtualResourceDefinition> virtualResources = new HashMap<>();

    public VirtualResourceDefinitionListener(BeamVisitor visitor) {
        this.visitor = visitor;
    }

    public VirtualResourceDefinition virtualResource(String name) {
        return virtualResources.get(name);
    }

    @Override
    public void exitVirtualResource(VirtualResourceContext contex) {
        VirtualResourceDefinition virtualResourceDefinition = visitor.visitVirtualResourceDefinition(contex);
        virtualResources.put(virtualResourceDefinition.name(), virtualResourceDefinition);
    }

}
