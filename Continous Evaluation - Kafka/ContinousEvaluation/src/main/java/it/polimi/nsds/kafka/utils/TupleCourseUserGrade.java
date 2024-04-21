package it.polimi.nsds.kafka.utils;

public class TupleCourseUserGrade {
	private final String course;
	private final String username;
	private final Integer grade;

	public TupleCourseUserGrade(String course, String username, Integer grade) {
		this.course = course;
		this.username = username;
		this.grade = grade;
	}

	public String getCourse() {
		return course;
	}

	public String getUsername() {
		return username;
	}

	public Integer getGrade() {
		return grade;
	}
}
