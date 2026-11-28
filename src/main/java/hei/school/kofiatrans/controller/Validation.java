package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.exception.ClientException;

import java.util.Arrays;

/** Small request checks shared by the controllers; failures become 400 responses. */
final class Validation {
    static final int MAX_ID_LENGTH = 20;

    private Validation() {
    }

    static String requireId(String id, String label) {
        if (id == null || id.isBlank() || id.length() > MAX_ID_LENGTH) {
            throw new ClientException("Identifiant de " + label + " invalide (1 à " + MAX_ID_LENGTH + " caractères)");
        }
        return id;
    }

    static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ClientException("Le champ '" + field + "' est obligatoire");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ClientException("Le champ '" + field + "' dépasse " + maxLength + " caractères");
        }
        return trimmed;
    }

    static <E extends Enum<E>> E requireEnum(String value, Class<E> type, String field) {
        if (value == null) {
            throw new ClientException("Le champ '" + field + "' est obligatoire");
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new ClientException("Valeur invalide pour '" + field + "' : " + value + " (attendu : "
                    + String.join(", ", Arrays.stream(type.getEnumConstants()).map(Enum::name).toList()) + ")");
        }
    }
}
