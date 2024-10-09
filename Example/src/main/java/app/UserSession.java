package app;

import com.google.gson.JsonObject;
import server.Session;


public class UserSession extends Session {


    private User user = null;


    public JsonObject login(JsonObject in) {

        User user = User.getUser(String.valueOf(in.get("username")));

        JsonObject data = new JsonObject();

        if (user == null) {
            data.addProperty("text","User does not exists!");
        }
        else if (!user.getPassword().equals(String.valueOf(in.get("password")))) {
            data.addProperty("text","Password incorrect!");
        }
        else {
            this.user = user;
            data.addProperty("text", "Logged in!");
        }
        return data;
    }


    public JsonObject register(JsonObject in) {

        JsonObject data = new JsonObject();

        if (!User.addUser(String.valueOf(in.get("username")), String.valueOf(in.get("password")))) {
            data.addProperty("text", "User already exists!");
        }else {
            data.addProperty("text", "Registered successfully!");
        }

        return data;
    }


    public JsonObject getUserName(JsonObject in) {

        JsonObject data = new JsonObject();

        if (user != null) data.addProperty("username", user.getUserName());
        else data.addProperty("username", "");

        return data;
    }


}
