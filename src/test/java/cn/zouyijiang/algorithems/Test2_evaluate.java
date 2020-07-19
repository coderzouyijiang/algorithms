package cn.zouyijiang.algorithems;

import cn.zouyijiang.algorithems.book.chapter1.Evaluate;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;

@Slf4j
@RunWith(JUnit4.class)
public class Test2_evaluate {

    @Test
    public void test() {
        Evaluate evaluate = new Evaluate();
//        BigDecimal result = evaluate.evaluate("11*3+2-3*(9-12*2)");
//        BigDecimal result = evaluate.evaluate("(2-1)*3+(4*(6-1)*7-(9)+8))");
//        BigDecimal result = evaluate.evaluate("11*3+2-(3*(9-(12*2)))");

        log.info("result:{},{}", (2 - 1) * 3 + (4 * (6 - 1) * 7 - (9) + 8), evaluate.evaluate("(2-1)*3+(4*(6-1)*7-(9)+8)"));
        log.info("result:{},{}", 1.2 * (44.5 - 22), evaluate.evaluate("1.2 * (44.5 - 22)"));
    }

    @Test
    public void test_var() {
        Evaluate evaluate = new Evaluate();
        evaluate.putVar("a", new BigDecimal("1"));
        evaluate.putVar("b", new BigDecimal("12.34"));
        log.info("result:{},{}", 1.2 * (44.5 - 1) * 12.34, evaluate.evaluate("1.2 * (44.5 - a)*b"));
        log.info("vars:" + evaluate.getVars());
        log.info("result:{},{}", 6 * Math.PI * Math.PI, evaluate.evaluate("6*pi*pi"));
        log.info("vars:" + evaluate.getVars());
    }

    @Test
    public void test_negate() {
        Evaluate evaluate = new Evaluate();
        evaluate.putVar("a", new BigDecimal("1"));
        log.info("result:{},{}", 2 + -11, evaluate.evaluate("2+-11"));
//        log.info("result:{},{}", 2 + -11, evaluate.evaluate("2+(-11)"));
    }

}
