package chire.asm;

import chire.asm.dynamic.ClassBuilder;
import org.objectweb.asm.Opcodes;

import java.io.FileOutputStream;
import java.io.IOException;

public class AsmBuddy {
    private ClassAsm classAsm;

    public static void main(String[] args) {
        try (FileOutputStream fos = new FileOutputStream("TestCl.class")) {
            fos.write(new AsmBuddy()
                            .defineClass("TestCl", Object.class)
                            .defineConstruct()
                            .definitObj("ssss")
                            .toVar("a")
                            .definitObj("wwww")
                            .toVar("b")
                            ._back()
                            .defineClinit()
                            .definitObj("asd")
                            .toVar("f")
                            ._back()
                            .defineFunction("tete")
                            ._return(false)
                            .make()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AsmBuddy() {
        this(new ClassAsm());
    }

    public AsmBuddy(ClassAsm classAsm) {
        this.classAsm = classAsm;
    }

    public ClassBuilder defineClass(String name, Class<?> superClass) {
        classAsm.defineClass(name, superClass);

        return new ClassBuilder(classAsm);
    }
}
