package chire.asm.dynamic;

import chire.asm.Args;
import chire.asm.ClassAsm;
import chire.asm.dynamic.definition.ClinitDefinition;
import chire.asm.dynamic.definition.ConstructDefinition;
import chire.asm.dynamic.definition.FunctionDefinition;
import org.objectweb.asm.Opcodes;

public class ClassBuilder {
    private final ClassAsm classAsm;

    public ClassBuilder(ClassAsm classAsm) {
        this.classAsm = classAsm;
    }

    public ConstructDefinition defineConstruct(int access, Args args){
        classAsm.defineFunction(access, "<init>", args, null);

        return new ConstructDefinition(classAsm);
    }

    public ConstructDefinition defineConstruct(){
        return defineConstruct(Opcodes.ACC_PUBLIC, new Args());
    }

    public ClinitDefinition defineClinit(int access){
        classAsm.defineFunction(access, "<clinit>", new Args(), null);

        return new ClinitDefinition(classAsm);
    }

    public ClinitDefinition defineClinit(){
        return defineClinit(Opcodes.ACC_PUBLIC);
    }

    public FunctionDefinition defineFunction(int access, String name, Args args, Class<?> returnType) {
        classAsm.defineFunction(access, name, args, returnType);

        return new FunctionDefinition(classAsm);
    }

    public FunctionDefinition defineFunction(int access, String name, Args args) {
        return defineFunction(access, name, args, null);
    }

    public FunctionDefinition defineFunction(int access, String name) {
        return defineFunction(access, name, new Args());
    }

    public FunctionDefinition defineFunction(String name) {
        return defineFunction(Opcodes.ACC_PUBLIC, name, new Args());
    }

    public byte[] make() {
        classAsm.closeClass();

        return classAsm.getByte();
    }
}
