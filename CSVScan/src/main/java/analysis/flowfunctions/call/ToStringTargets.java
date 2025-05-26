package analysis.flowfunctions.call;

import analysis.data.DFF;
import heros.FlowFunction;

import java.util.Collections;
import java.util.Set;

public class ToStringTargets implements FlowFunction<DFF> {

    /**
     * Computes the flow of a source flow fact through this flow function.
     * @param source The data flow fact that is passed to the flow function
     * @return A result set that contains data flow facts that are generated or passed by the flow function
     */
    @Override
    public Set<DFF> computeTargets(DFF source) {
            return Collections.emptySet();
    }

}
