package cn.zouyijiang.algorithems.book.chapter1;

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

    public void handle(List<Word> words) {
        handleWorkds.addAll(words);

        List<Word> workTree = words.stream()
                .map(it -> new Word(it.getTypeId(), it.getValue(), it.getStart(), it.getEnd()))
                .collect(Collectors.toList());
        ListIterator<Word> wordIterator = workTree.listIterator(0);
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

}
