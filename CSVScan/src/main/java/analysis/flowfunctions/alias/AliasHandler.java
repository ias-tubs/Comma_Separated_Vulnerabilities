package analysis.flowfunctions.alias;

import analysis.data.DFF;

import java.util.Set;

/**
 * Interface that describes the methods of the latter implemented various alias handlers.
 */
public interface AliasHandler {
    /**
     * Adds the aliases to the given result set.
     * @param res The set to which the aliases are added to
     */
    default void handleAliases(Set<DFF> res, DFF previousDFF){

    }
}
