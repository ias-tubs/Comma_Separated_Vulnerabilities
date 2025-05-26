package analysis.flowfunctions.alias;

import aliasing.AliasManager;
import analysis.data.DFF;
import boomerang.scene.Val;
import boomerang.scene.jimple.JimpleVal;
import boomerang.util.AccessPath;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.internal.JArrayRef;

import java.util.Set;

public class ArrayStoreAliasHandler implements AliasHandler {

    private JArrayRef arrayRef;
    private final Unit curr;
    private final SootMethod method;

    public ArrayStoreAliasHandler(SootMethod method, Unit curr, Value lhs) {
        if (lhs instanceof JArrayRef) {
            this.arrayRef = (JArrayRef) lhs;
        }
        this.curr = curr;
        this.method = method;
    }

    @Override
    public void handleAliases(Set<DFF> res, DFF previousDFF) {
        if (this.arrayRef != null) {
            AliasManager aliasManager = AliasManager.getInstance();
            Set<AccessPath> aliases = aliasManager.getAliases((Stmt) curr, method, arrayRef.getBase());
            for (AccessPath alias : aliases) {
                Val base = alias.getBase();
                if (base instanceof JimpleVal jval) {
                    Value delegate = jval.getDelegate();
                    if(!delegate.equals(arrayRef.getBase())){
                        JArrayRef newRef = new JArrayRef(delegate, arrayRef.getIndex());
                        res.add(new DFF(newRef, curr,false, previousDFF));
                    }
                }
            }
        }
    }

}
