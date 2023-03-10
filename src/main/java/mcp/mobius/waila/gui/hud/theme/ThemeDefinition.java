package mcp.mobius.waila.gui.hud.theme;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import mcp.mobius.waila.Waila;
import mcp.mobius.waila.api.ITheme;
import mcp.mobius.waila.api.WailaConstants;
import mcp.mobius.waila.registry.Registrar;
import mcp.mobius.waila.util.TypeUtil;
import net.minecraft.resources.ResourceLocation;

public final class ThemeDefinition<T extends ITheme> {

    private static Map<ResourceLocation, ThemeDefinition<?>> all;

    public final ResourceLocation id;
    public final ThemeType<T> type;
    public final boolean builtin;
    public final T instance;

    private boolean initialized = false;

    public ThemeDefinition(ResourceLocation id, ThemeType<T> type, boolean builtin, Map<String, Object> attr) {
        this.id = id;
        this.type = type;
        this.builtin = builtin;

        this.instance = type.create(attr);
    }

    public static Map<ResourceLocation, ThemeDefinition<?>> getAll() {
        if (all == null) {
            all = new HashMap<>(BuiltinThemeLoader.THEMES);
            all.putAll(Waila.CONFIG.get().getOverlay().getColor().getCustomThemes());
        }

        return all;
    }

    public static void resetAll() {
        all = null;
    }

    public T getInitializedInstance() {
        if (!initialized) {
            initialized = true;
            instance.processProperties(ThemeAccessor.INSTANCE);
        }

        return instance;
    }

    public static class Adapter implements JsonSerializer<ThemeDefinition<?>>, JsonDeserializer<ThemeDefinition<?>> {

        public static ThemeDefinition<?> deserialize(ResourceLocation id, JsonElement json, boolean builtin) {
            JsonObject object = json.getAsJsonObject();

            ResourceLocation typeId = object.has("type")
                ? new ResourceLocation(object.get("type").getAsString())
                : WailaConstants.THEME_TYPE_GRADIENT;

            ThemeType<?> type = Registrar.INSTANCE.themeTypes.get(typeId);
            Map<String, Object> values = new HashMap<>();

            type.properties.forEach((key, prop) -> {
                if (object.has(key)) {
                    Class<?> propType = prop.type;
                    JsonElement value = object.get(key);

                    if (propType == int.class) {
                        values.put(key, value.getAsInt());
                    } else if (propType == boolean.class) {
                        values.put(key, value.getAsBoolean());
                    } else if (propType == double.class) {
                        values.put(key, value.getAsDouble());
                    } else if (propType == String.class) {
                        values.put(key, value.getAsString());
                    } else if (propType.isEnum()) {
                        values.put(key, Enum.valueOf(TypeUtil.uncheckedCast(propType), value.getAsString()));
                    } else {
                        throw new IllegalArgumentException("Invalid property type " + propType.getSimpleName());
                    }
                } else {
                    values.put(key, Preconditions.checkNotNull(prop.defaultValue, "Property " + key + " is missing"));
                }
            });

            return new ThemeDefinition<>(id, type, builtin, values);
        }

        @Override
        public ThemeDefinition<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            ResourceLocation id = new ResourceLocation(object.get("id").getAsString());

            return deserialize(id, json, false);
        }

        @Override
        public JsonElement serialize(ThemeDefinition<?> src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            object.addProperty("id", src.id.toString());
            object.addProperty("type", Registrar.INSTANCE.themeTypes.inverse().get(src.type).toString());

            src.type.properties.forEach((key, prop) -> {
                Class<?> propType = prop.type;
                Object propValue = prop.get(src.instance);

                if (propType == int.class) {
                    object.addProperty(key, TypeUtil.<Integer>uncheckedCast(propValue));
                } else if (propType == boolean.class) {
                    object.addProperty(key, TypeUtil.<Boolean>uncheckedCast(propValue));
                } else if (propType == double.class) {
                    object.addProperty(key, TypeUtil.<Double>uncheckedCast(propValue));
                } else if (propType == String.class) {
                    object.addProperty(key, TypeUtil.<String>uncheckedCast(propValue));
                } else if (propType.isEnum()) {
                    object.addProperty(key, TypeUtil.<Enum<?>>uncheckedCast(propValue).name());
                } else {
                    throw new IllegalArgumentException("Invalid property type " + propType.getSimpleName());
                }
            });

            return object;
        }

    }

}
