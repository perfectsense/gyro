package beam.lang;

import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParserBaseListener;

import java.util.List;

public class BeamListener extends BeamParserBaseListener {

    private String configName;
    private BeamBlock parent;
    private BeamBlock config;
    private BeamInterp interp;

    public BeamListener(BeamInterp interp, String configName) {
        this.interp = interp;
        this.configName = configName;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public BeamBlock getConfig() {
        return config;
    }

    @Override
    public void enterBeamRoot(BeamParser.BeamRootContext ctx) {
        parent = null;
        config = new BeamBlock();
    }

    @Override
    public void exitBeamRoot(BeamParser.BeamRootContext ctx) {
    }

    @Override
    public void enterBlockBody(BeamParser.BlockBodyContext blockBody) {
        parent = config;

        String type = blockBody.blockType().getText();
        config = interp.createConfig(type);
        config.setCtx(blockBody);

        for (BeamParser.ParamContext paramContext : blockBody.param()) {
            if (paramContext.literal() != null) {
                config.getParams().add(BeamListener.parseLiteral(paramContext.literal()));
            } else if (paramContext.inlineList() != null) {
                config.getParams().add(BeamListener.parseInlineList(paramContext.inlineList()));
            }
        }
    }

    @Override
    public void exitBlockBody(BeamParser.BlockBodyContext ctx) {
        parent.getChildren().add(config);
        config = parent;
    }

    @Override
    public void exitKeyValue(BeamParser.KeyValueContext keyValue) {
        BeamValue beamValue = BeamListener.parseValue(keyValue.value());
        beamValue.setLine(keyValue.getStart().getLine());

        config.add(new BeamContextKey(keyValue.key().getText()), beamValue);
    }

    public static BeamValue parseValue(BeamParser.ValueContext valueContext) {
        if (valueContext.map() != null) {
            return parseMap(valueContext.map());
        } else if (valueContext.list() != null) {
            return parseList(valueContext.list());
        } else if (valueContext.scalar() != null) {
            return parseScalar(valueContext.scalar());
        } else if (valueContext.inlineList() != null) {
            return parseInlineList(valueContext.inlineList());
        } else {
            throw new IllegalStateException();
        }
    }

    public static BeamMap parseMap(BeamParser.MapContext mapContext) {
        BeamMap result = new BeamMap();
        for (BeamParser.KeyValueContext pairContext : mapContext.keyValue()) {
            String key = pairContext.key().getText();
            BeamParser.ValueContext value = pairContext.value();
            result.getMap().put(key, parseValue(value));
        }

        return result;
    }

    public static BeamList parseList(BeamParser.ListContext listContext) {
        BeamList result = new BeamList();
        for (BeamParser.ListEntryContext listEntryContext : listContext.listEntry()) {
            BeamParser.ScalarContext item = listEntryContext.scalar();
            result.getList().add(parseScalar(item));
        }

        return result;
    }

    public static BeamInlineList parseInlineList(BeamParser.InlineListContext listContext) {
        BeamInlineList result = new BeamInlineList();
        for (BeamParser.ScalarContext scalarContext : listContext.scalar()) {
            result.getList().add(parseScalar(scalarContext));
        }

        return result;
    }

    public static BeamScalar parseScalar(BeamParser.ScalarContext scalarContext) {
        BeamScalar beamScalar = new BeamScalar();
        beamScalar.getElements().add(parseLiteral(scalarContext.firstLiteral().literal()));
        if (scalarContext.restLiteral() != null) {
            for (BeamParser.RestLiteralContext literalContext : scalarContext.restLiteral()) {
                if (literalContext.literal() != null) {
                    beamScalar.getElements().add(parseLiteral(literalContext.literal()));

                } else if (literalContext.DASH() != null) {
                    beamScalar.getElements().add(new BeamLiteral(literalContext.DASH().getText()));
                }
            }
        }

        return beamScalar;
    }

    public static BeamLiteral parseLiteral(BeamParser.LiteralContext literalContext) {
        if (literalContext.reference() != null) {
            return parseReference(literalContext.reference());
        } else {
            String text = literalContext.getText();
            if (literalContext.QUOTED_STRING() != null) {
                text = stripQuotes(text);
            }

            return new BeamLiteral(text);
        }
    }

    public static BeamReference parseReference(BeamParser.ReferenceContext referenceContext) {
        BeamReference reference = new BeamReference();
        List<BeamParser.ReferenceScopeContext> scopeContexts = referenceContext.referenceScope();
        if (scopeContexts != null) {
            for (BeamParser.ReferenceScopeContext scopeContext : scopeContexts) {
                String id = scopeContext.referenceId().getText();
                String type = scopeContext.referenceType() != null ? scopeContext.referenceType().getText() : null;
                reference.getScopeChain().add(new BeamContextKey(id, type));
            }
        }

        BeamParser.ReferenceNameContext nameContext = referenceContext.referenceName();
        if (nameContext.referenceScope() != null) {
            String id = nameContext.referenceScope().referenceId().getText();
            String type = nameContext.referenceScope().referenceType() != null ? nameContext.referenceScope().referenceType().getText() : null;
            reference.getScopeChain().add(new BeamContextKey(id, type));
        } else if (nameContext.referenceChain() != null) {
            reference.setReferenceChain(reference.parseReferenceChain(nameContext.referenceChain().getText()));
        }

        return reference;
    }

    private static String stripQuotes(String string) {
        return string.replaceAll("^[\"\']|[\"\']$", "");
    }
}
