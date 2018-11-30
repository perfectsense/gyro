package beam.lang;

import beam.parser.antlr4.BeamParser;

public class ConfigExtension implements BeamExtension {

    public BeamConfig init() {
        return new BeamConfig();
    }

    public final void parse(BeamParser.ExtensionContext ctx, BeamConfig config) {
        config.setCtx(ctx);
        String type = ctx.extensionName().getText();
        config.setType(type);
        BeamParser.MethodBodyContext methodBodyContext = ctx.methodBody();
        if (methodBodyContext != null) {
            if (methodBodyContext.keyValuePair() != null) {
                for (BeamParser.KeyValuePairContext pairContext: methodBodyContext.keyValuePair()) {
                    config.getContext().put(new BeamConfigKey(null, pairContext.key().getText()), BeamListener.parseValue(pairContext.value()));
                }
            }

            if (methodBodyContext.extension() != null) {
                for (BeamParser.ExtensionContext extensionContext : methodBodyContext.extension()) {
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

        for (BeamParser.TagContext tagContext : ctx.tag()) {
            if (tagContext.TOKEN() != null) {
                config.getBeamTags().add(tagContext.TOKEN().getText());
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
    public BeamConfig applyExtension(BeamParser.ExtensionContext ctx) {
        BeamConfig config = init();
        parse(ctx, config);
        customize(config);
        return config;
    }
}
