package beam.lang;

import beam.lang.types.BeamBoolean;
import beam.lang.types.BeamList;
import beam.lang.types.BeamLiteral;
import beam.lang.types.BeamMap;
import beam.lang.types.BeamNumber;
import beam.lang.types.BeamReference;
import beam.lang.types.BeamString;
import beam.lang.types.BeamStringExpression;
import beam.lang.types.BeamValue;
import beam.lang.types.ContainerBlock;
import beam.lang.types.KeyValueBlock;
import beam.lang.types.ResourceBlock;
import beam.parser.antlr4.BeamParser.Beam_rootContext;
import beam.parser.antlr4.BeamParser.BlockContext;
import beam.parser.antlr4.BeamParser.Key_value_blockContext;
import beam.parser.antlr4.BeamParser.List_item_valueContext;
import beam.parser.antlr4.BeamParser.Literal_valueContext;
import beam.parser.antlr4.BeamParser.Reference_valueContext;
import beam.parser.antlr4.BeamParser.Resource_blockContext;
import beam.parser.antlr4.BeamParser.ValueContext;
import beam.parser.antlr4.BeamParserBaseVisitor;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class BeamVisitor extends BeamParserBaseVisitor {

    private BeamInterp interp;

    public BeamVisitor(BeamInterp interp) {
        this.interp = interp;
    }

    public ContainerBlock visitBeam_root(Beam_rootContext context) {
        ContainerBlock containerBlock = new ContainerBlock();

        parseContainerBlockChildren(containerBlock, context.block());

        return containerBlock;
    }

    public ResourceBlock visitResource_block(Resource_blockContext context, ContainerBlock parent) {
        ResourceBlock resourceBlock = createResourceBlock(context.resource_type().getText());
        resourceBlock.setResourceIdentifier(context.resource_name().getText());
        resourceBlock.setResourceType(context.resource_type().getText());
        resourceBlock.setParentBlock(parent);
        resourceBlock.setLine(context.getStart().getLine());
        resourceBlock.setColumn(context.getStart().getCharPositionInLine());

        parseContainerBlockChildren(resourceBlock, context.block());

        if (resourceBlock instanceof BeamLanguageExtension) {
            BeamLanguageExtension extension = (BeamLanguageExtension) resourceBlock;
            extension.execute();
        }

        return resourceBlock;
    }

    public void parseContainerBlockChildren(ContainerBlock containerBlock, List<BlockContext> blocks) {
        for (BlockContext blockContext : blocks) {
            if (blockContext.key_value_block() != null) {
                KeyValueBlock block = parseKeyValueBlock(blockContext.key_value_block());

                containerBlock.putKeyValue(block);
            } else if (blockContext.resource_block() != null) {
                ResourceBlock block = visitResource_block(blockContext.resource_block(), containerBlock);

                containerBlock.putResource(block);
            }
        }

    }

    public BeamValue parseValue(ValueContext context) {
        BeamValue value = null;

        if (context.boolean_value() != null) {
            value = new BeamBoolean(context.boolean_value().getText());
        } else if (context.number_value() != null) {
            value = new BeamNumber(context.number_value().getText());
        } else if (context.reference_value() != null) {
            value = parseReferenceValue(context.reference_value());
        } else if (context.literal_value() != null) {
            value = parseLiteralValue(context.literal_value());
        } else if (context.list_value() != null) {
            BeamList listValue = new BeamList();
            for (List_item_valueContext valueContext : context.list_value().list_item_value()) {
                BeamValue listItemValue = parseListItemValue(valueContext);
                listItemValue.setLine(valueContext.getStart().getLine());
                listItemValue.setColumn(valueContext.getStart().getCharPositionInLine());

                listValue.getValues().add(listItemValue);
            }

            value = listValue;
        } else if (context.map_value() != null) {
            BeamMap mapValue = new BeamMap();
            for (Key_value_blockContext valueContext : context.map_value().key_value_block()) {
                mapValue.getKeyValues().add(parseKeyValueBlock(valueContext));
            }

            value = mapValue;
        }

        value.setLine(context.start.getLine());
        value.setColumn(context.start.getCharPositionInLine());

        return value;
    }

    public BeamValue parseListItemValue(List_item_valueContext context) {
        if (context.literal_value() != null) {
            return parseLiteralValue(context.literal_value());
        } else {
            return parseReferenceValue(context.reference_value());
        }
    }

    public BeamReference parseReferenceValue(Reference_valueContext context) {
        if (context.reference_body().reference_attribute() != null) {
            return new BeamReference(
                context.reference_body().reference_type().getText(),
                context.reference_body().reference_name().getText(),
                context.reference_body().reference_attribute().getText()
            );
        } else {
            return new BeamReference(
                context.reference_body().reference_type().getText(),
                context.reference_body().reference_name().getText()
            );
        }
    }

    public BeamLiteral parseLiteralValue(Literal_valueContext context) {
        BeamLiteral literal;
        if (context.STRING_INTEPRETED() != null) {
            literal = new BeamStringExpression(context.getText());
        } else {
            literal = new BeamString(context.getText());
        }

        literal.setLine(context.start.getLine());
        literal.setColumn(context.start.getCharPositionInLine());

        return literal;
    }

    public KeyValueBlock parseKeyValueBlock(Key_value_blockContext context) {
        KeyValueBlock keyValueBlock = new KeyValueBlock();
        keyValueBlock.setKey(StringUtils.stripEnd(context.key().getText(), ":"));
        keyValueBlock.setValue(parseValue(context.value()));
        keyValueBlock.setLine(context.getStart().getLine());
        keyValueBlock.setColumn(context.getStart().getCharPositionInLine());

        return keyValueBlock;
    }

    public ResourceBlock createResourceBlock(String type) {
        Class klass = interp.getExtension(type);
        if (klass != null) {
            try {
                BeamLanguageExtension block = (BeamLanguageExtension) klass.newInstance();
                block.setResourceType(type);
                block.setInterp(interp);

                return block;
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new BeamLanguageException("Unable to instantiate " + klass.getClass().getSimpleName());
            }
        }

        return new ResourceBlock();
    }
}
