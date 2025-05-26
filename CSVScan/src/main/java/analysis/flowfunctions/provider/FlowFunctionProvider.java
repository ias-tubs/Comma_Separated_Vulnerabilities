package analysis.flowfunctions.provider;

import heros.FlowFunction;

public interface FlowFunctionProvider<D> {

    FlowFunction<D> getFlowFunction();

}
