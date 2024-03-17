package it.polimi.nsds.kafka.beans;

import it.polimi.nsds.kafka.utils.PairUsernameGrade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RegisteredProject implements Serializable {

	private final String name; // project_name
	private final List<PairUsernameGrade> graded_usernames = new ArrayList<>(); // username (student), grade

	public RegisteredProject(String name) {
		this.name = name;
	}

	public String getProjectName() {
		return name;
	}

	public void addUsernameAndGrade(String username,String grade) {
		graded_usernames.add(new PairUsernameGrade(username, grade));
	}

	public String getGradefromUsername(String username){
		for (PairUsernameGrade p:graded_usernames) {
			if(p.getUsername().equals(username))
				return p.getGrade();
		}
		return null;
	}
}
