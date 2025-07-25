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

import java.util.stream.IntStream;

import io.github.palexdev.mfxcore.utils.fx.FXCollectors;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import net.datafaker.providers.base.Name;

import static model.User.faker;

public class FXUser {
    private final StringProperty firstName = new SimpleStringProperty();
    private final StringProperty lastName = new SimpleStringProperty();
    private final IntegerProperty birthYear = new SimpleIntegerProperty();
    private final StringProperty zodiac = new SimpleStringProperty();
    private final StringProperty country = new SimpleStringProperty();
    private final StringProperty blood = new SimpleStringProperty();
    private final StringProperty pet = new SimpleStringProperty();

    public FXUser() {
        Name name = faker.name();
        setFirstName(name.firstName());
        setLastName(name.lastName());
        setBirthYear(faker.number().numberBetween(1930, 2024));
        setZodiac(faker.zodiac().sign());
        setCountry(faker.country().name());
        setBlood(faker.bloodtype().bloodGroup());
        setPet(faker.animal().name());
    }

    public FXUser(String firstName, String lastName, int birthYear) {
        setFirstName(firstName);
        setLastName(lastName);
        setBirthYear(birthYear);
        setZodiac("");
        setCountry("");
        setBlood("");
        setPet("");
    }

    public static ObservableList<FXUser> fxusers(int cnt) {
        return IntStream.range(0, cnt)
            .mapToObj(i -> new FXUser())
            .collect(FXCollectors.toList());
    }

    public String getFirstName() {
        return firstName.get();
    }

    public StringProperty firstNameProperty() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName.set(firstName);
    }

    public String getLastName() {
        return lastName.get();
    }

    public StringProperty lastNameProperty() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName.set(lastName);
    }

    public int getBirthYear() {
        return birthYear.get();
    }

    public IntegerProperty birthYearProperty() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear.set(birthYear);
    }

    public String getZodiac() {
        return zodiac.get();
    }

    public StringProperty zodiacProperty() {
        return zodiac;
    }

    public void setZodiac(String zodiac) {
        this.zodiac.set(zodiac);
    }

    public String getCountry() {
        return country.get();
    }

    public StringProperty countryProperty() {
        return country;
    }

    public void setCountry(String country) {
        this.country.set(country);
    }

    public String getBlood() {
        return blood.get();
    }

    public StringProperty bloodProperty() {
        return blood;
    }

    public void setBlood(String blood) {
        this.blood.set(blood);
    }

    public String getPet() {
        return pet.get();
    }

    public StringProperty petProperty() {
        return pet;
    }

    public void setPet(String pet) {
        this.pet.set(pet);
    }
}
