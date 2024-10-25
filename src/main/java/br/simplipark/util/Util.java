package br.simplipark.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Util {
    private Util() {
    }

    public static boolean isCpfInvalid(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return true;
        }

        String cpfNumbers = cpf.replaceAll("[^0-9]", "");

        return cpfNumbers.length() != 11;
    }

    public static long parseCpfToLong(String cpf) {
        if (isCpfInvalid(cpf)) {
            return 0;
        }

        return Long.parseLong(cpf.replaceAll("[^0-9]", ""));
    }

    public static boolean isNotInt(String str) {
        if (str == null || str.isBlank()) {
            return true;
        }

        try {
            Integer.parseInt(str);
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public static String serialize(Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error serializing object", e);

            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(String json, Class<T> outputType) {
        try {
            return new ObjectMapper().readValue(json, outputType);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing object", e);

            throw new RuntimeException(e);
        }
    }

    public static Runnable wrapRunnableWithTryCatch(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("An error occurred while executing the task", e);
            }
        };
    }
}
