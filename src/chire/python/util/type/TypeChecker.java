package chire.python.util.type;
import java.util.Objects;
import java.util.regex.Pattern;

public class TypeChecker {

    public static Class<?> checkType(String input) {
        if (input == null) {
            return null;
        }

        // 1. 检查带引号的字符串（单引号或双引号）
        if (isQuotedString(input)) {
            return String.class;
        }

        // 2. 检查整数
        if (isInteger(input)) {
            return Integer.class;
        }

        if (isBoolean(input)) {
            return Boolean.class;
        }

        // 3. 检查浮点数（float/double）
        return checkFloatingPoint(input);
    }

    public static boolean isBoolean(String input){
        return Objects.equals(input, "True") || Objects.equals(input, "False");
    }

    // 判断是否为带引号的字符串
    public static boolean isQuotedString(String input) {
        int len = input.length();
        if (len < 2) return false;

        char first = input.charAt(0);
        char last = input.charAt(len - 1);

        // 检查双引号字符串或单引号字符串
        return (first == '"' && last == '"') ||
                (first == '\'' && last == '\'');
    }

    // 判断是否为整数
    public static boolean isInteger(String input) {
        return Pattern.matches("^[+-]?\\d+$", input);
    }

    // 判断浮点数类型（float/double）
    public static Class<?> checkFloatingPoint(String input) {
        String numberPart = input;
        char lastChar = input.charAt(input.length() - 1);

        // 处理类型后缀 (f/F/d/D)
        if ("fFdD".indexOf(lastChar) >= 0) {
            numberPart = input.substring(0, input.length() - 1);
        } else {
            lastChar = 0; // 无后缀
        }

        // 检查是否符合浮点数格式
        if (isFloatingPointNumber(numberPart)) {
            return (lastChar == 'f' || lastChar == 'F') ? Float.class : Double.class;
        }

        return Object.class;
    }

    // 判断是否为浮点数（支持小数点、指数形式）
    public static boolean isFloatingPointNumber(String input) {
        return Pattern.matches("^[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?$", input);
    }
}
