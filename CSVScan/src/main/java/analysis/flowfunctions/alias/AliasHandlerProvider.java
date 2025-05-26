package analysis.flowfunctions.alias;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JInstanceFieldRef;

/**
 * Helper class to determine the fitting AliasHandler based on the type of the value of which we want to
 * find the aliases.
 */
public class AliasHandlerProvider {

    /**
     * Determines a fitting AliasHandler for the given Value.
     * @param method The method that contains aliasesOfAssign
     * @param aliasesOfAssign The Unit where the Value in question is defined
     * @param determineAliasesOf The Value of which the aliases are determined
     * @return A fitting AliasHandler that can determine the aliases of the given Value
     */
    public static AliasHandler get(SootMethod method, Unit aliasesOfAssign, Value determineAliasesOf) {
        if (determineAliasesOf instanceof JInstanceFieldRef) {
            return new FieldStoreAliasHandler(method, aliasesOfAssign, determineAliasesOf);
        } else if (determineAliasesOf instanceof JArrayRef) {
            return new ArrayStoreAliasHandler(method, aliasesOfAssign, determineAliasesOf);
        } else {
            return new AliasHandler() {
            };
        }
    }


}
