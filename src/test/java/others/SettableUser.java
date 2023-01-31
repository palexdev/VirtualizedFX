package others;

import net.datafaker.Faker;

public class SettableUser {
	//================================================================================
	// Properties
	//================================================================================
	private String name;
	private String surname;
	private int age;

	//================================================================================
	// Constructors
	//================================================================================
	public SettableUser(String name, String surname, int age) {
		this.name = name;
		this.surname = surname;
		this.age = age;
	}

	public static SettableUser random() {
		Faker f = Faker.instance();
		return new SettableUser(
				f.name().firstName(),
				f.name().lastName(),
				f.number().numberBetween(18, 90)
		);
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
}
