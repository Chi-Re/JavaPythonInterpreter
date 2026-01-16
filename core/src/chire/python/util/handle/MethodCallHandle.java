package chire.python.util.handle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

public class MethodCallHandle {
    /**
     * 动态调用实例方法
     *
     * @param target 目标对象
     * @param methodName 方法名
     * @param args 方法参数
     * @return 方法调用的结果
     */
    public static Object callMethod(Object target, String methodName, Object... args) throws Throwable {
        // 获取方法参数类型
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = (args[i] != null) ? args[i].getClass() : Object.class;
        }

        // 获取 MethodHandles.Lookup
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        // 创建方法类型
        MethodType methodType = MethodType.methodType(void.class, paramTypes);

        try {
            // 尝试查找方法
            MethodHandle handle = lookup.findVirtual(target.getClass(), methodName, methodType);

            // 调用方法
            return handle.invokeWithArguments(prependTarget(target, args));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // 尝试使用 Object 类型参数
            return fallbackMethodCall(target, methodName, args);
        }
    }

    /**
     * 后备方法调用策略（处理参数类型不匹配）
     */
    private static Object fallbackMethodCall(Object target, String methodName, Object... args) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?>[] paramTypes = new Class<?>[args.length];
        Arrays.fill(paramTypes, Object.class);

        try {
            // 使用 Object 类型参数查找方法
            MethodType methodType = MethodType.methodType(void.class, paramTypes);
            MethodHandle handle = lookup.findVirtual(target.getClass(), methodName, methodType);

            // 调用方法
            return handle.invokeWithArguments(prependTarget(target, args));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // 尝试查找带返回值的方法
            return fallbackWithReturnType(target, methodName, args);
        }
    }

    /**
     * 尝试带返回值的方法调用
     */
    private static Object fallbackWithReturnType(Object target, String methodName, Object... args) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?>[] paramTypes = new Class<?>[args.length];
        Arrays.fill(paramTypes, Object.class);

        // 尝试不同的返回类型
        Class<?>[] returnTypes = {void.class, Object.class, int.class, String.class};

        for (Class<?> returnType : returnTypes) {
            try {
                MethodType methodType = MethodType.methodType(returnType, paramTypes);
                MethodHandle handle = lookup.findVirtual(target.getClass(), methodName, methodType);
                return handle.invokeWithArguments(prependTarget(target, args));
            } catch (NoSuchMethodException | IllegalAccessException ignored) {
                // 继续尝试下一个返回类型
            }
        }

        throw new NoSuchMethodException("找不到方法: " + methodName +
                " 参数: " + Arrays.toString(args));
    }

    /**
     * 在参数数组前添加目标对象
     */
    private static Object[] prependTarget(Object target, Object... args) {
        Object[] newArgs = new Object[args.length + 1];
        newArgs[0] = target;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return newArgs;
    }

    /**
     * 调用静态方法
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param args 方法参数
     * @return 方法调用的结果
     */
    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = (args[i] != null) ? args[i].getClass() : Object.class;
        }

        try {
            MethodType methodType = MethodType.methodType(Object.class, paramTypes);
            MethodHandle handle = lookup.findStatic(clazz, methodName, methodType);
            return handle.invokeWithArguments(args);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // 尝试使用 Object 类型参数
            Arrays.fill(paramTypes, Object.class);
            MethodType methodType = MethodType.methodType(Object.class, paramTypes);
            MethodHandle handle = lookup.findStatic(clazz, methodName, methodType);
            return handle.invokeWithArguments(args);
        }
    }
}

