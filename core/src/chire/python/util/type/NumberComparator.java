package chire.python.util.type;

import java.math.BigDecimal;
import java.math.BigInteger;

public class NumberComparator {

    // 比较两个 Number 的大小
    public static int compare(Number a, Number b) {
        BigDecimal bdA = toBigDecimal(a);
        BigDecimal bdB = toBigDecimal(b);
        return bdA.compareTo(bdB);
    }

    // 将 Number 转换为 BigDecimal（精确表示）
    private static BigDecimal toBigDecimal(Number number) {
        // 根据实际类型选择最佳转换方式
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        } else if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        } else if (number instanceof Byte ||
                number instanceof Short ||
                number instanceof Integer ||
                number instanceof Long) {
            return BigDecimal.valueOf(number.longValue());
        } else if (number instanceof Float ||
                number instanceof Double) {
            // 使用字符串转换避免浮点精度问题
            return new BigDecimal(number.toString());
        } else {
            // 其他未知类型使用通用转换
            return BigDecimal.valueOf(number.doubleValue());
        }
    }

    // 大于比较
    public static boolean greaterThan(Number a, Number b) {
        return compare(a, b) > 0;
    }

    // 小于比较
    public static boolean lessThan(Number a, Number b) {
        return compare(a, b) < 0;
    }

    // 等于比较
    public static boolean equals(Number a, Number b) {
        return compare(a, b) == 0;
    }
}
