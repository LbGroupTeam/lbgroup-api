package br.simplipark;

import br.simplipark.util.FormatingUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class UtilTest {

    @Test
    void formatDate() {
        System.out.println(FormatingUtils.formatDate(LocalDateTime.now()));
    }
}