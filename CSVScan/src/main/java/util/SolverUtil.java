package util;

import analysis.data.DFF;
import soot.*;
import soot.jimple.toolkits.ide.JimpleIFDSSolver;

import java.util.*;

public class SolverUtil {

    public static Set<DFF> getResultAtUnit(Object analysis, Unit unit) {
        Map<DFF, Integer> res = null;
        Set<DFF> result = new HashSet<>();
        if (analysis instanceof JimpleIFDSSolver) {
            JimpleIFDSSolver solver = (JimpleIFDSSolver) analysis;
            res = (Map<DFF, Integer>) solver.resultsAt(unit);
        }
        if (res != null) {
            for (Map.Entry<DFF, Integer> e : res.entrySet()) {
                result.add(e.getKey());
            }
        }
        return result;
    }

}
