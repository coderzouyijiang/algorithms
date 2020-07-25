package cn.zouyijiang.algorithems;

import cn.zouyijiang.algorithems.book.chapter1.LexicalAnalysis;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RunWith(JUnit4.class)
public class Test3_regex {

    @Test
    public void test() {
        Pattern ptn1 = Pattern.compile("Windows (?=95|98|NT|2000)");
        Pattern ptn2 = Pattern.compile("Windows (?!95|98|NT|2000)");
        String str1 = "Windows 2000";
        String str2 = "Windows 98";
        Matcher m11 = ptn1.matcher(str1);
        log.info("" + m11.find());
        Matcher m12 = ptn1.matcher(str2);
        log.info("" + m12.find());
        Matcher m21 = ptn2.matcher(str1);
        log.info("" + m21.find());
        Matcher m22 = ptn2.matcher(str2);
        log.info("" + m22.find());
    }

    private List<String> match(String regex, String text) {
        Pattern ptn1 = Pattern.compile(regex);
        Matcher m1 = ptn1.matcher(text);
        List<String> list = new LinkedList<>();
        log.info("regex:{}", regex);
        log.info("text:{}", text);
        while (m1.find()) {
            list.add(m1.group());
            log.info("[{},{}):{}", m1.start(), m1.end(), m1.group());
        }
//        log.info("【regex】：{},【text】：{},\n【find】：{}", regex, text, list);
        log.info("");
        return list;
    }

    @Test
    public void test2() {
        Pattern ptn1 = Pattern.compile("[a-zA-Z_$][\\w$]*\\s*\\(");
        Matcher m1 = ptn1.matcher("max(1,2)+min\n\t (3,4)");
        while (m1.find()) {
            String group = m1.group();
            log.info("" + group);
        }
        log.info("");
    }

    @Test
    public void test3() {
//        match("\\s*(//.*)(\\n)", "// dksfjskdj //sdfjsdfjsk\n\tdksfsk\n   //  sdkfsk\n");
        int i2 = 0B1_0; // 二进制
        int i16 = 0xA_b_C; // 十六进制
        int i8 = 0_7123; // 8进制
        String text = "max  (0712+ max (1.1, d),Math.abs(b))+ pow(x,0xABc)+0+0b1_01*0x3Ffa+01234567+1-1.2/0.1+\"+a+\"";
//        match("[a-zA-Z_$][\\w$]*\\s*(\\($0(\\s*,\\s*$0)*\\))?|[\\(\\)\\+\\-\\*/\\^=]", text);
        match("[a-zA-Z_$][\\w$]*|" +
                "(0[bB][01](_*[01])*|0[xX][0-9a-fA-F](_*[0-9a-fA-F])*|0(_[0-7])+|\\d+(\\.\\d+)?)|" +
                "[\\.\\(\\)\\+\\-\\*/\\^=\"]", text);
    }

    @Test
    public void test_LexicalAnalysis() {
        LexicalAnalysis lexicalAnalysis = new LexicalAnalysis();
        log.info("");
    }

    @Test
    public void test_str() {
        String text = "\"k1\"+\"k2\\\"k3\\\"\"+\"k4\"\"";
        match("\"(\\\\\"|[^\"])*?\"", text);
        match("\"(\\\\\"|.)*?\"", text);

        String text2 = "\"k1\"+\"k2\\\"k3\"\"+\"k4\"\"";
        match("\"(\\\\\"|[^\"])*?\"", text2);
        match("\"(\\\\\"|.)*?\"", text2);
//        match("\"([^\"]|\\\\\")*?\"", text);
//        match("\\\\\"", "\\\"");
//        match(".*\n", "dskfdkfs\n djskfk\t\nskfskd\n");
    }

    @Test
    public void test_note() {
        String text = "a+max(b,c) // min(a,b)\n" +
                "x+b*c // a=b+c // dkfdk\r\n";
        match("//.*(?=\r?\n)", text);
    }

//    /* /*
//    dskfsk
//     */*/
    @Test
    public void test_multiNote() {
        String text = "a+max(b,c) /* min(a,b)\n" +
                "x+b*c;a=b+c; dkfdk\n" +
                "x+b*c */ a=b+c; dkfdk\n";
        String text2 = "/* /*\n" +
                "    dskfsk\n" +
                "     */*/";
        match("/\\*([^*]|\\*(?!/))*\\*/", "/*kdfjlsdj*/");
        match("/\\*([^*]|\\*(?!/))*\\*/", "/* kdfj\nlsdj*/dksfjdks/*skdfjsk\tksjs */");
//        match("/\\*[.\r\n]*\\*/", text);
//        match("/\\*[.\r\n]*\\*/", text2);
    }

}
