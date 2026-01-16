package chire.python.py;

import java.util.*;
import java.util.stream.Collectors;

public class PyDict {
    private final LinkedHashMap<Object, Object> data;

    public PyDict() {
        this.data = new LinkedHashMap<>();
    }

    public PyDict(Map<Object, Object> map) {
        this.data = new LinkedHashMap<>(map);
    }

    // 核心方法
    public Object __getitem__(Object key) {
        if (!data.containsKey(key)) {
            throw new NoSuchElementException("Key not found: " + key);
        }
        return data.get(key);
    }

    public void __setitem__(Object key, Object value) {
        data.put(key, value);
    }

    public int __len__() {
        return data.size();
    }

    public String __str__() {
        return data.entrySet().stream()
                .map(entry -> format(entry.getKey()) + ": " + format(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private String format(Object obj) {
        if (obj instanceof String) return "'" + obj + "'";
        if (obj instanceof Character) return "'" + obj + "'";
        return Objects.toString(obj);
    }

    // 添加的完整方法集
    public PyList keys() {
        return new PyList(new ArrayList<>(data.keySet()));
    }

    public PyList values() {
        return new PyList(new ArrayList<>(data.values()));
    }

    public PyList items() {
        List<Object> entries = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            entries.add(new PyList(Arrays.asList(entry.getKey(), entry.getValue())));
        }
        return new PyList(entries);
    }

    public Object get(Object key) {
        return get(key, null);
    }

    public Object get(Object key, Object defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }

    public Object setdefault(Object key, Object defaultValue) {
        if (!data.containsKey(key)) {
            data.put(key, defaultValue);
            return defaultValue;
        }
        return data.get(key);
    }

    public PyList popitem() {
        if (data.isEmpty()) {
            throw new NoSuchElementException("popitem(): dictionary is empty");
        }

        // 获取最后一个条目
        Iterator<Map.Entry<Object, Object>> iterator = data.entrySet().iterator();
        Map.Entry<Object, Object> lastEntry = null;
        while (iterator.hasNext()) {
            lastEntry = iterator.next();
        }

        // 移除并返回
        if (lastEntry != null) {
            data.remove(lastEntry.getKey());
            return new PyList(Arrays.asList(lastEntry.getKey(), lastEntry.getValue()));
        }

        throw new IllegalStateException("Failed to pop item from dictionary");
    }

    public void update(PyDict other) {
        data.putAll(other.data);
    }

    public void update(Map<Object, Object> map) {
        data.putAll(map);
    }

    public boolean __contains__(Object key) {
        return data.containsKey(key);
    }

    public void __delitem__(Object key) {
        if (!data.containsKey(key)) {
            throw new NoSuchElementException("Key not found: " + key);
        }
        data.remove(key);
    }

    // 额外实用方法
    public void clear() {
        data.clear();
    }

    public PyDict copy() {
        return new PyDict(new LinkedHashMap<>(this.data));
    }
}
