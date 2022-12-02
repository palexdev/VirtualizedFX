/*
 * Copyright (C) 2022 Parisi Alessandro
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX).
 *
 * VirtualizedFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package interactive.model;

import io.github.palexdev.mfxcore.utils.fx.FXCollectors;
import javafx.collections.ObservableList;
import net.datafaker.Faker;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public record User(int id, String name, int age, double height, String gender, String nationality, String cellPhone,
                   String university) {
	public static final Comparator<User> ID_COMPARATOR = Comparator.comparingInt(o -> o.id);
	public static final Comparator<User> NAME_COMPARATOR = Comparator.comparing(o -> o.name);
	public static final Comparator<User> AGE_COMPARATOR = Comparator.comparing(o -> o.age);
	public static final Comparator<User> HEIGHT_COMPARATOR = Comparator.comparing(o -> o.height);
	public static final Comparator<User> GENDER_COMPARATOR = Comparator.comparing(o -> o.gender);
	public static final Comparator<User> PHONE_COMPARATOR = Comparator.comparing(o -> o.cellPhone);
	public static final Comparator<User> NATION_COMPARATOR = Comparator.comparing(o -> o.nationality);
	public static final Comparator<User> UNI_COMPARATOR = Comparator.comparing(o -> o.university);

	public static User rand() {
		Faker fk = Faker.instance();
		int id = (int) fk.number().randomNumber(6, true);
		String name = fk.funnyName().name();
		int age = fk.number().numberBetween(18, 70);
		double height = fk.number().randomDouble(2, 155, 190);
		String gender = fk.gender().shortBinaryTypes();
		String nationality = fk.nation().nationality();
		String cellPhone = fk.phoneNumber().cellPhone();
		String university = fk.university().name();
		return new User(id, name, age, height, gender, nationality, cellPhone, university);
	}

	public static List<User> randList(int num) {
		return IntStream.range(0, num)
				.mapToObj(i -> User.rand())
				.toList();
	}

	public static ObservableList<User> randObsList(int num) {
		return IntStream.range(0, num)
				.mapToObj(i -> User.rand())
				.collect(FXCollectors.toList());
	}

	public static List<Comparator<User>> comparators() {
		return List.of(
				ID_COMPARATOR, NAME_COMPARATOR, AGE_COMPARATOR,
				HEIGHT_COMPARATOR, GENDER_COMPARATOR, NATION_COMPARATOR,
				PHONE_COMPARATOR, UNI_COMPARATOR
		);
	}
}
