package beam.lang.listeners;

import beam.core.BeamCore;
import beam.core.LocalStateBackend;
import beam.lang.BeamVisitor;
import beam.lang.StateBackend;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParserBaseListener;

/**
 * Parse stateBackend definitions during initial parse.
 */
public class StateBackendLoadingListener extends BeamParserBaseListener {

    private BeamVisitor visitor;
    private BeamCore core;
    private StateBackend stateBackend = new LocalStateBackend();

    private static final String STATE = "stateBackend";

    public StateBackendLoadingListener(BeamCore core, BeamVisitor visitor) {
        this.core = core;
        this.visitor = visitor;
    }

    public StateBackend getStateBackend() {
        return stateBackend;
    }

    @Override
    public void exitState_block(BeamParser.State_blockContext context) {
        stateBackend = visitor.visitState_block(context);
    }

}
