package com.oroarmor.profiler;

public class Test {
    @Profile
    public Test(){
    }

    @Profile
    public static void test() {
        System.out.println("wow expensive");
    }

    @Profile
    public static void exception() {
        try {
            if (System.currentTimeMillis() == 1000) {
                throw new RuntimeException();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Profile("Return multiple")
    public void multipleReturn() {
        if (System.currentTimeMillis() == 1000) {
            return;
        }
        System.out.println("not 1000");
    }

    @Profile
    public boolean multipleReturnValue() {
        if (System.currentTimeMillis() == 1000) {
            return false;
        }
        System.out.println("not 1000");
        return true;
    }
}
