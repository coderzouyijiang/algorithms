package cn.zouyijiang.algorithems;

import cn.zouyijiang.algorithems.book.chapter1.GrammarAnalysis;
import cn.zouyijiang.algorithems.book.chapter1.LexicalAnalysis;
import cn.zouyijiang.algorithems.book.chapter1.TokenType;
import cn.zouyijiang.algorithems.book.chapter1.Word;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RunWith(JUnit4.class)
public class Test3_LexicalAnalysis {

    private LexicalAnalysis lexicalAnalysis;

    @Before
    public void before() {
        this.lexicalAnalysis = new LexicalAnalysis();
    }


    @Test
    public void test() {
        String text = "max  (0712+ max (1.1, d),Math.abs(b))+ pow(x,0xABc)+0+0b1_01*0x3Ffa+01234567+1-1.2/0.1+\"+a+\"\n" +
                " a+\"k\"+c\n" +
                " \"kk\"k\" /* kdfdk /* kdfdk */ +8888 +9999\n" +
                " \"k1\"+\"k2\\\"k3\\\"\"\n" +
                " max(a,b)+ /*min(c,d);*/ min(c2,d2)\n" +
                " \t  \t a+2-4 // max(a,b)+ /*min(c,d);*/ min(c2,d2)\n";

        lexicalAnalysis.handle(text);
        List<Word> words = lexicalAnalysis.getWords();
        String str = words.stream().map(it -> it.getValue()).collect(Collectors.joining());
        Assert.assertEquals(text, str);
        log.info("");
        Set<Integer> blankTypeIds = Arrays.asList(TokenType.NOTE_INLINE, TokenType.NOTE_SINGLE, TokenType.NEXT_LINE, TokenType.BLANK)
                .stream().map(it -> it.getCode()).collect(Collectors.toSet());
        String trimText = words.stream().filter(it -> !blankTypeIds.contains(it.getTypeId()))
                .map(it -> it.getValue()).collect(Collectors.joining());
        log.info("trimText:\n{}", trimText);

        GrammarAnalysis grammerAnalysis = new GrammarAnalysis();
        grammerAnalysis.handle(words);
        log.info("");
    }

}
