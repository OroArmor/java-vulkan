package com.oroarmor.profiler;

public class TestTest {
    public static void main(String[] args) {
        Test test = new Test();
        test.multipleReturn();
        Test.exception();
        test.multipleReturnValue();
        Test.test();
        System.out.println(Profiler.PROFILER.dump());
    }
}
