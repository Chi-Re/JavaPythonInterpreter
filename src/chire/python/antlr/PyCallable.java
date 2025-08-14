package chire.python.antlr;

import java.util.ArrayList;
import java.util.List;

public interface PyCallable {
    Object call(PyExecutor exec, ArrayList<PyExecutor.PyInstruction> arguments);
}
