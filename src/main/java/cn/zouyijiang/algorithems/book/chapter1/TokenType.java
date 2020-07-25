package cn.zouyijiang.algorithems.book.chapter1;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TokenType {

    NOTE_SINGLE(1, "单行注释"),
    NOTE_INLINE(101, "行内注释"),
    LINE(2, "行"),
    NEXT_LINE(3, "换行"),
    BLANK(4, "空白"),
    STRING(5, "字符串"),
    VARIABLE(6, "变量"),
    SIGN(7, "符号"),
    DECIMAL_RADIX2(8, "二进制数字"),
    DECIMAL_RADIX16(9, "十六进制数字"),
    DECIMAL_RADIX8(10, "八进制数字"),
    DECIMAL_RADIX10(11, "十进制数字"),

    BRACKETS(1001, "括号");

    private final int code;
    private final String name;

    @Override
    public String toString() {
        return code + "-" + name;
    }

    public static TokenType codeOf(int id) {
        for (TokenType token : TokenType.values()) {
            if (token.code == id) return token;
        }
        return null;
    }
}
