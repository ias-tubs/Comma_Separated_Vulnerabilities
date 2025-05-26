package aliasing;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.results.BackwardBoomerangResults;
import boomerang.scene.ControlFlowGraph;
import boomerang.scene.DataFlowScope;
import boomerang.scene.SootDataFlowScope;
import boomerang.scene.Statement;
import boomerang.scene.jimple.JimpleMethod;
import boomerang.scene.jimple.JimpleStatement;
import boomerang.scene.jimple.JimpleVal;
import boomerang.scene.jimple.SootCallGraph;
import boomerang.util.AccessPath;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import soot.Scene;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;
import wpds.impl.Weight;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class AliasManager {


    private static AliasManager INSTANCE;
    private LoadingCache<BackwardQuery, Set<AccessPath>> queryCache;
    private Boomerang boomerangSolver;
    private final SootCallGraph sootCallGraph;
    private final DataFlowScope dataFlowScope;
    private final boolean disableAliasing = false;

    static class BoomerangOptions extends DefaultBoomerangOptions {


        public BoomerangOptions() {

        }

        @Override
        public int analysisTimeoutMS() {
            return 1000;
        }

        @Override
        public boolean onTheFlyCallGraph() {
            return false;
        }

        @Override
        public StaticFieldStrategy getStaticFieldStrategy() {
            return StaticFieldStrategy.SINGLETON;
        }

        @Override
        public boolean allowMultipleQueries() {
            return true;
        }

        @Override
        public boolean throwFlows() {
            return true;
        }

        @Override
        public boolean trackAnySubclassOfThrowable() {
            return true;
        }
    }

    public static synchronized AliasManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AliasManager();
        }
        return INSTANCE;
    }

    /**
     * Method to get aliases for value.
     * @param stmt   Statement that contains the value. E.g. Value can be the leftOp
     * @param method Method that contains the Stmt
     * @param value  We actually want to find this local's aliases
     * @return A Set of access paths that are the aliases of the value
     */
    public synchronized Set<AccessPath> getAliases(Stmt stmt, SootMethod method, Value value) {
        if (disableAliasing) {
            return Collections.emptySet();
        }
        BackwardQuery query = createQuery(stmt, method, value);
        return getAliases(query);
    }

    private AliasManager() {
        sootCallGraph = new SootCallGraph();
        dataFlowScope = SootDataFlowScope.make(Scene.v());
        setupQueryCache();
    }

    private void setupQueryCache() {
        queryCache =
                CacheBuilder.newBuilder().build(
                                new CacheLoader<BackwardQuery, Set<AccessPath>>() {
                                    @Override
                                    public Set<AccessPath> load(BackwardQuery query) throws Exception {
                                        Set<AccessPath> aliases = queryCache.getIfPresent(query);
                                        if (aliases == null) {
                                            boomerangSolver =
                                                    new Boomerang(
                                                            sootCallGraph, dataFlowScope, new BoomerangOptions());
                                            BackwardBoomerangResults<Weight.NoWeight> results = boomerangSolver.solve(query);
                                            aliases = results.getAllAliases();
                                            queryCache.put(query, aliases);
                                        }
                                        return aliases;
                                    }
                                });
    }

    private BackwardQuery createQuery(Stmt stmt, SootMethod method, Value value) {
        JimpleMethod jimpleMethod = JimpleMethod.of(method);
        Statement statement = JimpleStatement.create(stmt, jimpleMethod);
        JimpleVal val = new JimpleVal(value, jimpleMethod);
        Optional<Statement> first = statement.getMethod().getControlFlowGraph().getSuccsOf(statement).stream().findFirst();
        if (first.isPresent()) {
            return BackwardQuery.make(new ControlFlowGraph.Edge(statement, first.get()), val);
        }
        throw new RuntimeException("No successors for: " + statement);
    }

    private Set<AccessPath> getAliases(BackwardQuery query) {
        try {
            return queryCache.get(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptySet();
    }

}
