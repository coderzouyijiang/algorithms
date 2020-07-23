package cn.zouyijiang.algorithems.book.chapter1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class Evaluate {

    @Getter
    @AllArgsConstructor
    public enum NodeType {
        NUMBER("数字"),
        OPERATOR("操作符"),
        BRACKET("括号"),
        EXPR("表达式"),
        VAR("变量"),
        ASSIGN("赋值"), // 右结合
        END("赋值"), // 结束符号
        FUNCTION("函数"); // 函数

        private final String name;
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

//        private List<String> varNames;
//        private Node left;
//        private Node right;

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
        private String name;
        private List<String> argNames;

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
    }

    private static final boolean[] symbols_all = new boolean[128];
    private static final boolean[] symbols_number = new boolean[128];
    private static final boolean[] symbols_float = new boolean[128];
    private static final boolean[] symbols_letter = new boolean[128];
    private static final boolean[] symbols_operator = new boolean[128];
    private static final boolean[] symbols_bracket = new boolean[128];
    private static final boolean[] symbols_var_start = new boolean[128];
    private static final boolean[] symbols_var_name = new boolean[128];
    private static final boolean[] symbols_assign = new boolean[128];
    private static final boolean[] symbols_end = new boolean[128];

    private static final Map<String, Operator> token2Operator = new LinkedHashMap<>();

    private static final Set<String> specialTokens = new HashSet<>(Arrays.asList("+", "-"));

    static {
        for (int i = 0; i < symbols_all.length; i++) {
            // 数字
            symbols_number[i] = i >= '0' && i <= '9';
            symbols_float[i] = symbols_number[i] || i == '.';
            // 字母
            symbols_letter[i] = (i >= 'a' && i <= 'z') || (i >= 'A' && i <= 'Z');
            // 操作符
            symbols_operator[i] = i == '+' || i == '-' || i == '*' || i == '/' || i == '^';
            // 括号
            symbols_bracket[i] = i == '(' || i == ')';
            // 变量前缀
            symbols_var_start[i] = symbols_letter[i] || i == '_' || i == '$';
            // 变量名称
            symbols_var_name[i] = symbols_number[i] || symbols_var_start[i];
            // 赋值
            symbols_assign[i] = i == '=';
            symbols_end[i] = i == ';';
            // 所有有效字符
            symbols_all[i] = symbols_number[i] || symbols_float[i]
                    || symbols_letter[i] || symbols_operator[i] || symbols_bracket[i]
                    || symbols_var_start[i] || symbols_var_name[i]
                    || symbols_assign[i] || symbols_end[i];

            Arrays.asList(
                    new Operator("+", 2, 1, 1),
                    new Operator("-", 2, 1, 1),
                    new Operator("*", 1, 1, 1),
                    new Operator("/", 1, 1, 1),
                    new Operator("^", 0, 1, 1),
                    new Operator("=", 0, 1, 1)
            ).forEach(it -> token2Operator.put(it.getToken(), it));
        }
    }

    private final int scale;
    private final RoundingMode roundingMode;
    private final Map<String, Function<List<BigDecimal>, BigDecimal>> token2Func;
    // 变量
    private final Map<String, BigDecimal> varMap;
    // 函数表达式
    private final Map<String, Expr> exprMap;

    public Evaluate(int scale, RoundingMode roundingMode) {
        this.scale = scale;
        this.roundingMode = roundingMode;

        token2Func = new LinkedHashMap<>();
        token2Func.put("+", list -> list.get(0).add(list.get(1)));
        token2Func.put("-", list -> list.get(0).subtract(list.get(1)));
        token2Func.put("*", list -> list.get(0).multiply(list.get(1)));
        token2Func.put("/", list -> list.get(0).divide(list.get(1), scale, roundingMode));
        token2Func.put("^", list -> BigDecimal.valueOf(Math.pow(list.get(0).doubleValue(), list.get(1).doubleValue())));

        varMap = new LinkedHashMap<>();
        putVar("e", new BigDecimal(Math.E));
        putVar("pi", new BigDecimal(Math.PI));

        exprMap = new LinkedHashMap<>();
        exprMap.put("area", parseExpr("a*b"));
        log.info("计算器初始化完成...\n");
    }

    public Evaluate() {
        this(48, RoundingMode.HALF_UP);
    }


    public Expr parseExpr(String exprText) {
        log.info("解析表达式开始:{}", exprText);
        List<Node> nodes = expr2Nodes(exprText);
        log.info("解析token完成:{}", nodesToStr(nodes));
        // 处理括号,返回一个双向链表
        Expr expr = new Expr(NodeType.EXPR, "()", exprText);
        handleBracket(expr, nodes);
        expr.setArgNames(getArgNames(expr));
        log.info("处理括号完成:{}", exprToStr(expr));
        handleSerialOperator(expr);
        log.info("处理孤立的正负号完成:{}", exprToStr(expr));
//        handleAssign(expr);
//        log.info("处理赋值=完成:{}", exprToStr(expr));
        handleOperator(expr, true, new HashSet<>(Arrays.asList("^")));
        log.info("处理操作符^完成:{}", exprToStr(expr));
        handleOperator(expr, true, new HashSet<>(Arrays.asList("*", "/")));
        log.info("处理操作符*、/完成:{}", exprToStr(expr));
        handleOperator(expr, true, new HashSet<>(Arrays.asList("+", "-")));
        log.info("处理操作符+、-完成:{}", exprToStr(expr));
        handleOperator(expr, false, new HashSet<>(Arrays.asList("=")));
        log.info("处理赋值=完成:{}", exprToStr(expr));
        if (expr.getChildren() != null && expr.getChildren().size() > 0) {
            Node firstNode = expr.getChildren().get(0);
            if (firstNode.getType() == NodeType.ASSIGN) {
                Node varNode = firstNode.getChildren().get(0);
                expr.setName(varNode.getToken());
                /*
                Node valNode = firstNode.getChildren().get(1);
                Expr valExpr = new Expr(valNode.getType(), valNode.getToken(), exprText.substring(valNode.getRowBegin(), valNode.getRowEnd()));
                valExpr.setArgNames(expr.getArgNames());
                valExpr.setName(varNode.getToken());
                valExpr.setChildren();
                */
                exprMap.put(varNode.getToken(), expr);
            }
        }
        return expr;
    }

    private List<String> getArgNames(Expr expr) {
        HashSet<String> argNameSet = new HashSet<>();
        findVarNames(expr, argNameSet);
        return new ArrayList<>(argNameSet);
    }

    private void findVarNames(Node headNode, Set<String> varNames) {
        if (headNode.getType() == NodeType.VAR) {
            varNames.add(headNode.getToken());
        } else if (headNode.getChildren() != null) {
            Iterator<Node> iterator = headNode.getChildren().iterator();
            if (headNode.getType() == NodeType.ASSIGN && iterator.hasNext()) {
                iterator.next();
            }
            while (iterator.hasNext()) {
                findVarNames(iterator.next(), varNames);
            }
        }
    }

    private void handleAssign(Node headNode) {
        if (headNode.getChildren() == null) return;
        ListIterator<Node> iterator = headNode.getChildren().listIterator(headNode.getChildren().size());
        Node preNode = null;
        while (iterator.hasPrevious()) {
            Node node = iterator.previous();
            if (node.getType() == NodeType.ASSIGN) {
                if (preNode == null || preNode.getType() != NodeType.EXPR || preNode.getType() != NodeType.FUNCTION
                        || preNode.getType() != NodeType.VAR || preNode.getType() != NodeType.NUMBER) {
                    throw new IllegalArgumentException("赋值语句右值不是一个值:" + node);
                }
                Node varNode;
                if (!(iterator.hasPrevious() && (varNode = iterator.previous()).getType() == NodeType.VAR)) {
                    throw new IllegalArgumentException("赋值语句左值不是变量:" + node);
                }
                iterator.remove();
                node.setToken(varNode.getToken() + node.getToken());
                node.setRowBegin(varNode.getRowBegin());
            }
            preNode = node;
        }
    }

    // 处理连续的符号
    private void handleSerialOperator(Node headNode) {
        if (headNode.getChildren() == null) return;
        LinkedList<Node> children = new LinkedList<>();
//        Node preNode = new Node(NodeType.OPERATOR, "+", headNode.getRowBegin(), headNode.getRowBegin());
        Iterator<Node> iterator = headNode.getChildren().iterator();
        Node preNode = iterator.next();
        if (preNode.getType() == NodeType.OPERATOR) {
            if (specialTokens.contains(preNode.getToken())) {
                Node zeroNode = new Node(NodeType.NUMBER, "0", preNode.getRowBegin(), preNode.getRowBegin());
                children.add(zeroNode);
            } else {
                throw new IllegalArgumentException(preNode.getType().getName() + "缺少左侧参数:" + preNode);
            }
        }
        children.add(preNode);
        while (iterator.hasNext()) {
            Node node = iterator.next();
            if ((preNode.getType() == NodeType.OPERATOR || preNode.getType() == NodeType.ASSIGN)
                    && node.getType() == NodeType.OPERATOR) {
                if (specialTokens.contains(node.getToken()) && iterator.hasNext()) {
                    Node next = iterator.next();
                    if (next.getType() != NodeType.OPERATOR) {
                        Node zeroNode = new Node(NodeType.NUMBER, "0", node.getRowBegin(), node.getRowBegin());
                        Node exprNode = new Node(NodeType.EXPR, "()", node.getRowBegin(), node.getRowBegin());
                        exprNode.setChildren(Arrays.asList(zeroNode, node, next));
                        children.add(exprNode);
                        preNode = exprNode;
                        continue;
                    }
                }
                throw new IllegalArgumentException(node.getType().getName() + "缺少左侧参数:" + node);
            }
            handleSerialOperator(node);
            children.add(node);
            preNode = node;
        }
        headNode.setChildren(children);
    }

    public BigDecimal evaluate(String exprText) {
        Expr expr = parseExpr(exprText);
        log.info("计算开始:{}", exprToStr(expr));
        Node result = calculate(expr, varMap);
        log.info("计算完成:{}={}", result, exprToStr(expr));
        return new BigDecimal(result.getToken());
    }

    public Node calculate(Node headNode, Map<String, BigDecimal> varMap) {
        if (headNode.getType() == NodeType.NUMBER) {
            return headNode;
        } else if (headNode.getType() == NodeType.OPERATOR) {
            Operator operator = token2Operator.get(headNode.getToken());
            List<BigDecimal> args = headNode.getChildren().stream().map(it -> calculate(it, varMap))
                    .map(it -> new BigDecimal(it.getToken())).collect(Collectors.toList());
            if (args.size() < operator.getLeftArgNum() + operator.getRightArgNum()) {
                throw new IllegalArgumentException("计算参数不足:" + headNode);
            }
            BigDecimal result = token2Func.get(headNode.getToken()).apply(args);
            return new Node(NodeType.NUMBER, result + "", headNode.getRowBegin(), headNode.getRowEnd());
        } else if (headNode.getType() == NodeType.VAR) {
            BigDecimal val = varMap.get(headNode.getToken());
            if (val == null) {
                throw new IllegalArgumentException("找不到变量:" + headNode);
            }
            return new Node(NodeType.NUMBER, val + "", headNode.getRowBegin(), headNode.getRowEnd());
        } else if (headNode.getType() == NodeType.EXPR) {
            Node result = null;
            for (Node node : headNode.getChildren()) {
                result = calculate(node, varMap);
//                if (node.getType() != NodeType.ASSIGN) {
//                    putTempVar(new BigDecimal(result.getToken()));
//                }
            }
            return result;
        } else if (headNode.getType() == NodeType.FUNCTION) {
            Expr expr = exprMap.get(headNode.getToken());
            if (expr == null) {
                throw new IllegalArgumentException("函数不存在:" + headNode);
            }
            List<Node> children = Optional.ofNullable(headNode.getChildren()).orElse(Collections.EMPTY_LIST);
            if (children.size() < expr.getArgNames().size()) {
                throw new IllegalArgumentException("函数参数不匹配:" + exprToStr(headNode) + ",expr:" + exprToStr(expr));
            }
            Iterator<String> argIterator = expr.getArgNames().iterator();
            Iterator<Node> childIterator = children.iterator();
            Map<String, BigDecimal> argMap = new LinkedHashMap<>(varMap);
            while (argIterator.hasNext()) {
                argMap.put(argIterator.next(), new BigDecimal(calculate(childIterator.next(), varMap).getToken()));
            }
            Node result = calculate(expr, argMap);
//            putTempVar(new BigDecimal(result.getToken()));
            return result;
        } else if (headNode.getType() == NodeType.ASSIGN) {
            Node varNode = headNode.getChildren().get(0);
            if (varNode.getType() != NodeType.VAR) {
                throw new IllegalArgumentException("赋值语句左侧不是变量:" + headNode);
            }
            Node result = calculate(headNode.getChildren().get(1), varMap);
            putVar(varNode.getToken(), new BigDecimal(result.getToken()));
            return result;
        } else {
            throw new IllegalArgumentException("无效的token:" + headNode);
        }
    }

    private AtomicInteger tempVarIndex = new AtomicInteger(0);

    private String getTempVarName() {
        return "$" + tempVarIndex.incrementAndGet();
    }

    public void putVar(String name, BigDecimal val) {
//        Integer maxKey = varMap.keySet().stream().filter(it -> it.matches("\\$[0-9]+"))
//                -                .map(it -> Integer.valueOf(it.substring(1))).max(Integer::compareTo).orElse(0);
        if (name.matches("\\$[0-9]+")) {
            Integer index = Integer.valueOf(name.substring(1));
            for (int maxIndex; index > (maxIndex = tempVarIndex.get()) || !tempVarIndex.compareAndSet(maxIndex, index); ) {
            }
        }
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

    public String exprToStr(Node headNode) {
//        return exprToStr(headNode, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
//    }
//
//    public static String exprToStr(Node headNode, Map<String, BigDecimal> varMap, Map<String, Expr> exprMap) {
        if (headNode.getType() == NodeType.OPERATOR || headNode.getType() == NodeType.ASSIGN) {
            if (headNode.getChildren() == null || headNode.getChildren().isEmpty()) {
                return headNode.getToken();
            }
            Operator operator = token2Operator.get(headNode.getToken());
            Iterator<Node> iterator = headNode.getChildren().iterator();
            List<Node> lefts = new LinkedList<>(), rights = new LinkedList<>();
            for (int i = 0; i < operator.getLeftArgNum(); i++) lefts.add(iterator.next());
            for (int i = 0; i < operator.getRightArgNum(); i++) rights.add(iterator.next());
            return "(" + nodesToStr(lefts) + headNode.getToken() + nodesToStr(rights) + ")";
        } else if (headNode.getType() == NodeType.FUNCTION) {
            if (headNode.getChildren() == null || headNode.getChildren().isEmpty()) {
                return headNode.getToken();
            }
            Expr expr = exprMap.get(headNode.getToken());
            Iterator<String> argNameIterator = expr.getArgNames().iterator();
            Iterator<Node> iterator = headNode.getChildren().iterator();
            String str = headNode.getToken() + "(";
            while (argNameIterator.hasNext()) {
//                str += argNameIterator.next() + "=" + exprToStr(iterator.next()) + (argNameIterator.hasNext() ? "," : ")");
                argNameIterator.next();
                str += exprToStr(iterator.next()) + (argNameIterator.hasNext() ? "," : ")");
            }
            return str;
        } else if (headNode.getType() == NodeType.NUMBER) {
            return headNode.getToken();
        } else if (headNode.getType() == NodeType.VAR) {
            BigDecimal val = varMap.get(headNode.getToken());
            return (val != null ? (val + "@") : "") + headNode.getToken();
        } else {
            return nodesToStr(headNode.getChildren());
        }
    }

    private String nodesToStr(List<Node> nodes) {
//        return nodesToStr(nodes, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
//    }
//
//    private static String nodesToStr(List<Node> nodes, Map<String, BigDecimal> varMap, Map<String, Expr> exprMap) {
        if (nodes == null || nodes.isEmpty()) return "";
        if (nodes.size() == 1) {
            return exprToStr(nodes.get(0));
        } else {
            return nodes.stream().map(it -> exprToStr(it)).collect(Collectors.joining(",", "{", "}"));
        }
    }

    private Node handleOperator(Node parentNode, boolean isStartLeft, Set<String> operatorTokens) {
        if (parentNode.getChildren() == null || parentNode.getChildren().isEmpty()) return parentNode;
        if (parentNode.getChildren().size() == 1) {
            return handleOperator(parentNode.getChildren().get(0), isStartLeft, operatorTokens);
        }
//        log.info("handleOperator111:parent={},children={}", parentNode, nodesToStr(parentNode.getChildren()));
        LinkedList<Node> children = new LinkedList<>();
        Iterator<Node> iterator = getIterator(parentNode.getChildren(), isStartLeft);
        while (iterator.hasNext()) {
            Node node = iterator.next();
            if (operatorTokens.contains(node.getToken())) {
                Operator operator = token2Operator.get(node.getToken());
                if (operator == null) {
                    throw new IllegalArgumentException("token不合法:" + node);
                }
                LinkedList<Node> argNodes = new LinkedList<>();
                int argNum1 = isStartLeft ? operator.getLeftArgNum() : operator.getRightArgNum();
                for (int i = 0; i < argNum1; i++) {
                    if (children.isEmpty()) {
                        throw new IllegalArgumentException(node.getType().getName() + (isStartLeft ? "左" : "右") + "侧参数缺失:" + node);
                    }
                    argNodes.add(isStartLeft ? 0 : argNodes.size(), children.removeLast()); // 从左往右遍历,右侧参数已处理过
                }
                int argNum2 = isStartLeft ? operator.getRightArgNum() : operator.getLeftArgNum();
                for (int i = 0; i < argNum2; i++) {
                    if (!iterator.hasNext()) {
                        throw new IllegalArgumentException(node.getType().getName() + (isStartLeft ? "右" : "左") + "侧参数缺失:" + node);
                    }
                    argNodes.add(isStartLeft ? argNodes.size() : 0, handleOperator(iterator.next(), isStartLeft, operatorTokens));
                }
                node.setChildren(argNodes);
                children.add(node);
            } else {
                node = handleOperator(node, isStartLeft, operatorTokens);
                children.add(isStartLeft ? children.size() : 0, node);
            }
        }
//        log.info("handleOperator222:parent={},children={}", parentNode, nodesToStr(children));
        parentNode.setChildren(children);
        while (parentNode.getChildren() != null && parentNode.getChildren().size() == 1) {
            parentNode = parentNode.getChildren().get(0);
        }
        return parentNode;
    }

    private static <T> Iterator<T> getIterator(List<T> list, boolean isStartLeft) {
        if (isStartLeft) {
            return list.iterator();
        }
        ListIterator<T> iterator = list.listIterator(list.size());
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasPrevious();
            }

            @Override
            public T next() {
                return iterator.previous();
            }

            @Override
            public void remove() {
                iterator.remove();
            }

            @Override
            public void forEachRemaining(Consumer<? super T> action) {
                iterator.forEachRemaining(action);
            }
        };
    }

    private void handleBracket(Node headNode, List<Node> nodes) {
        headNode.setChildren(new LinkedList<>());
        Node leftNode = headNode;
//        Map<Integer, Node> leftMap = new LinkedHashMap<>();
//        Map<Integer, Node> rightMap = new LinkedHashMap<>();
        Deque<Node> deque = new LinkedList<>();
        deque.push(leftNode);
        for (Node node : nodes) {
            if (node.getType() == NodeType.BRACKET) {
                if (leftNode.getToken().equals("(") && node.getToken().equals(")")) {
                    Node preLeft = deque.pop();
//                    preLeft.getChildren().add(leftNode);
                    leftNode.setToken("()");
                    leftNode.setType(NodeType.EXPR);
                    leftNode.setRowEnd(node.getRowEnd());
                    // 如果前面有一个变量，视为函数
                    LinkedList<Node> children = (LinkedList<Node>) preLeft.getChildren();
                    Node varNode;
                    if (children.size() > 0 && (varNode = children.pollLast()) != null) {
                        if (varNode.getType() != NodeType.VAR) {
                            children.add(varNode);
                        } else {
                            leftNode.setToken(varNode.getToken());
                            leftNode.setType(NodeType.FUNCTION);
                        }
                    }
                    children.add(leftNode);
                    leftNode = preLeft;
                } else {
                    deque.push(leftNode);
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
            } else if (symbols_assign[ch]) {
                nodes.add(new Node(NodeType.ASSIGN, ch + "", i, i + 1));
                i++;
            } else if (symbols_end[ch]) {
                nodes.add(new Node(NodeType.END, ch + "", i, i + 1));
                i++;
            } else {
                i++;
            }
        }
        return nodes;
    }

}
