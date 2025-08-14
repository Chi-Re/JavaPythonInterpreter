package chire.python.util.handle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class VarCallHandle {

    // 缓存 MethodHandle 以提高性能
    private static final Map<String, MethodHandle> GETTER_CACHE = new HashMap<>();
    private static final Map<String, MethodHandle> SETTER_CACHE = new HashMap<>();

    /**
     * 访问对象实例变量
     *
     * @param target 目标对象
     * @param fieldName 变量名
     * @return 变量值
     */
    public static Object accessVariable(Object target, String fieldName) throws Throwable {
        String cacheKey = target.getClass().getName() + "#" + fieldName;

        if (!GETTER_CACHE.containsKey(cacheKey)) {
            // 获取字段
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true); // 允许访问私有字段

            // 创建 MethodHandle
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.unreflectGetter(field);

            GETTER_CACHE.put(cacheKey, handle);
        }

        // 使用缓存的 MethodHandle
        return GETTER_CACHE.get(cacheKey).invoke(target);
    }

    /**
     * 修改对象实例变量
     *
     * @param target 目标对象
     * @param fieldName 变量名
     * @param value 新值
     */
    public static void modifyVariable(Object target, String fieldName, Object value) throws Throwable {
        String cacheKey = target.getClass().getName() + "#" + fieldName;

        if (!SETTER_CACHE.containsKey(cacheKey)) {
            // 获取字段
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true); // 允许访问私有字段

            // 创建 MethodHandle
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.unreflectSetter(field);

            SETTER_CACHE.put(cacheKey, handle);
        }

        // 使用缓存的 MethodHandle
        SETTER_CACHE.get(cacheKey).invoke(target, value);
    }

    /**
     * 访问静态变量
     *
     * @param clazz 目标类
     * @param fieldName 变量名
     * @return 变量值
     */
    public static Object accessStaticVariable(Class<?> clazz, String fieldName) throws Throwable {
        String cacheKey = clazz.getName() + "#static#" + fieldName;

        if (!GETTER_CACHE.containsKey(cacheKey)) {
            // 获取静态字段
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);

            // 创建 MethodHandle
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.unreflectGetter(field);

            GETTER_CACHE.put(cacheKey, handle);
        }

        // 使用缓存的 MethodHandle
        return GETTER_CACHE.get(cacheKey).invoke();
    }

    /**
     * 修改静态变量
     *
     * @param clazz 目标类
     * @param fieldName 变量名
     * @param value 新值
     */
    public static void modifyStaticVariable(Class<?> clazz, String fieldName, Object value) throws Throwable {
        String cacheKey = clazz.getName() + "#static#" + fieldName;

        if (!SETTER_CACHE.containsKey(cacheKey)) {
            // 获取静态字段
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);

            // 创建 MethodHandle
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.unreflectSetter(field);

            SETTER_CACHE.put(cacheKey, handle);
        }

        // 使用缓存的 MethodHandle
        SETTER_CACHE.get(cacheKey).invoke(value);
    }
}
