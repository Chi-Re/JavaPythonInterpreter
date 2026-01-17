package chire.asm.dynamic.definition;

import chire.asm.ClassAsm;
import chire.asm.dynamic.BlockBuilder;
import chire.asm.dynamic.ClassBuilder;

public class ConstructDefinition extends BlockBuilder<ConstructDefinition> {
    public ConstructDefinition(ClassAsm classAsm) {
        super(classAsm, ConstructDefinition.class);
    }

    public ClassBuilder _back(){
        classAsm.returnBlock();
        return new ClassBuilder(classAsm);
    }
}
