package net.ssehub.kernel_haven.code_model.simple_ast;

import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A syntax element that represents a literal value.
 * 
 * @author Adam
 */
public class LiteralSyntaxElement implements ISyntaxElementType {

    private @NonNull String content;
    
    /**
     * Creates a new literal syntax element.
     * 
     * @param content The content of this literal.
     */
    public LiteralSyntaxElement(@NonNull String content) {
        this.content = content;
    }
    
    /**
     * Retrieves the content of this literal.
     * 
     * @return The content of this literal.
     */
    public @NonNull String getContent() {
        return content;
    }
    
    @Override
    public @NonNull String toString() {
        return "Literal: " + content;
    }
    
    @Override
    public int hashCode() {
        return content.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equal = false;
        
        if (obj instanceof LiteralSyntaxElement) {
            equal = this.content.equals(((LiteralSyntaxElement) obj).content);
        }
        
        return equal;
    }

}
