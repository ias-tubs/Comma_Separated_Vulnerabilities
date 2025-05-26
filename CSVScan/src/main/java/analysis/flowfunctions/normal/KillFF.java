package analysis.flowfunctions.normal;

import analysis.data.DFF;
import heros.FlowFunction;
import soot.Value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Kill flow function that kills exactly one Value.
 */
public class KillFF implements FlowFunction<DFF> {

    private final Value killedValue;
    private final DFF zeroValue;

    /**
     * Kill flow function that kills exactly one Value.
     * @param killedValue Value that will be killed
     * @param zeroValue The global zero flow fact as per IFDS problem definition
     */
    public KillFF(Value killedValue, DFF zeroValue) {
        this.killedValue = killedValue;
        this.zeroValue = zeroValue;
    }

    /**
     * Computes the flow of a source flow fact through this flow function.
     * @param source The data flow fact that is passed to the flow function
     * @return A result set that contains data flow facts that are generated or passed by the flow function
     */
    @Override
    public Set<DFF> computeTargets(DFF source) {

        // always allow the zero flow fact
        if (source.equals(zeroValue)) {
            return Collections.singleton(source);
        }
        Set<DFF> res = new HashSet<>();

        // allow every flow fact to pass except for the killed Value
        if (!source.getValue().equivTo(killedValue)) {
            res.add(source);
        }
        return res;
    }

}
