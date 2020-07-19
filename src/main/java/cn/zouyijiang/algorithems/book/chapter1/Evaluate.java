package cn.zouyijiang.algorithems.book.chapter1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class Evaluate {

    public enum NodeType {
        NUMBER, OPERATOR, BRACKET,
        EXPR, VAR
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Node {
        private NodeType type;
        private String token;
        private int rowBegin;
        private int rowEnd;
        private List<Node> children;
        private Node left;
        private Node right;

        @Override
        public String toString() {
            return "" + type + "[" + rowBegin + "," + rowEnd + "):" + token;
        }

        public Node(NodeType type, String token, int rowBegin, int rowEnd) {
            this.type = type;
            this.token = token;
            this.rowBegin = rowBegin;
            this.rowEnd = rowEnd;
        }
    }

    @Data
    public static class Expr extends Node {
        private String exprText;

        public Expr(NodeType type, String token, String extrText) {
            super(type, token, 0, extrText.length());
            this.exprText = extrText;
        }

    }

    @Data
    @AllArgsConstructor
    private static class Operator {
        private String token;
        private int level;
        private int leftArgNum;
        private int rightArgNum;
        private Function<List<BigDecimal>, BigDecimal> func;
    }

    private static final boolean[] symbols_all = new boolean[128];
    private static final boolean[] symbols_number = new boolean[128];
    private static final boolean[] symbols_float = new boolean[128];
    private static final boolean[] symbols_letter = new boolean[128];
    private static final boolean[] symbols_operator = new boolean[128];
    private static final boolean[] symbols_bracket = new boolean[128];
    private static final boolean[] symbols_var_start = new boolean[128];
    private static final boolean[] symbols_var_name = new boolean[128];

    static {
        for (int i = 0; i < symbols_all.length; i++) {
            // 数字
            symbols_number[i] = i >= '0' && i <= '9';
            symbols_float[i] = symbols_number[i] || i == '.';
            // 字母
            symbols_letter[i] = (i >= 'a' && i <= 'z') || (i >= 'A' && i <= 'Z');
            // 操作符
            symbols_operator[i] = i == '+' || i == '-' || i == '*' || i == '/';
            // 括号
            symbols_bracket[i] = i == '(' || i == ')';
            // 变量前缀
            symbols_var_start[i] = symbols_letter[i] || i == '_' || i == '$';
            // 变量名称
            symbols_var_name[i] = symbols_number[i] || symbols_var_start[i];
            // 所有有效字符
            symbols_all[i] = symbols_number[i] || symbols_float[i]
                    || symbols_letter[i] || symbols_operator[i] || symbols_bracket[i]
                    || symbols_var_start[i] || symbols_var_name[i];
        }
    }

    private final int scale;
    private final RoundingMode roundingMode;
    private final Map<String, Operator> token2Operator;

    private final Map<String, BigDecimal> varMap;

    public Evaluate(int scale, RoundingMode roundingMode) {
        this.scale = scale;
        this.roundingMode = roundingMode;
        this.token2Operator = new LinkedHashMap<>();
        Arrays.asList(
                new Operator("+", 2, 1, 1, list -> list.get(0).add(list.get(1))),
                new Operator("-", 2, 1, 1, list -> list.get(0).subtract(list.get(1))),
                new Operator("*", 1, 1, 1, list -> list.get(0).multiply(list.get(1))),
                new Operator("/", 1, 1, 1, list -> list.get(0).divide(list.get(1), this.scale, roundingMode))
        ).forEach(it -> token2Operator.put(it.getToken(), it));

        varMap = new LinkedHashMap<>();
        varMap.put("e", new BigDecimal(Math.E));
        varMap.put("pi", new BigDecimal(Math.PI));
    }

    public Evaluate() {
        this(48, RoundingMode.HALF_UP);
    }


    public Expr parseExpr(String exprText) {
        log.info("解析表达式开始:{}", exprText);
        List<Node> nodes = expr2Nodes(exprText);
        log.info("解析token完成:{}", nodes);
        // 处理括号,返回一个双向链表
        Expr expr = new Expr(NodeType.EXPR, "()", exprText);
        handleBracket(expr, nodes);
        handleOperator(expr, new HashSet<>(Arrays.asList("*", "/")));
        handleOperator(expr, new HashSet<>(Arrays.asList("+", "-")));
        log.info("解析表达式完成:{}", printNodes(expr, exprText));
        return expr;
    }

    public BigDecimal evaluate(String exprText) {
        Expr expr = parseExpr(exprText);
        BigDecimal result = calculate(expr);
        return result;
    }

    public BigDecimal calculate(Node headNode) {
        if (headNode.getType() == NodeType.NUMBER) {
            return new BigDecimal(headNode.token);
        } else if (headNode.getType() == NodeType.OPERATOR) {
            Operator operator = token2Operator.get(headNode.getToken());
            List<BigDecimal> args = headNode.getChildren().stream().map(this::calculate).collect(Collectors.toList());
            if (args.size() < operator.getLeftArgNum() + operator.getRightArgNum()) {
                throw new IllegalArgumentException("计算参数不足:" + headNode);
            }
            BigDecimal result = operator.getFunc().apply(args);
            return result;
        } else if (headNode.getType() == NodeType.VAR) {
            BigDecimal val = varMap.get(headNode.getToken());
            if (val == null) {
                throw new IllegalArgumentException("找不到变量:" + headNode);
            }
            return val;
        } else if (headNode.getType() == NodeType.EXPR) {
            BigDecimal result = calculate(headNode.getChildren().get(0));
            putTempVar(result);
            return result;
        } else {
            throw new IllegalArgumentException("无效的token:" + headNode);
        }
    }

    private void putTempVar(BigDecimal val) {
        Integer maxKey = varMap.keySet().stream().filter(it -> it.matches("\\$[0-9]+"))
                .map(it -> Integer.valueOf(it.substring(1))).max(Integer::compareTo).orElse(0);
        varMap.put("$" + (maxKey + 1), val);
    }

    public void putVar(String name, BigDecimal val) {
        varMap.put(name, val);
    }

    public BigDecimal getVar(String name) {
        return varMap.get(name);
    }

    public Map<String, BigDecimal> getVars() {
        return new LinkedHashMap<>(varMap);
    }

    private static void printNodes(Node head, String prefix, List<String> lines) {
        lines.add(prefix + "|-" + head);
        if (head.getChildren() == null) return;
        for (Node node : head.getChildren()) {
            printNodes(node, prefix + "  ", lines);
        }
    }

    public static String printNodes(Node headNode, String exprText) {
        List<String> lines = new LinkedList<>();
        lines.add("expr:" + exprText);
        printNodes(headNode, "", lines);
        return String.join("\n", lines);
    }

    private Node handleOperator(Node parentNode, Set<String> operatorTokens) {
        if (parentNode.getChildren() == null || parentNode.getChildren().isEmpty()) return parentNode;
        if (parentNode.getChildren().size() == 1) {
            return handleOperator(parentNode.getChildren().get(0), operatorTokens);
        }
//        log.info("handleOperator:parent={},children={}", parentNode, parentNode.getChildren());
        LinkedList<Node> children = new LinkedList<>();
        Iterator<Node> iterator = parentNode.getChildren().iterator();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            if (operatorTokens.contains(node.getToken())) {
                if (children.isEmpty()) {
                    throw new IllegalArgumentException("操作符左侧参数缺失:" + node);
                }
                Node last = children.pop(); // 左侧参数已处理过
                if (!iterator.hasNext()) {
                    throw new IllegalArgumentException("操作符右侧参数缺失:" + node);
                }
                node.setChildren(Arrays.asList(last, handleOperator(iterator.next(), operatorTokens)));
                children.add(node);
            } else {
                node = handleOperator(node, operatorTokens);
                children.add(node);
            }
        }
        log.info("handleOperator:parent={},children2={}", parentNode, children);
        parentNode.setChildren(children);
        while (parentNode.getChildren() != null && parentNode.getChildren().size() == 1) {
            parentNode = parentNode.getChildren().get(0);
        }
        return parentNode;
    }

    private void handleBracket(Node headNode, List<Node> nodes) {
        headNode.setChildren(new LinkedList<>());
        Node leftNode = headNode;
        for (Node node : nodes) {
            if (node.getType() == NodeType.BRACKET) {
                if (leftNode.getToken().equals("(") && node.getToken().equals(")")) {
                    Node preLeft = leftNode.getLeft();
                    preLeft.getChildren().add(leftNode);
                    preLeft.setRight(null);
                    leftNode.setLeft(null);
                    leftNode.setToken("()");
                    leftNode.setType(NodeType.EXPR);
                    leftNode.setRowEnd(node.getRowEnd());
                    leftNode = preLeft;
                } else {
                    leftNode.setRight(node);
                    node.setLeft(leftNode);
                    node.setChildren(new LinkedList<>());
                    leftNode = node;
                }
            } else {
                leftNode.getChildren().add(node);
            }
        }
        if (!headNode.equals(leftNode)) {
            throw new IllegalArgumentException("表达式的括号不成对:" + leftNode);
        }
    }

    private List<Node> expr2Nodes(String expr) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < expr.length(); ) {
            char ch = expr.charAt(i);
            if (!symbols_all[ch]) {
                i++;
            } else if (symbols_number[ch]) {
                int j = i + 1;
                while (j < expr.length() && symbols_float[expr.charAt(j)]) j++;
                String number = expr.substring(i, j);
                nodes.add(new Node(NodeType.NUMBER, number, i, j));
                i = j;
            } else if (symbols_operator[ch]) {
                nodes.add(new Node(NodeType.OPERATOR, ch + "", i, i + 1));
                i++;
            } else if (symbols_bracket[ch]) {
                nodes.add(new Node(NodeType.BRACKET, ch + "", i, i + 1));
                i++;
            } else if (symbols_var_start[ch]) {
                int j = i + 1;
                while (j < expr.length() && symbols_var_name[expr.charAt(j)]) j++;
                String varName = expr.substring(i, j);
                nodes.add(new Node(NodeType.VAR, varName, i, j));
                i = j;
            } else {
                i++;
            }
        }
        return nodes;
    }

}
