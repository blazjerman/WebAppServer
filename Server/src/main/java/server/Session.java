package server;

import com.google.gson.JsonObject;

import java.lang.reflect.Method;
import java.time.LocalTime;


public abstract class Session {

    private LocalTime sessionTime = LocalTime.now();

    public final synchronized Response runMethod(String methodName, JsonObject data) {

        try {
            Method method = this.getClass().getMethod(methodName, JsonObject.class);
            return new Response((JsonObject) method.invoke(this, data.getAsJsonObject("data")), 200);
        } catch (NoSuchMethodException e) {
            return new Response("Method " + methodName + " not found!", 400);
        } catch (Exception e) {
            return new Response("Error invoking method " + methodName + ": " + e.getMessage(), 500);
        }

    }

    public LocalTime getSessionTime() {
        return sessionTime;
    }

    public final JsonObject updateSession(JsonObject data) {
        sessionTime = LocalTime.now();
        return data;
    }


}
