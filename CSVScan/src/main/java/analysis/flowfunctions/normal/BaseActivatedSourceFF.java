package analysis.flowfunctions.normal;

import analysis.data.DFF;
import analysis.flowfunctions.alias.AliasHandler;
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
public class BaseActivatedSourceFF implements FlowFunction<DFF> {

    private final AliasHandler aliasHandler;
    private final DFF zeroValue;
    private final Value base;
    private final DFF genValue;

    /**
     * Flow function similar to the source flow function.
     * The base activated source flow function will create a new data flow fact if the
     * base is tainted.
     *
     * @param genValue     The flow fact that should be generated when the base is tainted
     * @param zeroValue    The global zero flow fact as per IFDS problem definition
     * @param aliasHandler The handler for aliases
     * @param base         The Value that needs to be tainted in order to create the genValue flow fact
     */
    public BaseActivatedSourceFF(DFF genValue, DFF zeroValue, AliasHandler aliasHandler, Value base) {
        this.genValue = genValue;
        this.aliasHandler = aliasHandler;
        this.zeroValue = zeroValue;
        this.base = base;
    }

    /**
     * Generates the genValue and determines computes the targets of the input source
     *
     * @param source The data flow fact that is passed to the flow function
     * @return A result set that contains data flow facts that are generated or passed by the flow function
     */
    @Override
    public Set<DFF> computeTargets(DFF source) {

        // always pass the zero flow fact
        if (source.equals(zeroValue)) {
            return Collections.singleton(source);
        }
        if (source.checkIfChainContainsGeneratedAt(genValue.getGeneratedAt())) {
            return Collections.emptySet();
        }
        if (source.getValue().equivTo(base)) {
            /*
             * We need to set the previous flow fact here as we dont know the previous flow fact
             * at the time of creation of the flow function
             */
            genValue.setPreviousDFF(source);

            // generate the value
            Set<DFF> res = new HashSet<>();
            res.add(source);
            res.add(genValue);
            // handle the aliases and add them to the result set
            aliasHandler.handleAliases(res, source);
            return res;
        } else {

            // keep all flow facts passing through
            Set<DFF> res = new HashSet<>();
            res.add(source);
            return res;
        }
    }

}
