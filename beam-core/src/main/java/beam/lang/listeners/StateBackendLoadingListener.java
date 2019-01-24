package beam.lang.listeners;

import beam.core.BeamCore;
import beam.core.LocalStateBackend;
import beam.lang.BeamVisitor;
import beam.lang.StateBackend;
import beam.parser.antlr4.BeamParser.StateContext;
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
    public void exitState(StateContext context) {
        stateBackend = visitor.visitState(context);
    }

}
