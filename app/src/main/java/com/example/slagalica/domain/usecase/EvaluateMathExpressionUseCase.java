package com.example.slagalica.domain.usecase;

public class EvaluateMathExpressionUseCase {

    public Double evaluate(String expression) {
        try {
            return eval(expression);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean canAppendNumber(java.util.List<String> expressionTokens) {
        if (!expressionTokens.isEmpty()) {
            String lastToken = expressionTokens.get(expressionTokens.size() - 1);
            if (isNumber(lastToken) || ")".equals(lastToken)) {
                return false;
            }
        }
        return true;
    }

    public boolean canAppendOperator(java.util.List<String> expressionTokens, String op) {
        if (!expressionTokens.isEmpty()) {
            String lastToken = expressionTokens.get(expressionTokens.size() - 1);

            if (isOperator(op)) {
                if (isOperator(lastToken) || "(".equals(lastToken)) {
                    return false;
                }
            } else if ("(".equals(op)) {
                if (isNumber(lastToken) || ")".equals(lastToken)) {
                    return false;
                }
            } else if (")".equals(op)) {
                if (isOperator(lastToken) || "(".equals(lastToken)) {
                    return false;
                }
            }
        } else {
            if (isOperator(op) || ")".equals(op)) {
                return false;
            }
        }
        return true;
    }

    private boolean isNumber(String token) {
        return token != null && token.matches("\\d+");
    }

    private boolean isOperator(String token) {
        return "+".equals(token) || "-".equals(token) || "*".equals(token) || "/".equals(token);
    }

    private double eval(final String str) {
        return new Object() {
            int pos = -1, ch;
            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }
            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }
            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }
            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }
            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }
            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();
                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }
                return x;
            }
        }.parse();
    }
}
