package chire.python.util.handle;

import chire.python.antlr.callable.PyFunction;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
        private final PyFunction<Object[], Object> call;

        public ConstructorInterceptor(PyFunction<Object[], Object> call) {
            this.call = call;
        }

        @RuntimeType
        public void intercept(@This Object self, @AllArguments Object[] args) {
            call.apply(self, args);
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
    public void addConstructor(Class<?>[] parameterTypes, PyFunction<Object[], Object> constructorLogic) {
        try {
            if (parameterTypes.length != 0) {
                builder = builder.defineConstructor(Modifier.PUBLIC)
                        .withParameters(parameterTypes)
                        .intercept(
                                MethodCall.invoke(superType.getConstructor())
                                        .andThen(MethodDelegation.to(new ConstructorInterceptor(constructorLogic)))
                        );
            } else {
                builder = builder.constructor(ElementMatchers.isConstructor())
                        .intercept(
                                MethodCall.invoke(superType.getConstructor())
                                        .andThen(MethodDelegation.to(new ConstructorInterceptor(constructorLogic)))
                        );
            }
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
            // 检查是否所有参数都为null，如果是则使用无参构造
            boolean allNull = Arrays.stream(constructorArgs).allMatch(Objects::isNull);

            Constructor<?> matchedConstructor;
            Object[] actualArgs;

            if (allNull || constructorArgs.length == 0) {
                matchedConstructor = generatedClass.getDeclaredConstructor();
                actualArgs = new Object[0];
            } else {
                // 处理包含null的参数
                Class<?>[] parameterTypes = new Class<?>[constructorArgs.length];
                actualArgs = new Object[constructorArgs.length];

                for (int i = 0; i < constructorArgs.length; i++) {
                    if (constructorArgs[i] != null) {
                        parameterTypes[i] = constructorArgs[i].getClass();
                        actualArgs[i] = constructorArgs[i];
                    } else {
                        // 对于null参数，使用Object.class作为类型
                        parameterTypes[i] = Object.class;
                        actualArgs[i] = null;
                    }
                }

                matchedConstructor = generatedClass.getDeclaredConstructor(parameterTypes);
            }

            // 创建实例
            Object instance = matchedConstructor.newInstance(actualArgs);

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