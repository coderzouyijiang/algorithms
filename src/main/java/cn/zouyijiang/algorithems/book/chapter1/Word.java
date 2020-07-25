package cn.zouyijiang.algorithems.book.chapter1;

import lombok.Data;

import java.util.List;

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
}
