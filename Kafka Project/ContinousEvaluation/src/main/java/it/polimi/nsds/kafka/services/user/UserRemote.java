package it.polimi.nsds.kafka.services.user;

import it.polimi.nsds.kafka.enums.Role;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface UserRemote extends Remote {
	Role checkUser(String username, String password) throws RemoteException;
	boolean addUser(String username,String password, String role, String gender) throws RemoteException;
}
