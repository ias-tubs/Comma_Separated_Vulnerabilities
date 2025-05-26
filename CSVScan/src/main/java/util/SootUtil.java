package util;

import heros.solver.Pair;
import soot.*;
import soot.jimple.internal.JIdentityStmt;

import java.util.Arrays;
import java.util.List;

public class SootUtil {

    public static Type [] primitiveOrString = {
            RefType.v("java.lang.String"),
            BooleanType.v(),
            ByteType.v(),
            CharType.v(),
            DoubleType.v(),
            FloatType.v(),
            IntType.v(),
            LongType.v(),
            ShortType.v()
    };

    public static SootMethod retrieveSootMethod(String sootClassName, String methodName, List<Type> parameterTypes, Type returnType){
        try {
            SootClass sc = Scene.v().getSootClass(sootClassName);
            return sc.getMethod(methodName,parameterTypes,returnType);
        }catch (Exception e){
            throw new IllegalArgumentException("Method does not exist in scene!");
        }
    }

    public static boolean isPrimitiveOrString(Type type){
        return Arrays.asList(primitiveOrString).contains(type);
    }

    public static boolean wrappedContainsMethodRef(List<SootMethodRef> refsList, SootMethodRef other){
        for(SootMethodRef thisRef: refsList){
            if(!thisRef.getDeclaringClass().getName().equals(other.getDeclaringClass().getName())){
                continue;
            }
            if (thisRef.isStatic() != other.isStatic()) {
                continue;
            }
            if (thisRef.getName() == null) {
                if (other.getName() != null) {
                    continue;
                }
            } else if (!thisRef.getName().equals(other.getName())) {
                continue;
            }

            if (thisRef.getParameterTypes() == null) {
                if (other.getParameterTypes() != null) {
                    continue;
                }
            } else if (!thisRef.getParameterTypes().equals(other.getParameterTypes())) {
                continue;
            }

            if (thisRef.getReturnType() == null) {
                if (other.getReturnType() != null) {
                    continue;
                }
            } else if (!thisRef.getReturnType().equals(other.getReturnType())) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * Determines if the argument of the given method is equal to another value.
     * @param method The method that the argument is passed to
     * @param actualParam The actual argument passed to the method
     * @param formalParam The value the argument is tested against
     * @return True if the formalParam is the same argument as actualParam
     */
    public static boolean isSameParam(SootMethod method, Pair<Value, Integer> actualParam, Value formalParam) {
        if (actualParam.getO1().getType() instanceof RefType) {
            Body activeBody = method.getActiveBody();
            UnitPatchingChain units = activeBody.getUnits();
            int idIndex = method.isStatic() ? 0 : -1; // @this is only in virtual invoke
            for (Unit unit : units) {
                if (unit instanceof JIdentityStmt id) {
                    Value rightOp = id.getRightOp();
                    Value leftOp = id.getLeftOp();
                    if (rightOp.getType().equals(actualParam.getO1().getType()) && leftOp.equals(formalParam) && actualParam.getO2().equals(idIndex)) {
                        return true;
                    }
                    idIndex++;
                }
            }
        }
        return false;
    }

    public static boolean isPrimitiveDataTypeOrStringOrWrapper(Type refType) {
        String name = refType.toString();
        return switch (name) {
            case "java.lang.String", "byte", "short", "int", "long", "float", "double", "boolean", "char", "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double", "java.lang.Boolean", "java.lang.Character" ->
                    true;
            default -> false;
        };
    }

    public static boolean isPrimitiveDataTypeOrWrapper(Type refType) {
        String name = refType.toString();
        return switch (name) {
            case "byte", "short", "int", "long", "float", "double", "boolean", "char", "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double", "java.lang.Boolean", "java.lang.Character" ->
                    true;
            default -> false;
        };
    }

}
