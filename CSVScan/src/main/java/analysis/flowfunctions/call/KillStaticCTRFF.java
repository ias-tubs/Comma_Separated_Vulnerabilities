package analysis.flowfunctions.call;

import analysis.data.DFF;
import heros.FlowFunction;
import soot.jimple.StaticFieldRef;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A flow function that kills flow facts that are static field references.
 */
public class KillStaticCTRFF implements FlowFunction<DFF> {

    /**
     * Computes the flow of a source flow fact through this flow function.
     * @param source The data flow fact that is passed to the flow function
     * @return A result set that contains data flow facts that are generated or passed by the flow function
     */
    @Override
    public Set<DFF> computeTargets(DFF source) {
        if (source.getValue() instanceof StaticFieldRef) {
            return Collections.emptySet();
        }
        Set<DFF> res = new HashSet<>();
        res.add(source);
        return res;
    }

}
