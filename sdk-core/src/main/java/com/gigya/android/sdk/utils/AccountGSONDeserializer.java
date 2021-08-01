package com.gigya.android.sdk.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Account GSON custom parser.
 * <p>
 * Specific profile fields that are handled here can return from the server as objects or as array of objects.
 *
 * @param <T>
 */
public class AccountGSONDeserializer<T> implements JsonDeserializer<T> {

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        final JsonElement copy = json.deepCopy();
        objectToArrayOnProfileField(json, copy, "certifications");
        objectToArrayOnProfileField(json, copy, "education");
        objectToArrayOnProfileField(json, copy, "favorites");
        objectToArrayOnProfileField(json, copy, "likes");
        objectToArrayOnProfileField(json, copy, "patents");
        objectToArrayOnProfileField(json, copy, "phones");
        objectToArrayOnProfileField(json, copy, "publications");
        objectToArrayOnProfileField(json, copy, "skills");
        objectToArrayOnProfileField(json, copy, "work");
        return new Gson().fromJson(copy, typeOfT);
    }

    /**
     * Check field response and make sure that data is transformed to an array of objects as
     * is declared on the base schema object.
     *
     * @param origin Origin JSON element.
     * @param copy   Deep copy of the JSON element.
     * @param field  Required field for transformation.
     */
    private void objectToArrayOnProfileField(JsonElement origin, JsonElement copy, String field) {
        final JsonElement profile = origin.getAsJsonObject().get("profile");
        if (profile != null) {
            final JsonElement transformedField = profile.getAsJsonObject().get(field);
            if (transformedField != null) {
                if (transformedField.isJsonObject()) {
                    final JsonArray array = new JsonArray();
                    array.add(transformedField);
                    copy.getAsJsonObject().get("profile").getAsJsonObject()
                            .add(field, array);
                }
            }
        }
    }
}
