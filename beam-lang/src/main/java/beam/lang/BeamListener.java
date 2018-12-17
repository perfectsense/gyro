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
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParserBaseListener;
import org.apache.commons.lang.StringUtils;

public class BeamListener extends BeamParserBaseListener {

    private BeamInterp interp;

    private ContainerBlock parentBlock;
    private ContainerBlock currentBlock;

    public ContainerBlock getRootBlock() {
        return currentBlock;
    }

    public BeamListener(BeamInterp interp) {
        this.interp = interp;
    }

    @Override
    public void enterBeam_root(BeamParser.Beam_rootContext ctx) {
        parentBlock = null;
        currentBlock = new ContainerBlock();
    }

    @Override
    public void enterResource_block(BeamParser.Resource_blockContext context) {
        ResourceBlock resourceBlock = parseResourceBlock(context);
        currentBlock.setParentBlock(currentBlock);
        currentBlock.getBlocks().add(resourceBlock);

        parentBlock = currentBlock;
        currentBlock = resourceBlock;
    }

    @Override
    public void exitResource_block(BeamParser.Resource_blockContext ctx) {
        currentBlock = parentBlock;
    }

    @Override
    public void enterKey_value_block(BeamParser.Key_value_blockContext context) {
        currentBlock.getBlocks().add(parseKeyValueBlock(context));
    }

    public BeamValue parseValue(BeamParser.ValueContext context) {
        if (context.boolean_value() != null) {
            return new BeamBoolean(context.boolean_value().getText());
        } else if (context.number_value() != null) {
            return new BeamNumber(context.number_value().getText());
        } else if (context.reference_value() != null) {
            return parseReferenceValue(context.reference_value());
        } else if (context.literal_value() != null) {
            return parseLiteralValue(context.literal_value());
        } else if (context.list_value() != null) {
            BeamList listValue = new BeamList();
            for (BeamParser.List_item_valueContext valueContext : context.list_value().list_item_value()) {
                listValue.getValues().add(parseListItemValue(valueContext));
            }

            return listValue;
        } else if (context.map_value() != null) {
            BeamMap mapValue = new BeamMap();
            for (BeamParser.Key_value_blockContext valueContext : context.map_value().key_value_block()) {
                mapValue.getKeyValues().add(parseKeyValueBlock(valueContext));
            }

            return mapValue;
        }

        return null;
    }

    public BeamValue parseListItemValue(BeamParser.List_item_valueContext context) {
        if (context.literal_value() != null) {
            return parseLiteralValue(context.literal_value());
        } else {
            return parseReferenceValue(context.reference_value());
        }
    }

    public BeamReference parseReferenceValue(BeamParser.Reference_valueContext context) {
        return new BeamReference(
            context.reference_body().reference_type().getText(),
            context.reference_body().reference_name().getText(),
            context.reference_body().reference_attribute().getText()
        );
    }

    public BeamLiteral parseLiteralValue(BeamParser.Literal_valueContext context) {
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

    public ResourceBlock parseResourceBlock(BeamParser.Resource_blockContext context) {
        ResourceBlock resourceBlock = createResourceBlock(context.resource_type().getText());
        resourceBlock.setName(context.resource_name().getText());

        return resourceBlock;
    }

    public KeyValueBlock parseKeyValueBlock(BeamParser.Key_value_blockContext context) {
        KeyValueBlock keyValueBlock = new KeyValueBlock();
        keyValueBlock.setKey(StringUtils.stripEnd(context.key().getText(), ":"));
        keyValueBlock.setValue(parseValue(context.value()));

        return keyValueBlock;
    }

    public ResourceBlock createResourceBlock(String type) {
        Class klass = interp.getExtension(type);
        if (klass != null) {
            try {
                BeamLanguageExtension block = (BeamLanguageExtension) klass.newInstance();
                block.setType(type);
                block.setInterp(interp);
                block.execute();

                return block;
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new BeamLangException("Unable to instantiate " + klass.getClass().getSimpleName());
            }
        }

        return new ResourceBlock();
    }

}
