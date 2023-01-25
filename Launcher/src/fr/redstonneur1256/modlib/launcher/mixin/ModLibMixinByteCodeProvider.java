package fr.redstonneur1256.modlib.launcher.mixin;

import fr.redstonneur1256.modlib.launcher.ModLibLauncher;
import fr.redstonneur1256.modlib.launcher.util.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.transformers.MixinClassReader;

import java.io.IOException;
import java.io.InputStream;

public class ModLibMixinByteCodeProvider implements IClassBytecodeProvider {

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        InputStream stream = ModLibLauncher.loader.getResourceAsStream(name.replace('.', '/') + ".class");
        if(stream == null) {
            throw new ClassNotFoundException(name);
        }

        byte[] data = Util.readFully(stream);

        ClassNode node = new ClassNode();
        ClassReader reader = new MixinClassReader(data, name);
        reader.accept(node, ClassReader.EXPAND_FRAMES);

        return node;
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return getClassNode(name);
    }

}
