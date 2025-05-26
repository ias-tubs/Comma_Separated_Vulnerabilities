package analysis.flowfunctions.provider;

import analysis.data.DFF;
import analysis.flowfunctions.call.CallFF;
import analysis.flowfunctions.call.ToStringTargets;
import heros.FlowFunction;
import heros.InterproceduralCFG;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.AbstractInstanceInvokeExpr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory for producing flow functions that connect call sites to callees.
 * The produced flow functions pass information about code elements that concern the callee, e.g. actual method arguments.
 */
public class CallFlowFunctionProvider implements FlowFunctionProvider<DFF>{

    private FlowFunction<DFF> flowFunction;

    /**
     * Factory for producing flow functions that connect call sites to callees.
     * The produced flow functions pass information about code elements that concern the callee, e.g. actual method arguments.
     *
     * @param icfg
     * @param callStmt  The caller calling a method
     * @param dest      The method that is being invoked
     * @param zeroValue The global zero flow fact as per IFDS problem definition
     */
    public CallFlowFunctionProvider(InterproceduralCFG<Unit, SootMethod> icfg, Unit callStmt, SootMethod dest, DFF zeroValue){
        Stmt s = (Stmt) callStmt;
        InvokeExpr ie = s.getInvokeExpr();

        List<Value> callArgs = ie.getArgs();
        List<Local> paramLocals = new ArrayList<>(callArgs.size());

        // map the arguments to local variables within the destination
        if(dest.getActiveBody().getParameterLocals().size() == callArgs.size()){
            for (int i = 0; i < dest.getParameterCount(); i++) {
                paramLocals.add(dest.getActiveBody().getParameterLocal(i));
            }
        }else {
            // LIMITATION Heros maps constructs like getLog(a.b.Class) wrong and gives a clinit as dest which does not
            // correspond to the actual call statement -> do not perform a mapping
            callArgs = Collections.emptyList();
            paramLocals = Collections.emptyList();
        }

        Value invokingVariable = null;
        if(ie instanceof AbstractInstanceInvokeExpr abstractInstanceInvokeExpr){
            invokingVariable = abstractInstanceInvokeExpr.getBase();
        }

        // return a new call flow function that has information regarding the arguments
        flowFunction = new CallFF(icfg.getMethodOf(callStmt).makeRef(), callArgs, dest, zeroValue, paramLocals, callStmt, invokingVariable);
        if(dest.getName().contains("toString")){
            flowFunction = new ToStringTargets();
        }
    }

    public FlowFunction<DFF> getFlowFunction(){
        return flowFunction;
    }

}
