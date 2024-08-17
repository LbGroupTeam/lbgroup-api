package br.simplipark.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class FormatingUtils {
    private FormatingUtils() {
    }

    public static String roundToTwoDecimals(double value) {
        return String.format("%.2f", value);
    }

    public static String formatDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).localizedBy(Locale.forLanguageTag("pt-BR")));
    }
}
