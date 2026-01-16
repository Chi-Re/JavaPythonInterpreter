package chire.python.antlr;

import java.util.ArrayList;

public class Interpreter {
    public void interpret(ArrayList<PyExecutor.PyInstruction> executors) {
        for (PyExecutor.PyInstruction executor : executors) {
            execute(executor);
        }
    }

    private void execute(PyExecutor.PyInstruction executor) {
    }
}
