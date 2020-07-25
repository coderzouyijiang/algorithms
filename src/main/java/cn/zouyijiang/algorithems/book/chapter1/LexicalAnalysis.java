package cn.zouyijiang.algorithems.book.chapter1;


import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@Slf4j
public class LexicalAnalysis {

    @Getter
    public static class LexicalPattern {
        private final int typeId;
        private final String regex;
        private List<LexicalPattern> childPatterns;

        private Pattern regexPtn;

        public LexicalPattern(TokenType tokenType, String regex) {
            Objects.requireNonNull(tokenType);
            Objects.requireNonNull(regex);
            this.typeId = tokenType.getCode();
            this.regex = regex;
            this.regexPtn = Pattern.compile(regex);
            this.childPatterns = new LinkedList<>();
        }

        public LexicalPattern(TokenType tokenType) {
            this(tokenType, null);
        }

        public LexicalPattern add(LexicalPattern childPattern) {
            Objects.requireNonNull(childPattern);
            if (childPatterns == null) {
                throw new IllegalArgumentException("regex != null");
            }
            childPatterns.add(childPattern);
            return this;
        }
    }

    public static String toRegexString(LexicalPattern ptn) {
        if (ptn.getChildPatterns().isEmpty()) {
            return ptn.getRegex();
        }
        String result = "";
        for (LexicalPattern childPtn : ptn.getChildPatterns()) {
            String str = toRegexString(childPtn);
            if (str == null || str.isEmpty()) continue;
            if (!result.isEmpty()) {
                result += "|";
            }
            if (str.contains("|")) {
                str = "(" + str + ")";
            }
            result += str;
        }
        return result;
    }

    private LexicalPattern linePattern;
    private LexicalPattern stringPattern;
    private LexicalPattern singleNotePattern;
    private LexicalPattern multiNotePattern;
    private List<LexicalPattern> wordPatterns;
    private List<LexicalPattern> decimalPatterns;

    public LexicalAnalysis() {
        // 多行注释
//        this.multiNotePattern = new LexicalPattern(TokenType.NOTE_INNER, "/\\*([^*]|\\*(?!/))*\\*/");
        this.linePattern = new LexicalPattern(TokenType.LINE, ".*\n");
        this.wordPatterns = Arrays.asList(
                // 换行
                new LexicalPattern(TokenType.NEXT_LINE, "\r?\n"),
                // 任意个空白字符，与 [ \f\n\r\t\v] 等效。
                new LexicalPattern(TokenType.BLANK, "\\s+"),
                // 字符串中，只能有\"或其他字符。可能多个字符串在一行，按最短匹配
                new LexicalPattern(TokenType.STRING, "\"(\\\\\"|.)*?\""),
                // 单行注释，一直到行尾，按最长匹配。最后的回车换行符不匹配
                new LexicalPattern(TokenType.NOTE_SINGLE, "//.*(?=\r?\n)"),
                // 行内注释
                new LexicalPattern(TokenType.NOTE_INLINE, "/\\*([^*]|\\*(?!/))*\\*/"),
                // 变量，函数名
                new LexicalPattern(TokenType.VARIABLE, "[a-zA-Z_$][\\w$]*"),
                // 预定义符号
                new LexicalPattern(TokenType.SIGN, "[,;\\.\\(\\)<>\\+\\-\\*/\\^%=!\\?:\"]"),
                // 数字字面量
//                new LexicalPattern(TokenType.DECIMAL, "\\d\\S+"),
                new LexicalPattern(TokenType.DECIMAL_RADIX2, "0[bB][01](_*[01])*"),
                new LexicalPattern(TokenType.DECIMAL_RADIX16, "0[xX][0-9a-fA-F](_*[0-9a-fA-F])*"),
                new LexicalPattern(TokenType.DECIMAL_RADIX8, "0(_[0-7])+"),
                new LexicalPattern(TokenType.DECIMAL_RADIX10, "\\d+(\\.\\d+)?")
        );
        info("wordPatterns:%s", wordPatterns);
        this.decimalPatterns = Arrays.asList(
                new LexicalPattern(TokenType.DECIMAL_RADIX2, "0[bB][01](_*[01])*"),
                new LexicalPattern(TokenType.DECIMAL_RADIX16, "0[xX][0-9a-fA-F](_*[0-9a-fA-F])*"),
                new LexicalPattern(TokenType.DECIMAL_RADIX8, "0(_[0-7])+"),
                new LexicalPattern(TokenType.DECIMAL_RADIX10, "\\d+(\\.\\d+)?")
        );
        info("decimalPatterns:%s", decimalPatterns);
    }

    // 处理完的文本
    public List<String> handleTexts = new LinkedList<>();
    // 处理完成的支付数
    public int handleOffset = 0;
    // 前一次处理剩余文本
    public String remainText = "";
    // 处理完生成的token流
    @Getter
    public List<Word> lines = new LinkedList<>();
    @Getter
    public List<Word> words = new LinkedList<>();

    /**
     * 找到行内的词素
     *
     * @param lineIndex
     * @param lineText
     * @param offset
     * @param ptns
     */
    private Word findWord(int lineIndex, String lineText, int offset, List<LexicalPattern> ptns) {
        String text = lineText.substring(offset);
        for (LexicalPattern ptn : ptns) {
            Matcher m = ptn.getRegexPtn().matcher(text);
            if (m.find() && m.start() == 0) {
                return new Word(ptn.getTypeId(), m.group(), m.start() + offset, m.end() + offset);
            }
        }
        throw new IllegalArgumentException(String.format("第%s行第%s个字符开始找不到有效的词素:%s", lineIndex, offset, text));
    }

//    /* /*private Stack*/ */
//    String s = "// dfjk//";
//    String s1 = "/*/ dfjk/*/";

    public void handle(String inputText) {
        String text = remainText + inputText;
        // 切割成行
        Matcher lineMatcher = linePattern.getRegexPtn().matcher(text);
        int offset = 0;
        for (int lineIndex = 0; lineMatcher.find(); lineIndex++) {
            String lineText = lineMatcher.group();
            Word line = new Word(TokenType.LINE.getCode(), lineText, offset + lineMatcher.start(), offset + lineMatcher.end());
            lines.add(line);
            info("解析行开始[%s]:%s", lineIndex, line);
            // 处理行内元素
            List<Word> lineWords = new LinkedList<>();
            int lineOffset = 0;
            while (lineOffset < lineText.length()) {
                Word word = findWord(lineIndex, lineText, lineOffset, wordPatterns);
                lineOffset = word.getEnd();
                lineWords.add(word);
                info("%s", word);
            }
            words.addAll(lineWords);
            line.setChildWords(lineWords);
            handleTexts.add(line.getValue());
            handleOffset += lineOffset;
            offset += lineOffset;
            info("解析行开始完成");
        }
        remainText = text.substring(offset);
        info("此次处理剩余文本:%s", remainText);
    }

    private void info(String format, Object... objs) {
        System.out.printf(format, objs);
        System.out.println();
    }

}
