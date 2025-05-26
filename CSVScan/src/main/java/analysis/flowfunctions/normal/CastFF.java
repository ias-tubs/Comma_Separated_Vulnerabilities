package analysis.flowfunctions.normal;

import analysis.data.DFF;
import analysis.flowfunctions.alias.AliasHandler;
import heros.FlowFunction;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JCastExpr;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Flow function modeling a cast expression.
 */
public class CastFF implements FlowFunction<DFF> {

    private final AliasHandler aliasHandler;
    private final JCastExpr castExpr;
    private final Value assignedTo;
    private final DFF zeroValue;
    private final Unit generatedAt;

    /**
     * Flow function modeling a cast expression.
     * @param castExpr The cast expression
     * @param assignedTo The Value the cast will be assigned to
     * @param zeroValue The global zero flow fact as per IFDS problem definition
     * @param aliasHandler The handler to determine aliases of the values in the result set
     * @param generatedAt The Unit where the new flow fact is generated at
     */
    public CastFF(JCastExpr castExpr, Value assignedTo, DFF zeroValue, AliasHandler aliasHandler, Unit generatedAt) {
        this.castExpr = castExpr;
        this.assignedTo = assignedTo;
        this.zeroValue = zeroValue;
        this.aliasHandler = aliasHandler;
        this.generatedAt = generatedAt;
    }
    /**
     * Computes the flow of a source flow fact through this flow function.
     * @param source The data flow fact that is passed to the flow function
     * @return A result set that contains data flow facts that are generated or passed by the flow function
     */
    @Override
    public Set<DFF> computeTargets(DFF source) {

        // always pass the zero flow fact
        if (source.equals(zeroValue)) {
            return Collections.singleton(source);
        }
        Set<DFF> res = new HashSet<>();
        res.add(source);
        // indicates circular references i.e. in recursion across multiple methods
        if(source.checkIfChainContainsGeneratedAt(generatedAt)){
            return Collections.emptySet();
        }
        // if a taint is cast and assigned to another variable, that variable will also be tainted
        if (source.getValue().equivTo(castExpr.getOp())) {
            DFF newFlowFact = new DFF(assignedTo, this.generatedAt,source.getAnyFields(),source);
            res.add(newFlowFact);
            aliasHandler.handleAliases(res, source);
        }
        return res;
    }

}
