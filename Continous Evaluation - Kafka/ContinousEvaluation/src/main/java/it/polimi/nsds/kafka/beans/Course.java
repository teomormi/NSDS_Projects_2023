package it.polimi.nsds.kafka.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Course implements Serializable {

	private final String course_name;
	private final List<String> enrolled_students = new ArrayList<>(); // contains student's username
	private final List<String> available_projects = new ArrayList<>(); // contains project's name

	public Course(String course_name){
		this.course_name = course_name;
	}

	// add student to enrolled students
	public boolean AddStudent(String username){
		if(enrolled_students.contains(username))
			return false;
		this.enrolled_students.add(username);
		return true;
	}

	// add project to list of available projects
	public boolean AddProject(String project_name){
		if(available_projects.contains(project_name))
			return false;
		this.available_projects.add(project_name);
		return true;
	}

	public String getCourse_name() {
		return course_name;
	}

	public List<String> getEnrolled_students() {
		return new ArrayList<>(enrolled_students);
	}

	public List<String> getAvailable_projects() {
		return new ArrayList<>(available_projects);
	}
}
