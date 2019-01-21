package beam.lang;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.LocalStateBackend;
import beam.lang.plugins.PluginLoader;
import beam.lang.types.BooleanValue;
import beam.lang.types.ListValue;
import beam.lang.types.MapValue;
import beam.lang.types.NumberValue;
import beam.lang.types.ReferenceValue;
import beam.lang.types.StringExpressionValue;
import beam.lang.types.StringValue;
import beam.lang.types.Value;
import beam.parser.antlr4.BeamParser.BeamFileContext;
import beam.parser.antlr4.BeamParser.FileContext;
import beam.parser.antlr4.BeamParser.ForStmtContext;
import beam.parser.antlr4.BeamParser.ForVariableContext;
import beam.parser.antlr4.BeamParser.IfStmtContext;
import beam.parser.antlr4.BeamParser.KeyContext;
import beam.parser.antlr4.BeamParser.KeySimpleValueContext;
import beam.parser.antlr4.BeamParser.KeyValueContext;
import beam.parser.antlr4.BeamParser.ListItemValueContext;
import beam.parser.antlr4.BeamParser.ListValueContext;
import beam.parser.antlr4.BeamParser.MapValueContext;
import beam.parser.antlr4.BeamParser.PluginContext;
import beam.parser.antlr4.BeamParser.ReferenceValueContext;
import beam.parser.antlr4.BeamParser.ResourceBodyContext;
import beam.parser.antlr4.BeamParser.ResourceContext;
import beam.parser.antlr4.BeamParser.SimpleValueContext;
import beam.parser.antlr4.BeamParser.StateContext;
import beam.parser.antlr4.BeamParser.StringContentsContext;
import beam.parser.antlr4.BeamParser.StringExpressionContext;
import beam.parser.antlr4.BeamParser.StringValueContext;
import beam.parser.antlr4.BeamParser.SubresourceBodyContext;
import beam.parser.antlr4.BeamParser.SubresourceContext;
import beam.parser.antlr4.BeamParser.ValueContext;
import beam.parser.antlr4.BeamParser.*;
import beam.parser.antlr4.BeamParserBaseVisitor;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class BeamVisitor extends BeamParserBaseVisitor {

    private BeamCore core;
    private String path;

    public BeamVisitor(BeamCore core, String path) {
        this.core = core;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public BeamFile visitBeamFile(BeamFileContext context) {
        BeamFile beamFile = new BeamFile();
        beamFile.path(getPath());

        for (FileContext fileContext : context.file()) {
            if (fileContext.keyValue() != null) {
                String key = parseKey(fileContext.keyValue().key());
                Value value = parseValue(fileContext.keyValue().value());
                value.line(fileContext.keyValue().getStart().getLine());
                value.column(fileContext.keyValue().getStart().getCharPositionInLine());

                beamFile.put(key, value);
            } else if (fileContext.resource() != null) {
                Resource resource = visitResource(fileContext.resource(), beamFile);
                beamFile.putResource(resource);
            } else if (fileContext.plugin() != null) {
                PluginLoader loader = visitPlugin(fileContext.plugin());
                beamFile.plugins().add(loader);
            } else if (fileContext.state() != null) {
                StateBackend stateBackend = visitState(fileContext.state());
                beamFile.stateBackend(stateBackend);
            } else if (fileContext.importStmt() != null) {
                String path = fileContext.importStmt().importPath().getText();

                Path currentPath = new File(getPath()).getParentFile().toPath();
                Path importPath = new File(path).toPath();

                String resolvedPath = currentPath.resolve(importPath).toString();
                String importFileName = new File(path).getName().replace(".bcl", "");

                if (!resolvedPath.endsWith(".bcl") && !resolvedPath.endsWith(".bcl.state")) {
                    resolvedPath += ".bcl";
                }

                try {
                    String importName = fileContext.importStmt().importName() != null
                        ? fileContext.importStmt().importName().getText()
                        : importFileName;

                    BeamFile importedFileNode = core.parseImport(resolvedPath);
                    importedFileNode.path(resolvedPath);

                    beamFile.putImport(importName, importedFileNode);
                } catch (IOException ioe) {
                    throw new BeamException("Failed to import '" + resolvedPath + "'");
                }
            } else if (fileContext.forStmt() != null) {
                ForControl forNode = visitForStmt(fileContext.forStmt(), beamFile);
                beamFile.putControl(forNode);
            } else if (fileContext.ifStmt() != null) {
                IfControl ifStmt = visitIfStmt(fileContext.ifStmt(), beamFile);
                beamFile.putControl(ifStmt);
            }
        }

        return beamFile;
    }

    public PluginLoader visitPlugin(PluginContext context) {
        PluginLoader loader = new PluginLoader();
        loader.core(core);

        for (KeySimpleValueContext keyValueContext : context.pluginBody().keySimpleValue()) {
            String key = StringUtils.stripEnd(keyValueContext.key().getText(), ":");
            Value value = parseValue(keyValueContext.simpleValue());

            if (key.equalsIgnoreCase("artifact")) {
                loader.artifact(value.getValue().toString());
            } else if (key.equalsIgnoreCase("repositories")) {
                loader.repositories(((ListValue) value).getValue());
            }
        }

        return loader;
    }

    public StateBackend visitState(StateContext context) {
        return new LocalStateBackend();
    }

    public Resource visitResource(ResourceContext context, Container parent) {
        Resource resource = createResource(context.resourceType().getText());
        resource.resourceType(context.resourceType().getText());
        resource.parent(parent);
        resource.line(context.getStart().getLine());
        resource.column(context.getStart().getCharPositionInLine());

        if (context.resourceName().stringValue() != null) {
            Value idValue = parseStringValue(context.resourceName().stringValue());
            if (idValue instanceof StringExpressionValue) {
                resource.resourceIdentifierExpression((StringExpressionValue) idValue);
            } else if (idValue instanceof StringValue) {
                resource.resourceIdentifier(((StringValue) idValue).getValue());
            }
        } else {
            resource.resourceIdentifier(context.resourceName().getText());
        }

        for (ResourceBodyContext bodyContext : context.resourceBody()) {
            if (bodyContext.keyValue() != null) {
                String key = parseKey(bodyContext.keyValue().key());
                Value value = parseValue(bodyContext.keyValue().value());
                value.line(bodyContext.keyValue().getStart().getLine());
                value.column(bodyContext.keyValue().getStart().getCharPositionInLine());

                resource.put(key, value);
            } else if (bodyContext.subresource() != null) {
                Resource node = visitSubresource(bodyContext.subresource(), resource);
                String type = bodyContext.subresource().resourceType().getText();
                resource.putSubresource(type, node);
            } else if (bodyContext.forStmt() != null) {
                ForControl forNode = visitForStmt(bodyContext.forStmt(), resource);
                resource.putControl(forNode);
            } else if (bodyContext.ifStmt() != null) {
                IfControl ifStmt = visitIfStmt(bodyContext.ifStmt(), resource);
                resource.putControl(ifStmt);
            }
        }

        resource.executeInternal();

        return resource;
    }

    public Resource visitSubresource(SubresourceContext context, Resource parent) {
        Resource resource = createSubresource(parent, context.resourceType().getText());
        resource.resourceType(context.resourceType().getText());
        resource.parent(parent);
        resource.line(context.getStart().getLine());
        resource.column(context.getStart().getCharPositionInLine());

        if (context.resourceName() != null) {
            resource.resourceIdentifier(context.resourceName().getText());
        }

        for (SubresourceBodyContext bodyContext : context.subresourceBody()) {
            if (bodyContext.keyValue() != null) {
                String key = parseKey(bodyContext.keyValue().key());
                Value value = parseValue(bodyContext.keyValue().value());
                value.line(bodyContext.keyValue().getStart().getLine());
                value.column(bodyContext.keyValue().getStart().getCharPositionInLine());

                resource.put(key, value);
            }
        }

        resource.executeInternal();

        return resource;
    }

    public ForControl visitForStmt(ForStmtContext context, Node parent) {
        ForControl forNode = new ForControl(this, context);
        forNode.line(context.getStart().getLine());
        forNode.column(context.getStart().getCharPositionInLine());
        forNode.parent(parent);

        for (ForVariableContext variableContext : context.forVariables().forVariable()) {
            forNode.variables().add(variableContext.IDENTIFIER().getText());
        }

        for (ListItemValueContext valueContext : context.listValue().listItemValue()) {
            Value value = null;

            if (valueContext.stringValue() != null) {
                value = parseStringValue(valueContext.stringValue());
            } else if (valueContext.referenceValue() != null) {
                value = parseReferenceValue(valueContext.referenceValue());
            } else if (valueContext.numberValue() != null) {
                value = new NumberValue(valueContext.numberValue().getText());
            } else if (valueContext.booleanValue() != null) {
                value = new BooleanValue(valueContext.booleanValue().getText());
            }

            if (value != null) {
                value.line(valueContext.getStart().getLine());
                value.column(valueContext.getStart().getCharPositionInLine());
                value.parent(forNode);

                forNode.listValues().add(value);
            }
        }

        return forNode;
    }

    public IfControl visitIfStmt(IfStmtContext context, Node parent) {
        IfControl ifStmt = new IfControl(this, context);
        ifStmt.line(context.getStart().getLine());
        ifStmt.column(context.getStart().getCharPositionInLine());
        ifStmt.parent(parent);

        return ifStmt;
    }

    public VirtualResourceDefinition visitVirtualResource(VirtualResourceContext context) {
        VirtualResourceDefinition virtualResourceDefinition = new VirtualResourceDefinition(this, context);
        virtualResourceDefinition.name(context.virtualResourceName().IDENTIFIER().getText());

        for (VirtualResourceParamContext paramContext : context.virtualResourceParam()) {
            virtualResourceDefinition.parameters().add(paramContext.IDENTIFIER().getText());
        }

        return virtualResourceDefinition;
    }

    public static String parseKey(KeyContext keyContext) {
        return StringUtils.stripEnd(keyContext.getText(), ":");
    }

    public static Value parseValue(SimpleValueContext context) {
        Value value = null;

        if (context.booleanValue() != null) {
            value = new BooleanValue(context.booleanValue().getText());
        } else if (context.numberValue() != null) {
            value = new NumberValue(context.numberValue().getText());
        } else if (context.STRING_LITERAL() != null) {
            value = new StringValue(context.STRING_LITERAL().getText());
        } else if (context.listValue() != null) {
            value = parseListValue(context.listValue());
        } else if (context.mapValue() != null) {
            value = parseMapNode(context.mapValue());
        }

        value.line(context.start.getLine());
        value.column(context.start.getCharPositionInLine());

        return value;
    }

    public static Value parseValue(ValueContext context) {
        Value value = null;

        if (context.booleanValue() != null) {
            value = new BooleanValue(context.booleanValue().getText());
        } else if (context.numberValue() != null) {
            value = new NumberValue(context.numberValue().getText());
        } else if (context.referenceValue() != null) {
            value = parseReferenceValue(context.referenceValue());
        } else if (context.stringValue() != null) {
            value = parseStringValue(context.stringValue());
        } else if (context.listValue() != null) {
            value = parseListValue(context.listValue());
        } else if (context.mapValue() != null) {
            value = parseMapNode(context.mapValue());
        }

        value.line(context.start.getLine());
        value.column(context.start.getCharPositionInLine());

        return value;
    }

    public static MapValue parseMapNode(MapValueContext context) {
        MapValue mapValue = new MapValue();
        for (KeyValueContext valueContext : context.keyValue()) {
            String key = parseKey(valueContext.key());
            Value value = parseValue(valueContext.value());
            value.line(valueContext.getStart().getLine());
            value.column(valueContext.getStart().getCharPositionInLine());

            mapValue.getKeyValues().put(key, value);
        }

        return mapValue;
    }

    public static ListValue parseListValue(ListValueContext context) {
        ListValue listValue = new ListValue();
        for (ListItemValueContext valueContext : context.listItemValue()) {
            Value listItemValue = parseListItemValue(valueContext);
            listItemValue.line(valueContext.getStart().getLine());
            listItemValue.column(valueContext.getStart().getCharPositionInLine());

            listValue.getValues().add(listItemValue);
        }

        return listValue;
    }

    public static Value parseListItemValue(ListItemValueContext context) {
        if (context.stringValue() != null) {
            return parseStringValue(context.stringValue());
        } else {
            return parseReferenceValue(context.referenceValue());
        }
    }

    public static ReferenceValue parseReferenceValue(ReferenceValueContext context) {
        return new ReferenceValue(context.referenceBody());
    }

    public static StringExpressionValue parseStringExpressionValue(StringExpressionContext context) {
        StringExpressionValue value = new StringExpressionValue();

        StringBuilder sb = new StringBuilder();
        for (StringContentsContext contentsContext : context.stringContents()) {
            if (contentsContext.DOLLAR() != null) {
                sb.append(contentsContext.getText());
            } else if (contentsContext.LPAREN() != null) {
                sb.append(contentsContext.getText());
            } else if (contentsContext.TEXT() != null) {
                sb.append(contentsContext.getText());
            } else if (contentsContext.referenceBody() != null) {
                if (sb.length() > 0) {
                    StringValue string = new StringValue(sb.toString());
                    string.line(contentsContext.referenceBody().getStart().getLine());
                    string.column(contentsContext.referenceBody().getStart().getCharPositionInLine());
                    string.parent(value);
                    sb.setLength(0);

                    value.getValues().add(string);
                }

                ReferenceValue reference = new ReferenceValue(contentsContext.referenceBody());
                reference.line(contentsContext.referenceBody().getStart().getLine());
                reference.column(contentsContext.referenceBody().getStart().getCharPositionInLine());
                reference.parent(value);

                value.getValues().add(reference);
            }
        }

        if (sb.length() > 0) {
            StringValue string = new StringValue(sb.toString());
            string.parent(value);
            value.getValues().add(string);
        }

        return value;
    }

    public static Value parseStringValue(StringValueContext context) {
        Value value;
        if (context.stringExpression() != null) {
            value = parseStringExpressionValue(context.stringExpression());
        } else {
            value = new StringValue(context.getText());
        }

        value.line(context.start.getLine());
        value.column(context.start.getCharPositionInLine());

        return value;
    }

    public Resource createResource(String type) {
        Class klass = core.getResourceType(type);
        if (klass != null) {
            try {
                Resource resource = (Resource) klass.newInstance();
                resource.resourceType(type);
                resource.core(core);

                return resource;
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new BeamLanguageException("Unable to instantiate " + klass.getClass().getSimpleName());
            }
        }

        throw new BeamLanguageException("Unknown resource type: " + type);
    }

    public Resource createSubresource(Resource parent, String type) {
        String key = String.format("%s::%s", parent.resourceType(), type);
        return createResource(key);
    }

}
