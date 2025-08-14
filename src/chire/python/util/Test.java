package chire.python.util;

import chire.python.py.PyList;

import java.lang.reflect.Method;
import java.util.Arrays;

public class Test {
    public static void main(String[] args) throws Exception {
//        Class<?> dynamicClass = new ByteBuddy()
//                .subclass(Greeter.class)
//                .name("Dynamic")
//                .make()
//                .load(Test.class.getClassLoader())
//                .getLoaded();
//
//        Greeter instance = (Greeter) dynamicClass.getDeclaredConstructor().newInstance();
//
//        System.out.println(instance.greet("cs"));
    }

    public static class Greeter{
        public String name;

        public Greeter() {
        }

        public String greet(String name) {
            return "Hello, " + name;
        }

        @Override
        public String toString() {
            return "Test{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
