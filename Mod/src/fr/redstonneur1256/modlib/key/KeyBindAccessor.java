package fr.redstonneur1256.modlib.key;

import arc.KeyBinds;
import org.jetbrains.annotations.ApiStatus;

/**
 * Internal accessor class to register new keybindings, use {@link KeyBindManager} instead
 */
@ApiStatus.Internal
public interface KeyBindAccessor {

    void registerKeyBinds(KeyBinds.KeyBind[] binds);

}
