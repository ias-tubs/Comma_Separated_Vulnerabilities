package analysis.flowfunctions.normal;

import analysis.data.DFF;
import analysis.flowfunctions.alias.AliasHandler;
import heros.FlowFunction;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JArrayRef;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ArrayLoadFF implements FlowFunction<DFF> {

    private final DFF zeroValue;
    private final AliasHandler aliasHandler;
    private final JArrayRef arrayRef;
    private final Value lhs;
    private final Unit generatedAt;

    public ArrayLoadFF(JArrayRef arrayRef, Value lhs, DFF zeroValue, AliasHandler aliasHandler, Unit generatedAt) {
        this.arrayRef = arrayRef;
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
        if(source.equals(zeroValue)){
            return Collections.singleton(source);
        }
        Set<DFF> res = new HashSet<>();
        res.add(source);
        // indicates circular references i.e. in recursion across multiple methods
        if(source.checkIfChainContainsGeneratedAt(generatedAt)){
            return Collections.emptySet();
        }
        if(source.getValue().equivTo(arrayRef)){
            res.add(new DFF(lhs,generatedAt,source.getAnyFields(),source));
            aliasHandler.handleAliases(res, source);
        }
        return res;
    }

}
