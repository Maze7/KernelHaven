package net.ssehub.kernel_haven.code_model.ast;

import java.io.File;
import java.util.NoSuchElementException;

import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * <p>
 * A single element of an abstract syntax tree (AST). Other {@link ISyntaxElement}s can be nested inside of this
 * element.
 * </p>
 * <p>
 * The ASTs created by this class are not fully parsed. Some leaf-nodes are instances of {@link Code}, which contain
 * unparsed code strings. If unparsed code elements contain variability (e.g. ifdef), then {@link CodeList} is used,
 * which contains {@link Code} and {@link CppBlock} children (the {@link CppBlock}s contain {@link Code} or more
 * {@link CppBlock}s).
 * </p>
 * 
 * @author Adam
 */
public interface ISyntaxElement extends CodeElement<ISyntaxElement> {

    /**
     * Replaces the given nested element with the given new element. This method should only be called by the extractor
     * that creates the AST.
     * 
     * @param oldElement The old element to replace.
     * @param newElement The new element to replace with.
     * 
     * @throws NoSuchElementException If oldElement is not nested inside this one.
     */
    public void replaceNestedElement(@NonNull ISyntaxElement oldElement, @NonNull ISyntaxElement newElement)
            throws NoSuchElementException;
    
    /**
     * Sets the source file that this element comes from.
     * 
     * @param sourceFile The source element that this element comes from.
     * 
     * @see #getSourceFile()
     */
    public void setSourceFile(@NonNull File sourceFile);
    
    /**
     * Sets the immediate condition of this element. This method should only be called by the extractor
     * that creates the AST.
     * 
     * @param condition The immediate condition of this element.
     * 
     * @see #getCondition()
     */
    public void setCondition(@Nullable Formula condition);
    
    /**
     * Sets the presence condition of this element. This method should only be called by the extractor
     * that creates the AST.
     * 
     * @param presenceCondition The presence condition of this element.
     * 
     * @see #getPresenceCondition()
     */
    public void setPresenceCondition(@NonNull Formula presenceCondition);
    
    /**
     * Accept this visitor.
     * 
     * @param visitor The visitor to accept.
     */
    public abstract void accept(@NonNull ISyntaxElementVisitor visitor);
    
}
