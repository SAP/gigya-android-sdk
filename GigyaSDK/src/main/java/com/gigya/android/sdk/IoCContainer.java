package com.gigya.android.sdk;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

@SuppressWarnings("unchecked")
public class IoCContainer {

    private static final String LOG_TAG = "IoCContainer";

    public static class BindInfo<T> {
        public Class<T> concrete;
        public boolean asSingleton;
        public T instance;

        public BindInfo(Class<T> c, boolean s) {
            concrete = c;
            asSingleton = s;
        }

        public BindInfo(T i) {
            concrete = (Class<T>) i.getClass();
            asSingleton = true;
            instance = i;
        }
    }

    private Map<Class<?>, BindInfo> _bindings = new HashMap<>();
    //private Map<Class<?>, Object> _instances = new HashMap<>();

    public <I, C extends I> void bind(Class<I> contractClazz, final Class<C> concreteClazz, final boolean asSingleton) {
        GigyaLogger.info(LOG_TAG, "Binding " + contractClazz.getCanonicalName() + " to " + concreteClazz.getCanonicalName() + " as " + (asSingleton ? "singleton" : "factory"));
        _bindings.put(contractClazz, new BindInfo<>(concreteClazz, asSingleton));
    }

    public <I, C extends I> void bind(Class<I> contractClazz, C concreteInstance) {
        //bind(contractClazz, (Class<C>)concreteInstance.getClass(), true);
        System.out.println("binding " + contractClazz.getCanonicalName() + " to instance (of type " + concreteInstance.getClass().getCanonicalName() + ")");
        //_instances.put(contractClazz, concreteInstance);
        _bindings.put(contractClazz, new BindInfo<>(concreteInstance));
    }

    public <T> T get(Class<T> contractClazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        GigyaLogger.info(LOG_TAG, "Trying to get: " + contractClazz.getCanonicalName());

        if (!_bindings.containsKey(contractClazz)) {
            GigyaLogger.info(LOG_TAG, "Contract was not registered");
            return null;
        }

        BindInfo<T> bindingInfo = _bindings.get(contractClazz);
        if (bindingInfo == null) {
            GigyaLogger.info(LOG_TAG, "Contract was not registered = null");
            return null;
        }

        if (bindingInfo.instance != null) {
            return bindingInfo.instance;
        }
        T instance = createInstance(bindingInfo.concrete);
        if (bindingInfo.asSingleton) {
            bindingInfo.instance = instance;
        }
        return instance;
    }

    private <T> T createInstance(Class<T> concreteClazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        GigyaLogger.info(LOG_TAG, "Trying to create new instance for: " + concreteClazz.getCanonicalName());
        Constructor<?>[] constructors = concreteClazz.getConstructors();
        if (constructors.length == 0) {
            GigyaLogger.info(LOG_TAG, "Default constructor - creating instance");
            return concreteClazz.newInstance();
        }

        for (Constructor<?> ctor : constructors) {
            Class<?>[] ctorParams = ctor.getParameterTypes();
            GigyaLogger.info(LOG_TAG, "For constructor with params #: " + ctorParams.length);

            List<Object> params = new ArrayList<>();
            for (Class paramContract : ctorParams) {
                GigyaLogger.info(LOG_TAG, "Getting required param: " + paramContract.getCanonicalName());
                Object paramInstance = get(paramContract);
                if (paramInstance != null) {
                    params.add(paramInstance);
                } else {
                    // missing param, so keep searching
                    break;
                }
            }

            if (params.size() == ctorParams.length) {
                GigyaLogger.info(LOG_TAG, "Creating new instance for " + concreteClazz.getCanonicalName());
                return (T) ctor.newInstance(params.toArray());
            } else {
                GigyaLogger.info(LOG_TAG, "Constructor wasn't suitable");
            }
        }

        throw new MissingResourceException("Concrete class missing dependencies", concreteClazz.getName(), "iocContainer");
    }
}
