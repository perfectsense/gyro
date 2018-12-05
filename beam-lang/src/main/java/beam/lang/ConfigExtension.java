package beam.lang;

import beam.parser.antlr4.BeamParser;

public class ConfigExtension implements BeamExtension {

    public BeamConfig init() {
        return new BeamConfig();
    }

    public final void parse(BeamParser.ConfigContext ctx, BeamConfig config) {
        config.setCtx(ctx);
        String type = ctx.configType().getText();
        config.setType(type);
        BeamParser.ConfigBodyContext configBodyContext = ctx.configBody();
        if (configBodyContext != null) {
            if (configBodyContext.keyValuePair() != null) {
                for (BeamParser.KeyValuePairContext pairContext: configBodyContext.keyValuePair()) {
                    config.getContext().put(new BeamConfigKey(null, pairContext.key().getText()), BeamListener.parseValue(pairContext.value()));
                }
            }

            if (configBodyContext.config() != null) {
                for (BeamParser.ConfigContext extensionContext : configBodyContext.config()) {
                    BeamExtension extension = new ConfigExtension();
                    BeamConfig subConfig = extension.applyExtension(extensionContext);
                    config.getUnResolvedContext().add(subConfig);
                }
            }
        }

        for (BeamParser.ParamContext paramContext : ctx.param()) {
            if (paramContext.literal() != null) {
                config.getParams().add(BeamListener.parseLiteral(paramContext.literal()));
            } else if (paramContext.inlineList() != null) {
                config.getParams().add(BeamListener.parseInlineList(paramContext.inlineList()));
            }
        }
    }

    public void customize(BeamConfig config) {
    }

    @Override
    public String getName() {
        return "config";
    }

    @Override
    public BeamConfig applyExtension(BeamParser.ConfigContext ctx) {
        BeamConfig config = init();
        parse(ctx, config);
        customize(config);
        return config;
    }
}
