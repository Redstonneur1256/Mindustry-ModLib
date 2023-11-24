package fr.redstonneur1256.modlib.launcher;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class AccessWidenerTransformer implements ClassTransformer {

    private AccessWidener widener;

    public AccessWidenerTransformer() {
        this(new AccessWidener());
    }

    public AccessWidenerTransformer(AccessWidener widener) {
        this.widener = widener;
    }

    @Override
    public byte[] transform(byte[] clazz, String name) {
        if (clazz == null || !widener.getTargets().contains(name)) {
            return clazz;
        }

        ClassReader reader = new ClassReader(clazz);
        ClassWriter writer = new ClassWriter(0);
        ClassVisitor visitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, writer, widener);
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    public AccessWidener getWidener() {
        return widener;
    }

}
