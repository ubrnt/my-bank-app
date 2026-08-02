package ru.yandex.practicum.mybank.accounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

	@Column(nullable = false, updatable = false)
	private String login;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private LocalDate birthdate;

	protected Customer() {
	}

	public Customer(String login, String name, LocalDate birthdate) {
		this.login = login;
		this.name = name;
		this.birthdate = birthdate;
	}

	public String getLogin() {
		return login;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDate getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(LocalDate birthdate) {
		this.birthdate = birthdate;
	}
}
