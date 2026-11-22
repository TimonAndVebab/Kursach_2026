package ua.edu.nupp.kpp.server.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class PriorityCalculatorTest {
    private final LocalDate today = LocalDate.of(2026,11,18);

    @Test void demoRequestScores75() {
        var result = PriorityCalculator.calculate(today.plusDays(2), today, 80, 3);
        assertEquals(new PriorityCalculator.Score(80,80,60,75), result);
    }
    @Test void overdueRequestGetsMaximumUrgency() {
        assertEquals(100, PriorityCalculator.calculate(today.minusDays(1), today, 0, 0).f1());
    }
    @Test void distantDeadlineGetsZeroUrgency() {
        assertEquals(0, PriorityCalculator.calculate(today.plusDays(12), today, 0, 0).f1());
    }
    @Test void fiveOrMoreProcessesGiveOneHundred() {
        assertEquals(100, PriorityCalculator.calculate(today, today, 100, 6).index());
    }
    @Test void fractionalResultIsPreserved() {
        assertEquals(0.35, PriorityCalculator.calculate(today.plusDays(10), today, 1, 0).index(), 0.00001);
    }
    @Test void criticalityAboveScaleIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.calculate(today,today,101,0));
    }
    @Test void negativeProcessCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.calculate(today,today,50,-1));
    }
}
