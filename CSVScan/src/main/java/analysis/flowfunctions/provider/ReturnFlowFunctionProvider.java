package analysis.flowfunctions.provider;

import analysis.data.DFF;
import analysis.flowfunctions.call.ReturnFF;
import analysis.flowfunctions.call.ReturnVoidFF;
import analysis.flowfunctions.alias.FieldStoreAliasHandler;
import heros.FlowFunction;
import heros.flowfunc.KillAll;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;

public class ReturnFlowFunctionProvider implements FlowFunctionProvider<DFF> {

    private FlowFunction<DFF> flowFunction;

    public ReturnFlowFunctionProvider(Unit callSite, Unit exitStmt, SootMethod caller, SootMethod callee){
        flowFunction = KillAll.v();
        if (exitStmt instanceof ReturnStmt returnStmt) {
            Value op = returnStmt.getOp();
            if (op instanceof Local) {
                if (callSite instanceof DefinitionStmt defnStmt) {
                    Value leftOp = defnStmt.getLeftOp();
                    if (leftOp instanceof Local tgtLocal) {
                        final Local retLocal = (Local) op;
                        flowFunction = new ReturnFF(callSite, callee, tgtLocal, retLocal, new FieldStoreAliasHandler(caller, callSite, tgtLocal), exitStmt);
                    }
                }
            }
        } else if (exitStmt instanceof JReturnVoidStmt){
            flowFunction = new ReturnVoidFF(callSite, callee, exitStmt);
        }
    }

    public FlowFunction<DFF> getFlowFunction(){
        return flowFunction;
    }

}
