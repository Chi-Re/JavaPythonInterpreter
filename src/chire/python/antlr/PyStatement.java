package chire.python.antlr;

import chire.python.util.type.RemoveQuotes;
import chire.python.util.SmartIndenter;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.Objects;

public abstract class PyStatement {
    public abstract PyExecutor.PyInstruction build(PyAssembler builder);

    public void toString(SmartIndenter indenter){
    }

    @Override
    public String toString() {
        var str = new SmartIndenter("  ");
        toString(str);
        return str.toString();
    }

    public static class BreakStatement extends PyStatement{
        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            return new PyExecutor.BreakPy();
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("Break");
        }
    }

    /**定义变量*/
    public static class VarStatement extends PyStatement{
        public final Token name;

        public PyStatement value;

        public VarStatement(Token name, PyStatement value){
            if (name == null) throw new RuntimeException("name 不能为空");
            this.name = name;
            this.value = value;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            return new PyExecutor.VarPy(name.getText(), value.build(builder));
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("Var{").newLine()
                    .indent()
                    .add("name=").add(name.getText()).newLine()
                    .add("value:").indent();
            value.toString(indenter);
            indenter.unindent().newLine()
                    .unindent()
                    .add("}");
        }
    }

    public static class FunStatement extends PyStatement{

        public final Token token;

        public final ArrayList<ArgStatement> args;

        public final ArrayList<PyStatement> statements;

        public FunStatement(Token token, ArrayList<ArgStatement> args, ArrayList<PyStatement> statements){
            this.args = args;
            this.token = token;
            this.statements = statements;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            ArrayList<PyExecutor.ArgPy> instArgs = new ArrayList<>();
            ArrayList<PyExecutor.PyInstruction> instStmts = new ArrayList<>();

            for (PyStatement.ArgStatement arg : this.args) {
                instArgs.add((PyExecutor.ArgPy) arg.build(builder));
            }

            for (PyStatement statement : this.statements) {
                instStmts.add(statement.build(builder));
            }

            return new PyExecutor.FunPy(token.getText(), instArgs, instStmts);
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().addLine("Fun{")
                    .indent()
                    .add("token=").addLine(token.getText())
                    .add("args=");

            for (ArgStatement arg : args) {
                arg.toString(indenter);
            }

            indenter.newLine()
                    .add("stmts=[")
                    .indent();

            for (PyStatement statement : statements) {
                statement.toString(indenter);
            }

            indenter.newLine()
                    .unindent()
                    .addLine("]")
                    .unindent()
                    .add("}");
        }
    }

    public static class WhileStatement extends PyStatement{

        public final PyStatement conditions;

        public final ArrayList<PyStatement> statements;

        public WhileStatement(PyStatement conditions, ArrayList<PyStatement> statements){
            this.conditions = conditions;
            this.statements = statements;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            ArrayList<PyExecutor.PyInstruction> instructions = new ArrayList<>();

            for (PyStatement statement : this.statements) {
                instructions.add(statement.build(builder));
            }

            return new PyExecutor.WhilePy(conditions.build(builder), instructions);
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("While{").newLine()
                    .indent()
                    .add("cond:").indent();
            conditions.toString(indenter);

            indenter.unindent().newLine()
                    .add("stmt=[")
                    .indent();
            for (PyStatement statement : statements) {
                statement.toString(indenter);
            }

            indenter.newLine()
                    .unindent()
                    .addLine("]")
                    .unindent()
                    .add("}");
        }
    }

    public static class SubCallStatement extends PyStatement{

        public final PyStatement key;

        public final PyStatement call;

        public SubCallStatement(PyStatement var, PyStatement call) {
            this.key = var;
            this.call = call;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            return new PyExecutor.SubCallPy(this.key.build(builder), this.call.build(builder));
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("SubCall{")
                    .indent()
                    .newLine().add("key:").indent();
            key.toString(indenter);

            indenter.unindent().newLine().add("call:").indent();

            call.toString(indenter);

            indenter.unindent().newLine().unindent().add("}");
        }
    }

    public static class SubSetStatement extends PyStatement{

        public final PyStatement key;

        public final PyStatement call;

        public final PyStatement var;

        public SubSetStatement(PyStatement key, PyStatement call, PyStatement var) {
            this.key = key;
            this.call = call;
            this.var = var;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            return new PyExecutor.SubSetPy(key.build(builder), call.build(builder), var.build(builder));
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("SubSet{")
                    .indent()
                    .newLine().add("key:").indent();
            key.toString(indenter);

            indenter.unindent().newLine().add("call:").indent();

            call.toString(indenter);

            indenter.unindent().newLine().add("var:").indent();

            var.toString(indenter);

            indenter.unindent().newLine().unindent().add("}");
        }
    }

    public static class ListStatement extends PyStatement{

        public final ArrayList<PyStatement> list;

        public ListStatement(ArrayList<PyStatement> list){
            this.list = list;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            ArrayList<PyExecutor.PyInstruction> instructions = new ArrayList<>();

            for (PyStatement statement : this.list) {
                instructions.add(statement.build(builder));
            }

            return new PyExecutor.ListPy(instructions);
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("List[")
                    .indent();

            for (PyStatement statement : this.list) {
                statement.toString(indenter);
            }

            indenter.newLine().unindent().add("]");
        }
    }

    public static class NoneStatement extends PyStatement{
        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            return new PyExecutor.NonePy();
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("null");
        }
    }

    public static class ReturnStatement extends PyStatement{
        public final PyStatement returnStmt;

        public ReturnStatement(PyStatement returnStmt){
            this.returnStmt = returnStmt;
        }

        public ReturnStatement(){
            this.returnStmt = new NoneStatement();
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            return new PyExecutor.ReturnPy(returnStmt.build(builder));
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("Return{")
                    .indent();
            returnStmt.toString(indenter);
            indenter.newLine()
                    .unindent()
                    .add("}");
        }
    }

    public static class ArgStatement extends PyStatement{

        public final Token token;

        public final Class<?> type;

        public ArgStatement(Token token, Class<?> type){
            this.token = token;
            this.type = type;
        }

        public ArgStatement(Token token){
            this.token = token;
            this.type = Object.class;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            return new PyExecutor.ArgPy(token.getText(), type);
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("Arg{").add(token.getText()).add("|").add(String.valueOf(type)).add("}");
        }
    }

    public static class IfStatement extends PyStatement {

        public final PyStatement conditions;

        public final ArrayList<PyStatement> statements;

        public IfStatement(PyStatement conditions, ArrayList<PyStatement> statements){
            this.conditions = conditions;
            this.statements = statements;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            ArrayList<PyExecutor.PyInstruction> instructions = new ArrayList<>();

            for (PyStatement statement : this.statements) {
                instructions.add(statement.build(builder));
            }

            return new PyExecutor.IfPy(conditions.build(builder), instructions);
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("If{").newLine()
                    .indent()
                    .add("cond:").indent();
            conditions.toString(indenter);

            indenter.unindent().newLine()
                    .add("stmt=[")
                    .indent();
            for (PyStatement statement : statements) {
                statement.toString(indenter);
            }

            indenter.newLine()
                    .unindent()
                    .addLine("]")
                    .unindent()
                    .add("}");
        }
    }

    public static class JudgmentStatement extends PyStatement{

        public final PyStatement left;

        public final Token operator;

        public final PyStatement right;

        public JudgmentStatement(PyStatement left, Token operator, PyStatement right){
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            return new PyExecutor.JudgmentPy(left.build(builder), operator.getType(), right.build(builder));
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().addLine("Judg{").indent();
            if (operator != null) {
                indenter.add("left:").indent();
                left.toString(indenter);
                indenter.unindent().newLine().add("operator='")
                        .indent().add(operator.getText()).add("'").unindent().newLine()
                        .add("indenter:").indent();
                right.toString(indenter);
                indenter.unindent();
            } else {
                indenter.newLine().indent();
                indenter.addLine("left:").indent();
                left.toString(indenter);
                indenter.unindent();
            }
            indenter.newLine().unindent().add("}");
        }
    }

    public static class NumberStatement<T> extends PyStatement{

        public final Token token;

        public final Class<T> type;

        public final boolean range;

        public NumberStatement(Token token, Class<T> type){
            this(true, token, type);
        }

        public NumberStatement(boolean range , Token token, Class<T> type){
            this.token = token;
            this.type = type;
            this.range = range;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            return new PyExecutor.NumbePy(range, cast());
        }

        private Number cast() {
            if (Integer.class.equals(type)) {
                return Integer.valueOf(token.getText());
            } else if (Double.class.equals(type)) {
                return Double.valueOf(token.getText());
            } else if (Float.class.equals(type)) {
                return Float.valueOf(token.getText());
            }
            throw new RuntimeException("don't is num");
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("Num{")
                    .add("key=").add(range ? "+" : "-").add(token.getText())
                    .add(", ")
                    .add("type=").add(String.valueOf(type))
                    .add("}");
        }
    }

    /**保存常数*/
    public static class ConstStatement<T> extends PyStatement{

        public final Token token;

        public final Class<T> type;

        public ConstStatement(Token token, Class<T> type){
            this.token = token;
            this.type = type;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            if (type == String.class) {
                return new PyExecutor.ConstPy(RemoveQuotes.removeQuotes(token.getText()));
            } else {
                return new PyExecutor.ConstPy(cast());
            }
        }

        private Object cast() {
            if (Boolean.class.equals(type)) {
                if (Objects.equals(token.getText(), "True")) return true;
                if (Objects.equals(token.getText(), "False")) return false;
            } else if (Object.class.equals(type)) {
                return null;
            }
            return null;
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("Cons{")
                    .add("key=").add(token.getText())
                    .add(", ")
                    .add("type=").add(type.toString())
                    .add("}");
        }
    }

    /**方法调用*/
    public static class FunCallStatement extends PyStatement{
        public final Token name;

        public ArrayList<PyStatement> args = new ArrayList<>();

        public FunCallStatement(Token name) {
            this.name = name;
        }

        public void setArg(ArrayList<PyStatement> args){
            this.args = args;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            ArrayList<PyExecutor.PyInstruction> insts = new ArrayList<>();

            for (PyStatement arg : this.args) {
                insts.add(arg.build(builder));
            }

            return new PyExecutor.FunCallPy(name.getText(), insts);
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().addLine("CallFun{")
                    .indent()
                    .add("name=").addLine(name.getText())
                    .add("args=[");

            if (args.size() > 0) {
                indenter.indent();
                for (PyStatement arg : args) {
                    arg.toString(indenter);
                }
                indenter.newLine().unindent();
            }

            indenter.addLine("]")
                    .unindent()
                    .add("}");
        }
    }

    /**变量调用*/
    public static class VarCallStatement extends PyStatement{
        public final Token name;

        public VarCallStatement(Token name) {
            this.name = name;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            return new PyExecutor.VarCallPy(name.getText());
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("CallVar{").add("name=").add(name.getText()).add("}");
        }
    }

    public static class LogicalStatement extends PyStatement{
        public final PyStatement left;
        public final Token operator;
        public final String operatorStr;
        public final PyStatement right;

        public LogicalStatement(PyStatement left, Token operator, PyStatement right){
            this.left = left;
            this.operator = operator;
            this.right = right;

            this.operatorStr = null;
        }

        public LogicalStatement(PyStatement left, String operator, PyStatement right){
            this.left = left;
            this.operatorStr = operator;
            this.right = right;

            this.operator = null;
        }

        @Override
        public PyExecutor.PyInstruction build(PyAssembler builder) {
            if (operator != null) {
                return new PyExecutor.LogicalPy(left.build(builder), operator.getText(), right.build(builder));
            } else {
                return new PyExecutor.LogicalPy(left.build(builder), operatorStr, right.build(builder));
            }
        }

        @Override
        public void toString(SmartIndenter indenter) {
            indenter.newLine().add("Logi{").indent()
                    .newLine().add("left:").indent();
            left.toString(indenter);
            indenter.unindent().newLine().add("operator=")
                    .add(operator != null ? operator.getText() : operatorStr)
                    .newLine().add("right:").indent();
            right.toString(indenter);
            indenter.unindent().newLine().unindent().add("}");
        }
    }
}
