package chire.asm;

import chire.asm.util.Format;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class ClassAsm {
    private ClassWriter cw;

    private MethodVisitor mv = null;

    private final Map<String, Integer> varsKey = new HashMap<>();

    public ClassAsm() {
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    }

    public void defineClass(String className, Class<?> superClass) {
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, Format.formatPack(superClass), null);
    }

    public void defineFunction(int access, String name, Args args, Class<?> returnType) {
        mv = cw.visitMethod(access, name,
                args+(returnType==null?"V":(Format.formatPack(returnType)+";")),
                null, null);

        for (String var : args.getArgNames()) {
            varsKey.put(var, varsKey.size()+1);
        }

        mv.visitCode();
    }

    public void toVar(String name) {
        if (varsKey.containsKey(name)) {
            mv.visitVarInsn(ASTORE, varsKey.get(name));
        } else {
            varsKey.put(name, varsKey.size());
            mv.visitVarInsn(ASTORE, varsKey.size());
        }
    }

    public void ldcInsn(Object obj){
        mv.visitLdcInsn(obj);
    }

    public void toReturn(boolean returnValue) {
        if (returnValue) {
            mv.visitInsn(ARETURN);
        } else {
            mv.visitInsn(RETURN);
        }
    }

    public void toReturn() {
        toReturn(false);
    }

    public void returnBlock(){
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public void closeClass(){
        mv.visitEnd();
    }

    public byte[] getByte(){
        return cw.toByteArray();
    }
}
