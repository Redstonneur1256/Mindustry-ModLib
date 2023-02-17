package fr.redstonneur1256.modlib.launcher.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unchecked")
public class ModLibMixinPropertyService implements IGlobalPropertyService {

    private Map<IPropertyKey, Object> values = new HashMap<>();

    @Override
    public IPropertyKey resolveKey(String name) {
        return new Key(name);
    }

    @Override
    public <T> T getProperty(IPropertyKey key) {
        return (T) values.get(key);
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        values.put(key, value);
    }

    @Override
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        return (T) values.getOrDefault(key, defaultValue);
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        return (String) values.getOrDefault(key, defaultValue);
    }

    private static class Key implements IPropertyKey {

        private final String name;

        public Key(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Key{name='" + name + "'}";
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(name, key.name);
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }

    }

}
