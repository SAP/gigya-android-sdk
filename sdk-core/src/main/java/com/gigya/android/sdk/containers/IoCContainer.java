package com.gigya.android.sdk.containers;

import com.gigya.android.sdk.GigyaLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

@SuppressWarnings("unchecked")
public class IoCContainer {

    private static final String LOG_TAG = "IoCContainer";

    public static class BindInfo<T> {
        Class<T> concrete;
        boolean asSingleton;
        public T instance;

        BindInfo(Class<T> c, boolean s) {
            concrete = c;
            asSingleton = s;
        }

        BindInfo(T i) {
            concrete = (Class<T>) i.getClass();
            asSingleton = true;
            instance = i;
        }
    }

    private Map<Class<?>, BindInfo> _bindings = new HashMap<>();

    public <I, C extends I> IoCContainer bind(Class<I> contractClazz, final Class<C> concreteClazz, final boolean asSingleton) {
        GigyaLogger.ioc(LOG_TAG, "Binding " + contractClazz.getCanonicalName() + " to " + concreteClazz.getCanonicalName() + " as " + (asSingleton ? "singleton" : "factory"));
        _bindings.put(contractClazz, new BindInfo<>(concreteClazz, asSingleton));
        return this;
    }

    public <I, C extends I> IoCContainer bind(Class<I> contractClazz, C concreteInstance) {
        GigyaLogger.ioc(LOG_TAG, "binding " + contractClazz.getCanonicalName() + " to instance (of type " + concreteInstance.getClass().getCanonicalName() + ")");
        _bindings.put(contractClazz, new BindInfo<>(concreteInstance));
        return this;
    }

    public <T> T get(Class<T> contractClazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        GigyaLogger.ioc(LOG_TAG, "Trying to get: " + contractClazz.getCanonicalName());

        if (!isBound(contractClazz)) {
            GigyaLogger.ioc(LOG_TAG, "Contract was not registered");
            return null;
        }

        BindInfo<T> bindingInfo = _bindings.get(contractClazz);
        if (bindingInfo == null) {
            GigyaLogger.ioc(LOG_TAG, "Contract was not registered = null");
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

    public <T> T createInstance(Class<T> concreteClazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return createInstance(concreteClazz, false);
    }

    public <T> T createInstance(Class<T> concreteClazz, boolean forceCreation) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        GigyaLogger.ioc(LOG_TAG, "Trying to create new instance for: " + concreteClazz.getCanonicalName());
        Constructor<?>[] constructors = concreteClazz.getDeclaredConstructors();
        if (constructors.length == 0) {
            GigyaLogger.ioc(LOG_TAG, "Default constructor - creating instance");
            return concreteClazz.newInstance();
        }

        for (Constructor<?> ctor : constructors) {
            Class<?>[] ctorParams = ctor.getParameterTypes();
            GigyaLogger.ioc(LOG_TAG, "For constructor with params #: " + ctorParams.length);

            List<Object> params = new ArrayList<>();
            for (Class paramContract : ctorParams) {
                GigyaLogger.ioc(LOG_TAG, "Getting required param: " + paramContract.getCanonicalName());

                Object paramInstance = get(paramContract);

                if (paramInstance == null && forceCreation) {
                    paramInstance = createInstance(paramContract);
                }

                if (paramInstance != null) {
                    params.add(paramInstance);
                } else {
                    // missing param, so keep searching
                    break;
                }
            }

            if (params.size() == ctorParams.length) {
                GigyaLogger.ioc(LOG_TAG, "Creating new instance for " + concreteClazz.getCanonicalName());
                if (Modifier.isProtected(ctor.getModifiers())) {
                    GigyaLogger.ioc(LOG_TAG, "Constructor is protected");
                    ctor.setAccessible(true);
                }
                return (T) ctor.newInstance(params.toArray());
            } else {
                GigyaLogger.ioc(LOG_TAG, "Constructor wasn't suitable");
            }
        }

        throw new MissingResourceException("Concrete class missing dependencies", concreteClazz.getName(), "iocContainer");
    }

    public boolean isBound(Class contractClazz) {
        return _bindings.containsKey(contractClazz);
    }

    public IoCContainer clone() {
        IoCContainer clone = new IoCContainer();
        clone._bindings = new HashMap<>(this._bindings);
        return clone;
    }

    public void dispose() {
        _bindings.clear();
        _bindings = null;
    }

    public void clear() {
        _bindings.clear();
    }
}
