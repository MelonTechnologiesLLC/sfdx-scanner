package com.salesforce.graph.ops.expander;

import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.NullAccessCheckedVertex;
import javax.annotation.Nullable;

/**
 * This is thrown in conditions when the code accesses an ApexValue that has an explicit null value.
 * This exception is caught by {@link ApexPathExpanderUtil} which removes the current {@link
 * ApexPathExpander} from the list of possible paths.
 *
 * <p>The ApexValue can be null because it is a real bug in the code, or it could be that the org is
 * setup in such a way that what we interpret as a null condition will never happen in practice.
 */
public final class NullValueAccessedException extends ApexPathExpanderRuntimeException {
    private final ApexValue<?> apexValue;
    private final NullAccessCheckedVertex vertex;

    /**
     * @param apexValue that is explicitly set to null
     * @param vertex {@link BaseSFVertex} that was invoked when the object was null
     */
    public NullValueAccessedException(
            ApexValue<?> apexValue, @Nullable NullAccessCheckedVertex vertex) {
        super("ApexValue=" + apexValue + ", vertex=" + (vertex != null ? vertex : "<null>"));
        this.apexValue = apexValue;
        this.vertex = vertex;
    }

    public NullAccessCheckedVertex getVertex() {
        return vertex;
    }

    @Override
    public String toString() {
        return "NullValueAccessedException{"
                + "apexValue="
                + apexValue
                + ", vertex="
                + vertex
                + "}";
    }
}
