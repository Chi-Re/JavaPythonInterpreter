package chire.python.util;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class Test {
    public static void main(String[] args) throws Exception {
        Class<?> dynamicClass = new ByteBuddy()
                .subclass(Greeter.class)
                .defineConstructor(Modifier.PUBLIC)
                .withParameters(String.class)
                .intercept(MethodCall.invoke(Greeter.class.getConstructor())
                        .andThen(MethodDelegation.to(new ParameterInterceptor())))
                .make()
                .load(Test.class.getClassLoader())
                .getLoaded();

        dynamicClass.getDeclaredConstructor(String.class).newInstance("aaa");
    }

    public static class ParameterInterceptor {
        public ParameterInterceptor() {

        }

        @RuntimeType
        public void intercept(@AllArguments Object[] args) {
            System.out.println("参数: " + Arrays.toString(args));
        }
    }

    public static class Teee{
        public String name;

        public Teee(String name) {
            this.name = name;
        }

        public void intercept(@AllArguments Object[] args) {
            System.out.println("this is "+this.name);
        }
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
