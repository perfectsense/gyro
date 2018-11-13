package beam.core.extensions;

import beam.core.BeamException;
import beam.lang.BeamConfig;
import beam.lang.BeamConfigKey;
import beam.lang.BeamExtension;
import beam.lang.BeamListener;
import beam.parser.antlr4.BeamParser;

import java.util.ArrayList;
import java.util.List;

public abstract class MethodExtension implements BeamExtension {

    public abstract void call(BeamConfig globalContext, List<String> arguments, BeamConfig methodContext);

    @Override
    public final void enterExtension(BeamParser.ExtensionContext ctx, BeamConfig context) {

    }

    @Override
    public final void exitExtension(BeamParser.ExtensionContext ctx, BeamConfig context) {
        BeamConfig config = new BeamConfig();

        BeamParser.MethodBodyContext methodBodyContext = ctx.methodBody();
        if (methodBodyContext != null) {
            if (methodBodyContext.keyValuePair() != null) {
                for (BeamParser.KeyValuePairContext pairContext: methodBodyContext.keyValuePair()) {
                    config.getContext().put(new BeamConfigKey(null, pairContext.key().getText()), BeamListener.parseValue(pairContext.value()));
                }
            }

            if (!methodBodyContext.extension().isEmpty()) {
                throw new BeamException("Method calls are not allowed inside " + getName());
            }
        }

        List<String> arguments = new ArrayList<>();
        for (BeamParser.ParamContext paramContext : ctx.param()) {
            arguments.add(paramContext.getText());
        }

        call(context, arguments, config);
    }

}
