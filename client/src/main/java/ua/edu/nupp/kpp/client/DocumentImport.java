package ua.edu.nupp.kpp.client;

import javafx.concurrent.Task;
import ua.edu.nupp.kpp.client.model.Document;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.logging.Logger;

final class DocumentImport extends Task<String> {
    static final String HEADER = "registration_number;title;author_id;criticality_score;dependent_process_count";
    private static final Logger log = Logger.getLogger(DocumentImport.class.getName());
    private final Path file;
    private final ApiClient api;
    private int added;
    private int skipped;
    private String lastIssue = "";

    DocumentImport(Path file, ApiClient api) {
        this.file = file;
        this.api = api;
    }

    private Document parse(String row) {
        String[] fields = row.split(";", -1);
        if (fields.length != 5 || Arrays.stream(fields).anyMatch(String::isBlank))
            throw new IllegalArgumentException("потрібно п’ять заповнених полів");
        try {
            return new Document(0, fields[0].strip(), fields[1].strip(), Integer.parseInt(fields[2].strip()),
                    Integer.parseInt(fields[3].strip()), Integer.parseInt(fields[4].strip()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("ID автора, критичність і кількість процесів мають бути цілими числами");
        }
    }

    String summary() {
        return "Додано: " + added + ". Пропущено: " + skipped + "." + lastIssue;
    }

    @Override protected String call() throws Exception {
        log.info("Початок імпорту документів");
        String outcome = "Перервано";
        try {
            var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty() || !HEADER.equals(lines.getFirst().replaceFirst("^\\uFEFF", "")))
                throw new IOException("Відсутній або неправильний заголовок CSV");
            long total = lines.stream().skip(1).filter(row -> !row.isBlank()).count();
            long processed = 0;
            updateProgress(0, Math.max(1, total));
            for (int i = 1; i < lines.size(); i++) {
                if (isCancelled()) return summary();
                String row = lines.get(i);
                if (row.isBlank()) continue;
                try {
                    api.addDocument(parse(row));
                    // Рахуємо тільки підтверджені сервером вставки.
                    added++;
                } catch (IllegalArgumentException | ApiClient.RejectedRow ex) {
                    skipped++;
                    lastIssue = " Рядок " + (i + 1) + ": " + ex.getMessage();
                    log.warning(lastIssue.strip());
                } catch (IOException ex) {
                    throw new IOException("Рядок " + (i + 1) + ": немає підтвердження від сервера. Перевірте з’єднання та список документів.");
                }
                updateProgress(++processed, total);
                updateMessage("Опрацьовано " + processed + " із " + total);
            }
            updateProgress(1, 1);
            outcome = "Завершено";
            return summary();
        } catch (NoSuchFileException ex) {
            log.warning("Файл CSV не знайдено");
            throw new IOException("Файл CSV не знайдено");
        } catch (java.nio.charset.CharacterCodingException ex) {
            log.warning("Файл не закодовано в UTF-8");
            throw new IOException("CSV потрібно зберегти в UTF-8");
        } catch (FileSystemException ex) {
            log.warning("Неможливо прочитати CSV");
            throw new IOException("Перевірте доступ до CSV-файлу");
        } catch (IOException ex) {
            log.warning(ex.getMessage());
            throw ex;
        } finally {
            log.info(outcome + ". " + summary());
        }
    }
}
