package cn.zouyijiang.algorithems.book.chapter1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
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
        EXPR
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
    private static final boolean[] symbols_letter = new boolean[128];
    private static final boolean[] symbols_operator = new boolean[128];
    private static final boolean[] symbols_bracket = new boolean[128];

    static {
        for (int i = 0; i < symbols_all.length; i++) {
            // 数字
            symbols_number[i] = (i >= '0' && i <= '9') || i == '.';
            // 字母
            symbols_letter[i] = (i >= 'a' && i <= 'z') || (i >= 'A' && i <= 'Z') || i == '_';
            // 操作符
            symbols_operator[i] = i == '+' || i == '-' || i == '*' || i == '/';
            // 括号
            symbols_bracket[i] = i == '(' || i == ')';
            // 所有有效字符
            symbols_all[i] = symbols_number[i] || symbols_letter[i] || symbols_operator[i] || symbols_bracket[i];
        }
    }

    private final int divideScale;
    private final RoundingMode divideRoundingModel;
    private final Map<String, Operator> token2Operator;

    public Evaluate(int divideScale, RoundingMode divideRoundingModel) {
        this.divideScale = divideScale;
        this.divideRoundingModel = divideRoundingModel;
        this.token2Operator = new LinkedHashMap<>();
        Arrays.asList(
                new Operator("+", 2, 1, 1, list -> list.get(0).add(list.get(1))),
                new Operator("-", 2, 1, 1, list -> list.get(0).subtract(list.get(1))),
                new Operator("*", 1, 1, 1, list -> list.get(0).multiply(list.get(1))),
                new Operator("/", 1, 1, 1, list -> list.get(0).divide(list.get(1), divideScale, divideRoundingModel))
        ).forEach(it -> token2Operator.put(it.getToken(), it));
    }

    public Evaluate() {
        this(8, RoundingMode.HALF_UP);
    }

    // 参数队列
    private Deque<String> argmentDeque = new LinkedList<>();
    // 操作符队列
    private Deque<String> operatorDeque = new LinkedList<>();

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

    private BigDecimal calculate(Node headNode) {
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
        } else if (headNode.getType() == NodeType.EXPR) {
            return calculate(headNode.getChildren().get(0));
        } else {
            throw new IllegalArgumentException("无效的token:" + headNode);
        }
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
        Node left = null;
        for (int i = 0; i < expr.length(); ) {
            char ch = expr.charAt(i);
            if (!symbols_all[ch]) {
                i++;
            } else if (symbols_number[ch]) {
                int j = i + 1;
                while (j < expr.length() && symbols_number[expr.charAt(j)]) j++;
                String number = expr.substring(i, j);
                nodes.add(new Node(NodeType.NUMBER, number, i, j));
                i = j;
            } else if (symbols_operator[ch]) {
                nodes.add(new Node(NodeType.OPERATOR, ch + "", i, i + 1));
                i++;
            } else if (symbols_bracket[ch]) {
                nodes.add(new Node(NodeType.BRACKET, ch + "", i, i + 1));
                i++;
            } else {
                i++;
            }
        }
        return nodes;
    }

}
