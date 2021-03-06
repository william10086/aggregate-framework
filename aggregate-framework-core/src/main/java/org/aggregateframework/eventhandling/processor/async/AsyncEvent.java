package org.aggregateframework.eventhandling.processor.async;

import java.lang.reflect.Method;

/**
 * Created by changmingxie on 12/2/15.
 */
public class AsyncEvent {

    private Class payloadType;

    private Method method;

    private Object target;

    private Object[] params;

    public void reset(Class payloadType, Method method, Object target, Object... params) {
        this.payloadType = payloadType;
        this.method = method;
        this.target = target;
        this.params = params;
    }

    public Method getMethod() {
        return method;
    }

    public Object getTarget() {
        return target;
    }

    public Object[] getParams() {
        return params;
    }

    public Class getPayloadType() {
        return payloadType;
    }
}
