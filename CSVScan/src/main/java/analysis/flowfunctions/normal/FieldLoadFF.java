package analysis.flowfunctions.normal;

import analysis.data.DFF;
import analysis.flowfunctions.alias.AliasHandler;
import heros.FlowFunction;
import soot.Unit;
import soot.Value;
import soot.jimple.FieldRef;
import soot.jimple.internal.JInstanceFieldRef;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static util.SootUtil.isPrimitiveDataTypeOrStringOrWrapper;

/**
 * Flow function to model a field load assignment like A = B.field
 */
public class FieldLoadFF implements FlowFunction<DFF> {

    private final AliasHandler aliasHandler;
    private final FieldRef rhsFieldRef;
    private final Value lhs;
    private final DFF zeroValue;
    private final Unit generatedAt;

    /**
     * Constructor.
     * @param rhsFieldRef The field reference on the right hand side of the assignment
     * @param lhs The Value on the left hand side of the assignment
     * @param zeroValue The global zero flow fact as per IFDS problem definition
     * @param aliasHandler The handler for aliases
     * @param generatedAt The call site for tracing where flow facts originate from
     */
    public FieldLoadFF(FieldRef rhsFieldRef, Value lhs, DFF zeroValue, AliasHandler aliasHandler, Unit generatedAt) {
        this.rhsFieldRef = rhsFieldRef;
        this.lhs = lhs;
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
        if(source.equals(zeroValue)){
            return Collections.singleton(source);
        }
        Set<DFF> res = new HashSet<>();
        res.add(source);
        // indicates circular references i.e. in recursion across multiple methods
        if(source.checkIfChainContainsGeneratedAt(generatedAt)){
            return Collections.emptySet();
        }
        // A = tainted_field -> tainted_A
        if(source.getValue().equivTo(rhsFieldRef)){
            res.add(new DFF(lhs,generatedAt,source.getAnyFields(),source));
            aliasHandler.handleAliases(res, source);
        } else if (rhsFieldRef instanceof JInstanceFieldRef instanceRhsFieldRef) {
            if(source.getValue().equivTo(instanceRhsFieldRef.getBase()) && source.getAnyFields()){
                if(isPrimitiveDataTypeOrStringOrWrapper(lhs.getType())){
                    res.add(new DFF(lhs,generatedAt,false,source));
                }else {
                    res.add(new DFF(lhs,generatedAt,true,source));
                }

                aliasHandler.handleAliases(res, source);
            }
        }
        return res;
    }

}
