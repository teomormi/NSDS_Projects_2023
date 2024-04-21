package it.polimi.nsds.kafka.utils;

public class PairUsernameGrade {
	private final String username;
	private final String grade;

	public PairUsernameGrade(String username, String grade) {
		this.username = username;
		this.grade = grade;
	}

	public String getUsername() {
		return username;
	}

	public String getGrade() {
		return grade;
	}
}
