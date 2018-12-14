package beam.lang;

import beam.lang.types.BeamInlineList;
import beam.lang.types.BeamList;
import beam.lang.types.BeamMap;
import beam.lang.types.BeamScalar;
import beam.lang.types.BeamValue;
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

        for (BeamParser.ParameterContext parameter : blockBody.parameter()) {
            if (parameter.scalar() != null) {
                config.getParameters().add(BeamListener.parseScalar(parameter.scalar()));
            } else if (parameter.list() != null) {
                config.getParameters().add(BeamListener.parseList(parameter.list()));
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
        } else {
            throw new IllegalStateException();
        }
    }

    public static BeamMap parseMap(BeamParser.MapContext mapContext) {
        BeamMap result = new BeamMap();

        for (BeamParser.MapKeyValueContext pairContext : mapContext.mapKeyValue()) {
            String key = pairContext.key().getText();
            BeamParser.ValueContext value = pairContext.value();
            result.getMap().put(key, parseValue(value));
        }

        return result;
    }

    public static BeamList parseList(BeamParser.ListContext listContext) {
        BeamList result = new BeamList();
        for (BeamParser.ScalarContext item : listContext.scalar()) {
            result.getList().add(parseScalar(item));
        }

        return result;
    }

    public static BeamScalar parseScalar(BeamParser.ScalarContext scalar) {
        BeamScalar value = new BeamScalar();

        if (scalar.reference() != null) {
            BeamReference reference = parseReference(scalar.reference());
            value.getElements().add(reference);
        } else if (scalar.QUOTED_STRING() != null) {
            BeamLiteral literal = new BeamLiteral(stripQuotes(scalar.QUOTED_STRING().getText()));
            value.getElements().add(literal);
        } else {
            BeamLiteral literal = new BeamLiteral(scalar.getText());
            value.getElements().add(literal);
        }

        return value;
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
