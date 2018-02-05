package net.ssehub.kernel_haven.code_model.ast;

import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

public class SwitchStatement extends SyntaxElementWithChildreen {

    private @NonNull SyntaxElement header;
    
    public SwitchStatement(@NonNull Formula presenceCondition, @NonNull SyntaxElement header) {
        
        super(presenceCondition);
        this.header = header;
    }
    
    public @NonNull SyntaxElement getHeader() {
        return header;
    }

    @Override
    protected @NonNull String elementToString(@NonNull String indentation) {
        return "Switch\n" + header.toString(indentation + "\t");
    }

    @Override
    public void accept(@NonNull ISyntaxElementVisitor visitor) {
        visitor.visitSwitchStatement(this);
    }

}
