package it.polimi.nsds.kafka.services.registration;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegistrationRemote extends Remote {
	Integer checkCompletedCourse(String course_name, String username) throws RemoteException;
}