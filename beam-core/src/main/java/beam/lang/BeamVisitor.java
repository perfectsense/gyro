package beam.lang;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.LocalStateBackend;
import beam.lang.types.BooleanValue;
import beam.lang.types.ListValue;
import beam.lang.types.MapValue;
import beam.lang.types.NumberValue;
import beam.lang.types.ReferenceValue;
import beam.lang.types.StringExpressionValue;
import beam.lang.types.StringValue;
import beam.lang.types.Value;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParser.Beam_rootContext;
import beam.parser.antlr4.BeamParser.Key_value_blockContext;
import beam.parser.antlr4.BeamParser.List_item_valueContext;
import beam.parser.antlr4.BeamParser.Provider_blockContext;
import beam.parser.antlr4.BeamParser.Reference_valueContext;
import beam.parser.antlr4.BeamParser.Resource_blockContext;
import beam.parser.antlr4.BeamParser.State_blockContext;
import beam.parser.antlr4.BeamParser.ValueContext;
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

    public BeamFile visitBeam_root(Beam_rootContext context) {
        BeamFile container = new BeamFile();
        container.path(getPath());

        for (BeamParser.File_blockContext blockContext : context.file_block()) {
            if (blockContext.key_value_block() != null) {
                String key = StringUtils.stripEnd(blockContext.key_value_block().key().getText(), ":");
                Value value = parseValue(blockContext.key_value_block().value());
                value.line(blockContext.key_value_block().getStart().getLine());
                value.column(blockContext.key_value_block().getStart().getCharPositionInLine());

                container.put(key, value);
            } else if (blockContext.resource_block() != null) {
                Resource block = visitResource_block(blockContext.resource_block(), container);

                container.putResource(block);
            } else if (blockContext.provider_block() != null) {
                Provider provider = visitProvider_block(blockContext.provider_block());
                container.providers().add(provider);
            } else if (blockContext.state_block() != null) {
                StateBackend stateBackend = visitState_block(blockContext.state_block());
                container.stateBackend(stateBackend);
            } else if (blockContext.import_block() != null) {
                String path = blockContext.import_block().import_path().getText();

                Path currentPath = new File(getPath()).getParentFile().toPath();
                Path importPath = new File(path).toPath();

                String resolvedPath = currentPath.resolve(importPath).toString();
                String importFileName = new File(path).getName().replace(".bcl", "");

                if (!resolvedPath.endsWith(".bcl") && !resolvedPath.endsWith(".bcl.state")) {
                    resolvedPath += ".bcl";
                }

                try {
                    String importName = blockContext.import_block().import_name() != null
                        ? blockContext.import_block().import_name().getText()
                        : importFileName;

                    BeamFile importedFileNode = core.parseImport(resolvedPath);
                    importedFileNode.path(resolvedPath);

                    container.putImport(importName, importedFileNode);
                } catch (IOException ioe) {
                    throw new BeamException("Failed to import '" + resolvedPath + "'");
                }
            } else if (blockContext.for_block() != null) {
                ForControl forNode = visitFor_block(blockContext.for_block(), container);
                container.putControlNode(forNode);
            }
        }

        return container;
    }

    public Provider visitProvider_block(Provider_blockContext context) {
        Provider provider = new Provider();
        provider.setName(context.provider_name().getText());
        provider.setCore(core);

        for (BeamParser.Key_simple_value_blockContext blockContext : context.provider_block_body().key_simple_value_block()) {
            String key = StringUtils.stripEnd(blockContext.key().getText(), ":");
            Value value = parseValue(blockContext.simple_value());

            if (key.equalsIgnoreCase("artifact")) {
                provider.setArtifact(value.getValue().toString());
            } else if (key.equalsIgnoreCase("repositories")) {
                provider.setRepositories(((ListValue) value).getValue());
            }
        }

        return provider;
    }

    public StateBackend visitState_block(State_blockContext context) {
        return new LocalStateBackend();
    }

    public Resource visitResource_block(Resource_blockContext context, Container parent) {
        Resource resourceBlock = createResourceBlock(context.resource_type().getText());
        resourceBlock.resourceType(context.resource_type().getText());
        resourceBlock.parent(parent);
        resourceBlock.line(context.getStart().getLine());
        resourceBlock.column(context.getStart().getCharPositionInLine());

        if (context.resource_name().string_value() != null) {
            Value idValue = parseStringValue(context.resource_name().string_value());
            if (idValue instanceof StringExpressionValue) {
                resourceBlock.resourceIdentifierExpression((StringExpressionValue) idValue);
            } else if (idValue instanceof StringValue) {
                resourceBlock.resourceIdentifier(((StringValue) idValue).getValue());
            }
        } else {
            resourceBlock.resourceIdentifier(context.resource_name().getText());
        }

        for (BeamParser.Resource_block_bodyContext blockContext : context.resource_block_body()) {
            if (blockContext.key_value_block() != null) {
                String key = StringUtils.stripEnd(blockContext.key_value_block().key().getText(), ":");
                Value value = parseValue(blockContext.key_value_block().value());
                value.line(blockContext.key_value_block().getStart().getLine());
                value.column(blockContext.key_value_block().getStart().getCharPositionInLine());

                resourceBlock.put(key, value);
            } else if (blockContext.subresource_block() != null) {
                Resource node = visitSubresource_block(blockContext.subresource_block(), resourceBlock);
                String type = blockContext.subresource_block().resource_type().getText();
                resourceBlock.putSubresource(type, node);
            } else if (blockContext.for_block() != null) {
                ForControl forNode = visitFor_block(blockContext.for_block(), resourceBlock);
                resourceBlock.putControlNode(forNode);
            }
        }

        resourceBlock.executeInternal();

        return resourceBlock;
    }

    public Resource visitSubresource_block(BeamParser.Subresource_blockContext context, Resource parent) {
        Resource resourceBlock = createSubResourceBlock(parent, context.resource_type().getText());
        resourceBlock.resourceType(context.resource_type().getText());
        resourceBlock.parent(parent);
        resourceBlock.line(context.getStart().getLine());
        resourceBlock.column(context.getStart().getCharPositionInLine());

        if (context.resource_name() != null) {
            resourceBlock.resourceIdentifier(context.resource_name().getText());
        }

        for (BeamParser.Subresource_block_bodyContext blockContext : context.subresource_block_body()) {
            if (blockContext.key_value_block() != null) {
                String key = StringUtils.stripEnd(blockContext.key_value_block().key().getText(), ":");
                Value value = parseValue(blockContext.key_value_block().value());
                value.line(blockContext.key_value_block().getStart().getLine());
                value.column(blockContext.key_value_block().getStart().getCharPositionInLine());

                resourceBlock.put(key, value);
            }
        }

        resourceBlock.executeInternal();

        return resourceBlock;
    }

    public ForControl visitFor_block(BeamParser.For_blockContext context, Node parent) {
        ForControl forNode = new ForControl();
        forNode.line(context.getStart().getLine());
        forNode.column(context.getStart().getCharPositionInLine());
        forNode.parent(parent);

        for (BeamParser.For_list_itemContext itemContext : context.for_list().for_list_item()) {
            forNode.variables().add(itemContext.IDENTIFIER().getText());
        }

        for (BeamParser.List_item_valueContext valueContext : context.list_value().list_item_value()) {
            Value value = null;

            if (valueContext.string_value() != null) {
                value = parseStringValue(valueContext.string_value());
            } else if (valueContext.reference_value() != null) {
                value = parseReferenceValue(valueContext.reference_value());
            } else if (valueContext.number_value() != null) {
                value = new NumberValue(valueContext.number_value().getText());
            } else if (valueContext.boolean_value() != null) {
                value = new BooleanValue(valueContext.boolean_value().getText());
            }

            if (value != null) {
                value.line(valueContext.getStart().getLine());
                value.column(valueContext.getStart().getCharPositionInLine());
                value.parent(forNode);

                forNode.listValues().add(value);
            }
        }

        for (BeamParser.For_block_bodyContext blockContext : context.for_block_body()) {
            if (blockContext.key_value_block() != null) {
                String key = StringUtils.stripEnd(blockContext.key_value_block().key().getText(), ":");
                Value value = parseValue(blockContext.key_value_block().value());
                value.line(blockContext.key_value_block().getStart().getLine());
                value.column(blockContext.key_value_block().getStart().getCharPositionInLine());

                forNode.put(key, value);
            } else if (blockContext.resource_block() != null) {
                if (!(parent instanceof ResourceContainer)) {
                    throw new BeamLanguageException("Resource is not valid.");
                }

                Resource block = visitResource_block(blockContext.resource_block(), forNode);
                forNode.putResource(block);
            } else if (blockContext.subresource_block() != null) {
                if (!(parent instanceof Resource)) {
                    throw new BeamLanguageException("Subresource is not valid.");
                }

                Resource subresourceNode = visitSubresource_block(blockContext.subresource_block(), (Resource) parent);
                forNode.putSubResource(subresourceNode);
            }
        }

        return forNode;
    }

    public Value parseValue(BeamParser.Simple_valueContext context) {
        Value value = null;

        if (context.boolean_value() != null) {
            value = new BooleanValue(context.boolean_value().getText());
        } else if (context.number_value() != null) {
            value = new NumberValue(context.number_value().getText());
        } else if (context.STRING_LITERAL() != null) {
            value = new StringValue(context.STRING_LITERAL().getText());
        } else if (context.list_value() != null) {
            value = parseListValue(context.list_value());
        } else if (context.map_value() != null) {
            value = parseMapNode(context.map_value());
        }

        value.line(context.start.getLine());
        value.column(context.start.getCharPositionInLine());

        return value;
    }

    public Value parseValue(ValueContext context) {
        Value value = null;

        if (context.boolean_value() != null) {
            value = new BooleanValue(context.boolean_value().getText());
        } else if (context.number_value() != null) {
            value = new NumberValue(context.number_value().getText());
        } else if (context.reference_value() != null) {
            value = parseReferenceValue(context.reference_value());
        } else if (context.string_value() != null) {
            value = parseStringValue(context.string_value());
        } else if (context.list_value() != null) {
            value = parseListValue(context.list_value());
        } else if (context.map_value() != null) {
            value = parseMapNode(context.map_value());
        }

        value.line(context.start.getLine());
        value.column(context.start.getCharPositionInLine());

        return value;
    }

    public MapValue parseMapNode(BeamParser.Map_valueContext context) {
        MapValue mapValue = new MapValue();
        for (Key_value_blockContext valueContext : context.key_value_block()) {
            String key = StringUtils.stripEnd(valueContext.key().getText(), ":");
            Value value = parseValue(valueContext.value());
            value.line(valueContext.getStart().getLine());
            value.column(valueContext.getStart().getCharPositionInLine());

            mapValue.getKeyValues().put(key, value);
        }

        return mapValue;
    }

    public ListValue parseListValue(BeamParser.List_valueContext context) {
        ListValue listValue = new ListValue();
        for (List_item_valueContext valueContext : context.list_item_value()) {
            Value listItemValue = parseListItemValue(valueContext);
            listItemValue.line(valueContext.getStart().getLine());
            listItemValue.column(valueContext.getStart().getCharPositionInLine());

            listValue.getValues().add(listItemValue);
        }

        return listValue;
    }

    public Value parseListItemValue(List_item_valueContext context) {
        if (context.string_value() != null) {
            return parseStringValue(context.string_value());
        } else {
            return parseReferenceValue(context.reference_value());
        }
    }

    public ReferenceValue parseReferenceValue(Reference_valueContext context) {
        return new ReferenceValue(context.reference_body());
    }

    public static StringExpressionValue parseStringExpressionValue(BeamParser.String_expressionContext context) {
        StringExpressionValue value = new StringExpressionValue();

        StringBuilder sb = new StringBuilder();
        for (BeamParser.String_contentsContext contentsContext : context.string_contents()) {
            if (contentsContext.DOLLAR() != null) {
                sb.append(contentsContext.getText());
            } else if (contentsContext.LPAREN() != null) {
                sb.append(contentsContext.getText());
            } else if (contentsContext.TEXT() != null) {
                sb.append(contentsContext.getText());
            } else if (contentsContext.reference_body() != null) {
                if (sb.length() > 0) {
                    StringValue string = new StringValue(sb.toString());
                    string.line(contentsContext.reference_body().getStart().getLine());
                    string.column(contentsContext.reference_body().getStart().getCharPositionInLine());
                    string.parent(value);
                    sb.setLength(0);

                    value.getValues().add(string);
                }

                ReferenceValue reference = new ReferenceValue(contentsContext.reference_body());
                reference.line(contentsContext.reference_body().getStart().getLine());
                reference.column(contentsContext.reference_body().getStart().getCharPositionInLine());
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

    public static Value parseStringValue(BeamParser.String_valueContext context) {
        Value value;
        if (context.string_expression() != null) {
            value = parseStringExpressionValue(context.string_expression());
        } else {
            value = new StringValue(context.getText());
        }

        value.line(context.start.getLine());
        value.column(context.start.getCharPositionInLine());

        return value;
    }

    public Resource createResourceBlock(String type) {
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

        return null;
    }

    public Resource createSubResourceBlock(Resource parent, String type) {
        String key = String.format("%s::%s", parent.resourceType(), type);
        return createResourceBlock(key);
    }

}
