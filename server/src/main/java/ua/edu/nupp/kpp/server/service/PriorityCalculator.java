package ua.edu.nupp.kpp.server.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class PriorityCalculator {
    private PriorityCalculator() {}

    public record Score(int f1, int f2, int f3, double index) {}

    public static Score calculate(LocalDate due, LocalDate onDate, int criticality, int processes) {
        if (due == null || onDate == null || criticality < 0 || criticality > 100 || processes < 0)
            throw new IllegalArgumentException("Перевірте дати, критичність і кількість процесів");
        // За два дні до строку даємо 80 балів. У день строку і після нього — 100.
        long days = ChronoUnit.DAYS.between(onDate, due);
        int f1 = 100 - 10 * (int) Math.clamp(days, 0L, 10L);
        // П’ять залежних процесів уже дають усі 100 балів за цей фактор.
        int f3 = 20 * Math.min(processes, 5);
        // Ваги у відсотках: ділимо один раз, коли вже склали всі три частини.
        double index = (40 * f1 + 35 * criticality + 25 * f3) / 100.0;
        return new Score(f1, criticality, f3, index);
    }
}
