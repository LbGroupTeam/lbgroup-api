package br.simplipark.test;

import br.simplipark.user.User;

public class TestUtils {

    private TestUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static User createSampleUser() {
        return new User(1, "56788269050", "VIP");
    }
}
