package cn.zouyijiang.algorithems.book.chapter1;

import lombok.Data;

import java.util.List;

@Data
public class Expr {

    private int typeId;

    private List<Word> words;

    private List<Expr> childExprs;

    public Expr(int typeId, List<Word> words) {
        this.typeId = typeId;
        this.words = words;
    }
}
