package beam.lang;

import beam.parser.antlr4.BeamParser;

public class ConfigParser {

    public BeamConfig init() {
        return new BeamConfig();
    }

    public final void parse(BeamParser.ConfigContext ctx, BeamConfig config) {
        config.setCtx(ctx);
        String type = ctx.configType().getText();
        config.setType(type);
        parseBody(ctx.configBody(), config);

        for (BeamParser.ParamContext paramContext : ctx.param()) {
            if (paramContext.literal() != null) {
                config.getParams().add(BeamListener.parseLiteral(paramContext.literal()));
            } else if (paramContext.inlineList() != null) {
                config.getParams().add(BeamListener.parseInlineList(paramContext.inlineList()));
            }
        }
    }

    public BeamConfig parse(BeamParser.ConfigContext ctx) {
        BeamConfig config = init();
        parse(ctx, config);
        customize(config);
        return config;
    }

    public final void parseBody(BeamParser.ConfigBodyContext configBodyContext, BeamConfig config) {
        if (configBodyContext != null) {
            if (configBodyContext.keyValuePair() != null) {
                for (BeamParser.KeyValuePairContext pairContext : configBodyContext.keyValuePair()) {
                    BeamValue beamValue = BeamListener.parseValue(pairContext.value());
                    beamValue.setLine(pairContext.getStart().getLine());
                    config.addReferable(new BeamContextKey(pairContext.key().getText()), beamValue);
                }
            }

            if (configBodyContext.config() != null) {
                for (BeamParser.ConfigContext configContext : configBodyContext.config()) {
                    ConfigParser parser = new ConfigParser();
                    BeamConfig subConfig = parser.parse(configContext);
                    config.getSubConfigs().add(subConfig);
                }
            }
        }
    }

    public void customize(BeamConfig config) {
    }
}
