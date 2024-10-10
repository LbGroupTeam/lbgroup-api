package br.simplipark.test;

import br.simplipark.user.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class TestUtils {

    private TestUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static User createSampleUser() {
        return new User(1, "56788269050", "VIP");
    }

    public static String readFileFromResources(String fileName) throws IOException {
        ClassLoader classLoader = TestUtils.class.getClassLoader();
        Path path = Paths.get(Objects.requireNonNull(classLoader.getResource(fileName)).getPath());
        return Files.readString(path);
    }
}