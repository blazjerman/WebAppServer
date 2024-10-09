package server;
import com.google.gson.JsonObject;

public class Response {

    final public JsonObject data;
    final public int rCode;

    public Response(JsonObject data, int rCode) {
        this.data = data;
        this.rCode = rCode;
    }

    public Response(String error, int rCode) {
        this.data = new JsonObject();
        this.data.addProperty("error", error);
        this.rCode = rCode;
    }

    public Response(String error, boolean resetSession, int rCode) {
        this.data = new JsonObject();
        this.data.addProperty("error", error);
        this.data.addProperty("resetSession", resetSession);
        this.rCode = rCode;
    }
}
