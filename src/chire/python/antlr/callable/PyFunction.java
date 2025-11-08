package chire.python.antlr.callable;

public interface PyFunction<T, R> {
    R apply(Object self, T args);
}
