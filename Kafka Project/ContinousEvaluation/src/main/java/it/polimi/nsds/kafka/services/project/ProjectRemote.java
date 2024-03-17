package it.polimi.nsds.kafka.services.project;

import it.polimi.nsds.kafka.beans.Submission;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface ProjectRemote extends Remote {
	boolean addSubmission(String course_name,String project_name,String username, String file_path) throws RemoteException;
	boolean addGrade(String course_name,String project_name, String username, String grade) throws RemoteException;
	Submission getSubmissionDetails(String course_name, String project_name, String username) throws RemoteException;
	Map<String,Submission> getSubmissions(String course_name, String project_name) throws RemoteException;
}