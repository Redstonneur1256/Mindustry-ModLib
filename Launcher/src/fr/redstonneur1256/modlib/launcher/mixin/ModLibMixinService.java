package fr.redstonneur1256.modlib.launcher.mixin;

import fr.redstonneur1256.modlib.launcher.ModLibLauncher;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;
import org.spongepowered.asm.logging.LoggerAdapterDefault;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.util.IConsumer;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public class ModLibMixinService extends MixinServiceAbstract {

    private ModLibMixinClassProvider mixinClassProvider;
    private ModLibMixinByteCodeProvider mixinByteCodeProvider;
    private ContainerHandleVirtual containerHandleVirtual;
    private IConsumer<MixinEnvironment.Phase> phaseConsumer;

    public ModLibMixinService() {
        mixinClassProvider = new ModLibMixinClassProvider();
        mixinByteCodeProvider = new ModLibMixinByteCodeProvider();
        containerHandleVirtual = new ContainerHandleVirtual(getName());
    }

    @Override
    public String getName() {
        return "ModLib-Mixin";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return MixinEnvironment.Phase.PREINIT;
    }

    @Override
    public void init() {
    }

    public void onGameStart() {
        phaseConsumer.accept(MixinEnvironment.Phase.DEFAULT);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
        super.wire(phase, phaseConsumer);
        this.phaseConsumer = phaseConsumer;
    }

    @Override
    public IClassProvider getClassProvider() {
        return mixinClassProvider;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return mixinByteCodeProvider;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return null; // null
    }

    @Override
    public IClassTracker getClassTracker() {
        return null;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null; // null
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Collections.emptySet();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return containerHandleVirtual;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return ModLibLauncher.launcher.loader.getResourceAsStream(name);
    }

    @Override
    protected ILogger createLogger(String name) {
        LoggerAdapterConsole logger = new LoggerAdapterConsole("ModLib-" + name);
        logger.setDebugStream(System.out);
        return new LoggerAdapterDefault(name);
    }

}
