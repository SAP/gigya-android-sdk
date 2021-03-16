package com.gigya.android.sdk.schema;

import java.util.Map;

public class GigyaSchema {

    public GigyaProfileSchema profileSchema;
    public GigyaDataSchema dataSchema;
    public GigyaSubscriptionSchema subscriptionsSchema;
    public GigyaPreferencesSchema preferencesSchema;

    public static class GigyaProfileSchema {

        public Map<String, ProfileSchema> fields;
        public boolean dynamicSchema;
    }

    public static class GigyaDataSchema {

        public Map<String, DataSchema> fields;
        public boolean dynamicSchema;
    }

    public static class GigyaSubscriptionSchema {

        public Map<String, SubscriptionSchema> fields;
        public boolean dynamicSchema;
    }

    public static class GigyaPreferencesSchema {

        public Map<String, PreferenceSchema> fields;
        public boolean dynamicSchema;
    }
}
