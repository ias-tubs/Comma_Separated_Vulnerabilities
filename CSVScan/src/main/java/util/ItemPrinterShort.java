package util;

import analysis.data.DFF;
import heros.ItemPrinter;

public class ItemPrinterShort<N, DFF, M> implements ItemPrinter<N, DFF, M> {

    @Override
    public String printNode(Object node, Object parentMethod) {
        return node.toString();
    }

    @Override
    public String printFact(DFF fact) {
        return ((analysis.data.DFF) fact).toStringLong();
    }

    @Override
    public String printMethod(Object method) {
        return method.toString();
    }
}
