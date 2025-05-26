package analysis.flowfunctions.normal;

import analysis.data.DFF;
import analysis.flowfunctions.alias.AliasHandler;
import heros.FlowFunction;
import soot.ArrayType;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.FieldRef;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JInstanceFieldRef;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Flow function that models assign statements like A = B
 */
public class AssignFF implements FlowFunction<DFF> {

    private final Local rhs;
    private final Value lhs;
    private final DFF zeroValue;
    private final AliasHandler aliasHandler;
    private final Unit generatedAt;

    /**
     * Flow function that models assign statements like A = B.
     * @param rhs The Local on the right hand side of the assignment
     * @param lhs The Value on the left hand side of the assignment
     * @param zeroValue The global zero flow fact as per IFDS problem definition
     * @param aliasHandler The handler for aliases
     * @param generatedAt The call site for tracing where flow facts originate from
     */
    public AssignFF(Local rhs, Value lhs, DFF zeroValue, AliasHandler aliasHandler, Unit generatedAt) {
        this.rhs = rhs;
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
        if (source.equals(zeroValue)) {
            return Collections.singleton(source);
        }

        // indicates circular references i.e. in recursion across multiple methods
        if(source.checkIfChainContainsGeneratedAt(generatedAt)){
            return Collections.emptySet();
        }
        // an assign that does NOT assign a constant will not kill a flow fact
        Set<DFF> res = new HashSet<>();
        res.add(source);

        if (source.getValue().equivTo(rhs)) {
            DFF newFlowFact = new DFF(lhs, this.generatedAt,source.getAnyFields(),source);
            res.add(newFlowFact);
            aliasHandler.handleAliases(res, source);
        }

        if (source.getValue() instanceof JArrayRef sourceArrayRef) {

            // rhs = tainted_array -> tainted_rhs
            if (sourceArrayRef.getBase().equivTo(rhs)) {
                if (!(lhs instanceof FieldRef) && lhs.getType() instanceof ArrayType) {
                    // tainted_array[index] -> tainted_lhs[index]
                    JArrayRef newRef = new JArrayRef(lhs, sourceArrayRef.getIndex());
                    res.add(new DFF(newRef, this.generatedAt, source.getAnyFields(), source));
                    aliasHandler.handleAliases(res, source);
                }
            }
        } else if (source.getValue() instanceof JInstanceFieldRef) {
            // tainted_ref.b -> tainted_rhs.b
            if (source.getBase().equivTo(rhs)) {
                // tainted_ref.b -> tainted_lhs.b
                if(lhs instanceof JInstanceFieldRef lhsField){
                    DFF newFlowFact = new DFF(lhsField, this.generatedAt,source.getAnyFields(),source);
                    newFlowFact.addField(((JInstanceFieldRef) source.getValue()).getField());
                    res.add(newFlowFact);
                    aliasHandler.handleAliases(res, source);
                }else if(lhs instanceof JArrayRef lhsArray){
                    res.add(new DFF(lhsArray, this.generatedAt, source.getAnyFields(), source));
                    aliasHandler.handleAliases(res, source);
                }else{
                    JInstanceFieldRef lhsFieldRef = new JInstanceFieldRef(lhs, ((JInstanceFieldRef) source.getValue()).getFieldRef());
                    DFF newFlowFact = new DFF(lhsFieldRef, this.generatedAt,source.getAnyFields(),source);
                    res.add(newFlowFact);
                    aliasHandler.handleAliases(res, source);
                }
            }
        }
        return res;
    }

}
