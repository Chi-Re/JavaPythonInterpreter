package chire.asm;

import chire.asm.util.Format;

import java.util.HashMap;
import java.util.Map;

public class Args {
    private final Map<String, Class<?>> args;

    public Args() {
        this.args = new HashMap<>();
    }

    public String[] getArgNames() {
        return args.keySet().toArray(new String[0]);
    }

    public void put(String key, Class<?> value) {
        args.put(key, value);
    }

    @Override
    public String toString() {
        return Format.formatPacks(args.values().toArray(new Class<?>[0]));
    }
}
