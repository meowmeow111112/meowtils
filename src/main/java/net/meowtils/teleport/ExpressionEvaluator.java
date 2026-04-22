package net.meowtils.teleport;

/**
 * Safe arithmetic parser for command arguments.
 * Supports +, -, *, /, unary +/- and parentheses.
 */
public final class ExpressionEvaluator {
    private final String input;
    private int index;

    private ExpressionEvaluator(String input) {
        this.input = input;
    }

    public static double evaluate(String expression) {
        if (expression == null) {
            throw new NumberFormatException("Expression is null");
        }

        ExpressionEvaluator parser = new ExpressionEvaluator(expression);
        double value = parser.parseExpression();
        parser.skipWhitespace();

        if (parser.index != parser.input.length()) {
            throw parser.invalid("Unexpected token");
        }

        if (!Double.isFinite(value)) {
            throw parser.invalid("Result is not finite");
        }

        return value;
    }

    private double parseExpression() {
        double value = parseTerm();

        while (true) {
            skipWhitespace();
            if (match('+')) {
                value += parseTerm();
            } else if (match('-')) {
                value -= parseTerm();
            } else {
                return value;
            }
        }
    }

    private double parseTerm() {
        double value = parseFactor();

        while (true) {
            skipWhitespace();
            if (match('*')) {
                value *= parseFactor();
            } else if (match('/')) {
                double divisor = parseFactor();
                if (divisor == 0.0d) {
                    throw invalid("Division by zero");
                }
                value /= divisor;
            } else {
                return value;
            }
        }
    }

    private double parseFactor() {
        skipWhitespace();

        if (match('+')) {
            return parseFactor();
        }
        if (match('-')) {
            return -parseFactor();
        }

        if (match('(')) {
            double value = parseExpression();
            skipWhitespace();
            if (!match(')')) {
                throw invalid("Missing closing ')' ");
            }
            return value;
        }

        return parseNumber();
    }

    private double parseNumber() {
        skipWhitespace();
        int start = index;

        while (index < input.length()) {
            char c = input.charAt(index);
            if (Character.isDigit(c) || c == '.') {
                index++;
            } else {
                break;
            }
        }

        if (index < input.length()) {
            char c = input.charAt(index);
            if (c == 'e' || c == 'E') {
                index++;
                if (index < input.length() && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
                    index++;
                }
                int expStart = index;
                while (index < input.length() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
                if (expStart == index) {
                    throw invalid("Invalid exponent");
                }
            }
        }

        if (start == index) {
            throw invalid("Number expected");
        }

        try {
            return Double.parseDouble(input.substring(start, index));
        } catch (NumberFormatException e) {
            throw invalid("Invalid number");
        }
    }

    private void skipWhitespace() {
        while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
            index++;
        }
    }

    private boolean match(char expected) {
        if (index < input.length() && input.charAt(index) == expected) {
            index++;
            return true;
        }
        return false;
    }

    private NumberFormatException invalid(String reason) {
        return new NumberFormatException(reason + " at position " + index + " in '" + input + "'");
    }
}