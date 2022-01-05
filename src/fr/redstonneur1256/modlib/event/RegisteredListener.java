package fr.redstonneur1256.modlib.event;

import arc.func.Cons;

public class RegisteredListener {

    private Object type;
    private Cons<?> action;

    public RegisteredListener(Object type, Cons<?> action) {
        this.type = type;
        this.action = action;
    }

    public Object getType() {
        return type;
    }

    public Cons<?> getAction() {
        return action;
    }

    public void unregister() {
        EventUtil.unregister(this);
    }

}
