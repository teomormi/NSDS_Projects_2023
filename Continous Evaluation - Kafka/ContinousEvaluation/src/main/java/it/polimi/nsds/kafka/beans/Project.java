package it.polimi.nsds.kafka.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Project implements Serializable {

	private final String name;
	private final Map<String,Submission> submissions = new HashMap<>(); // username, submission details

	public Project(String name) {
		this.name = name;
	}

	public String getProjectName() {
		return name;
	}

	public boolean addSubmission(String username,String file_path){
		if(submissions.get(username)!=null)
			return false; // submission already present
		submissions.put(username,new Submission(file_path));
		return true;
	}

	public Submission getSubmissionDetails(String username){
		if(submissions.get(username)==null)
			return null; // submission not found
		return submissions.get(username);
	}

	public boolean addGrade(String username,int grade){
		if(submissions.get(username)==null)
			return false;
		Submission sub = submissions.get(username);
		return sub.setGrade(grade);
	}

	public Map<String,Submission> getSubmissions(){
		return new HashMap<>(submissions);
	}
}
