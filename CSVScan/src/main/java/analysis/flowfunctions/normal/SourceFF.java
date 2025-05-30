package analysis.flowfunctions.normal;

import analysis.data.DFF;
import analysis.flowfunctions.alias.AliasHandler;
import heros.flowfunc.Gen;

import java.util.Set;

/**
 * Flow function that models the creation of new flow facts.
 * Computes the flow of the created flow facts.
 */
public class SourceFF extends Gen<DFF> {

    private final AliasHandler aliasHandler;

    /**
     * Flow function that models the creation of new flow facts.
     * Computes the flow of the created flow facts.
     * @param genValue The flow fact that is generated by the flow function
     * @param zeroValue The global zero flow fact as per IFDS problem definition
     * @param aliasHandler The handler to determine aliases of the values in the result set
     */
    public SourceFF(DFF genValue, DFF zeroValue, AliasHandler aliasHandler) {
        super(genValue, zeroValue);
        this.aliasHandler = aliasHandler;
    }

    /**
     * Generates the genValue and determines computes the targets of the input source
     * @param source The data flow fact that is passed to the flow function
     * @return A result set that contains data flow facts that are generated or passed by the flow function
     */
    @Override
    public Set<DFF> computeTargets(DFF source) {
        Set<DFF> res = super.computeTargets(source);
        aliasHandler.handleAliases(res, source);
        return res;
    }

}
