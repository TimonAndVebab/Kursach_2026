package ua.edu.nupp.kpp.client;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import ua.edu.nupp.kpp.client.model.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class App extends Application {
    private final ApiClient api = new ApiClient();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ExecutorService importer = Executors.newSingleThreadExecutor();
    private final TabPane tabs = new TabPane();
    private final Label message = new Label("Готово");
    private final Map<String,String> states = Map.of("DRAFT","Чернетка","SUBMITTED","Подано",
            "IN_REVIEW","Розгляд","APPROVED","Схвалено");

    public void start(Stage stage) {
        tab("Автори","authors",Author.class, "fullName","ПІБ автора");
        tab("Документи","documents",Document.class, "registrationNumber","Реєстраційний номер","title","Назва",
                "authorId","ID автора","criticalityScore","Критичність (0–100)","dependentProcessCount","Кількість процесів");
        tab("Рецензенти","reviewers",Reviewer.class,"fullName","ПІБ рецензента");
        tab("Запити","requests",ApprovalRequest.class,"documentId","ID документа","reviewerId","ID рецензента",
                "createdOn","Дата створення","dueOn","Строк погодження","status","Стан");
        Label title = new Label("Погодження внутрішніх документів");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        message.setWrapText(true);
        VBox root = new VBox(12, title, tabs, message);
        root.setPadding(new Insets(18)); VBox.setVgrow(tabs,Priority.ALWAYS);
        stage.setTitle("Погодження документів"); stage.setScene(new Scene(root,1080,690)); stage.show();
    }

    private <T> void perform(Callable<T> work, Consumer<T> done) {
        tabs.setDisable(true);
        message.setText("Виконується запит…");
        var task = new javafx.concurrent.Task<T>() {
            protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(e -> { tabs.setDisable(false); done.accept(task.getValue()); });
        task.setOnFailed(e -> {
            tabs.setDisable(false);
            var cause = task.getException();
            if (cause instanceof java.net.ConnectException) message.setText("Сервер недоступний. Перевірте запуск Tomcat.");
            else message.setText("Помилка: " + cause.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, message.getText(), ButtonType.OK);
            alert.initOwner(tabs.getScene().getWindow());
            alert.setHeaderText("Операцію не виконано");
            alert.show();
        });
        worker.submit(task);
    }

    private Button button(String label, Runnable click) {
        Button b = new Button(label);
        b.setOnAction(e -> {
            try { click.run(); }
            catch (IllegalArgumentException ex) { message.setText(ex.getMessage()); }
        });
        return b;
    }

    private <T> void tab(String name, String route, Class<T> type, String... pairs) {
        TableView<T> table = new TableView<>();
        table.setId(route+"Table");
        var fields = new LinkedHashMap<String,TextField>();
        GridPane form = new GridPane(); form.setHgap(12); form.setVgap(6);
        var labels = new LinkedHashMap<String,String>(); labels.put("id","ID");
        for (int i=0; i<pairs.length; i+=2) {
            String key=pairs[i], label=pairs[i+1]; labels.put(key,label);
            TextField input=new TextField(); input.setId(route+"-"+key); fields.put(key,input);
            if (key.equals("status")) { input.setEditable(false); input.setText("Чернетка"); }
            if (key.endsWith("On")) input.setPromptText("РРРР-ММ-ДД");
            form.add(new VBox(3,new Label(label),input),i/2%3,i/6);
        }
        labels.forEach((key,label) -> {
            TableColumn<T,String> column=new TableColumn<>(label);
            column.setCellValueFactory(cell -> {
                var value=api.json.valueToTree(cell.getValue()).path(key).asText();
                return new ReadOnlyStringWrapper(key.equals("status") ? states.getOrDefault(value,value) : value);
            });
            table.getColumns().add(column);
        });
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().selectedItemProperty().addListener((o,old,row) -> {
            if (row == null) return;
            var values=api.json.valueToTree(row);
            fields.forEach((key,input) -> {
                String value=values.path(key).asText();
                input.setText(key.equals("status") ? states.getOrDefault(value,value) : value);
            });
        });
        // Після запису заново читаємо таблицю: ID і фактичний стан повертає сервер.
        Consumer<String> refresh = note -> perform(() -> api.list(route,type), rows -> {
            table.getItems().setAll(rows); message.setText(note+" · Записів: "+rows.size());
        });
        Consumer<String> write = action -> {
            Map<String,String> data=new LinkedHashMap<>(); data.put("action",action);
            T row=table.getSelectionModel().getSelectedItem();
            if (!action.equals("add")) {
                if (row==null) throw new IllegalArgumentException("Спочатку виберіть рядок таблиці");
                data.put("id",api.json.valueToTree(row).path("id").asText());
            }
            if (!action.equals("delete")) fields.forEach((key,input) -> {
                if (key.equals("status")) data.put(key,action.equals("add") ? "DRAFT" : api.json.valueToTree(row).path(key).asText());
                else data.put(key,input.getText());
            });
            perform(() -> { api.post(route,data); return "Зміни збережено"; },refresh);
        };
        FlowPane buttons=new FlowPane(8,6,button("Додати",()->write.accept("add")),
                button("Зберегти зміни",()->write.accept("update")),button("Видалити",()->write.accept("delete")),
                button("Оновити",()->refresh.accept("Оновлено")));
        VBox box=new VBox(10,table,form,buttons); box.setPadding(new Insets(12,0,0,0)); VBox.setVgrow(table,Priority.ALWAYS);
        if (route.equals("documents")) {
            TextField path = new TextField(Path.of("sample-data","documents.csv").toString());
            path.setPrefColumnCount(24);
            ProgressBar progress = new ProgressBar(0);
            Label status = new Label("Виберіть файл або залиште запропонований шлях");
            status.setWrapText(true);
            Button start = new Button("Імпорт CSV");
            start.setOnAction(event -> {
                Path source;
                try { source = Path.of(path.getText()); }
                catch (java.nio.file.InvalidPathException ex) { status.setText("Некоректний шлях до CSV"); return; }
                DocumentImport task = new DocumentImport(source, api);
                start.setDisable(true);
                progress.progressProperty().bind(task.progressProperty());
                status.textProperty().bind(task.messageProperty());
                Consumer<String> finish = result -> {
                    progress.progressProperty().unbind();
                    status.textProperty().unbind();
                    status.setText(result);
                    start.setDisable(false);
                };
                task.setOnSucceeded(e -> {
                    finish.accept(task.getValue());
                    refresh.accept("Список документів оновлено");
                });
                task.setOnFailed(e -> {
                    finish.accept("Імпорт зупинено. " + task.getException().getMessage() + " " + task.summary());
                    // Частина рядків могла зберегтися до обриву зв’язку.
                    refresh.accept("Список документів оновлено");
                });
                task.setOnCancelled(e -> finish.accept("Імпорт скасовано"));
                importer.submit(task);
            });
            box.getChildren().addAll(new FlowPane(8,6,new Label("CSV-файл"),path,start,progress),status);
        }
        if (route.equals("requests")) {
            ComboBox<String> next=new ComboBox<>();
            next.getItems().addAll("Чернетка","Подано","Розгляд","Схвалено"); next.setValue("Подано");
            DatePicker day=new DatePicker(LocalDate.now()); day.setEditable(false);
            box.getChildren().add(new FlowPane(8,6,new Label("Перехід у стан"),next,button("Змінити стан",()->{
                int id=selectedId(table);
                String state=states.entrySet().stream().filter(e->e.getValue().equals(next.getValue())).findFirst().orElseThrow().getKey();
                perform(()->{api.post(route,Map.of("action","transition","id",String.valueOf(id),"state",state));return "Стан змінено";},refresh);
            }),new Label("Дата оцінки"),day,button("Розрахувати пріоритет",()->{
                int id=selectedId(table);
                if(day.getValue()==null) throw new IllegalArgumentException("Виберіть дату оцінювання");
                String onDate=day.getValue().toString();
                perform(()->api.priority(id,onDate),s->message.setText("Запит №"+id+"   ·   F1 = "+s.f1()
                        +"   F2 = "+s.f2()+"   F3 = "+s.f3()+"   ·   Пріоритет: "+s.index()));
            })));
        }
        Tab tab=new Tab(name,box);tab.setClosable(false);
        tab.setOnSelectionChanged(e->{if(tab.isSelected())refresh.accept(name);});
        tabs.getTabs().add(tab);
    }

    private int selectedId(TableView<?> table) {
        Object row=table.getSelectionModel().getSelectedItem();
        if(row==null) throw new IllegalArgumentException("Виберіть запит у таблиці");
        return api.json.valueToTree(row).path("id").asInt();
    }

    public void stop(){ worker.shutdownNow(); importer.shutdownNow(); }
    public static void main(String[] args){ launch(args); }
}
