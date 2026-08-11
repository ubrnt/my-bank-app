package ru.yandex.practicum.mybank.accounts.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

public class AdultValidator implements ConstraintValidator<Adult, LocalDate> {

	private int minimumAge;

	@Override
	public void initialize(Adult constraint) {
		minimumAge = constraint.value();
	}

	@Override
	public boolean isValid(LocalDate birthdate, ConstraintValidatorContext context) {
		if (birthdate == null) {
			return true;
		}

		return Period.between(birthdate, LocalDate.now()).getYears() >= minimumAge;
	}
}
