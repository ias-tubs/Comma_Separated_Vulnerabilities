package analysis.flowfunctions.call;

import analysis.data.DFF;
import heros.FlowFunction;
import soot.Value;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Used to model flows to interesting Lambda functions.
 */
public class ConsumerFF implements FlowFunction<DFF> {

    private final DFF zeroValue;
    private final Value base;
    private final List<DFF> genValues;

    /**
     * Used to model flows to interesting Lambda functions.
     *
     * @param genValues A list of flow facts that are produced if the base is tainted
     * @param zeroValue The global zero flow fact as per IFDS problem definition
     * @param base      The Value of the variable invoking the consumer flow function
     */
    public ConsumerFF(List<DFF> genValues, DFF zeroValue, Value base) {
        this.base = base;
        this.zeroValue = zeroValue;
        this.genValues = genValues;
    }

    /**
     * Computes the flow of a source flow fact through this flow function.
     *
     * @param source The data flow fact that is passed to the flow function
     * @return A result set that contains data flow facts that are generated or passed by the flow function
     */
    @Override
    public Set<DFF> computeTargets(DFF source) {

        // always pass zero flow fact
        if (source.equals(zeroValue)) {
            return Collections.singleton(source);
        }
        if (source.checkIfChainContainsGeneratedAt(genValues.get(0).getGeneratedAt())) {
            return Collections.emptySet();
        }
        // if the base is tainted the flow function will produce the taints in genValues
        if (base.equivTo(source.getValue())) {
            Set<DFF> res = new HashSet<>();
            res.add(source);

            /*
             * we need to set the predecessor here as opposed to the constructor of DFF because only here we will know
             * about the source DFF
             */
            genValues.forEach(genValue -> genValue.setPreviousDFF(source));
            res.addAll(genValues);
            return res;
        } else {
            // a consumer flow function does not invalidate flow facts
            Set<DFF> res = new HashSet<>();
            res.add(source);
            return res;
        }
    }

}
