package com.example.calculator.service;

import com.example.calculator.exception.ExpressionException;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全表达式解析器。
 *
 * <p>实现方式：
 * <ol>
 *   <li>词法分析：把输入拆分为 数字 / 运算符 / 括号 token，并剔除空格；</li>
 *   <li>递归下降解析：expr → term (+|-) term；term → factor (*|/|%) factor；factor → ( expr ) | - factor | number；</li>
 *   <li>求值使用 double，并对除零、溢出、非有限结果进行显式检查。</li>
 * </ol>
 *
 * <p>安全性：纯 Java 手写解析，不执行任何动态代码，杜绝注入；只接受白名单字符
 * （数字、+ - * / % ( ) . 空格），其余一律报错。
 */
public final class ExpressionEvaluator {

    /** 单个 token。type 取值：NUMBER, PLUS, MINUS, STAR, SLASH, PERCENT, LPAREN, RPAREN, END。 */
    private static final class Token {
        enum Type { NUMBER, PLUS, MINUS, STAR, SLASH, PERCENT, LPAREN, RPAREN, END }
        final Type type;
        final String text;

        Token(Type type, String text) {
            this.type = type;
            this.text = text;
        }

        @Override
        public String toString() {
            return type + (text == null ? "" : "(" + text + ")");
        }
    }

    private ExpressionEvaluator() {
    }

    /**
     * 计算表达式，返回 double 结果。
     *
     * @param input 用户输入的表达式
     * @return 计算结果
     * @throws ExpressionException 表达式为空、非法字符、括号不匹配、连续运算符、除零、溢出等
     */
    public static double evaluate(String input) {
        if (input == null) {
            throw new ExpressionException("表达式不能为空");
        }
        String expr = input.trim();
        if (expr.isEmpty()) {
            throw new ExpressionException("表达式不能为空");
        }
        if (expr.length() > 500) {
            throw new ExpressionException("表达式过长（最多 500 个字符）");
        }

        List<Token> tokens = lex(expr);
        // 解析完毕后必须恰好消费完所有 token（末尾不得有多余内容，如 "1 2"、"1)"）
        Parser parser = new Parser(tokens);
        double result = parser.parseExpression();
        if (parser.current().type != Token.Type.END) {
            throw new ExpressionException("表达式格式非法：存在无法解析的多余内容");
        }
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new ExpressionException("计算结果溢出或无法计算");
        }
        return result;
    }

    // ------------------------------------------------------------------
    // 词法分析
    // ------------------------------------------------------------------
    private static List<Token> lex(String expr) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = expr.length();
        while (i < n) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (Character.isDigit(c) || c == '.') {
                // 读取数字：整数 / 小数（含前导小数点 ".5" 与尾部小数点 "5."）
                int start = i;
                boolean hasDigit = false;
                boolean hasDot = false;
                while (i < n && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    if (expr.charAt(i) == '.') {
                        if (hasDot) {
                            throw new ExpressionException("数字格式错误：数字包含多个小数点");
                        }
                        hasDot = true;
                    } else {
                        hasDigit = true;
                    }
                    i++;
                }
                if (!hasDigit) {
                    throw new ExpressionException("数字格式错误：单独的圆点不是合法数字");
                }
                String text = expr.substring(start, i);
                try {
                    // 用 parseDouble 校验，同时保持后续用 Double.parseDouble 读取
                    Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    throw new ExpressionException("数字格式错误：" + text);
                }
                tokens.add(new Token(Token.Type.NUMBER, text));
                continue;
            }
            if (c == 'e' || c == 'E') {
                throw new ExpressionException("表达式包含不支持的字符：科学计数法未启用");
            }
            switch (c) {
                case '+': tokens.add(new Token(Token.Type.PLUS, "+")); i++; break;
                case '-': tokens.add(new Token(Token.Type.MINUS, "-")); i++; break;
                case '*': tokens.add(new Token(Token.Type.STAR, "*")); i++; break;
                case '/': tokens.add(new Token(Token.Type.SLASH, "/")); i++; break;
                case '%': tokens.add(new Token(Token.Type.PERCENT, "%")); i++; break;
                case '(': tokens.add(new Token(Token.Type.LPAREN, "(")); i++; break;
                case ')': tokens.add(new Token(Token.Type.RPAREN, ")")); i++; break;
                default:
                    throw new ExpressionException("表达式包含不支持的字符：" + c);
            }
        }
        tokens.add(new Token(Token.Type.END, null));
        return tokens;
    }

    // ------------------------------------------------------------------
    // 递归下降解析
    // ------------------------------------------------------------------
    private static final class Parser {
        private final List<Token> tokens;
        private int pos;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
            this.pos = 0;
        }

        Token current() {
            return tokens.get(pos);
        }

        Token consume() {
            return tokens.get(pos++);
        }

        boolean match(Token.Type type) {
            if (current().type == type) {
                pos++;
                return true;
            }
            return false;
        }

        void expect(Token.Type type, String errorMessage) {
            if (!match(type)) {
                throw new ExpressionException(errorMessage);
            }
        }

        /** expr := term ((+|-) term)* */
        double parseExpression() {
            double left = parseTerm();
            while (true) {
                if (match(Token.Type.PLUS)) {
                    left = left + parseTerm();
                } else if (match(Token.Type.MINUS)) {
                    left = left - parseTerm();
                } else {
                    return left;
                }
            }
        }

        /** term := factor ((*|/|%) factor)* */
        double parseTerm() {
            double left = parseFactor();
            while (true) {
                if (match(Token.Type.STAR)) {
                    left = left * parseFactor();
                } else if (match(Token.Type.SLASH)) {
                    double divisor = parseFactor();
                    if (divisor == 0.0) {
                        throw new ExpressionException("除数不能为零");
                    }
                    left = left / divisor;
                } else if (match(Token.Type.PERCENT)) {
                    double divisor = parseFactor();
                    if (divisor == 0.0) {
                        throw new ExpressionException("除数不能为零");
                    }
                    left = left % divisor;
                } else {
                    return left;
                }
            }
        }

        /** factor := ( expr ) | - factor | + factor | number */
        double parseFactor() {
            if (match(Token.Type.LPAREN)) {
                double value = parseExpression();
                expect(Token.Type.RPAREN, "括号不匹配：缺少右括号 )");
                return value;
            }
            if (match(Token.Type.MINUS)) {
                // 一元负号
                return -parseFactor();
            }
            if (match(Token.Type.PLUS)) {
                // 一元正号
                return parseFactor();
            }
            if (current().type == Token.Type.NUMBER) {
                String text = consume().text;
                try {
                    return Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    throw new ExpressionException("数字格式错误：" + text);
                }
            }
            // 数字后紧接左括号 "2(3)" 或连续运算符 "2 + * 3" 或 "2++3" 等
            Token.Type t = current().type;
            if (t == Token.Type.RPAREN) {
                throw new ExpressionException("括号不匹配：多余的右括号 )");
            }
            if (t == Token.Type.END) {
                throw new ExpressionException("表达式不完整：缺少操作数");
            }
            throw new ExpressionException("表达式格式非法：不允许连续运算符");
        }
    }
}
