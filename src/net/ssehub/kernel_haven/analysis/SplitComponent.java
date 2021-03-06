package net.ssehub.kernel_haven.analysis;

import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A special analysis component that passes the data it receives to multiple other components. The input component
 * is passed to this class via the constructor. Multiple output components can be created via
 * {@link #createOutputComponent()}; each of these will get a copy of the input data. These output components serve
 * as the input for the next components. The multiple pipelines that are created this way should be joined via the
 * {@link JoinComponent}, so that each of them is properly started and logs its output.
 * 
 * @param <T> The type of result object that the next components will get.
 * 
 * @author Adam
 */
public final class SplitComponent<T> extends AnalysisComponent<Void> {

    private @NonNull Configuration config;
    
    private @NonNull AnalysisComponent<T> inputComponent;
    
    private @NonNull List<@NonNull OutputComponent> outputComponents;
    
    /**
     * Creates this double analysis component with the given input component.
     * 
     * @param config The global configuration.
     * @param inputComponent The component to get the results to pass to both other components.
     */
    public SplitComponent(@NonNull Configuration config, @NonNull AnalysisComponent<T> inputComponent) {
        super(config);
        this.config = config;
        this.inputComponent = inputComponent;
        this.outputComponents = new LinkedList<>();
    }

    /**
     * Creates another output component. This can be used as the input for other analysis components. Each of these
     * will get a copy of the data that is passed to this {@link SplitComponent}.
     * 
     * @return The output component.
     */
    public @NonNull AnalysisComponent<T> createOutputComponent() {
        OutputComponent component = new OutputComponent(config);
        outputComponents.add(component);
        return component;
    }

    @Override
    protected void execute() {
        T data;
        while ((data = inputComponent.getNextResult()) != null) {
            for (OutputComponent out : outputComponents) {
                out.addResult(data);
            }
        }
        
        for (OutputComponent out : outputComponents) {
            out.done = true;
            synchronized (out) {
                out.notifyAll();
            }
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "SplitComponent";
    }
    
    @Override
    boolean isInternalHelperComponent() {
        return true;
    }
    
    /**
     * The pseudo component that the next components will get as the input. 
     */
    private class OutputComponent extends AnalysisComponent<T> {

        private volatile boolean done;
        
        /**
         * Creates this output component.
         * 
         * @param config The global configuration.
         */
        public OutputComponent(@NonNull Configuration config) {
            super(config);
        }

        @Override
        protected void execute() {
            // make sure that SplitComponent is started; multiple calls to start() will do no harm
            SplitComponent.this.start();
            
            while (!done) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        @Override
        public @NonNull String getResultName() {
            return "SplitOutputComponent";
        }
        
        @Override
        boolean isInternalHelperComponent() {
            return true;
        }
        
    }

}
