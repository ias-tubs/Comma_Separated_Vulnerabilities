package analysis.flowfunctions.provider;

import analysis.data.DFF;
import analysis.flowfunctions.alias.AliasHandlerProvider;
import analysis.flowfunctions.normal.*;
import heros.FlowFunction;
import heros.flowfunc.Identity;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JCastExpr;

public class NormalFlowFunctionProvider implements FlowFunctionProvider<DFF> {

    private FlowFunction<DFF> flowFunction;

    public NormalFlowFunctionProvider(SootMethod method, Unit curr, DFF zeroValue) {
        flowFunction = Identity.v(); // always id as fallback
        if (curr instanceof DefinitionStmt assignment) {
            Value lhs = assignment.getLeftOp();
            Value rhs = assignment.getRightOp();
            if (rhs instanceof Local right) {
                // assignment of local
                flowFunction = new AssignFF(right, lhs, zeroValue, AliasHandlerProvider.get(method, curr, lhs), curr);
            } else if (rhs instanceof FieldRef fieldRef) {
                // assignment of instance field
                flowFunction = new FieldLoadFF(fieldRef, lhs, zeroValue, AliasHandlerProvider.get(method, curr, lhs), curr);
            } else if (rhs instanceof JArrayRef arrRef) {
                flowFunction = new ArrayLoadFF(arrRef, lhs, zeroValue, AliasHandlerProvider.get(method, curr, lhs), curr);
            } else if(rhs instanceof Constant){
                flowFunction = new KillFF(lhs, zeroValue);
            } else if(rhs instanceof JCastExpr jCastExpr){
                flowFunction = new CastFF(jCastExpr, lhs, zeroValue, AliasHandlerProvider.get(method, curr, lhs), curr);
            }
        }
    }

    public FlowFunction<DFF> getFlowFunction() {
        return flowFunction;
    }

}
