package it.polimi.nsds.kafka.services.course;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CourseRemote extends Remote {
	boolean addCourse(String course_name) throws RemoteException;
	boolean removeCourse(String course_name) throws RemoteException;
	boolean enrollStudent(String username, String course_name) throws RemoteException;
	List<String> getCoursesName() throws RemoteException;
	List<String> getCoursesNameEnrolled(String username) throws RemoteException;
	List<String> getCoursesNameNotEnrolled(String username) throws RemoteException;
	boolean addProject(String course_name,String project_name) throws RemoteException;
	List<String> getProjectsName(String course_name) throws RemoteException;
}
