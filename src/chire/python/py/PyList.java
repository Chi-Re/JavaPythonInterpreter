package chire.python.py;

import java.util.*;
import java.util.stream.Collectors;

public class PyList {
    private final List<Object> data;

    // 构造方法
    public PyList() {
        this.data = new ArrayList<>();
    }

    public PyList(Object iterable) {
        this.data = new ArrayList<>();
        if (iterable instanceof Collection) {
            data.addAll((Collection<?>) iterable);
        } else if (iterable instanceof Object[]) {
            Collections.addAll(data, (Object[]) iterable);
        }
    }

    // 添加元素到末尾
    public void append(Object item) {
        data.add(item);
    }

    // 获取元素
    public Object __getitem__(int index) {
        if (index < 0) index += data.size();
        return data.get(index);
    }

    // 设置元素
    public void __setitem__(int index, Object value) {
        if (index < 0) index += data.size();
        data.set(index, value);
    }

    // 获取长度
    public int __len__() {
        return data.size();
    }

    // 字符串表示
    public String __str__() {
        return data.stream()
                .map(obj -> {
                    if (obj instanceof String) return "'" + obj + "'";
                    return Objects.toString(obj);
                })
                .collect(Collectors.joining(", ", "[", "]"));
    }

    // 插入元素
    public void insert(int index, Object item) {
        if (index < 0) index += data.size();
        data.add(index, item);
    }

    // 移除元素
    public void remove(Object item) {
        data.remove(item);
    }

    // 弹出元素
    public Object pop(int index) {
        if (index < 0) index += data.size();
        return data.remove(index);
    }

    // 包含检查
    public boolean __contains__(Object item) {
        return data.contains(item);
    }

    @Override
    public String toString() {
        return __str__();
    }
}
