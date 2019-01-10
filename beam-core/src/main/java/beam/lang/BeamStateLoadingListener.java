package beam.lang;

import beam.core.BeamCore;
import beam.core.BeamLocalState;
import beam.core.BeamState;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParserBaseListener;

/**
 * Parse state definitions during initial parse.
 */
public class BeamStateLoadingListener extends BeamParserBaseListener {

    private BeamVisitor visitor;
    private BeamCore core;
    private BeamState state = new BeamLocalState();

    private static final String STATE = "state";

    public BeamStateLoadingListener(BeamCore core, BeamVisitor visitor) {
        this.core = core;
        this.visitor = visitor;
    }

    public BeamState getState() {
        return state;
    }

    @Override
    public void exitState_block(BeamParser.State_blockContext context) {
        state = visitor.visitState_block(context);
    }

}
