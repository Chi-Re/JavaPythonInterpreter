package chire.asm.dynamic.definition;

import chire.asm.ClassAsm;
import chire.asm.dynamic.BlockBuilder;
import chire.asm.dynamic.ClassBuilder;

public class FunctionDefinition extends BlockBuilder<FunctionDefinition> {
    public FunctionDefinition(ClassAsm classAsm) {
        super(classAsm, FunctionDefinition.class);
    }

    public ClassBuilder _return(boolean retu) {
        classAsm.toReturn(retu);

        return new ClassBuilder(classAsm);
    }
}
