package chire.asm.util;

public class Format {
    public static String formatPack(Class<?> clazz) {
        return clazz.getName().replace(".", "/").replace(";", "");
    }

    public static String formatPacks(Class<?>[] classes) {
        StringBuilder bs = new StringBuilder();
        bs.append("(");

        for (int i = 0; i < classes.length; i++) {
            String string = formatPack(classes[i]);
            if (!((string.indexOf("[") == 0 && string.indexOf("L") == 1) || (string.indexOf("L") == 0))) {
                bs.append("L");
            }

            bs.append(string).append(";");
        }
        bs.append(")");

        return bs.toString();
    }
}
