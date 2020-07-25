package cn.zouyijiang.algorithems.book.chapter1;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class Word {
    private int typeId;
    private String value;

    // 在源码中的位置,[start,end)
    private int start;
    private int end;

    public Word(int typeId, String value, int start, int end) {
        this.typeId = typeId;
        this.value = value;
        this.start = start;
        this.end = end;
    }

    private List<Word> childWords;

    @Override
    public String toString() {
        return TokenType.codeOf(typeId) + "[" + start + "," + end + "):【" + value + "】";
    }

    public static String joinWords(List<Word> words) {
        return words.stream().map(it -> it.getValue()).collect(Collectors.joining());
    }
}

