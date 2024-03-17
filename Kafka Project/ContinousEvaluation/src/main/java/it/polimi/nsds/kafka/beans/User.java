package it.polimi.nsds.kafka.beans;

import it.polimi.nsds.kafka.enums.Gender;
import it.polimi.nsds.kafka.enums.Role;
import java.io.Serializable;

public class User implements Serializable {

    private final String name;
    private final String password;
    private final Gender gender;
    private final Role role;

    public User(String name, String password, Role role, Gender gender){
        this.name = name;
        this.password = password;
        this.role = role;
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", password='" + password + '\'' +
                ", gender=" + gender +
                ", role=" + role +
                '}';
    }
}
