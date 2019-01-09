package beam.lang;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParser.Beam_rootContext;
import beam.parser.antlr4.BeamParser.Key_value_blockContext;
import beam.parser.antlr4.BeamParser.List_item_valueContext;
import beam.parser.antlr4.BeamParser.Reference_valueContext;
import beam.parser.antlr4.BeamParser.Resource_blockContext;
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

    public FileNode visitBeam_root(Beam_rootContext context) {
        FileNode containerNode = new FileNode();
        containerNode.setPath(getPath());

        for (BeamParser.File_blockContext blockContext : context.file_block()) {
            if (blockContext.key_value_block() != null) {
                String key = StringUtils.stripEnd(blockContext.key_value_block().key().getText(), ":");
                ValueNode valueNode = parseValue(blockContext.key_value_block().value());
                valueNode.setLine(blockContext.key_value_block().getStart().getLine());
                valueNode.setColumn(blockContext.key_value_block().getStart().getCharPositionInLine());

                containerNode.put(key, valueNode);
            } else if (blockContext.resource_block() != null) {
                ResourceNode block = visitResource_block(blockContext.resource_block(), containerNode);

                containerNode.putResource(block);
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

                    FileNode importedFileNode = core.parseImport(resolvedPath);
                    importedFileNode.setPath(resolvedPath);

                    containerNode.putImport(importName, importedFileNode);
                } catch (IOException ioe) {
                    throw new BeamException("Failed to import '" + resolvedPath + "'");
                }
            } else if (blockContext.for_block() != null) {
                ForNode forNode = visitFor_block(blockContext.for_block(), containerNode);
                containerNode.putControlNode(forNode);
            }
        }

        return containerNode;
    }

    public ResourceNode visitResource_block(Resource_blockContext context, ContainerNode parent) {
        ResourceNode resourceBlock = createResourceBlock(context.resource_type().getText());
        resourceBlock.setResourceType(context.resource_type().getText());
        resourceBlock.setParentNode(parent);
        resourceBlock.setLine(context.getStart().getLine());
        resourceBlock.setColumn(context.getStart().getCharPositionInLine());

        if (context.resource_name().string_value() != null) {
            ValueNode idValue = parseStringValue(context.resource_name().string_value());
            if (idValue instanceof StringExpressionNode) {
                resourceBlock.setResourceIdentifierExpression((StringExpressionNode) idValue);
            } else if (idValue instanceof StringNode) {
                resourceBlock.setResourceIdentifier(((StringNode) idValue).getValue());
            }
        } else {
            resourceBlock.setResourceIdentifier(context.resource_name().getText());
        }

        for (BeamParser.Resource_block_bodyContext blockContext : context.resource_block_body()) {
            if (blockContext.key_value_block() != null) {
                String key = StringUtils.stripEnd(blockContext.key_value_block().key().getText(), ":");
                ValueNode valueNode = parseValue(blockContext.key_value_block().value());
                valueNode.setLine(blockContext.key_value_block().getStart().getLine());
                valueNode.setColumn(blockContext.key_value_block().getStart().getCharPositionInLine());

                resourceBlock.put(key, valueNode);
            } else if (blockContext.subresource_block() != null) {
                ResourceNode node = visitSubresource_block(blockContext.subresource_block(), resourceBlock);
                String type = blockContext.subresource_block().resource_type().getText();
                resourceBlock.putSubresource(type, node);
            } else if (blockContext.for_block() != null) {
                ForNode forNode = visitFor_block(blockContext.for_block(), resourceBlock);
                resourceBlock.putControlNode(forNode);
            }
        }

        resourceBlock.executeInternal();

        return resourceBlock;
    }

    public ResourceNode visitSubresource_block(BeamParser.Subresource_blockContext context, ResourceNode parent) {
        ResourceNode resourceBlock = createSubResourceBlock(parent, context.resource_type().getText());
        resourceBlock.setResourceType(context.resource_type().getText());
        resourceBlock.setParentNode(parent);
        resourceBlock.setLine(context.getStart().getLine());
        resourceBlock.setColumn(context.getStart().getCharPositionInLine());

        if (context.resource_name() != null) {
            resourceBlock.setResourceIdentifier(context.resource_name().getText());
        }

        for (BeamParser.Subresource_block_bodyContext blockContext : context.subresource_block_body()) {
            if (blockContext.key_value_block() != null) {
                String key = StringUtils.stripEnd(blockContext.key_value_block().key().getText(), ":");
                ValueNode valueNode = parseValue(blockContext.key_value_block().value());
                valueNode.setLine(blockContext.key_value_block().getStart().getLine());
                valueNode.setColumn(blockContext.key_value_block().getStart().getCharPositionInLine());

                resourceBlock.put(key, valueNode);
            }
        }

        resourceBlock.executeInternal();

        return resourceBlock;
    }

    public ForNode visitFor_block(BeamParser.For_blockContext context, Node parent) {
        ForNode forNode = new ForNode();
        forNode.setLine(context.getStart().getLine());
        forNode.setColumn(context.getStart().getCharPositionInLine());
        forNode.setParentNode(parent);

        for (BeamParser.For_list_itemContext itemContext : context.for_list().for_list_item()) {
            forNode.variables().add(itemContext.IDENTIFIER().getText());
        }

        for (BeamParser.List_item_valueContext valueContext : context.list_value().list_item_value()) {
            ValueNode valueNode = null;

            if (valueContext.string_value() != null) {
                valueNode = parseStringValue(valueContext.string_value());
            } else if (valueContext.reference_value() != null) {
                valueNode = parseReferenceValue(valueContext.reference_value());
            } else if (valueContext.number_value() != null) {
                valueNode = new NumberNode(valueContext.number_value().getText());
            } else if (valueContext.boolean_value() != null) {
                valueNode = new BooleanNode(valueContext.boolean_value().getText());
            }

            if (valueNode != null) {
                valueNode.setLine(valueContext.getStart().getLine());
                valueNode.setColumn(valueContext.getStart().getCharPositionInLine());
                valueNode.setParentNode(forNode);

                forNode.listValues().add(valueNode);
            }
        }

        for (BeamParser.For_block_bodyContext blockContext : context.for_block_body()) {
            if (blockContext.key_value_block() != null) {
                String key = StringUtils.stripEnd(blockContext.key_value_block().key().getText(), ":");
                ValueNode valueNode = parseValue(blockContext.key_value_block().value());
                valueNode.setLine(blockContext.key_value_block().getStart().getLine());
                valueNode.setColumn(blockContext.key_value_block().getStart().getCharPositionInLine());

                forNode.put(key, valueNode);
            } else if (blockContext.resource_block() != null) {
                if (!(parent instanceof ResourceContainerNode)) {
                    throw new BeamLanguageException("Resource is not valid.");
                }

                ResourceNode block = visitResource_block(blockContext.resource_block(), forNode);
                forNode.putResource(block);
            } else if (blockContext.subresource_block() != null) {
                if (!(parent instanceof ResourceNode)) {
                    throw new BeamLanguageException("Subresource is not valid.");
                }

                ResourceNode subresourceNode = visitSubresource_block(blockContext.subresource_block(), (ResourceNode) parent);
                forNode.putSubResource(subresourceNode);
            }
        }

        return forNode;
    }

    public ValueNode parseValue(ValueContext context) {
        ValueNode value = null;

        if (context.boolean_value() != null) {
            value = new BooleanNode(context.boolean_value().getText());
        } else if (context.number_value() != null) {
            value = new NumberNode(context.number_value().getText());
        } else if (context.reference_value() != null) {
            value = parseReferenceValue(context.reference_value());
        } else if (context.string_value() != null) {
            value = parseStringValue(context.string_value());
        } else if (context.list_value() != null) {
            ListNode listValue = new ListNode();
            for (List_item_valueContext valueContext : context.list_value().list_item_value()) {
                ValueNode listItemValue = parseListItemValue(valueContext);
                listItemValue.setLine(valueContext.getStart().getLine());
                listItemValue.setColumn(valueContext.getStart().getCharPositionInLine());

                listValue.getValues().add(listItemValue);
            }

            value = listValue;
        } else if (context.map_value() != null) {
            MapNode mapValue = new MapNode();
            for (Key_value_blockContext valueContext : context.map_value().key_value_block()) {
                String key = StringUtils.stripEnd(valueContext.key().getText(), ":");
                ValueNode valueNode = parseValue(valueContext.value());
                valueNode.setLine(valueContext.getStart().getLine());
                valueNode.setColumn(valueContext.getStart().getCharPositionInLine());

                mapValue.getKeyValues().put(key, valueNode);
            }

            value = mapValue;
        }

        value.setLine(context.start.getLine());
        value.setColumn(context.start.getCharPositionInLine());

        return value;
    }

    public ValueNode parseListItemValue(List_item_valueContext context) {
        if (context.string_value() != null) {
            return parseStringValue(context.string_value());
        } else {
            return parseReferenceValue(context.reference_value());
        }
    }

    public ReferenceNode parseReferenceValue(Reference_valueContext context) {
        return new ReferenceNode(context.reference_body());
    }

    public static StringExpressionNode parseStringExpressionValue(BeamParser.String_expressionContext context) {
        StringExpressionNode value = new StringExpressionNode();

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
                    StringNode string = new StringNode(sb.toString());
                    string.setLine(contentsContext.reference_body().getStart().getLine());
                    string.setColumn(contentsContext.reference_body().getStart().getCharPositionInLine());
                    string.setParentNode(value);
                    sb.setLength(0);

                    value.getValueNodes().add(string);
                }

                ReferenceNode reference = new ReferenceNode(contentsContext.reference_body());
                reference.setLine(contentsContext.reference_body().getStart().getLine());
                reference.setColumn(contentsContext.reference_body().getStart().getCharPositionInLine());
                reference.setParentNode(value);

                value.getValueNodes().add(reference);
            }
        }

        if (sb.length() > 0) {
            StringNode string = new StringNode(sb.toString());
            string.setParentNode(value);
            value.getValueNodes().add(string);
        }

        return value;
    }

    public static ValueNode parseStringValue(BeamParser.String_valueContext context) {
        ValueNode value;
        if (context.string_expression() != null) {
            value = parseStringExpressionValue(context.string_expression());
        } else {
            value = new StringNode(context.getText());
        }

        value.setLine(context.start.getLine());
        value.setColumn(context.start.getCharPositionInLine());

        return value;
    }

    public ResourceNode createResourceBlock(String type) {
        Class klass = core.getResourceType(type);
        if (klass != null) {
            try {
                ResourceNode node = (ResourceNode) klass.newInstance();
                node.setResourceType(type);
                node.setCore(core);

                return node;
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new BeamLanguageException("Unable to instantiate " + klass.getClass().getSimpleName());
            }
        }

        return new ResourceNode();
    }

    public ResourceNode createSubResourceBlock(ResourceNode parent, String type) {
        String key = String.format("%s::%s", parent.resourceType(), type);
        return createResourceBlock(key);
    }

}
