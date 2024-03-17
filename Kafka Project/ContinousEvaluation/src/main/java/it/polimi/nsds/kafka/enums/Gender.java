package it.polimi.nsds.kafka.enums;

public enum Gender {
	MALE("male"),
	FEMALE("female"),
	OTHER("other");

	private final String description;

	Gender(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	// retrieve Gender enum from its description
	public static Gender getGender(String description) {
		for (Gender gender : Gender.values()) {
			if (gender.getDescription().equalsIgnoreCase(description)) {
				return gender;
			}
		}
		throw new IllegalArgumentException("Gender not found!");
	}
}
