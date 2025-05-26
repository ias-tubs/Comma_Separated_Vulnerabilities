package analysis.flowfunctions.call;

import analysis.data.DFF;
import heros.FlowFunction;
import soot.*;
import soot.jimple.StaticFieldRef;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JimpleLocal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Flow function that models the flow from a call site to the callee.
 */
public class CallFF implements FlowFunction<DFF> {

    private final List<Value> callArgs;
    private final SootMethod dest;
    private final DFF zeroValue;
    private final Value invokingVariable;
    private final List<Local> paramLocals;
    private final Unit generatedAt;
    private final SootMethodRef methodRef;

    /**
     * Flow function that models the flow from a call site to the callee.
     * @param callArgs The arguments passed to the callee
     * @param dest The callee
     * @param zeroValue The global zero flow fact as per IFDS problem definition
     * @param paramLocals The local values WITHIN the callee relating to the call arguments
     * @param generatedAt The call site for tracing where flow facts originate from
     * @param invokingVariable The variable invoking the callee IF the callee method is NOT static
     */
    public CallFF(SootMethodRef methodRef, List<Value> callArgs, SootMethod dest, DFF zeroValue, List<Local> paramLocals, Unit generatedAt, Value invokingVariable) {
        this.methodRef = methodRef;
        this.callArgs = callArgs;
        this.dest = dest;
        this.zeroValue = zeroValue;
        this.paramLocals = paramLocals;
        this.generatedAt = generatedAt;
        this.invokingVariable = invokingVariable;
    }

    /**
     * Computes the flow of a source flow fact through this flow function.
     * @param source The data flow fact that is passed to the flow function
     * @return A result set that contains data flow facts that are generated or passed by the flow function
     */
    @Override
    public Set<DFF> computeTargets(DFF source) {
        //ignore implicit calls to static initializers
        if (dest.isStaticInitializer() && dest.getParameterCount() == 0) {
            return Collections.emptySet();
        }
        // stop infinite loops for recursive methods -> improve DFF equality to also account for recursive functions
        if(methodRef.equals(dest.makeRef())){
            return Collections.emptySet();
        }

        Set<DFF> res = new HashSet<>();

        // always pass zero flow fact and static field references
        if (source == zeroValue || source.getValue() instanceof StaticFieldRef) {
            res.add(source);
        }
        // indicates circular references i.e. in recursion across multiple methods
        if(source.checkIfChainContainsGeneratedAt(generatedAt)){
            return Collections.emptySet();
        }
        // pass flow facts to the callee if they are passed to the callee via method argument
        for (int i = 0; i < callArgs.size(); i++) {
            if (callArgs.get(i).equivTo(source.getValue())) {
                DFF newFlowFact = new DFF(paramLocals.get(i),this.generatedAt,source.getAnyFields(),source);
                res.add(newFlowFact);
            } else if (source.getValue() instanceof JInstanceFieldRef && source.getBase().equivTo(callArgs.get(i))) {
                JInstanceFieldRef jInstanceFieldRef = new JInstanceFieldRef(paramLocals.get(i), ((JInstanceFieldRef) source.getValue()).getFieldRef());
                DFF newFlowFact = new DFF(jInstanceFieldRef,this.generatedAt,source.getAnyFields(),source);
                res.add(newFlowFact);
            }
        }

        /*
        * if the variable that invokes the callee is tainted then we use "this" to model that the class instance is
        * tainted within the callee
        */
        if(invokingVariable != null && invokingVariable.equivTo(source.getValue())){
            JimpleLocal thisRef = (JimpleLocal) dest.retrieveActiveBody().getThisLocal();
            res.add(new DFF(thisRef,this.generatedAt,source.getAnyFields(),source));
        }
        if(invokingVariable != null && invokingVariable.equivTo(source.getBase()) && source.getFields() != null){
            JimpleLocal thisRef = (JimpleLocal) dest.retrieveActiveBody().getThisLocal();
            for(SootField sf: source.getFields()){
                JInstanceFieldRef constructorValue = new JInstanceFieldRef(thisRef, sf.makeRef());
                res.add(new DFF(constructorValue,this.generatedAt,source.getAnyFields(),source));
            }

        }
        return res;
    }

}
