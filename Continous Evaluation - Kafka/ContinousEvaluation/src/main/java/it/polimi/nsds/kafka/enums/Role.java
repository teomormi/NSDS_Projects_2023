package it.polimi.nsds.kafka.enums;

import java.io.Serializable;

public enum Role implements Serializable {
	STUDENT("student"),
	PROFESSOR("professor"),
	ADMIN("admin");

	private final String description;

	Role(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	// retrieve Role enum from description
	public static Role getRole(String description) {
		for (Role role : Role.values()) {
			if (role.getDescription().equalsIgnoreCase(description)) {
				return role;
			}
		}
		throw new IllegalArgumentException("Role not found!");
	}
}

