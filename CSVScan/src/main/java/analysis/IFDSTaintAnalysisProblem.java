package analysis;

import analysis.data.DFF;
import analysis.flowfunctions.provider.CallFlowFunctionProvider;
import analysis.flowfunctions.provider.CallToReturnFlowFunctionProvider;
import analysis.flowfunctions.provider.NormalFlowFunctionProvider;
import analysis.flowfunctions.provider.ReturnFlowFunctionProvider;
import heros.DefaultSeeds;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import soot.*;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import util.CFGUtil;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The class that represents our actual IFDS problem and provides the flow functions to the IFDS algorithm
 */
public class IFDSTaintAnalysisProblem extends DefaultJimpleIFDSTabulationProblem<DFF, InterproceduralCFG<Unit, SootMethod>> {


    // methods that trigger a flow fact to be created
    private final List<SootMethodRef> sources;

    // methods that trigger a flow fact to be created if a flow fact is used as a parameter
    private final List<SootMethodRef> argActivatedSources;

    // methods that trigger a flow fact to be created if a flow fact is used as a base value
    private final List<SootMethodRef> baseActivatedSources;
    private final List<SootMethodRef> invokerAndOptionalAssignTaintedByArg;
    private final List<SootMethodRef> invokerTaintedByArg;

    // inter procedural control flow graph as described in the IFDS algorithm
    protected InterproceduralCFG<Unit, SootMethod> icfg;

    /**
     * Creates a new IFDS tabulation problem
     * @param icfg inter procedural control flow graph as described in the IFDS algorithm
     * @param sources methods that trigger a flow fact to be created
     * @param argActivatedSources methods that trigger a flow fact to be created if a flow fact is used as a parameter
     * @param baseActivatedSources methods that trigger a flow fact to be created if a flow fact is used as a base value
     * @param invokerTaintedByArg methods that will taint the base if an argument is tainted
     */
    public IFDSTaintAnalysisProblem(InterproceduralCFG<Unit, SootMethod> icfg, List<SootMethodRef> sources, List<SootMethodRef> argActivatedSources, List<SootMethodRef> baseActivatedSources,  List<SootMethodRef> invokerTaintedByArg, List<SootMethodRef> invokerAndOptionalAssignTaintedByArg) {
        super(icfg);
        this.icfg = icfg;
        this.sources = sources;
        this.argActivatedSources = argActivatedSources;
        this.baseActivatedSources = baseActivatedSources;
        this.invokerTaintedByArg = invokerTaintedByArg;
        this.invokerAndOptionalAssignTaintedByArg = invokerAndOptionalAssignTaintedByArg;
    }

    /**
     * Create flow functions that model a taint propagation problem
     * @return FlowFunctions that are used to solve the tabulation problem
     */
    @Override
    protected FlowFunctions<Unit, DFF, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<>() {

            /**
             * Modeling a normal flow between two successive nodes.
             * Check that we do not create infinite loops
             * @param curr The current Unit
             * @param successor The succeeding Unit
             * @return A flow function modeling a normal flow from one node to another
             */
            @Override
            public FlowFunction<DFF> getNormalFlowFunction(Unit curr, Unit successor) {
                return new NormalFlowFunctionProvider(icfg.getMethodOf(curr), curr, zeroValue()).getFlowFunction();
            }

            /**
             * Connect call sites to callees, passing information about code elements that concern the callee, e.g. actual method arguments.
             * @param callStmt The caller calling a method
             * @param dest The method that is being invoked
             * @return A flow function modeling a call statement
             */
            @Override
            public FlowFunction<DFF> getCallFlowFunction(Unit callStmt, SootMethod dest) {
                CallFlowFunctionProvider ffp = new CallFlowFunctionProvider(icfg, callStmt, dest, zeroValue());
                return ffp.getFlowFunction();
            }

            /**
             * Pass information from the return statement to the caller.
             * @param callSite The Unit from where the method was invoked
             * @param calleeMethod The method that was being called and contains the return statement
             * @param exitStmt The Unit from where the flow returns to the callSite
             * @param returnSite The Unit the flow returns to
             * @return A flow function modeling a return statement
             */
            @Override
            public FlowFunction<DFF> getReturnFlowFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
                ReturnFlowFunctionProvider ffp = new ReturnFlowFunctionProvider(callSite, exitStmt, icfg.getMethodOf(callSite), icfg.getMethodOf(exitStmt));
                return ffp.getFlowFunction();
            }

            /**
             * Pass information from directly before a call site to all the call site’s successor statements.
             * Such edges typically pass information that do not concern the callee.
             * @param callSite The Unit containing a call statement
             * @param returnSite The Unit the flow is directed to after the method call is finished
             * @return A flow function modeling the flow from before a call statement to after a call statement
             */
            @Override
            public FlowFunction<DFF> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                CallToReturnFlowFunctionProvider ffp = new CallToReturnFlowFunctionProvider(icfg.getMethodOf(callSite), callSite, zeroValue(), sources,argActivatedSources,baseActivatedSources, invokerTaintedByArg, invokerAndOptionalAssignTaintedByArg, icfg);
                return ffp.getFlowFunction();
            }
        };
    }

    /**
     * Creates a zero flow fact that always is propagated
     * @return A zero flow fact
     */
    @Override
    protected DFF createZeroValue() {
        return new DFF(new JimpleLocal("<<zero>>", NullType.v()),null,false,null);
    }

    /**
     * Initialize zero flow functions for the main method
     * @return A map containing the head unit of the main function that has a zero flow fact as a value
     */
    @Override
    public Map<Unit, Set<DFF>> initialSeeds() {
        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (!m.hasActiveBody()) {
                    continue;
                }
                if (m.toString().contains("void main(java.lang.String[])")) {
                    DirectedGraph<Unit> unitGraph = new BriefUnitGraph(m.getActiveBody());
                    return DefaultSeeds.make(Collections.singleton(CFGUtil.getHead(unitGraph)), zeroValue());
                }
            }
        }
        throw new IllegalStateException("scene does not contain main method");
    }

    /**
     * Decides if the algorithm should keep track of the edges between the nodes
     * @return True if the edges between the nodes should be recorded
     */
    @Override
    public boolean recordEdges() {
        return true;
    }

    @Override
    public int numThreads() {
        return 800;
    }

}
