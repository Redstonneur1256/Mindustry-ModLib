package fr.redstonneur1256.examplemod;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.util.Log;
import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.net.call.CallResult;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Player;

import java.util.concurrent.TimeUnit;

public class CustomCallExample {

    public interface CustomCall {

        /**
         * All methods must return CallResult or void, on server side the methods can have a Player as first argument
         */
        CallResult<Boolean> isFoo(Player player, String text);

    }

    public static void init() {
        MVars.net.registerCall(CustomCall.class, new CustomCall() {
            @Override
            public CallResult<Boolean> isFoo(Player player, String text) {
                if("foo".equals(text)) {
                    return CallResult.of(true);
                }
                throw new RuntimeException("it's not foo");
            }
        });


        // For the client side, every game tick:
        Events.run(EventType.Trigger.update, () -> {
            // Check if the key J has been tapped
            if(Core.input.keyTap(KeyCode.j) && Vars.net.client()) {
                // Obtain our custom call:
                CustomCall call = MVars.net.getCall(CustomCall.class);
                try {
                    // Invoke the method (on client side passing the player parameter is ignored, on server side it will call at the player)
                    CallResult<Boolean> result = call.isFoo(null, Core.input.keyDown(KeyCode.h) ? "notFoo" :  "foo");
                    // Listen for the result
                    result.listen(
                            is -> Log.info("Is it foo ? @", is),
                            err -> Log.err("It might not be foo", err),
                            () -> Log.warn("too long, ignoring result"),
                            1, TimeUnit.SECONDS
                    );
                }catch(NoSuchMethodError error) {
                    // In case where the server is missing the specified call class or method a NoSuchMethodError is thrown
                    Log.warn("The server doesn't have isFoo");
                }
            }
        });
    }

}
