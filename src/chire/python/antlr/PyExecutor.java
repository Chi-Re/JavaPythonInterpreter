package chire.python.antlr;

import chire.python.py.PyList;
import chire.python.util.handle.MethodCallHandle;
import chire.python.util.handle.VarCallHandle;
import chire.python.util.type.NumberComparator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PyExecutor {

    private final Map<String, Object> locals = new HashMap<>();

    private PyExecutor global;

    private boolean returnState = false;

    public void setVar(String key, Object value){
        locals.put(key, value);
    }

    public Object getVar(String key) {
        return locals.containsKey(key) ? locals.get(key) : global.getVar(key);
    }

    public boolean has(String key){
        return locals.containsKey(key);
    }

    public void setGlobal(PyExecutor executor){
        this.global = executor;
    }

    /**判断是否在函数或者类里，如果是则false*/
    public boolean isGlobal(){
        return global == null;
    }

    public boolean isReturn(){
        return returnState;
    }

    public void setReturn(boolean state) {
        returnState = state;
    }

    public interface PyInstruction{
        Object run(PyExecutor exec);
    }

    public static class NonePy implements PyInstruction{
        @Override
        public Object run(PyExecutor exec) {
            return null;
        }
    }

    public static class BreakPy implements PyInstruction{
        @Override
        public Object run(PyExecutor exec) {
            return false;
        }
    }

    public static class VarPy implements PyInstruction{
        public Class<?> type;

        public final String name;

        public final PyInstruction value;

        public VarPy(String name, PyInstruction value){
            this.name = name;
            this.value = value;
        }

        @Override
        public Object run(PyExecutor exec) {
            var val = value.run(exec);

            exec.setVar(name, val);

            return val;
        }
    }

    public static class FunPy implements PyInstruction{

        public final String name;

        public final ArrayList<ArgPy> args;

        public final ArrayList<PyInstruction> instructions;

        public FunPy(String name, ArrayList<ArgPy> args, ArrayList<PyInstruction> instructions){
            this.args = args;
            this.name = name;
            this.instructions = instructions;
        }

        @Override
        public Object run(PyExecutor exec) {
            var call = new PyCallable() {
                @Override
                public Object call(PyExecutor exec, ArrayList<PyInstruction> arguments) {
                    PyExecutor execLocal = new PyExecutor();

                    execLocal.setGlobal(exec);

                    if (arguments.size() != args.size()) throw new RuntimeException("all args");

                    for (int i = 0; i < arguments.size(); i++) {
                        execLocal.setVar(args.get(i).name, arguments.get(i).run(exec));
                    }

                    Object returnObj = null;

                    for (PyInstruction instruction : instructions) {
                        if (execLocal.isReturn()){
                            execLocal.setReturn(false);
                            return returnObj;
                        } else if (instruction instanceof ReturnPy) {
                            return instruction.run(execLocal);
                        }
                        returnObj = instruction.run(execLocal);
                    }

                    if (execLocal.isReturn()){
                        execLocal.setReturn(false);
                        return returnObj;
                    }

                    return null;
                }
            };

            exec.setVar(name, call);

            return call;
        }
    }

    public static class ReturnPy implements PyInstruction{

        public final PyInstruction instruction;

        public ReturnPy(PyInstruction instruction) {
            this.instruction = instruction;
        }

        @Override
        public Object run(PyExecutor exec) {
            exec.setReturn(true);
            return instruction.run(exec);
        }

        @Override
        public String toString() {
            return "ReturnPy{" +
                    "instruction=" + instruction +
                    '}';
        }
    }

    public static class ArgPy implements PyInstruction{

        public final String name;

        public final Class<?> type;

        public ArgPy(String name, Class<?> type){
            this.name = name;
            this.type = type;
        }

        @Override
        public Object run(PyExecutor exec) {
            return null;
        }
    }

    public static class FunCallPy implements PyInstruction{

        public final String name;

        public final ArrayList<PyInstruction> instructions;

        public FunCallPy(String name, ArrayList<PyInstruction> instructions){
            this.name = name;
            this.instructions = instructions;
        }

        @Override
        public Object run(PyExecutor exec) {
            var funCall = exec.getVar(name);

            if (funCall instanceof PyCallable) {
                return ((PyCallable) funCall).call(exec, instructions);
            }

            return null;
        }
    }

    public static class WhilePy implements PyInstruction {

        public final PyInstruction holds;

        public final ArrayList<PyInstruction> instructions;

        public WhilePy(PyInstruction holds, ArrayList<PyInstruction> instructions){
            this.holds = holds;
            this.instructions = instructions;
        }

        @Override
        public Object run(PyExecutor exec) {
            var valid = holds.run(exec);

            if (!(valid instanceof Boolean)) throw new RuntimeException("Type Invalid value");

            Object returnObj = null;

            while ((boolean) valid) {
                for (PyInstruction instruction : instructions) {
                    if (exec.isReturn()) {
                        return returnObj;
                    }

                    if (instruction instanceof BreakPy) {
                        return null;
                    }

                    if (instruction instanceof ReturnPy) {
                        exec.setReturn(true);
                        return instruction.run(exec);
                    }
                    returnObj = instruction.run(exec);
                }

                valid = holds.run(exec);
            }

            return null;
        }
    }

    public static class SubCallPy implements PyInstruction {

        public final PyInstruction key;

        public final PyInstruction build;

        public SubCallPy(PyInstruction key, PyInstruction build) {
            this.key = key;
            this.build = build;
        }

        @Override
        public Object run(PyExecutor exec) {
            var key = this.key.run(exec);
            if (this.build instanceof FunCallPy) {
                ArrayList<Object> args = new ArrayList<>();

                for (PyInstruction instruction : ((FunCallPy) build).instructions) {
                    args.add(instruction.run(exec));
                }

                try {
                    return MethodCallHandle.callMethod(key, ((FunCallPy) build).name, args.toArray());
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            } else if (this.build instanceof VarCallPy) {
                try {
                    return VarCallHandle.accessVariable(key, ((VarCallPy) build).name);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("not hava key");
            }
        }
    }

    public static class SubSetPy implements PyInstruction {
        public final PyInstruction key;
        public final PyInstruction build;
        public final PyInstruction call;

        public SubSetPy(PyInstruction key, PyInstruction build, PyInstruction call) {
            this.key = key;
            this.build = build;
            this.call = call;
        }

        @Override
        public Object run(PyExecutor exec) {
            var key = this.key.run(exec);
            if (this.build instanceof FunCallPy) {
                throw  new RuntimeException("not can key");
            } else if (this.build instanceof VarCallPy) {
                try {
                    VarCallHandle.modifyVariable(key, ((VarCallPy) build).name, call.run(exec));
                    return null;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("not hava key");
            }
        }
    }

    public static class ListPy implements PyInstruction {

        public final ArrayList<PyInstruction> list;

        public ListPy(ArrayList<PyInstruction> list) {
            this.list = list;
        }

        @Override
        public Object run(PyExecutor exec) {
            ArrayList<Object> returnList = new ArrayList<>();

            for (PyInstruction instruction : this.list) {
                returnList.add(instruction.run(exec));
            }

            return new PyList(returnList);
        }
    }

    public static class IfPy implements PyInstruction {

        public final PyInstruction holds;

        public final ArrayList<PyInstruction> instructions;

        public IfPy(PyInstruction holds, ArrayList<PyInstruction> instructions){
            this.holds = holds;
            this.instructions = instructions;
        }

        @Override
        public Object run(PyExecutor exec) {
            var valid = holds.run(exec);

            if (!(valid instanceof Boolean)) throw new RuntimeException("Type Invalid value");

            if ((boolean) valid){
                Object returnObj = null;
                for (PyInstruction instruction : instructions) {
                    if (exec.isReturn()) {
                        return returnObj;
                    }
                    if (instruction instanceof BreakPy) {
                        return null;
                    }

                    if (instruction instanceof ReturnPy) {
                        exec.setReturn(true);
                        return instruction.run(exec);
                    }
                    returnObj = instruction.run(exec);
                }
            }

            return null;
        }
    }

    public static class JudgmentPy implements PyInstruction{
        public final PyInstruction left;

        public final int type;

        public final PyInstruction right;

        public JudgmentPy(PyInstruction left, int type, PyInstruction right) {
            this.left = left;
            this.type = type;
            this.right = right;
        }

        @Override
        public Object run(PyExecutor exec) {
            var left = this.left.run(exec);
            var right = this.right.run(exec);

            //'<'=79
            //'>'=80
            //'=='=81
            //'>='=82
            //'<='=83
            //'<>'=84
            //'!='=85
            switch (this.type) {
                case 79, 83:
                    if (left instanceof Number && right instanceof Number) {
                        if (this.type == 83) {
                            return NumberComparator.lessThan((Number) left, (Number) right) ||
                                    NumberComparator.equals((Number) left, (Number) right);
                        }
                        return NumberComparator.lessThan((Number) left, (Number) right);
                    }
                    break;

                case 80, 82:
                    if (left instanceof Number && right instanceof Number) {
                        if (this.type == 82) {
                            return NumberComparator.greaterThan((Number) left, (Number) right) ||
                                    NumberComparator.equals((Number) left, (Number) right);
                        }
                        return NumberComparator.greaterThan((Number) left, (Number) right);
                    }
                    break;

                case 81:
                    if (left instanceof Number && right instanceof Number) {
                        return NumberComparator.equals((Number) left, (Number) right);
                    }
                    return Objects.equals(left, right);

                case 85:
                    if (left instanceof Number && right instanceof Number) {
                        return !NumberComparator.equals((Number) left, (Number) right);
                    }
                    return !Objects.equals(left, right);

            }

            throw new RuntimeException("The returned character is a non-existent judgment symbol");
        }
    }

    public static class VarCallPy implements PyInstruction{
        public final String name;

        public VarCallPy(String name){
            this.name = name;
        }

        @Override
        public Object run(PyExecutor exec) {
            return exec.getVar(name);
        }
    }

    public static class ConstPy implements PyInstruction{
        public final Object value;

        public ConstPy(Object value){
            this.value = value;
        }

        @Override
        public Object run(PyExecutor exec) {
            return value;
        }
    }

    public static class NumbePy implements PyInstruction{
        public final Number value;

        public final boolean range;

        public NumbePy(Number value){
            this(true, value);
        }

        public NumbePy(boolean range, Number value){
            this.value = value;
            this.range = range;
        }

        @Override
        public Object run(PyExecutor exec) {
            var v = range ? 1 : -1;

            if (value instanceof Integer) {
                return value.intValue()*v;
            } else if (value instanceof Double) {
                return  value.doubleValue()*v;
            } else if (value instanceof Long) {
                return  value.longValue()*v;
            } else if (value instanceof Float) {
                return value.floatValue()*v;
            } else {
                throw new RuntimeException("don't have num");
            }
        }

        @Override
        public String toString() {
            return "NumbePy{" +
                    "value=" + value +
                    ", range=" + range +
                    '}';
        }
    }

    public static class LogicalPy implements PyInstruction{

        public final PyInstruction left;
        public final String operator;
        public final PyInstruction right;

        public LogicalPy(PyInstruction left, String operator, PyInstruction right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public Object run(PyExecutor exec) {
            var le = left.run(exec);
            var ri = right.run(exec);

            if (le instanceof Number && ri instanceof Number) {
                if (le instanceof Float || ri instanceof Float) {
                    switch (operator) {
                        case "-":
                        case "+":
                            return ((Number) le).floatValue() + ((Number) ri).floatValue();
                        case "/":
                            return ((Number) le).floatValue() / ((Number) ri).floatValue();
                        case "*":
                            return ((Number) le).floatValue() * ((Number) ri).floatValue();
                    }
                } else {
                    switch (operator) {
                        case "-":
                        case "+":
                            return ((Number) le).intValue() + ((Number) ri).intValue();
                        case "/":
                            return divideNum(((Number) le).intValue(), ((Number) ri).intValue());
                        case "*":
                            return ((Number) le).intValue() * ((Number) ri).intValue();
                    }
                }
            } else if (le instanceof String && ri instanceof String) {
                switch (operator) {
                    case "+":
                        return le + (String) ri;

                    default:
                        throw new RuntimeException("string no +");
                }
            } else if (le instanceof String && ri instanceof Number) {
                switch (operator) {
                    case "+":
                        return le + ri.toString();

                    default:
                        throw new RuntimeException("string no +");
                }
            }

            throw new RuntimeException("no logical");
        }

        private Number divideNum(int dividend, int divisor) {
            // 检查除数不能为零
            if (divisor == 0) {
                throw new ArithmeticException("除数不能为零");
            }

            // 计算余数判断是否为整数
            int remainder = dividend % divisor;

            if (remainder == 0) {
                // 结果为整数
                return dividend / divisor;
            } else {
                // 结果为小数
                return (float) dividend / divisor;
            }
        }

        @Override
        public String toString() {
            return "LogicalPy{" +
                    "left=" + left +
                    ", operator='" + operator + '\'' +
                    ", right=" + right +
                    '}';
        }
    }
}
