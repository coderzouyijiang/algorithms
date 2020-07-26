package cn.zouyijiang.algorithems.book.chapter1;

import com.sun.corba.se.impl.oa.toa.TOA;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class GrammarAnalysis {

    public static final Set<Integer> blankTypeIds = Arrays.asList(
            TokenType.NOTE_INLINE, TokenType.NOTE_SINGLE, TokenType.NEXT_LINE, TokenType.BLANK)
            .stream().map(it -> it.getCode()).collect(Collectors.toSet());

    private List<Word> handleWorkds = new LinkedList<>();
    private List<Word> words = new LinkedList<>();

    public void handle(List<Word> words) {
        handleWorkds.addAll(words);
        List<Word> words2 = words.stream()
                .map(it -> new Word(it.getTypeId(), it.getValue(), it.getStart(), it.getEnd()))
                .collect(Collectors.toList());
        log.info("处理括号开始:");
        handleBrackets(words2);
        log.info("处理子语句:");
        handleExpr(words2);
        log.info("处理复合变量和方法调用开始:");
        handleVariableAndMethodCall(words2);

        this.words.addAll(words2);
    }

    public void handleBrackets(List<Word> words) {
        if (words.isEmpty()) return;
        ListIterator<Word> wordIterator = words.listIterator(0);
        while (wordIterator.hasNext()) {
            Word word = wordIterator.next();
            if (blankTypeIds.contains(word.getTypeId())) {
                wordIterator.remove();
                continue;
            }
            if (!(word.getTypeId() == TokenType.SIGN.getCode() && word.getValue().equals(")"))) {
                continue;
            }
            // 发现右括号时
            LinkedList<Word> subWords = new LinkedList<>();
            wordIterator.remove();
            while (wordIterator.hasPrevious()) {
                Word word2 = wordIterator.previous();
                if (word2.getTypeId() == TokenType.SIGN.getCode() && word2.getValue().equals("(")) {
                    word2.setTypeId(TokenType.BRACKETS.getCode());
                    word2.setValue("(" + Word.joinWords(subWords) + ")");
                    word2.setEnd(subWords.getLast().getEnd());
                    word2.setChildWords(subWords);
                    log.info("subWorkTree:{}", word2.getValue());
                    subWords = null;
                    break;
                }
                subWords.addFirst(word2);
                wordIterator.remove();
            }
            if (subWords != null) {
                throw new IllegalArgumentException("找不到对应的左括号:" + word);
            }
        }
    }

    public void handleVariableAndMethodCall(List<Word> words) {
        if (words.isEmpty()) return;
        LinkedList<Word> subWords = null;
        ListIterator<Word> wordIterator = words.listIterator(0);
        while (wordIterator.hasNext()) {
            Word word = wordIterator.next();
            if (word.getTypeId() == TokenType.VARIABLE.getCode()) {
                if (subWords == null) {
                    subWords = new LinkedList<>();
//                } else if (subWords.size() % 2 != 0) {
//                    throw new IllegalArgumentException("语法错误，有连续多个变量名:" + word);
                } else {
                    wordIterator.remove();
                }
                subWords.add(word);
            } else if (word.getTypeId() == TokenType.SIGN.getCode() && word.getValue().equals(".")) {
                if (subWords == null || subWords.size() % 2 != 1) {
                    throw new IllegalArgumentException("语法错误，有连续多个符号:" + word);
                }
                subWords.add(word);
                wordIterator.remove();
            } else if (word.getTypeId() == TokenType.BRACKETS.getCode() || word.getTypeId() == TokenType.EXPR.getCode()) {
                if (subWords != null) {
                    wordIterator.remove();
                    // 函数调用 a.b(c,d)+
                    Word word2 = subWords.getFirst();
                    Word word3 = new Word(TokenType.COMPLEX_VARIABLE.getCode(), Word.joinWords(subWords)
                            , word2.getStart(), subWords.getLast().getEnd());
                    word3.setChildWords(subWords);
                    List<Word> childWords = Arrays.asList(word3, word);

                    word2.setTypeId(TokenType.METHOD_CALL.getCode());
                    word2.setValue(Word.joinWords(childWords));
                    word2.setEnd(word.getEnd());
                    word2.setChildWords(childWords);
                    log.info("方法调用:{}", word2.getValue());
                    subWords = null;
                }
                handleVariableAndMethodCall(word.getChildWords());
            } else if (subWords != null) {
                // a.b+
                Word word2 = subWords.getFirst();
                word2.setTypeId(TokenType.COMPLEX_VARIABLE.getCode());
                word2.setValue(Word.joinWords(subWords));
                word2.setEnd(subWords.getLast().getEnd());
                word2.setChildWords(subWords);
                log.info("复合变量:{}", word2.getValue());
                subWords = null;
            }
        }
        if (subWords != null) {
            // a.b+
            Word word2 = subWords.getFirst();
            word2.setTypeId(TokenType.COMPLEX_VARIABLE.getCode());
            word2.setValue(Word.joinWords(subWords));
            word2.setEnd(subWords.getLast().getEnd());
            word2.setChildWords(null);
            log.info("复合变量:{}", word2.getValue());
            subWords = null;
        }
    }

    private void handleExpr(List<Word> words) {
        if (words == null || words.isEmpty()) return;
        LinkedList<Word> newWords = new LinkedList<>();
        LinkedList<Word> subWords = new LinkedList<>();
        ListIterator<Word> wordIterator = words.listIterator(0);
        while (wordIterator.hasNext()) {
            Word word = wordIterator.next();
            wordIterator.remove();
            if (word.getTypeId() == TokenType.SIGN.getCode()
                    && (word.getValue().equals(",") || word.getValue().equals(";"))) {
                if (subWords.isEmpty()) {
                    throw new IllegalArgumentException("风格符位置错误:" + word);
                }
                if (subWords.size() == 1) {
                    newWords.add(subWords.getFirst());
                } else {
                    Word expr = new Word(TokenType.EXPR.getCode(), subWords);
                    log.info("expr:{}", expr);
                    newWords.add(expr);
                }
                subWords = new LinkedList<>();
            } else {
                subWords.add(word);
                if (word.getTypeId() == TokenType.METHOD_CALL.getCode()) {
                    handleExpr(word.getChildWords().get(1).getChildWords());
                } else if (word.getTypeId() == TokenType.BRACKETS.getCode()) {
                    handleExpr(word.getChildWords());
                }
            }
        }
        if (subWords.size() == 1) {
            newWords.add(subWords.getFirst());
        } else if (subWords.size() > 1) {
            Word expr = new Word(TokenType.EXPR.getCode(), subWords);
            log.info("expr:{}", expr);
            newWords.add(expr);
        }
        words.addAll(newWords);
    }
}
