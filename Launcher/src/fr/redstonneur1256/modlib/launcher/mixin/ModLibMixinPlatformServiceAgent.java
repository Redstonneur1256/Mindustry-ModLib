package fr.redstonneur1256.modlib.launcher.mixin;

import fr.redstonneur1256.modlib.launcher.ModLibLauncher;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.MixinPlatformAgentAbstract;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.util.Constants;

import java.util.Collection;

public class ModLibMixinPlatformServiceAgent extends MixinPlatformAgentAbstract implements IMixinPlatformServiceAgent {

    @Override
    public void init() {

    }

    @Override
    public String getSideName() {
        return ModLibLauncher.launcher.server ? Constants.SIDE_SERVER : Constants.SIDE_CLIENT;
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return null;
    }

}
