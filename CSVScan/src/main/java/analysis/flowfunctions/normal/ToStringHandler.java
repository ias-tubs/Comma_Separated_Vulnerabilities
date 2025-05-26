package analysis.flowfunctions.normal;

import analysis.data.DFF;
import heros.FlowFunction;
import soot.Value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Flow function similar to the source flow function.
 * The base activated source flow function will create a new data flow fact if the
 * base is tainted.
 */
public class ToStringHandler implements FlowFunction<DFF> {

    private final DFF zeroValue;
    private final Value base;
    private final DFF genValue;

    /**
     * Flow function similar to the source flow function.
     * The base activated source flow function will create a new data flow fact if the
     * base is tainted.
     * @param genValue The flow fact that should be generated when the base is tainted
     * @param zeroValue The global zero flow fact as per IFDS problem definition
     * @param base The Value that needs to be tainted in order to create the genValue flow fact
     */
    public ToStringHandler(DFF genValue, DFF zeroValue, Value base) {
        this.genValue = genValue;
        this.zeroValue = zeroValue;
        this.base = base;
    }

    /**
     * Generates the genValue and determines computes the targets of the input source
     * @param source The data flow fact that is passed to the flow function
     * @return A result set that contains data flow facts that are generated or passed by the flow function
     */
    @Override
    public Set<DFF> computeTargets(DFF source) {

        // always pass the zero flow fact
        if (source.equals(zeroValue)) {
            return Collections.singleton(source);
        }
        if(source.getValue().equivTo(base)){
            /*
             * We need to set the previous flow fact here as we dont know the previous flow fact
             * at the time of creation of the flow function
             */
            if (source.checkIfChainContainsGeneratedAt(genValue.getGeneratedAt())) {
                return Collections.emptySet();
            }
            genValue.setPreviousDFF(source);

            // generate the value
            Set<DFF> res = new HashSet<>();
            res.add(source);
            res.add(genValue);
            return res;
        }else {

            // keep all flow facts passing through
            Set<DFF> res = new HashSet<>();
            res.add(source);
            return res;
        }
    }

}
