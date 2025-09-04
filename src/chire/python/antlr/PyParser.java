package chire.python.antlr;

import chire.python.py.PyDict;
import chire.python.py.PyList;
import chire.python.util.type.TypeChecker;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class PyParser {
    private final CommonTokenStream tokenStream;

    public static ArrayList<PyStatement> statements = new ArrayList<>();

    private int current = 0;

    private static HashMap<String, Class<?>> keyMap = new HashMap<>();

    static {
        keyMap.put("object", Object.class);
        keyMap.put("float", Float.class);
        keyMap.put("int", Integer.class);
        keyMap.put("str", String.class);

        keyMap.put("dict", PyDict.class);
        keyMap.put("list", PyList.class);
    }

    public PyParser(CommonTokenStream token) {
        this.tokenStream = token;
    }

    public boolean isEnd(){
        return peek().getType() == -1;
    }

    public ArrayList<PyStatement> parse(){
        while (!isEnd()){
            Token token = peek();

            //System.out.println(token);

            switch (token.getType()) {
                case 45:
                    if (match(current+1, 44, 63, 88, 89, 90, 92)){
                        statements.add(varDeclaration());
                    } else if (match(current+1, 57)){
                        statements.add(methodCall());
                    } else if (match(current+1, 54)) {
                        statements.add(submethodCall(varCall()));
                    }
                    break;

                case 15:
                    statements.add(defDeclaration());
                    break;

                case 25:
                    statements.add(ifDeclaration());
                    break;

                case 41:
                    statements.add(whileDeclaration());
                    break;

                case 13:
                    statements.add(classDeclaration(1));
                    break;

                default:
                    break;
            }

            current++;
        }

        return statements;
    }

    private PyStatement submethodCall(PyStatement var){
        current += 2;
        PyStatement call;
        if (match(current+1, 57)) {
            call = methodCall();
        } else {
            call = varCall();
        }

        if (match(current+1, 63)) {
            current++;
            return new PyStatement.SubSetStatement(var, call, assignment(1));
        }

        if (last().getType() == 54) {
            return submethodCall(
                    new PyStatement.SubCallStatement(var, call)
            );
        }

        return new PyStatement.SubCallStatement(var, call);
    }

    private ArrayList<PyStatement> bodyDeclaration(){
        return bodyDeclaration(0);
    }

    private ArrayList<PyStatement> bodyDeclaration(int cur){
        this.current += cur;

        ArrayList<PyStatement> body = new ArrayList<>();

        var key = peek();
        switch (key.getType()){
            case 45:
                if (match(current+1, 44, 63, 88, 89, 90, 92)){
                    body.add(varDeclaration());
                } else if (match(current+1, 57)){
                    body.add(methodCall());
                }
                break;

            case 15:
                body.add(defDeclaration());
                break;

            case 25:
                body.add(ifDeclaration());
                break;

            case 41:
                body.add(whileDeclaration());
                break;
        }

        return body;
    }

    private PyStatement classDeclaration(){
        return classDeclaration(0);
    }

    private PyStatement classDeclaration(int cur){
        this.current += cur;

        Token class_token = peek();

        if (class_token.getType() != 45) throw new RuntimeException("no 45 key");

        ArrayList<PyStatement> body = new ArrayList<>();

        while (!isEnd()) {
            current++;

            Token key = peek();

            switch (key.getType()) {
                case 1://"    "
                case 44:
                case 60:
                    break;

                case 45, 25, 41, 15:
                    body.addAll(bodyDeclaration());
                    break;

                case 2:
                    return new PyStatement.ClassStatement(class_token, body);
            }
        }

        return null;
    }

    private PyStatement whileDeclaration(){
        return whileDeclaration(0);
    }

    private PyStatement whileDeclaration(int cur){
        this.current += cur;

        var ifStmt = ifCondition();
        ArrayList<PyStatement> body = new ArrayList<>();

        while (!isEnd()) {
            switch (peek().getType()){
                case 44, 1:
                    break;

                case 45:
                case 25:
                    body.addAll(bodyDeclaration());
                    break;

                case 11:
                    body.add(new PyStatement.BreakStatement());
                    break;

                case 37:
                    if (match(this.current+1, 44)) {
                        body.add(new PyStatement.ReturnStatement());
                    } else {
                        body.add(new PyStatement.ReturnStatement(assignment(1)));
                    }
                    break;

                case 2:
                    return new PyStatement.WhileStatement(ifStmt, body);
            }

            this.current++;
        }

        return null;
    }

    private ArrayList<PyStatement.ArgStatement> argsDeclaration(){
        ArrayList<PyStatement.ArgStatement> args = new ArrayList<>();

        while (!isEnd()) {
            current++;

            var token = peek();

            switch (token.getType()) {
                case 45:
                    Class<?> type;
                    if (match(this.current+1, 60)){
                        current+=2;
                        if (!match(peek().getType(), 45)) throw new RuntimeException("no key");
                        type = keyMap.getOrDefault(peek().getText(), Object.class);
                    } else {
                        type = Object.class;
                    }

                    args.add(new PyStatement.ArgStatement(token, type));
                    break;

                case 59:
                    break;
                case 58:
                    return args;
            }
        }

        throw new RuntimeException("no args");
    }

    private PyStatement.FunStatement defDeclaration(){
        current++;

        var token = peek();

        if (token.getType() == 45) {
            ArrayList<PyStatement.ArgStatement> args = argsDeclaration();
            ArrayList<PyStatement> body = new ArrayList<>();

            while (!isEnd()) {
                current++;

                switch (peek().getType()) {
                    case 1://"    "
                    case 44:
                    case 60:
                        break;

                    case 45, 25, 41:
                        body.addAll(bodyDeclaration());
                        break;

                    case 2:
                        return new PyStatement.FunStatement(token, args, body);

                    case 37:
                        if (match(this.current+1, 44)) {
                            body.add(new PyStatement.ReturnStatement());
                        } else {
                            body.add(new PyStatement.ReturnStatement(assignment(1)));
                        }
                        break;
                }
            }
        }
        throw new RuntimeException("no key");
    }

    private PyStatement varDeclaration(){
        current++;

        var key = peek();

        switch (key.getType()){
            case 63:
                var name = previous();
                var asm = assignment(1);
                return new PyStatement.VarStatement(name, asm);

            //TODO '@='=91 ? 这是什么鬼
            case 88, 89, 90, 92:
                String operator;

                if (key.getType() == 88) {
                    operator = "+";
                } else if (key.getType() == 89) {
                    operator = "-";
                } else if (key.getType() == 90) {
                    operator = "*";
                } else if (key.getType() == 92) {
                    operator = "/";
                } else {
                    throw new RuntimeException("Characters are not recognized");
                }

                return new PyStatement.VarStatement(previous(),
                        new PyStatement.LogicalStatement(
                                new PyStatement.VarCallStatement(previous()),
                                operator,
                                assignment(1)
                        ));

            case 44:
                return new PyStatement.VarStatement(previous(), null);
        }

        return null;
    }

    private PyStatement ifDeclaration(){
        return ifDeclaration(0);
    }

    private PyStatement ifDeclaration(int cur){
        this.current += cur;

        var ifStmt = ifCondition();
        ArrayList<PyStatement> body = new ArrayList<>();

        while (!isEnd()) {
            switch (peek().getType()){
                case 44, 1:
                    break;

                case 37:
                    if (match(this.current+1, 44)) {
                        body.add(new PyStatement.ReturnStatement());
                    } else {
                        body.add(new PyStatement.ReturnStatement(assignment(1)));
                    }
                    break;

                case 45, 25, 41:
                    body.addAll(bodyDeclaration());
                    break;

                case 11:
                    body.add(new PyStatement.BreakStatement());
                    break;

                case 2:
                    return new PyStatement.IfStatement(ifStmt, body);
            }

            this.current++;
        }

        return null;
    }

    private PyStatement ifCondition(){
        PyStatement left = null;
        Token operator = null;
        PyStatement right = null;

        while (!isEnd()) {
            switch (peek().getType()) {
                case 45, 4, 3:
                    if (left != null) {
                        right = assignment();
                    } else {
                        left = assignment();
                    }
                    break;

                case 79, 80, 81, 82, 83, 85:
                    operator = peek();
                    break;

                case 60:
                    if (operator != null && right != null) {
                        return new PyStatement.JudgmentStatement(left, operator, right);
                    } else if (operator == null && right == null){
                        return left;
                    } else {
                        throw new RuntimeException("no key?");
                    }

                case 38, 20:
                    var key = peek();
                    if (Objects.equals(key.getText(), "True")) {
                        return new PyStatement.ConstStatement<>(key, Boolean.class);
                    } else if (Objects.equals(key.getText(), "False")){
                        return new PyStatement.ConstStatement<>(key, Boolean.class);
                    } else {
                        throw new RuntimeException("parser error");
                    }
            }

            this.current++;
        }

        throw new RuntimeException("no key?");
    }

    private PyStatement assignment(){
        return assignment(0);
    }

    private PyStatement assignment(int cur){
        this.current += cur;

        var key = peek();

        switch (key.getType()) {
            case 3:
                return new PyStatement.ConstStatement<>(key, String.class);
            case 4:
                PyStatement constStmt;

                if (TypeChecker.isInteger(key.getText())) {
                    if (match(this.current-1, 72, 89)) {
                        constStmt = new PyStatement.NumberStatement<>(false, key, Integer.class);
                    } else {
                        constStmt = new PyStatement.NumberStatement<>(key, Integer.class);
                    }
                } else if (TypeChecker.isFloatingPointNumber(key.getText())) {
                    constStmt = new PyStatement.NumberStatement<>(key, Float.class);
                } else {
                    throw new RuntimeException("parser error");
                }

                if (match(this.current+1, 71, 72)) {
                    current++;
                    return new PyStatement.LogicalStatement(constStmt, peek(), assignment(1));
                } else if (match(this.current+1, 73, 56)){
                    current++;
                    var lgc = new PyStatement.LogicalStatement(constStmt, peek(), logicalAssignment(1));

                    if (match(this.current+1, 71, 72, 73, 56)) {
                        current++;
                        return new PyStatement.LogicalStatement(
                                lgc,
                                peek(),
                                assignment(1)
                        );
                    } else {
                        return lgc;
                    }
                } else {
                    return constStmt;
                }
            case 38, 20:
                if (Objects.equals(key.getText(), "True")) {
                    return new PyStatement.ConstStatement<>(key, Boolean.class);
                } else if (Objects.equals(key.getText(), "False")){
                    return new PyStatement.ConstStatement<>(key, Boolean.class);
                } else {
                    throw new RuntimeException("parser error");
                }

            case 45:
                PyStatement varmetStmt;

                if (match(this.current+1, 57)){
                    varmetStmt = methodCall();
                } else {
                    varmetStmt = varCall();
                }

                if (match(this.current+1, 71, 72)) {
                    current++;
                    return new PyStatement.LogicalStatement(varmetStmt, peek(), assignment(1));
                } else if (match(this.current+1, 73, 56)) {
                    current++;
                    var lgc = new PyStatement.LogicalStatement(varmetStmt, peek(), logicalAssignment(1));

                    if (match(this.current + 1, 71, 72, 73, 56)) {
                        current++;
                        return new PyStatement.LogicalStatement(
                                lgc,
                                peek(),
                                assignment(1)
                        );
                    } else {
                        return lgc;
                    }
                } else if (match(current+1, 54)) {
                    return submethodCall(varCall());
                } else {
                    return varmetStmt;
                }

            case 64:
                return listAssignment();

            case 31:
                return new PyStatement.NoneStatement();

            default:
                throw new RuntimeException("parser error in "+key);
        }
    }

    private PyStatement logicalAssignment(){
        return logicalAssignment(0);
    }

    private PyStatement logicalAssignment(int cur) {
        this.current += cur;

        var key = peek();

        switch (key.getType()) {
            case 3:
                return new PyStatement.ConstStatement<>(key, String.class);
            case 4:
                if (TypeChecker.isInteger(key.getText())) {
                    return new PyStatement.NumberStatement<>(key, Integer.class);
                } else if (TypeChecker.isFloatingPointNumber(key.getText())) {
                    return new PyStatement.NumberStatement<>(key, Float.class);
                } else {
                    throw new RuntimeException("parser error");
                }

            case 45:
                if (match(this.current+1, 57)){
                    return methodCall();
                } else {
                    return varCall();
                }

            default:
                throw new RuntimeException("parser error");
        }
    }

    private PyStatement varCall(){
        return varCall(0);
    }

    private PyStatement varCall(int current){
        this.current += current;
        return new PyStatement.VarCallStatement(peek());
    }

    private PyStatement listAssignment() {
        return new PyStatement.ListStatement(keyDeclaration());
    }

    private ArrayList<PyStatement> keyDeclaration(){
        ArrayList<PyStatement> args = new ArrayList<>();

        while (!isEnd()) {
            current++;
            switch (peek().getType()) {
                case 4, 3, 38, 45:
                    args.add(assignment());
                    break;

                case 64:
                    break;
                case 65:
                    return args;
            }
        }

        throw new RuntimeException("no args");
    }

    private ArrayList<PyStatement> argsCallDeclaration(){
        ArrayList<PyStatement> args = new ArrayList<>();

        while (!isEnd()) {
            current++;
            switch (peek().getType()) {
                case 4, 3, 38, 45:
                    args.add(assignment());
                    break;

                case 59:
                    break;
                case 58:
                    return args;
            }
        }

        throw new RuntimeException("no args");
    }

    private PyStatement.FunCallStatement methodCall(){
        var funCall = new PyStatement.FunCallStatement(peek());

        current += 1;

        funCall.setArg(argsCallDeclaration());

        return funCall;
    }

    private Token peek() {
        return tokenStream.get(current);
    }

    private Token previous() {
        return tokenStream.get(current - 1);
    }

    private Token last(){
        return tokenStream.get(current + 1);
    }

    private Token advance() {
        if (!isEnd()) current++;
        return previous();
    }

    private boolean match(int current, Integer... types) {
        for (Integer type : types) {
            if (tokenStream.get(current).getType() == type) {
                return true;
            }
        }
        return false;
    }
}
