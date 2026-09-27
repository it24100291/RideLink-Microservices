package com.example.accountservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @io.swagger.v3.oas.annotations.media.Schema(
            description = "Stored password hash. This field is never returned in API responses.",
            accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY
    )
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false)
    private long tokenVersion;

    @Version
    private long version;

    public long getTokenVersion() {
        return tokenVersion;
    }

    public void invalidateTokens() {
        tokenVersion++;
    }

    public Account() {
    }

    public Account(String name, String email, String password, Role role) {
        this(name, email, password, role, AccountStatus.ACTIVE);
    }

    public Account(String name, String email, String password, Role role, AccountStatus status) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status == null ? AccountStatus.ACTIVE : status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status == null ? AccountStatus.ACTIVE : status;
    }
}
