/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package model;

import io.github.palexdev.mfxcore.utils.fx.FXCollectors;
import javafx.collections.ObservableList;
import net.datafaker.Faker;
import net.datafaker.providers.base.Name;

import java.util.Objects;
import java.util.stream.IntStream;

public class User {
	private static int COUNT = 0;
	public static final Faker faker = new Faker();
	private final int id;
	private String firstName;
	private String lastName;
	private int birthYear;
	private String zodiac;
	private String country;
	private String blood;
	private String pet;

	public User() {
		Name name = faker.name();
		this.id = COUNT++;
		this.firstName = name.firstName();
		this.lastName = name.lastName();
		this.birthYear = faker.number().numberBetween(1930, 2024);
		this.zodiac = faker.zodiac().sign();
		this.country = faker.country().name();
		this.blood = faker.bloodtype().bloodGroup();
		this.pet = faker.animal().name();
	}

	public User(String firstName, String lastName, int birthYear) {
		this.id = COUNT++;
		this.firstName = firstName;
		this.lastName = lastName;
		this.birthYear = birthYear;
		this.zodiac = "";
		this.country = "";
		this.blood = "";
		this.pet = "";
	}

	public static ObservableList<User> users(int cnt) {
		return IntStream.range(0, cnt)
			.mapToObj(i -> new User())
			.collect(FXCollectors.toList());
	}

	public int id() {
		return id;
	}

	public String firstName() {return firstName;}

	public String lastName() {return lastName;}

	public int birthYear() {return birthYear;}

	public String zodiac() {
		return zodiac;
	}

	public String country() {
		return country;
	}

	public String blood() {
		return blood;
	}

	public String pet() {
		return pet;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setBirthYear(int birthYear) {
		this.birthYear = birthYear;
	}

	public void setZodiac(String zodiac) {
		this.zodiac = zodiac;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public void setBlood(String blood) {
		this.blood = blood;
	}

	public void setPet(String pet) {
		this.pet = pet;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (User) obj;
		return Objects.equals(this.firstName, that.firstName) &&
			Objects.equals(this.lastName, that.lastName) &&
			this.birthYear == that.birthYear;
	}

	@Override
	public int hashCode() {
		return Objects.hash(firstName, lastName, birthYear);
	}

	@Override
	public String toString() {
		return "User[" +
			"firstName=" + firstName + ", " +
			"lastName=" + lastName + ", " +
			"birthYear=" + birthYear + ']';
	}
}
