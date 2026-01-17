package chire.asm.dynamic.definition;

import chire.asm.ClassAsm;
import chire.asm.dynamic.BlockBuilder;
import chire.asm.dynamic.ClassBuilder;

public class ClinitDefinition extends BlockBuilder<ClinitDefinition> {

    public ClinitDefinition(ClassAsm classAsm) {
        super(classAsm, ClinitDefinition.class);
    }

    public ClassBuilder _back(){
        classAsm.returnBlock();
        return new ClassBuilder(classAsm);
    }
}
