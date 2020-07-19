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
        BigDecimal result = evaluate.evaluate("(2-1)*3+(4*(6-1)*7-(9)+8)");
//        BigDecimal result = evaluate.evaluate("(2-1)*3+(4*(6-1)*7-(9)+8))");
//        BigDecimal result = evaluate.evaluate("11*3+2-(3*(9-(12*2)))");
        log.info("result:{},{}", (2 - 1) * 3 + (4 * (6 - 1) * 7 - (9) + 8), result);
    }
}
