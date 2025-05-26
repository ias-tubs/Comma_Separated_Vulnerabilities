package analysis.flowfunctions.provider;

import analysis.data.DFF;
import analysis.flowfunctions.alias.AliasHandlerProvider;
import analysis.flowfunctions.call.ConsumerFF;
import analysis.flowfunctions.call.KillStaticCTRFF;
import analysis.flowfunctions.normal.*;
import heros.FlowFunction;
import heros.InterproceduralCFG;
import soot.*;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import util.SootUtil;

import java.util.*;
import java.util.stream.Collectors;

import static util.SootUtil.wrappedContainsMethodRef;

/**
 * Pass information from directly before a call site to all the call site’s successor statements.
 * Such edges typically pass information that do not concern the callee.
 */
public class CallToReturnFlowFunctionProvider implements FlowFunctionProvider<DFF> {

    private FlowFunction<DFF> flowFunction;

    /**
     * Provider that will return the fitting flow function based on the arguments.
     * Pass information from directly before a call site to all the call site’s successor statements.
     * Such edges typically pass information that do not concern the callee.
     * @param methodOfCallSite The method containing the call site
     * @param callSite The Unit that contains the call expression
     * @param zeroValue The global zero flow fact as per IFDS problem definition
     * @param sources A list of method references that will cause a taint to be produced
     * @param argActivatedSources A list of method references that will cause a taint to be produced if
     *                            a specific argument is tainted
     * @param baseActivatedSources A list of method references that will cause a taint to be produced if the base is
     *                             tainted
     * @param invokerTaintedByArg A list of method references that will cause the base to be tainted if an argument
     *                           tainted
     * @param invokerAndOptionalAssignTaintedByArg   A list of method references that will cause the base to be tainted if an argument is
     *                                              tainted, as well as the assigned Value if present
     * @param icfg The inter procedural control flow graph
     */
    public CallToReturnFlowFunctionProvider(SootMethod methodOfCallSite, Unit callSite, DFF zeroValue, List<SootMethodRef> sources, List<SootMethodRef> argActivatedSources, List<SootMethodRef> baseActivatedSources, List<SootMethodRef> invokerTaintedByArg, List<SootMethodRef> invokerAndOptionalAssignTaintedByArg, InterproceduralCFG<Unit,SootMethod> icfg) {
        flowFunction = new KillStaticCTRFF();
        // determine reachability of the variable the call returns to
        if (callSite instanceof DefinitionStmt def) {
            Value lhs = def.getLeftOp();
            Value rhs = def.getRightOp();
            if (rhs instanceof InvokeExpr invoke) {
                SootMethodRef methodRef = invoke.getMethodRef();

                // right hand invoke is a source of a taint
                if (wrappedContainsMethodRef(sources,methodRef)) {

                    // if primitive or string datatype then not any field is tainted
                    if(SootUtil.isPrimitiveOrString(lhs.getType())){
                        flowFunction = new SourceFF(new DFF(lhs, callSite,false, null), zeroValue, AliasHandlerProvider.get(methodOfCallSite, callSite, lhs));
                    }else {
                        SootClass lhsClass = Scene.v().getSootClass(lhs.getType().toString());
                        if(!lhsClass.getFields().isEmpty()){
                            flowFunction = new SourceFF(new DFF(lhs, callSite,true, null), zeroValue, AliasHandlerProvider.get(methodOfCallSite, callSite, lhs));
                        }else {
                            flowFunction = new SourceFF(new DFF(lhs, callSite,false, null), zeroValue, AliasHandlerProvider.get(methodOfCallSite, callSite, lhs));
                        }
                    }
                } else if (wrappedContainsMethodRef(argActivatedSources,methodRef)) {
                    if(SootUtil.isPrimitiveOrString(lhs.getType())){
                        flowFunction = new ArgActivatedSourceFF(List.of(new DFF(lhs, callSite,false, null)), zeroValue, AliasHandlerProvider.get(methodOfCallSite, callSite, lhs),invoke.getArgs());
                    }else {
                        SootClass lhsClass = Scene.v().getSootClass(lhs.getType().toString());
                        if(!lhsClass.getFields().isEmpty()){
                            flowFunction = new ArgActivatedSourceFF(List.of(new DFF(lhs, callSite,true, null)), zeroValue, AliasHandlerProvider.get(methodOfCallSite, callSite, lhs),invoke.getArgs());

                        }else {
                            flowFunction = new ArgActivatedSourceFF(List.of(new DFF(lhs, callSite,false, null)), zeroValue, AliasHandlerProvider.get(methodOfCallSite, callSite, lhs), invoke.getArgs());
                        }
                    }
                } else if (wrappedContainsMethodRef(invokerTaintedByArg,methodRef) && invoke instanceof AbstractInstanceInvokeExpr instanceInvokeExpr) {
                    flowFunction = new ArgActivatedSourceFF(List.of(new DFF(instanceInvokeExpr.getBase(), callSite,true,null)), zeroValue, AliasHandlerProvider.get(methodOfCallSite, callSite, instanceInvokeExpr.getBase()),instanceInvokeExpr.getArgs());
                } else if (wrappedContainsMethodRef(invokerAndOptionalAssignTaintedByArg, methodRef) && invoke instanceof AbstractInstanceInvokeExpr instanceInvokeExpr) {
                    flowFunction = new ArgActivatedSourceFF(List.of(new DFF(lhs, callSite,true,null),new DFF(instanceInvokeExpr.getBase(), callSite,true,null)), zeroValue, AliasHandlerProvider.get(methodOfCallSite, callSite, instanceInvokeExpr.getBase()),instanceInvokeExpr.getArgs());
                } else if (wrappedContainsMethodRef(baseActivatedSources,methodRef)) {
                    if(SootUtil.isPrimitiveOrString(lhs.getType())){
                        flowFunction = new BaseActivatedSourceFF(new DFF(lhs, callSite,true,null), zeroValue, AliasHandlerProvider.get(methodOfCallSite, callSite, lhs),((InstanceInvokeExpr)invoke).getBase());
                    }else {
                        SootClass lhsClass = Scene.v().getSootClass(lhs.getType().toString());
                        flowFunction = new BaseActivatedSourceFF(new DFF(lhs, callSite,true,null), zeroValue, AliasHandlerProvider.get(methodOfCallSite, callSite, lhs),((InstanceInvokeExpr)invoke).getBase());
                        return;
                    }
                } else if (invoke.getMethodRef().getName().equals("toString") && invoke instanceof AbstractInstanceInvokeExpr) {
                    flowFunction = new ToStringHandler(new DFF(lhs, callSite,true,null), zeroValue,((InstanceInvokeExpr)invoke).getBase());
                }
            }
        } else if (callSite instanceof JInvokeStmt jInvokeStmt) {
            if(jInvokeStmt.getInvokeExpr() instanceof AbstractInstanceInvokeExpr currInstanceInvoke){
                SootMethodRef methodRef = currInstanceInvoke.getMethodRef();
                Value base = currInstanceInvoke.getBase();

                if(wrappedContainsMethodRef(invokerTaintedByArg,methodRef)){
                    flowFunction = new ArgActivatedSourceFF(List.of(new DFF(base, callSite,true,null)), zeroValue, AliasHandlerProvider.get(methodOfCallSite, callSite, base),currInstanceInvoke.getArgs());
                    return;
                }

                // check if methodOfCallSite takes a consumer
                if(methodRef.getParameterTypes().stream().anyMatch(param -> param.toString().equals("java.util.function.Consumer"))){
                    List<Unit> predsOf = icfg.getPredsOf(callSite);
                    for(Unit pred: predsOf){
                        if (pred instanceof DefinitionStmt def_pred) {
                            Value lhs_pred = def_pred.getLeftOp();
                            Value rhs_pred = def_pred.getRightOp();
                            if (rhs_pred instanceof InvokeExpr invoke_pred) {
                                SootMethodRef methodRef_pred = invoke_pred.getMethodRef();

                                // check if the previous statement bootstraps a consumer that matches the one consumed in the current statement
                                if(methodRef_pred.getName().endsWith("bootstrap$") && methodRef_pred.getReturnType().toString().equals("java.util.function.Consumer") && currInstanceInvoke.getArgs().stream().anyMatch(arg -> arg.toString().equals(lhs_pred.toString()))){
                                    // determine if the function call produces an interesting consumer
                                    List<Integer> interestingArgumentIndexes = interestingLambdaArguments(methodRef_pred);
                                    if(!interestingArgumentIndexes.isEmpty()){
                                        List<Value> interestingArguments = new ArrayList<>();
                                        for(int argumentIndex: interestingArgumentIndexes){
                                            interestingArguments.add(invoke_pred.getArg(argumentIndex));
                                        }
                                        flowFunction = new ConsumerFF(interestingArguments.stream().map(argument -> new DFF(argument,pred,true,null)).collect(Collectors.toList()), zeroValue, currInstanceInvoke.getBase());
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }

                if(methodRef.getName().equals("append") && methodRef.getDeclaringClass().getName().equals("java.lang.StringBuilder")){
                    if(base instanceof Local baseLocal){
                        Value stringBuilderBase = base;
                        if (baseLocal.getName().startsWith("$stack")){
                            List<Unit> preds =  new ArrayList<>(methodOfCallSite.retrieveActiveBody().getUnits());
                            stringBuilderBase = findStringbuilderBase(baseLocal,preds, callSite);
                            if(stringBuilderBase == null){
                                return;
                            }
                        }
                        flowFunction = new ArgActivatedSourceFF(List.of(new DFF(stringBuilderBase, callSite,true,null)), zeroValue, AliasHandlerProvider.get(methodOfCallSite, callSite, base),currInstanceInvoke.getArgs());
                    }
                }
            }
        }
    }

    private static Value findStringbuilderBase(Local currentBase, List<Unit> preds, Unit currentUnit){
        int indexPrevious = preds.indexOf(currentUnit) - 2;
        if(indexPrevious >= 0){
            Unit previousStringbuilderCall = preds.get(indexPrevious);
            if(previousStringbuilderCall instanceof DefinitionStmt previousStringbuilderDefinition){
                Value lhs = previousStringbuilderDefinition.getLeftOp();
                Value rhs = previousStringbuilderDefinition.getRightOp();
                if (lhs instanceof Local lhsLocal){
                    if(lhsLocal.getName().equals(currentBase.getName())){
                        if(rhs instanceof AbstractInstanceInvokeExpr rhsInvoke){
                            SootMethodRef methodRef = rhsInvoke.getMethodRef();
                            Value base = rhsInvoke.getBase();
                            if(methodRef.getName().equals("append") && methodRef.getDeclaringClass().getName().equals("java.lang.StringBuilder") && base instanceof Local){
                                if (((Local)base).getName().startsWith("$stack")){
                                    return findStringbuilderBase(((Local)base),preds,previousStringbuilderCall);
                                } else {
                                    return base;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if the bootstrap method is interesting for taint flows.
     * @param sootMethodRef The method reference of the function we want to check
     * @return A list with indexes of vulnerable parameters if the bootstrap method is interesting
     */
    private static List<Integer> interestingLambdaArguments(SootMethodRef sootMethodRef){

        // default way how bootstrap functions look in Jimple: bootstrap$name
        String[] split = sootMethodRef.getDeclaringClass().getName().split("\\$");

        // list of lambda function names together with the respective indexes of vulnerable parameters
        Map<String,List<Integer>> interestingLambdaFunctionNames = new HashMap<>();
        interestingLambdaFunctionNames.put("add",new ArrayList<>(Set.of(new Integer[]{0})));

        // check if the name of the bootstrap function matches one of the functions in our list
        if (split.length == 2){
            for (Map.Entry<String,List<Integer>> interestingLambdaFunctionName: interestingLambdaFunctionNames.entrySet()){
                if(split[1].contains(interestingLambdaFunctionName.getKey())){
                    return interestingLambdaFunctionName.getValue();
                }
            }
        }
        return new ArrayList<>();
    }

    @Override
    public FlowFunction<DFF> getFlowFunction() {
        return flowFunction;
    }

}
