package chire.python.util.handle;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 修复版的 SubClass，确保每个方法有独立的注册键
 */
public class SubClass {
    private final String className;
    private final Class<?> superType;
    private final Map<String, MethodDefinition> methods = new HashMap<>();
    private final Map<String, VariableDefinition> variables = new HashMap<>();
    private Class<?> generatedClass;

    // 全局方法注册表
    private static final Map<String, Function<Object[], Object>> METHOD_REGISTRY = new ConcurrentHashMap<>();

    public String getClassName() {
        return className;
    }

    /**
     * 方法定义
     */
    private static class MethodDefinition {
        final String name;
        final Class<?> returnType;
        final Class<?>[] parameterTypes;
        final String registryKey;
        final boolean hasParameters;

        MethodDefinition(String name, Class<?> returnType, Class<?>[] parameterTypes, String registryKey) {
            this.name = name;
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
            this.registryKey = registryKey;
            this.hasParameters = parameterTypes != null && parameterTypes.length > 0;
        }
    }

    /**
     * 变量定义
     */
    private static class VariableDefinition {
        final String name;
        final Class<?> type;
        final Object value;

        VariableDefinition(String name, Class<?> type, Object value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }
    }

    /**
     * 方法拦截器 - 根据方法名查找注册键
     */
    public static class MethodInterceptor {
        @RuntimeType
        public static Object intercept(
                @Origin Method method,
                @This Object self,
                @AllArguments Object[] args,
                @FieldValue("methodRegistryKeys") Map<String, String> registryKeys) {

            String methodName = method.getName();
            String registryKey = registryKeys.get(methodName);

            if (registryKey == null) {
                throw new IllegalStateException("No registry key found for method: " + methodName);
            }

            Function<Object[], Object> function = METHOD_REGISTRY.get(registryKey);
            if (function != null) {
                // 安全地处理参数
                try {
                    return function.apply(args != null ? args : new Object[0]);
                } catch (Exception e) {
                    throw new RuntimeException("Error executing method: " + methodName, e);
                }
            }
            throw new IllegalStateException("No method registered for key: " + registryKey);
        }
    }

    public SubClass(String name) {
        this(name, Object.class);
    }

    public SubClass(String name, Class<?> superType) {
        this.className = name;
        this.superType = superType;
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
        String registryKey = className + "_" + methodName + "_" + UUID.randomUUID().toString();
        METHOD_REGISTRY.put(registryKey, function);
        methods.put(methodName, new MethodDefinition(methodName, returnType, parameterTypes, registryKey));
    }

    /**
     * 添加变量
     */
    public void addVariable(String name, Object value) {
        Class<?> type = value != null ? value.getClass() : Object.class;
        variables.put(name, new VariableDefinition(name, type, value));
    }

    /**
     * 添加带类型的变量
     */
    public void addVariable(String name, Class<?> type, Object value) {
        variables.put(name, new VariableDefinition(name, type, value));
    }

    /**
     * 创建类
     */
    public Class<?> create() {
        try {
            // 开始构建类
            ByteBuddy byteBuddy = new ByteBuddy();
            DynamicType.Builder<?> builder = byteBuddy
                    .subclass(superType)
                    .name(className)
                    .defineField("methodRegistryKeys", Map.class, Visibility.PRIVATE);

            // 添加变量字段
            for (VariableDefinition varDef : variables.values()) {
                builder = builder.defineField(varDef.name, varDef.type, Visibility.PRIVATE)
                        .defineMethod("get" + capitalize(varDef.name), varDef.type, Visibility.PUBLIC)
                        .intercept(net.bytebuddy.implementation.FieldAccessor.ofField(varDef.name))
                        .defineMethod("set" + capitalize(varDef.name), void.class, Visibility.PUBLIC)
                        .withParameter(varDef.type)
                        .intercept(net.bytebuddy.implementation.FieldAccessor.ofField(varDef.name));
            }

            // 添加所有方法
            for (MethodDefinition methodDef : methods.values()) {
                if (methodDef.parameterTypes.length == 0) {
                    builder = builder.defineMethod(
                            methodDef.name,
                            methodDef.returnType,
                            Visibility.PUBLIC
                    ).intercept(MethodDelegation.to(MethodInterceptor.class));
                } else {
                    builder = builder.defineMethod(
                                    methodDef.name,
                                    methodDef.returnType,
                                    Visibility.PUBLIC
                            ).withParameters(methodDef.parameterTypes)
                            .intercept(MethodDelegation.to(MethodInterceptor.class));
                }
            }

            // 创建类
            DynamicType.Unloaded<?> dynamicType = builder.make();

            // 加载类
            generatedClass = dynamicType.load(
                    getClass().getClassLoader(),
                    ClassLoadingStrategy.Default.WRAPPER
            ).getLoaded();

            return generatedClass;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create class: " + e.getMessage(), e);
        }
    }

    /**
     * 创建实例
     */
    public Object newInstance() {
        if (generatedClass == null) {
            throw new IllegalStateException("Class not created. Call create() first.");
        }

        try {
            Object instance = generatedClass.newInstance();

            // 创建方法注册键映射
            Map<String, String> registryKeys = new HashMap<>();
            for (MethodDefinition methodDef : methods.values()) {
                registryKeys.put(methodDef.name, methodDef.registryKey);
            }

            // 设置方法注册键映射
            Field registryField = generatedClass.getDeclaredField("methodRegistryKeys");
            registryField.setAccessible(true);
            registryField.set(instance, registryKeys);

            // 设置变量值
            for (VariableDefinition varDef : variables.values()) {
                try {
                    Field field = generatedClass.getDeclaredField(varDef.name);
                    field.setAccessible(true);
                    field.set(instance, varDef.value);
                } catch (NoSuchFieldException e) {
                    // 如果字段不存在，尝试使用setter方法
                    String setterName = "set" + capitalize(varDef.name);
                    try {
                        Method setter = generatedClass.getMethod(setterName, varDef.type);
                        setter.invoke(instance, varDef.value);
                    } catch (Exception ex) {
                        System.err.println("Failed to set variable '" + varDef.name + "': " + ex.getMessage());
                    }
                }
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance: " + e.getMessage(), e);
        }
    }

    /**
     * 获取生成的类
     */
    public Class<?> getGeneratedClass() {
        return generatedClass;
    }

    /**
     * 字符串首字母大写
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 使用示例
     */
    public static void main(String[] as) {
        try {
            SubClass subClass = new SubClass("AdvancedExample");

            // 添加变量
            subClass.addVariable("name", "Test Object");
            subClass.addVariable("count", 42);
            subClass.addVariable("active", Boolean.class, true);

            // 添加无参方法
            subClass.addMethod("getMessage", String.class, () -> {
                return "Hello from dynamic method!";
            });

            // 添加带参数的方法
            subClass.addMethod("calculate", Integer.class,
                    new Class<?>[]{Integer.class, Integer.class},
                    args -> {
                        // 安全地访问参数
                        if (args == null || args.length < 2) {
                            throw new IllegalArgumentException("Expected 2 arguments, got " + (args == null ? 0 : args.length));
                        }
                        int a = (Integer) args[0];
                        int b = (Integer) args[1];
                        return a + b;
                    });

            // 添加字符串处理方法
            subClass.addMethod("concatenate", String.class,
                    new Class<?>[]{String.class, String.class},
                    args -> {
                        // 安全地访问参数
                        if (args == null || args.length < 2) {
                            throw new IllegalArgumentException("Expected 2 arguments, got " + (args == null ? 0 : args.length));
                        }
                        String s1 = (String) args[0];
                        String s2 = (String) args[1];
                        return s1 + " " + s2;
                    });

            // 创建类
            Class<?> dynamicClass = subClass.create();
            System.out.println("Class created: " + dynamicClass.getName());

            // 创建实例
            Object instance = subClass.newInstance();
            System.out.println("Instance created: " + instance);

            // 检查类中的方法
            System.out.println("\nClass methods:");
            for (Method method : dynamicClass.getDeclaredMethods()) {
                System.out.print(" - " + method.getName() + " : " + method.getReturnType().getSimpleName());
                if (method.getParameterTypes().length > 0) {
                    System.out.print(" (");
                    for (int i = 0; i < method.getParameterTypes().length; i++) {
                        if (i > 0) System.out.print(", ");
                        System.out.print(method.getParameterTypes()[i].getSimpleName());
                    }
                    System.out.print(")");
                }
                System.out.println();
            }

            // 调用无参方法
            Method getMessage = dynamicClass.getMethod("getMessage");
            String messageResult = (String) getMessage.invoke(instance);
            System.out.println("\nMessage result: " + messageResult);

            // 调用带参数的方法
            Method calculate = dynamicClass.getMethod("calculate", Integer.class, Integer.class);
            Integer calcResult = (Integer) calculate.invoke(instance, 5, 3);
            System.out.println("Calculate result: " + calcResult);

            Method concatenate = dynamicClass.getMethod("concatenate", String.class, String.class);
            String concatResult = (String) concatenate.invoke(instance, "Hello", "World");
            System.out.println("Concatenate result: " + concatResult);

            // 访问变量
            Field nameField = dynamicClass.getDeclaredField("name");
            nameField.setAccessible(true);
            String name = (String) nameField.get(instance);

            Field countField = dynamicClass.getDeclaredField("count");
            countField.setAccessible(true);
            Integer count = (Integer) countField.get(instance);

            Field activeField = dynamicClass.getDeclaredField("active");
            activeField.setAccessible(true);
            Boolean active = (Boolean) activeField.get(instance);

            System.out.println("\nVariables - Name: " + name + ", Count: " + count + ", Active: " + active);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}