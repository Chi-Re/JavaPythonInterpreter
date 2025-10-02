package chire.python.util.handle;

import chire.python.util.Test;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 修复版的 SubClass，确保每个方法有独立的注册键，支持自定义构造函数
 */
public class SubClass {
    private final String className;
    private final Class<?> superType;
    private DynamicType.Builder<?> builder;

    private final Map<String, Object> variables = new HashMap<>();

    public SubClass(String name) {
        this(name, Object.class);
    }

    public SubClass(String name, Class<?> superType) {
        this.className = name;
        this.superType = superType;
        this.builder = new ByteBuddy()
                .subclass(superType)
                .name(name);
    }

    /**
     * 方法拦截器 - 根据方法名查找注册键
     */
    public static class MethodInterceptor {
        private final Function<Object[], Object> call;

        public MethodInterceptor(Function<Object[], Object> call) {
            this.call = call;
        }

        @RuntimeType
        public Object intercept(
                @Origin Method method,
                @This Object self,
                @AllArguments Object[] args) {
            return call.apply(args != null ? args : new Object[0]);
        }
    }

    public static class ConstructorInterceptor {
        private final Function<Object[], Object> call;

        public ConstructorInterceptor(Function<Object[], Object> call) {
            this.call = call;
        }

        @RuntimeType
        public Object intercept(@AllArguments Object[] args) {
            return call.apply(args != null ? args : new Object[0]);
        }
    }

    /**
     * 添加无参方法
     */
    public void addMethod(String methodName, Class<?> returnType, Supplier<?> supplier) {
        addMethod(methodName, returnType, new Class<?>[0], args -> supplier.get());
    }

    /**
     * 添加带参数的方法
     */
    public void addMethod(String methodName, Class<?> returnType, Class<?>[] parameterTypes, Function<Object[], Object> function) {
        if (parameterTypes.length == 0) {
            builder = builder.defineMethod(
                    methodName,
                    returnType,
                    Visibility.PUBLIC
            ).intercept(MethodDelegation.to(new MethodInterceptor(function)));
        } else {
            builder = builder.defineMethod(
                            methodName,
                            returnType,
                            Visibility.PUBLIC
                    ).withParameters(parameterTypes)
                    .intercept(MethodDelegation.to(new MethodInterceptor(function)));
        }
    }

    /**
     * 添加变量
     */
    public void addVariable(String name, Object value) {
        Class<?> type = value != null ? value.getClass() : Object.class;
        addVariable(name, type, value);
    }

    /**
     * 添加带类型的变量
     */
    public void addVariable(String name, Class<?> type, Object value) {
        builder = builder.defineField(name, type, Visibility.PUBLIC);
        variables.put(name, value);
    }

    /**
     * 添加或覆盖构造函数
     */
    public void addConstructor(Class<?>[] parameterTypes, Function<Object[], Object> constructorLogic) {
        try {
            builder = builder.defineConstructor(Modifier.PUBLIC)
                    .withParameters(parameterTypes)
                    .intercept(
                            MethodCall.invoke(superType.getConstructor())
                                    .andThen(MethodDelegation.to(new ConstructorInterceptor(constructorLogic)))
                    );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建类
     */
    public Class<?> create() {
        try {
            return builder.make().load(
                    getClass().getClassLoader(),
                    ClassLoadingStrategy.Default.WRAPPER
            ).getLoaded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create class: " + e.getMessage(), e);
        }
    }

    /**
     * 创建实例（无参）
     */
    public Object newInstance() {
        return newInstance(new Object[0]);
    }

    /**
     * 创建实例（带参数）
     */
    public Object newInstance(Object[] constructorArgs) {
        Class<?> generatedClass = create();

        if (generatedClass == null) {
            throw new IllegalStateException("Class not created. Call create() first.");
        }

        try {
            // 查找匹配的构造函数
            Constructor<?> matchedConstructor;
            Class<?>[] parameterTypes = Arrays.stream(constructorArgs).filter(Objects::nonNull).map(Object::getClass).toArray(Class<?>[]::new);

            // 根据参数类型查找构造函数
            if (parameterTypes.length == 0) {
                matchedConstructor = generatedClass.getDeclaredConstructor();
            } else {
                matchedConstructor = generatedClass.getDeclaredConstructor(parameterTypes);
            }

            // 创建实例
            Object instance;
            if (parameterTypes.length == 0) {
                instance = matchedConstructor.newInstance();
            } else {
                instance = matchedConstructor.newInstance(constructorArgs);
            }

            // 设置变量值
            for (var name : variables.keySet()) {
                try {
                    Field field = generatedClass.getDeclaredField(name);
                    field.setAccessible(true);
                    field.set(instance, variables.get(name));
                } catch (NoSuchFieldException e) {
                    System.err.println("Field '" + name + "' not found in generated class");
                }
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance: " + e.getMessage(), e);
        }
    }

//    public static void main(String[] a) {
//        // 创建子类
//        SubClass subClass = new SubClass("MyDynamicClass");
//
//        subClass.addVariable("a", Integer.class, 1);
//
//        // 覆盖为带参构造函数
//        subClass.addConstructor(new Class<?>[]{String.class, Integer.class}, args -> {
//            System.out.println("带参构造函数被调用，参数: " + Arrays.toString(args));
//            return null;
//        });
//
//        // 创建实例
//        Object instance = subClass.newInstance(new Object[]{"test", 2});
//    }
}