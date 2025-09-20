package chire.python.antlr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface PyCallable {
    Object call(PyExecutor exec, ArrayList<PyExecutor.PyInstruction> arguments);

    Object call(PyExecutor exec, Object[] arguments);
}
