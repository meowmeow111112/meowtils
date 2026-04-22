package net.meowtils.teleport;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * Safe arithmetic evaluator for command arguments.
 * Uses exp4j with no variables and no custom functions.
 */
public final class ExpressionEvaluator {
    private ExpressionEvaluator() {}

    public static double evaluate(String expression) {
        try {
            if (expression == null || expression.trim().isEmpty()) {
                throw new NumberFormatException("Expression is empty");
            }

            Expression exp = new ExpressionBuilder(expression).build();
            double value = exp.evaluate();

            if (!Double.isFinite(value)) {
                throw new NumberFormatException("Result is not finite");
            }

            return value;
        } catch (NumberFormatException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new NumberFormatException("Invalid expression: " + expression);
        }
    }
}