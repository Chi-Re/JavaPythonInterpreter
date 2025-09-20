package chire.python.util;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class Test {
    public static void main(String[] args) throws Exception {
//        Class<?> dynamicClass = new ByteBuddy()
//                .subclass(Greeter.class)
//                .name("Dynamic")
//                .method(
//                        named("greet").and(takesArguments(1)).and(returns(TypeDescription.ForLoadedType.of(String.class)))
//                )
//                .intercept(
//                        FixedValue.value("Hello, World!")
//                )
//                .make()
//                .load(Test.class.getClassLoader())
//                .getLoaded();
//
//        Greeter instance = (Greeter) dynamicClass.getDeclaredConstructor().newInstance();
//
//        System.out.println(instance.greet("sss"));

//        Class<?> personClass = Class.forName("chire.python.util.Test$Greeter");
//
//        System.out.println(personClass.getMethod("greet", String.class));

        System.out.println(new boolean[]{true, false}.length);
    }

    public static class Greeter{
        public String name;

        public Greeter() {
        }

        public String greet(String name) {
            return "Hello, " + name;
        }

        public  String greet() {
            return "World!";
        }

        @Override
        public String toString() {
            return "Test{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
