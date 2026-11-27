package hei.school.kofiatrans.model;

import hei.school.kofiatrans.model.enums.Role;

public record AppUser(String username, String passwordHash, Role role) {
}
