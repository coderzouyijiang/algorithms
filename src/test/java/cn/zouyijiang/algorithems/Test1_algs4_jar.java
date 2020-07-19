package cn.zouyijiang.algorithems;

import edu.princeton.cs.algs4.StdDraw;
import edu.princeton.cs.algs4.StdIn;
import edu.princeton.cs.algs4.StdOut;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Slf4j
@RunWith(JUnit4.class)
public class Test1_algs4_jar {

    // 得使用debug maven -> debug test()
    @Test
    public void test() {
        StdOut.print("Type a string: ");
        String s = StdIn.readString();
        StdOut.println("Your string was: " + s);
        StdOut.println();
        StdOut.print("Type an int: ");
        int a = StdIn.readInt();
        StdOut.println("Your int was: " + a);
        StdOut.println();
        StdOut.print("Type a boolean: ");
        boolean b = StdIn.readBoolean();
        StdOut.println("Your boolean was: " + b);
        StdOut.println();
        StdOut.print("Type a double: ");
        double c = StdIn.readDouble();
        StdOut.println("Your double was: " + c);
        StdOut.println();
    }

    @Test
    public void test_draw() {

        StdDraw.point(0.2, 0.4);
        StdDraw.circle(0.2, 0.4, 0.05);
        StdDraw.line(0.1, 0.1, 0.8, 0.8);
        StdDraw.show(5);
        StdDraw.clear();
        StdDraw.point(0.2, 0.8);
        StdDraw.circle(0.2, 0.8, 0.05);
        StdDraw.line(0.1, 0.8, 0.3, 0.8);
        StdDraw.show(10);
    }
}
