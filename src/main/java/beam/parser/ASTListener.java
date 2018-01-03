package beam.parser;

import beam.parser.antlr4.BeamBaseListener;
import beam.parser.antlr4.BeamParser;
import beam.parser.ast.ASTBeamRoot;
import beam.parser.ast.ASTKeyValue;

public class ASTListener extends BeamBaseListener {

    private ASTBeamRoot root;

    public ASTBeamRoot getRoot() {
        return root;
    }

    public void setRoot(ASTBeamRoot root) {
        this.root = root;
    }

    @Override
    public void enterBeamRoot(BeamParser.BeamRootContext ctx) {
        setRoot(new ASTBeamRoot());
    }

    @Override
    public void enterGlobalScope(BeamParser.GlobalScopeContext ctx) {
        super.enterGlobalScope(ctx);


        //for (BeamParser.MethodContext mc : ctx.method()) {
          //  System.out.println(mc.METHOD().size());
            //getRoot().getNodes().add(new ASTMethod(mc.METHOD().getSymbol().getText()));
        //}
    }

    @Override
    public void enterKey(BeamParser.KeyContext ctx) {
        super.enterKey(ctx);

        ASTKeyValue a = new ASTKeyValue();

        a.setKey(ctx.ID().getSymbol().getText());

        System.out.println("Found: " + ctx.ID().getSymbol().getText());
    }

    @Override
    public void enterResourceScope(BeamParser.ResourceScopeContext ctx) {
        super.enterResourceScope(ctx);
    }

    @Override
    public void enterProviderBlock(BeamParser.ProviderBlockContext ctx) {
        super.enterProviderBlock(ctx);
    }

    @Override
    public void enterBlock(BeamParser.BlockContext ctx) {
        super.enterBlock(ctx);
    }

    @Override
    public void enterValue(BeamParser.ValueContext ctx) {
        super.enterValue(ctx);

        a.setValue(ctx.QUOTED_STRING(0));
    }

    @Override
    public void enterKeyValueBlock(BeamParser.KeyValueBlockContext ctx) {
        super.enterKeyValueBlock(ctx);
    }

    @Override
    public void enterMethodInclude(BeamParser.MethodIncludeContext ctx) {
        super.enterMethodInclude(ctx);
    }

    @Override
    public void enterMethod(BeamParser.MethodContext ctx) {
        super.enterMethod(ctx);
    }

    @Override
    public void enterMethodArguments(BeamParser.MethodArgumentsContext ctx) {
        super.enterMethodArguments(ctx);
    }

    @Override
    public void enterMethodNamedArgument(BeamParser.MethodNamedArgumentContext ctx) {
        super.enterMethodNamedArgument(ctx);
    }
}
