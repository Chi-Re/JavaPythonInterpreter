package chire.python.antlr;

import chire.python.util.handle.SubClass;

public class PyClassExecutor extends PyExecutor {
    private SubClass subclass;

    public PyClassExecutor(SubClass subclass) {
        this.subclass = subclass;
    }

    @Override
    public void setVar(String key, Object value) {
        super.setVar(key, value);
        this.subclass.addVariable(key, value);
    }

    public SubClass getSubClass() {
        return subclass;
    }
}
