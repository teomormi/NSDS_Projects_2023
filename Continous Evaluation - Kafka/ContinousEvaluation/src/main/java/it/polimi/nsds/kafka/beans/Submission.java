package it.polimi.nsds.kafka.beans;

import java.io.Serializable;

public class Submission implements Serializable {
	private final String file_path;
	private int grade = 0;

	public Submission(String file_path){
		this.file_path = file_path;
	}

	public boolean setGrade(int grade){
		if(grade > 0){
			this.grade = grade;
			return true;
		}
		return false;
	}

	public int getGrade(){
		return grade;
	}

	public String getFile_path(){
		return file_path;
	}
}
