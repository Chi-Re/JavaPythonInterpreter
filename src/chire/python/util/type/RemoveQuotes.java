package chire.python.util.type;

public class RemoveQuotes {
    public static String removeQuotes(String input) {
        if (input == null) return null;

        // 移除开头双引号
        if (input.startsWith("\"") || input.startsWith("'")) {
            input = input.substring(1);
        }

        // 移除结尾双引号（确保移除后仍有内容）
        if (input.endsWith("\"") || input.endsWith("'")) {
            input = input.substring(0, input.length() - 1);
        }

        return input;
    }
}
