package beam.lang;

import beam.core.BeamCore;
import beam.core.BeamCore.ResourceType;
import beam.lang.types.Value;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParser.VirtualResourceContext;

import java.util.ArrayList;
import java.util.List;

public class VirtualResourceDefinition {

    private String name;
    private List<String> parameters = new ArrayList<>();
    private BeamCore core;
    private BeamVisitor visitor;
    private VirtualResourceContext context;

    public VirtualResourceDefinition(BeamCore core, BeamVisitor visitor, VirtualResourceContext context) {
        this.core = core;
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

    public void evaluate(String scope, Frame frame) {
        for (BeamParser.VirtualResourceBodyContext bodyContext : context.virtualResourceBody()) {
            if (bodyContext.keyValue() != null) {
                String key = visitor.parseKey(bodyContext.keyValue().key());
                Value value = visitor.parseValue(bodyContext.keyValue().value());

                frame.put(key, value);
            } else if (bodyContext.forStmt() != null) {
                ForControl forControl = visitor.visitForStmt(bodyContext.forStmt(), frame);
                frame.putControl(forControl);
            } else if (bodyContext.ifStmt() != null) {
                IfControl ifControl = visitor.visitIfStmt(bodyContext.ifStmt(), frame);
                frame.putControl(ifControl);
            } else if (bodyContext.resource() != null) {
                String type = bodyContext.resource().resourceType().getText();
                ResourceType resourceType = core.resourceType(type);
                if (resourceType == ResourceType.RESOURCE) {
                    Resource resource = visitor.visitResource(bodyContext.resource(), frame);

                    String resourceIdentifier = String.format("%s.%s", scope, resource.resourceIdentifier());
                    resource.resourceIdentifier(resourceIdentifier);

                    frame.putResource(resource);
                } else if (resourceType == ResourceType.VIRTUAL_RESOURCE) {
                    VirtualResourceControl virtualResourceControl = visitor.visitVirtualResource(bodyContext.resource(), frame);
                    frame.putControl(virtualResourceControl);
                }
            }
        }
    }

}
