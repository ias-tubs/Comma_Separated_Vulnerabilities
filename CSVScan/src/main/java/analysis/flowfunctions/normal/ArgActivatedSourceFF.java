package analysis.flowfunctions.normal;

import analysis.data.DFF;
import analysis.flowfunctions.alias.AliasHandler;
import heros.FlowFunction;
import heros.flowfunc.Gen;
import soot.Value;

import java.util.*;

/**
 * Flow function similar to the source flow function.
 * The arg activated source flow function will create a new data flow fact if one of the target
 * arguments is tainted.
 */
public class ArgActivatedSourceFF implements FlowFunction<DFF> {

    private final AliasHandler aliasHandler;
    private final List<Value> targetArgs;
    private final DFF zeroValue;
    private final DFF genValueGuaranteed;
    private final List<DFF> genValueOptional = new ArrayList<>();

    /**
     * Flow function similar to the source flow function.
     * The arg activated source flow function will create a new data flow fact if one of the target
     * arguments is tainted.
     *
     * @param genValue     The flow fact that should be generated when one of the target arguments is tainted
     * @param zeroValue    The global zero flow fact as per IFDS problem definition
     * @param aliasHandler The handler for aliases
     * @param targetArgs   A list of Values that will trigger the genValue to be produced
     */
    public ArgActivatedSourceFF(List<DFF> genValue, DFF zeroValue, AliasHandler aliasHandler, List<Value> targetArgs) {
        this.genValueGuaranteed = genValue.get(0);
        this.zeroValue = zeroValue;
        this.aliasHandler = aliasHandler;
        this.targetArgs = targetArgs;
        if (genValue.size() > 1) {
            this.genValueOptional.addAll(genValue.subList(1, genValue.size()));
        }
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
        if (source.checkIfChainContainsGeneratedAt(genValueGuaranteed.getGeneratedAt())) {
            return Collections.emptySet();
        }
        // produce new taint if we pass a tainted argument like method(tainted_arg)
        if (targetArgs.stream().anyMatch(ta -> ta.equivTo(source.getValue()))) {
            /*
             * We need to set the previous flow fact here as we dont know the previous flow fact
             * at the time of creation of the flow function
             */
            genValueGuaranteed.setPreviousDFF(source);
            Set<DFF> res = new HashSet<>();
            res.add(source);

            res.add(genValueGuaranteed);

            for (DFF optionalDFF : this.genValueOptional) {
                optionalDFF.setPreviousDFF(source);

                res.add(optionalDFF);
            }
            aliasHandler.handleAliases(res, source);
            return res;
        } else {
            Set<DFF> res = new HashSet<>();
            res.add(source);
            return res;
        }
    }

}
