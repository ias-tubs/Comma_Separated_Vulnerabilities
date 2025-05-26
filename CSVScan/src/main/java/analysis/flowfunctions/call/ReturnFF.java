package analysis.flowfunctions.call;

import analysis.data.DFF;
import analysis.flowfunctions.alias.FieldStoreAliasHandler;
import heros.FlowFunction;
import heros.solver.Pair;
import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JimpleLocal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static util.SootUtil.isSameParam;

/**
 * Flow function that models the flow from a return statement to the local variable the value will be returned to.
 */
public class ReturnFF implements FlowFunction<DFF> {

    private final Local tgtLocal;
    private final Local retLocal;
    private final FieldStoreAliasHandler aliasHandler;
    private final Unit generatedAt;
    private final Unit callSite;
    private final SootMethod method;

    /**
     * Flow function that models the flow from a return statement to the local variable the value will be returned to.
     * @param callSite The Unit that contains the call expression
     * @param method The invoked method
     * @param tgtLocal The variable the return value is assigned to
     * @param retLocal The variable that is being returned by the method
     * @param aliasHandler The handler for aliases
     * @param generatedAt The Unit where the taint is generated at
     */
    public ReturnFF(Unit callSite, SootMethod method, Local tgtLocal, Local retLocal, FieldStoreAliasHandler aliasHandler, Unit generatedAt) {
        this.tgtLocal = tgtLocal;
        this.retLocal = retLocal;
        this.aliasHandler = aliasHandler;
        this.callSite = callSite;
        this.method = method;
        this.generatedAt = generatedAt;
    }

    /**
     * Computes the flow of a source flow fact through this flow function.
     * @param source The data flow fact that is passed to the flow function
     * @return A result set that contains data flow facts that are generated or passed by the flow function
     */
    @Override
    public Set<DFF> computeTargets(DFF source) {
        Set<DFF> res = new HashSet<>();
        Value sourceValue = source.getValue();

        // taint the tgtLocal if the return value is tainted
        if (source.getValue().equivTo(retLocal)) {
            res.add(new DFF(tgtLocal,generatedAt,source.getAnyFields(),source));
            aliasHandler.handleAliases(res, source);
        } else if (sourceValue instanceof JInstanceFieldRef sourceFieldRef) {
            if (callSite instanceof InvokeStmt invoke) {
                // transport relevant local field references outside
                List<Value> calleeArguments = invoke.getInvokeExpr().getArgs();
                Value sourceBase = sourceFieldRef.getBase();
                int argIndex = 0;
                for (Value arg : calleeArguments) {
                    Pair<Value, Integer> mArg = new Pair<>(arg, argIndex);
                    if (isSameParam(method, mArg, sourceBase)) {
                        JInstanceFieldRef mapRef = new JInstanceFieldRef(arg, sourceFieldRef.getFieldRef());
                        res.add(new DFF(mapRef, generatedAt, source.getAnyFields(), source));
                    }
                    argIndex++;
                }

                // export taints from nested context if they are likely to be used in the calling context, i.e. newly created objects
                if (((JimpleLocal) sourceBase).getName().equals("this") && ((RefType) sourceBase.getType()).getClassName().equals(invoke.getInvokeExpr().getMethodRef().getDeclaringClass().getName())) {
                    JInstanceFieldRef constructorValue = new JInstanceFieldRef(((InstanceInvokeExpr) invoke.getInvokeExpr()).getBase(), sourceFieldRef.getFieldRef());
                    res.add(new DFF(constructorValue,this.callSite,false, source));
                }


            } else if (callSite instanceof JAssignStmt) {
                if (!(source.getBase()).getName().equals("this") && source.getBase().equivTo(retLocal)) {
                    JInstanceFieldRef retField = new JInstanceFieldRef(tgtLocal, sourceFieldRef.getFieldRef());
                    res.add(new DFF(retField,this.callSite,false, source));
                }
            }
        }else if (sourceValue instanceof JimpleLocal jl) {
            if (callSite instanceof InvokeStmt invoke) {
                if (jl.getName().equals("this") && ((RefType)jl.getType()).getClassName().equals(invoke.getInvokeExpr().getMethodRef().getDeclaringClass().getName())) {
                    res.add(new DFF(((InstanceInvokeExpr) invoke.getInvokeExpr()).getBase(),this.callSite,true, source));
                }
            }
        }else if (sourceValue instanceof JArrayRef) {
            if (callSite instanceof InvokeStmt invoke) {
                // transport relevant local field references outside
                List<Value> calleeArguments = invoke.getInvokeExpr().getArgs();
                JArrayRef sourceArrayRef = (JArrayRef) sourceValue;
                Value sourceBase = sourceArrayRef.getBase();
                int argIndex = 0;
                for (Value arg : calleeArguments) {
                    Pair<Value, Integer> mArg = new Pair<>(arg, argIndex);
                    if (isSameParam(method, mArg, sourceBase)) {
                        JArrayRef mapRef = new JArrayRef(arg, sourceArrayRef.getIndex());
                        res.add(new DFF(mapRef,generatedAt,source.getAnyFields(),source));
                    }
                    argIndex++;
                }
            }
        }
        if(source.getValue() instanceof StaticFieldRef){
            res.add(source);
        }
        return res;
    }

}
