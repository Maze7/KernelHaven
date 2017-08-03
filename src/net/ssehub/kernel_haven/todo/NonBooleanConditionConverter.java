package net.ssehub.kernel_haven.todo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.ssehub.kernel_haven.PipelineConfigurator;
import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.IConfiguration;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.variability_model.FiniteIntegerVariable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Converts a given non Boolean expression into a Boolean expression.
 * @author Adam
 * @author El-Sharkawy
 */
public class NonBooleanConditionConverter {

    public static final String PROPERTY_VARIABLE_PATTERN = "code.extractor.variable_regex";
    
    private static final String GROUP_NAME_VARIABLE = "variable";
    private static final String GROUP_NAME_OPERATOR = "operator";
    private static final String GROUP_NAME_VALUE = "value";
    
    private Pattern leftSideFinder;
    private VariabilityModel varModel;
    private String booleanFunction;

    /**
     * Created a {@link NonBooleanConditionConverter} based on a configuration.
     * Requires the {@value #PROPERTY_VARIABLE_PATTERN} to be specified.
     * @param config The configuration passed to KernelHaven, must not be <tt>null</tt>.
     * @throws SetUpException If configuring fails.
     */
    public NonBooleanConditionConverter(IConfiguration config) throws SetUpException {
        this(config, null);
    }
    
    /**
     * Created a {@link NonBooleanConditionConverter} based on a configuration.
     * Requires the {@value #PROPERTY_VARIABLE_PATTERN} to be specified.
     * @param config The configuration passed to KernelHaven, must not be <tt>null</tt>.
     * @param booleanFunction Optional a Boolean function/expression in which variables should be wrapped in. Will be
     * ignored if it is <tt>null</tt> or does not contain a percentage sign (<tt>&#037;</tt>). The generated variables
     * will be inserted at the first percentage sign (<tt>&#037;</tt>).<br/>
     * For instance, to create C-preprocessor compatible constraints, this may be: <br/>
     * <tt>defined(&#037;)</tt>
     * @throws SetUpException If configuring fails.
     */
    public NonBooleanConditionConverter(IConfiguration config, String booleanFunction) throws SetUpException {
        String variableRegex = config.getProperty(PROPERTY_VARIABLE_PATTERN);
        
        this.booleanFunction = (null != booleanFunction && booleanFunction.contains("%")) ? booleanFunction : null;
        
        if (null == variableRegex) {
            throw new SetUpException(PROPERTY_VARIABLE_PATTERN + " was not specified.");
        }
        
        try {
            leftSideFinder = Pattern.compile(
                createdNamedCaptureGroup(GROUP_NAME_VARIABLE, variableRegex)
                + "\\s*"
                + createdNamedCaptureGroup(GROUP_NAME_OPERATOR, "==|!=|<|>|<=|>=")
                + "\\s*"
                + createdNamedCaptureGroup(GROUP_NAME_VALUE, "-?[0-9]+"));
        } catch (PatternSyntaxException e) {
            throw new SetUpException(e);
        }
        
        varModel = PipelineConfigurator.instance().getVmProvider().getResult();
        if (null == varModel) {
            throw new SetUpException("No Variability Model Provider specified.");
        }
        
        boolean usedFiniteIntegers = false;
        for (VariabilityVariable var : varModel.getVariables()) {
            if (var instanceof FiniteIntegerVariable) {
                usedFiniteIntegers = true;
                break;
            }
        }
        if (!usedFiniteIntegers) {
            throw new SetUpException("Variability Model does not contain "
                + FiniteIntegerVariable.class.getSimpleName() + "s.");
        }
    }
    
    /**
     * Replaces the passed NonBoolean expression into a boolean representation.
     * @param line An expression which contains comparisons and equality expressions.
     * @return A Boolean expression.
     * @throws FormatException If line contains an unsupported operation.
     */
    public String replaceInLine(String line) throws FormatException {
        String result = line;
        Matcher m = leftSideFinder.matcher(line);
        
        while (m.find()) {
            String whole = m.group();
            String name = m.group(GROUP_NAME_VARIABLE);
            String op = m.group(GROUP_NAME_OPERATOR);
            String strValue = m.group(GROUP_NAME_VALUE);
            int value;
            try {
                value = Integer.parseInt(strValue);
            } catch (NumberFormatException e) {
                throw new FormatException("Right-hand side contains not supported value: " + strValue);
            }
            String replacement = "ERROR_WHILE_REPLACING";
            FiniteIntegerVariable var = (FiniteIntegerVariable) varModel.getVariableMap().get(name);
            
            if (var != null) {
                switch (op) {
                case "==":
                    replacement = toConstantExpression(var, value);
                    break;
                    
                case "!=":
                    replacement = "!" + toConstantExpression(var, value);
                    break;
                    
                case ">":
                    value++;
                    // fall through
                case ">=":
                    List<Integer> greaterValuesToAdd = new ArrayList<>(var.getSizeOfRange());
                    
                    for (int c : var) {
                        if (c >= value) {
                            greaterValuesToAdd.add(c);
                        }
                    }
                    
                    replacement = expandComparison(var, greaterValuesToAdd);
                    break;
                    
                case "<":
                    value--;
                    // fall through
                case "<=":
                    List<Integer> lesserValuesToAdd = new ArrayList<>(var.getSizeOfRange());
                    
                    for (int c : var) {
                        if (c <= value) {
                            lesserValuesToAdd.add(c);
                        }
                    }
                    
                    replacement = expandComparison(var, lesserValuesToAdd);
                    break;
                default:
                    throw new FormatException("Expression contains unsupported operation: " + op);
                }
            } else {
                throw new FormatException("Left-hand side contains unknown variable: " + name);
            }
            
            result = result.replace(whole, replacement);
        }
        
        return result;
    }
    
    /**
     * Creates a disjunction constraints containing comparisons for all values passed to this method.
     * @param var A variable for which multiple comparisons shall be created for.
     * @param legalValues The values which shall be added to the comparison.
     * @return One Boolean disjunction expression.
     */
    private String expandComparison(FiniteIntegerVariable var, List<Integer> legalValues) {
        String replacement;
        replacement = "(" + toConstantExpression(var, legalValues.get(0));
        for (int i = 1; i < legalValues.size(); i++) {
            replacement += " || " + toConstantExpression(var, legalValues.get(i));
        }
        
        replacement += ")";
        return replacement;
    }
    
    /**
     * Generates an artificial comparison variable for the given variable and its value. 
     * @param var The variable for which the comparison shall be created for.
     * @param value A value of the variable.
     * @return A comparison variable in the form of <tt>variable_eq_value</tt>.
     */
    private String toConstantExpression(VariabilityVariable var, int value) {
        String expression = var.getName() + "_eq_" + value;
        
        // wrap expression in Boolean function if one is defined.
        if (null != booleanFunction) {
            expression = String.format(booleanFunction, expression);
        }
        
        return expression;
    }
    
    /**
     * Creates a named capturing group.
     * @param groupName The name of the captured group.
     * @param groupContents The sub RegEx of the group without parenthesis.
     * 
     * @return A named capturing group, with enclosing parenthesis.
     */
    private static String createdNamedCaptureGroup(String groupName, String groupContents) {
        return "(?<" + groupName + ">" + groupContents + ")";
    }
}
