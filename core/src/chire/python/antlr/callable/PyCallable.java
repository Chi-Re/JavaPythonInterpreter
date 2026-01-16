package chire.python.antlr.callable;

import chire.python.antlr.PyExecutor;

import java.util.ArrayList;

public interface PyCallable {
    default Object call(PyExecutor exec, ArrayList<PyExecutor.PyInstruction> arguments) {
        return call(exec, null, arguments);
    }

    default Object call(PyExecutor exec, Object self, ArrayList<PyExecutor.PyInstruction> arguments) {
        return call(exec, self, arguments.stream().map(instruction -> instruction.run(exec)).toArray());
    };

    default Object call(PyExecutor exec, Object[] arguments) {
        return call(exec, null, arguments);
    }

    Object call(PyExecutor exec, Object self, Object[] arguments);
}
