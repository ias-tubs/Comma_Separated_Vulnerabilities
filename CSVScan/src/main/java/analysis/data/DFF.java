package analysis.data;

import soot.Local;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.internal.JArrayRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Data Flow Fact, typically access paths
 */
public class DFF {

    private final Value value;
    private final Local base;
    private List<SootField> fields;
    private final boolean isApproximated;
    private final boolean anyFields = true;
    private boolean invalid = false;
    private DFF previousDFF;
    private final Unit generatedAt;

    /**
     * Constructor for a dataflow fact.
     *
     * @param value       The value of the fact.
     * @param generatedAt Unit where the dataflow fact is generated
     * @param anyFields   Boolean that determines if all fields with the base value should hold the fact or not
     * @param previousDFF The dataflow fact that caused this dataflow fact
     */
    public DFF(Value value, Unit generatedAt, boolean anyFields, DFF previousDFF) {
        this(value, generatedAt, false, anyFields, previousDFF);
    }

    /**
     * Constructor for a dataflow fact where only specific fields hold the fact.
     * The anyFields boolean therefore is set to false.
     *
     * @param base        The base of the fields
     * @param generatedAt Unit where the dataflow fact is generated
     * @param fields      The fields for which the fact holds
     * @param previousDFF The dataflow fact that caused this dataflow fact
     */
    public DFF(Value base, Unit generatedAt, List<SootField> fields, DFF previousDFF) {
        this(base, generatedAt, false, previousDFF);
        if (this.fields == null) {
            this.fields = new ArrayList<>();
        }
        this.fields.addAll(fields);
    }

    /**
     * Constructor for a dataflow fact.
     *
     * @param value          The value of the fact.
     * @param generatedAt    Unit where the dataflow fact is generated
     * @param isApproximated Boolean determining if the flow fact is approximated
     * @param anyFields      Boolean that determines if all fields with the base value should hold the fact or not
     * @param previousDFF    The dataflow fact that caused this dataflow fact
     */
    private DFF(Value value, Unit generatedAt, boolean isApproximated, boolean anyFields, DFF previousDFF) {
        this.previousDFF = previousDFF;
        this.generatedAt = generatedAt;
        if (value instanceof Local) {
            base = (Local) value;
            fields = null;
        } else if (value instanceof InstanceFieldRef instanceField) {
            base = (Local) instanceField.getBase();
            fields = new ArrayList<>();
            fields.add(instanceField.getField());
        } else if (value instanceof StaticFieldRef staticField) {
            base = null;
            fields = new ArrayList<>();
            fields.add(staticField.getField());
        } else if (value instanceof JArrayRef arr) {
            /*
             * We are modeling array indexes as access paths. Type of index is array's type.
             * arr[0] -> arr.i_0
             * arr[10] -> arr.i_10
             */
            base = (Local) (arr).getBase();
            fields = new ArrayList<>();
            SootField sootField = ArtificialObjectsCache.getSootField(arr);
            fields.add(sootField);
        } else {
            // only in case we compare to other values. E.g. when RHS is JNewExpression
            invalid = true;
            base = null;
        }
        this.value = value;
        this.isApproximated = isApproximated;
    }


    /**
     * Set the dataflow fact that caused the current dataflow fact
     *
     * @param previousDFF The dataflow fact that caused the current dataflow fact
     */
    public void setPreviousDFF(DFF previousDFF) {
        this.previousDFF = previousDFF;
    }

    /**
     * Returns true if any field with the base will hold a dataflow fact.
     *
     * @return true if any field with the base of this dataflow fact will hold a dataflow fact
     */
    public boolean getAnyFields() {
        return anyFields;
    }

    /**
     * Getter for the Value of the dataflow fact.
     *
     * @return The Value variable of the dataflow fact
     */
    public Value getValue() {
        return value;
    }

    /**
     * Getter for the Value of the dataflow fact.
     *
     * @return The base variable of the dataflow fact
     */
    public Local getBase() {
        return base;
    }

    /**
     * Getter for the previous dataflow fact of this dataflow fact.
     *
     * @return The previousDFF variable of the dataflow fact
     */
    public DFF getPreviousDFF() {
        return previousDFF;
    }

    /**
     * Getter for the fields for which the fact holds
     *
     * @return The fields variable of the dataflow fact
     */
    public List<SootField> getFields() {
        return fields;
    }

    /**
     * Getter for the generatedAt variable of the dataflow fact.
     *
     * @return The unit where the fact was generated
     */
    public Unit getGeneratedAt() {
        return generatedAt;
    }


    /**
     * Override of the standard toString method.
     *
     * @return The String representation of the dataflow fact
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (base == null) {
            if (fields == null) {
                // this case happens when we convert something irrelevant e.g. JNewExpression or JNewArrayExpression to DFF only for comparison purposes
                return "irrelevant Object";
            }
            final SootField staticField = fields.get(0);
            sb.append(staticField.getDeclaringClass().getName());
            sb.append(".");
            sb.append(staticField.getName());
        } else {
            sb.append(base.getName());
            if (fields != null) {
                for (SootField field : fields) {
                    sb.append(".");
                    sb.append(field.getName());
                }
            }
            if (isApproximated) {
                sb.append(".*");
            }
        }
        return sb.toString();
    }

    /**
     * Shorter version of toString that returns a shortened version of the zero flow fact.
     *
     * @return A shortened String representation of the flow fact
     */
    public String toStringShort() {
        if (this.base.getName().equals("<<zero>>")) {
            return "<>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(base.getName());
        if (fields != null) {
            for (SootField field : fields) {
                sb.append(".");
                sb.append(field.getName());
            }
        }
        if (isApproximated) {
            sb.append(".*");
        }
        return sb.toString();
    }

    public String toStringLong() {
        if (this.base.getName().equals("<<zero>>")) {
            return "<>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(base.getName());
        if (fields != null) {
            for (SootField field : fields) {
                sb.append(".");
                sb.append(field.getName());
            }
        }
        if (isApproximated) {
            sb.append(".*");
        }
        if (this.previousDFF != null) {
            sb.append("\nprev:" + this.previousDFF.toStringShort());
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((base == null) ? 0 : base.hashCode());
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime * result + (isApproximated ? 0 : 1);
        result = prime * result + (invalid ? 0 : 1);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final DFF other = (DFF) obj;
        if (isApproximated != other.isApproximated) return false;
        if (invalid != other.invalid) return false;
        if (base == null) {
            if (other.base != null) {
                return false;
            }
        } else if (!(base.getName().equals(other.base.getName()) && base.getType().equals(other.base.getType()))) {
            return false;
        }
        if ((fields != null && other.fields == null) || (fields == null && other.fields != null)) {
            return false;
        } else {
            if (fields != null) {

                if (fields.size() != other.fields.size()) {
                    return false;
                } else {
                    for (int i = fields.size() - 1; i >= 0; i--) {
                        // field equality except number equality (field.getNumber())
                        // because we create artificial SootFields for array indices, and each one gets a new number
                        if (!fields.get(i).equals(other.fields.get(i))) {
                            return false;
                        }
                    }
                }
            }
        }
        if(((this.generatedAt != null && other.generatedAt == null) || ((this.generatedAt == null && other.generatedAt != null))) || !Objects.equals(this.generatedAt, other.generatedAt)){
            return false;
        }
        if(((this.previousDFF != null && other.previousDFF == null) || ((this.previousDFF == null && other.previousDFF != null))) || !Objects.equals(this.previousDFF, other.previousDFF)){
            return false;
        }
        return true;
    }

    public void addField(SootField sf){
        this.fields.add(sf);
    }

    // interesting for recursion
    public boolean checkIfChainContainsGeneratedAt(Unit unit){
        if(Objects.equals(this.generatedAt,unit)){
            return true;
        }
        if(this.previousDFF != null){
            return previousDFF.checkIfChainContainsGeneratedAt(unit);
        }
        return false;
    }
}
