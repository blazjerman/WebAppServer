package app;

import java.util.LinkedList;
import java.util.List;

public class User {

    private static final List<User> users = new LinkedList<>();

    private final String userName;
    private final String password;


    private User(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }


    public static boolean addUser(String userName, String password) {

        for (User user : users) {
            if (user.userName.equals(userName)) return false;
        }

        users.add(new User(userName, password));
        return true;
    }

    public static User getUser(String userName) {

        for (User user : users) {
            if (user.userName.equals(userName)) return user;
        }

        return null;

    }

    public String getPassword() {
        return password;
    }

    public String getUserName() {
        return userName;
    }
}
