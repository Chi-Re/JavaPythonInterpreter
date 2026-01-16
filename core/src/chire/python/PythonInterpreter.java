package chire.python;

import chire.antlr.Python3Lexer;
import chire.antlr.Python3Parser;
import chire.python.antlr.PyAssembler;
import chire.python.antlr.PyExecutor;
import chire.python.antlr.PyParser;
import chire.python.antlr.PyStatement;
import chire.python.antlr.callable.PyCallable;
import chire.python.util.SmartIndenter;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;

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
                a = 1
                b = 4
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

        executor.setVar("print", (PyCallable) (exec, self, arguments) -> {
            for (var argument : arguments) {
                System.out.print(argument);
            }
            System.out.print("\n");

            return null;
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
