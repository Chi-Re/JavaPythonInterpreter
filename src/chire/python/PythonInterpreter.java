package chire.python;

import chire.antlr.Python3Lexer;
import chire.antlr.Python3Parser;
import chire.python.antlr.*;
import chire.python.py.PyList;
import chire.python.util.SmartIndenter;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

public class PythonInterpreter {
    public static void main(String[] args) {
        //TODO
        // 38 bool
        // 4 int do
        // 3 str
        // 56 *
        // 72 -
        // 71 +
        // 73 /
        String pythonCode = """
                a = [1, 2, 3]
                
                a.append(9)
                
                print(a)
                print(a.data.size())
                """;

        // 创建词法分析器和语法分析器
        CharStream input = CharStreams.fromString(pythonCode);
        Python3Lexer lexer = new Python3Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Python3Parser parser = new Python3Parser(tokens);
        parser.file_input();

        ArrayList<PyStatement> statements = new PyParser(tokens).parse();

        ArrayList<PyExecutor.PyInstruction> instructions = new ArrayList<>();

        PyAssembler assembler = new PyAssembler();
        SmartIndenter indenter = new SmartIndenter("  ");
        for (PyStatement statement : statements) {
            statement.toString(indenter);
            instructions.add(statement.build(assembler));
        }
        System.out.println(indenter.toString());


        PyExecutor executor = new PyExecutor();

        executor.setVar("print", (PyCallable) (exec, arguments) -> {
            for (PyExecutor.PyInstruction argument : arguments) {
                System.out.print(argument.run(exec));
            }
            System.out.print("\n");

            return null;
        });

        executor.setVar("test", (PyCallable) (exec, arguments) -> {
            StringBuilder r = new StringBuilder();

            for (PyExecutor.PyInstruction argument : arguments) {
                r.append("get [").append(argument.run(exec)).append("] it");
            }

            return r.toString();
        });

        for (PyExecutor.PyInstruction instruction : instructions) {
            instruction.run(executor);
        }



//        Interpreter interpreter = new Interpreter(parser);
//
//        interpreter.visit(tree);
    }

    public PythonInterpreter() {
//        set result 0
//        wait 0.5
//        op add result a b

    }
}
