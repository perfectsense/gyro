package beam.lang.nodes;

import beam.core.BeamCore;
import beam.lang.BeamLanguageException;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParser.Beam_rootContext;
import beam.parser.antlr4.BeamParser.Key_value_blockContext;
import beam.parser.antlr4.BeamParser.List_item_valueContext;
import beam.parser.antlr4.BeamParser.Literal_valueContext;
import beam.parser.antlr4.BeamParser.Reference_valueContext;
import beam.parser.antlr4.BeamParser.Resource_blockContext;
import beam.parser.antlr4.BeamParser.ValueContext;
import beam.parser.antlr4.BeamParserBaseVisitor;
import org.apache.commons.lang.StringUtils;

public class BeamVisitor extends BeamParserBaseVisitor {

    private BeamCore core;

    public BeamVisitor(BeamCore core) {
        this.core = core;
    }

    public RootNode visitBeam_root(Beam_rootContext context) {
        RootNode containerNode = new RootNode();

        for (BeamParser.Root_blockContext blockContext : context.root_block()) {
            if (blockContext.key_value_block() != null) {
                String key = StringUtils.stripEnd(blockContext.key_value_block().key().getText(), ":");
                ValueNode valueNode = parseValue(blockContext.key_value_block().value());
                valueNode.setLine(blockContext.key_value_block().getStart().getLine());
                valueNode.setColumn(blockContext.key_value_block().getStart().getCharPositionInLine());

                containerNode.put(key, valueNode);
            } else if (blockContext.resource_block() != null) {
                ResourceNode block = visitResource_block(blockContext.resource_block(), containerNode);

                containerNode.putResource(block);
            }
        }

        return containerNode;
    }

    public ResourceNode visitResource_block(Resource_blockContext context, ContainerNode parent) {
        ResourceNode resourceBlock = createResourceBlock(context.resource_type().getText());
        resourceBlock.setResourceIdentifier(context.resource_name().getText());
        resourceBlock.setResourceType(context.resource_type().getText());
        resourceBlock.setParentNode(parent);
        resourceBlock.setLine(context.getStart().getLine());
        resourceBlock.setColumn(context.getStart().getCharPositionInLine());

        for (BeamParser.Resource_block_bodyContext blockContext : context.resource_block_body()) {
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

    public ValueNode parseValue(ValueContext context) {
        ValueNode value = null;

        if (context.boolean_value() != null) {
            value = new BooleanNode(context.boolean_value().getText());
        } else if (context.number_value() != null) {
            value = new NumberNode(context.number_value().getText());
        } else if (context.reference_value() != null) {
            value = parseReferenceValue(context.reference_value());
        } else if (context.literal_value() != null) {
            value = parseLiteralValue(context.literal_value());
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
        if (context.literal_value() != null) {
            return parseLiteralValue(context.literal_value());
        } else {
            return parseReferenceValue(context.reference_value());
        }
    }

    public ReferenceNode parseReferenceValue(Reference_valueContext context) {
        if (context.reference_body().reference_attribute() != null) {
            return new ReferenceNode(
                context.reference_body().reference_type().getText(),
                context.reference_body().reference_name().getText(),
                context.reference_body().reference_attribute().getText()
            );
        } else {
            return new ReferenceNode(
                context.reference_body().reference_type().getText(),
                context.reference_body().reference_name().getText()
            );
        }
    }

    public LiteralNode parseLiteralValue(Literal_valueContext context) {
        LiteralNode literal;
        if (context.STRING_INTEPRETED() != null) {
            literal = new StringExpressionNode(context.getText());
        } else {
            literal = new StringNode(context.getText());
        }

        literal.setLine(context.start.getLine());
        literal.setColumn(context.start.getCharPositionInLine());

        return literal;
    }

    public ResourceNode createResourceBlock(String type) {
        Class klass = core.getExtension(type);
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
}
