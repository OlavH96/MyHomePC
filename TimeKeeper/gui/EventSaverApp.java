/**
 * Created by Olav Husby on 01.07.2016.
 */

import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.LocalTimeStringConverter;

import javax.swing.*;
import java.io.File;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;

public class EventSaverApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private static ArrayList<Event> events;
    private static BorderPane border;
    private static Stage stage;

    public void start(Stage theStage) {

        stage = theStage;
        events = Manager.read();
        System.out.println(Manager.getJarFolder());

        if(events == null){
            //String path = JOptionPane.showInputDialog("Input path to storage-file:");
            events = External.noStorageFileFoundAlert(stage);
            //JOptionPane.showMessageDialog(null, Manager.getJarFolder());

            //events = External.loadNewEvents(stage);

            //events = Manager.read();

            if(events == null) {
                System.out.println("give up");
                System.exit(-1); // just give up
            }
        }
        
        if(events.size() == 0){
            events = Manager.getDefaultEvents();
        }

        border = new BorderPane();
        border.setPadding(new Insets(20));

        border.setCenter(tableOfEventsPane());
        //border.setRight(newEventPane());
        border.setLeft(eventDetailsPane(table.getSelectionModel().getSelectedItem()));
        // border.setBottom(tableButtonsPane());

        table.getSelectionModel().selectedIndexProperty().addListener((event)->{

            ObservableList<Event> eventObservableList = table.getSelectionModel().getSelectedItems();

            /*for (Event e : eventObservableList){
                System.out.println("Selected: "+e);
            }*/

            //System.out.println("rows selected: "+table.getSelectionModel().getSelectedItems().size());

            if(table.getSelectionModel().getSelectedIndices().size() == 1){
                border.setLeft(eventDetailsPane(table.getSelectionModel().getSelectedItem()));
               // stage.centerOnScreen();
                stage.sizeToScene();


            }
            else {
                border.setLeft(severalSelectedItemsPane(table.getSelectionModel().getSelectedItems().toArray(new Event[table.getSelectionModel().getSelectedItems().size()])));
                //stage.centerOnScreen();
                stage.sizeToScene();
            }
        });
        //table.setPrefSize(250,400);
        stage.setScene(new Scene(border));
        stage.setOnCloseRequest((event)->
            Manager.write(events)
        );
        stage.show();
    }
    private static TableView<Event> table;
    public static Pane tableOfEventsPane(){

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER); vBox.setPadding(new Insets(20)); vBox.setSpacing(20);
        vBox.setMaxWidth(500); vBox.setMinWidth(500);

        /*GridPane grid = new GridPane();
        grid.setVgap(20); grid.setHgap(20); */

        // Table
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // No dumb extra colomns
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setMinWidth(400); table.setMaxWidth(400);


        TableColumn<Event, LocalDate> date = new TableColumn<>("Date");
        date.setCellValueFactory( (data) -> new SimpleObjectProperty<>(data.getValue().getDate()) ); // "data" is a CellData object made my JavaFx containing the data in the cells

        TableColumn<Event, LocalTime> startTime = new TableColumn<>("Start");
        startTime.setCellValueFactory( (data) -> new SimpleObjectProperty<>(data.getValue().getStart())); // Need to use SimpleObjectProperty because javafx says so (-.-*)==\\7

        TableColumn<Event, LocalTime> endTime = new TableColumn<>("End");
        endTime.setCellValueFactory( (data) -> new SimpleObjectProperty<>(data.getValue().getEnd()));

        TableColumn<Event, String> desc = new TableColumn<>("Description");
        desc.setCellValueFactory( (data) -> new SimpleObjectProperty<>(data.getValue().getDescription()));

        table.getColumns().addAll(date, startTime, endTime, desc);

        table.getItems().addAll(events);
        //table.autosize();
        table.getSelectionModel().select(0);

        // Table

        Label label = new Label("Events");
        label.setFont(new Font("Consolas",20));
        label.setAlignment(Pos.CENTER);

        vBox.getChildren().addAll(label, table, tableButtonsPane());

        return vBox;
    }
    public static Pane newEventPane(){

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER); vBox.setSpacing(20); vBox.setPadding(new Insets(20));
        GridPane grid = new GridPane();

        grid.setVgap(20); grid.setHgap(20); grid.setPadding(new Insets(20)); grid.setAlignment(Pos.CENTER);

        TextField dateTextField = new TextField(LocalDate.now().toString());
            dateTextField.setTooltip(new Tooltip("Format is YYYY-MM-DD"));
        TextField startTextField = new TextField();
            startTextField.setTooltip(new Tooltip("Format is H, HH:MM, or H:MM | H = Hour(24-clock), M = Minute"));
        TextField endTextField = new TextField();
            endTextField.setTooltip(new Tooltip("Format is H, HH:MM, or H:MM | H = Hour(24-clock), M = Minute"));
        TextField descriptionTextField = new TextField();

        dateTextField.setOnKeyPressed((event -> {
            if(event.getCode() == KeyCode.ENTER) startTextField.requestFocus();
        }));
        startTextField.setOnKeyPressed((event -> {
            if(event.getCode() == KeyCode.ENTER) endTextField.requestFocus();
        }));
        endTextField.setOnKeyPressed((event -> {
            if(event.getCode() == KeyCode.ENTER) descriptionTextField.requestFocus();
        }));


        // Button
        Button addNewEventButton = new Button("Add");
        addNewEventButton.setAlignment(Pos.CENTER);

        addNewEventButton.setOnAction((e)->{

            try{
                String dateString = dateTextField.getText();
                String startString = startTextField.getText();
                String endString = endTextField.getText();
                String description = descriptionTextField.getText();

                LocalDate date = LocalDate.parse(dateString);
                LocalTime start;
                LocalTime end;

                Event event = new Event(date);
                event.setDescription(description);

                if(!startString.trim().equals("")) {
                    event.setStart(parse(startString));
                }

                if(!endString.trim().equals("")) {
                    event.setEnd(parse(endString));
                }

                System.out.println(event+" got added!");
                events.add(event);
                table.getItems().add(event);
                clearAll(startTextField, endTextField, descriptionTextField);
                dateTextField.setText(LocalDate.now().toString());
            }
            catch (DateTimeParseException ex){
                dateTextField.clear();
            }
        });
        // Button
        descriptionTextField.setOnKeyPressed((event -> {
            if(event.getCode() == KeyCode.ENTER) addNewEventButton.requestFocus();
        }));
        addNewEventButton.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER) addNewEventButton.fire();
        });

        grid.addRow(0, new Label("Date: "), dateTextField);
        grid.addRow(1, new Label("Start (Optional): "), startTextField);
        grid.addRow(2, new Label("End (Optional): "), endTextField);
        grid.addRow(3, new Label("Description (Optional): "), descriptionTextField);

        Label label = new Label("New Event");
        label.setFont(new Font("Consolas",20));

        vBox.getChildren().addAll(label, grid, addNewEventButton);

        return vBox;
    }
    public static Pane eventDetailsPane(Event event){

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER); vBox.setSpacing(20); vBox.setPadding(new Insets(20));

        GridPane grid = new GridPane();
        grid.setVgap(20); grid.setHgap(20); grid.setPadding(new Insets(20));

        if(event == null){
            event = new Event(LocalDate.now().plusDays(1));
            event.setDescription("ph'nglui mglw'nafh Cthulhu R'lyeh wgah'nagl fhtagn");
        }
        // date, start, end, desc, date created, duration?

        TextField date = new TextField(event.getDate().toString());
        date.setEditable(false);

        TextField start = new TextField(event.getStart().toString());
        start.setEditable(false);

        TextField end = new TextField(event.getEnd().toString());
        end.setEditable(false);

        Duration d = event.getDuration();
        TextField duration = new TextField(d.toHours()+" hours and "+((d.toMinutes())-(60*d.toHours()))+" minutes.");
        duration.setEditable(false);

        TextArea desc = new TextArea(event.getDescription());
        desc.setEditable(false);

        TextField dateCreated = new TextField(event.getDateAdded().toString());
        dateCreated.setEditable(false);

        TextField createdBy = new TextField(event.getAddedBy());
        createdBy.setEditable(false);

        TextField history = new TextField(event.getHistory());
        history.setEditable(false);

        grid.addRow(0, new Label("Date: "), date);
        grid.addRow(1, new Label("Start: "), start);
        grid.addRow(2, new Label("End: "), end);
        grid.addRow(3, new Label("Duration: "), duration);
        grid.addRow(4, new Label("Description"), desc);
        grid.addRow(5, new Label("Created: "), dateCreated);
        grid.addRow(6, new Label("By: "), createdBy);
        grid.addRow(7, new Label("History"), history); // For safety

        Label label = new Label("Details");
        label.setFont(new Font("Consolas",20));

        vBox.getChildren().addAll(label, grid);

        return vBox;


    }

    public static Pane tableButtonsPane(){

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER); //vBox.setPadding(new Insets(20));
        vBox.setPrefWidth(500);

        Label deleteLabel = new Label("Type \"delete\" to delete Event. This is final!");
        deleteLabel.setFont(new Font("Consolas",20));
        deleteLabel.setVisible(false);

        TextField deleteTextField = new TextField(); // Write "delete" to delete, security
        deleteTextField.setVisible(false);
        deleteTextField.setOnAction(event -> {

            if(deleteTextField.getText().equalsIgnoreCase("delete")){

                events.remove(table.getSelectionModel().getSelectedItem());
                table.getItems().remove(table.getSelectionModel().getSelectedItem());

            }
            deleteTextField.clear();
            deleteTextField.setVisible(false);
            deleteLabel.setVisible(false);
            table.requestFocus();
        });


        // Buttons
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER); hBox.setSpacing(20); hBox.setPadding(new Insets(20));

        Button newEventButton = new Button("New");
        newEventButton.setOnAction(event -> {
            border.setRight(newEventPane());
            stage.sizeToScene();
            //stage.centerOnScreen();
        });

        Button editButton = new Button("Edit");
        editButton.setOnAction(event -> {

            Event currentEvent = table.getSelectionModel().getSelectedItem();

            if(currentEvent != null) {
                border.setLeft(eventEditPane());
                stage.sizeToScene();
                //stage.centerOnScreen();
            }

        });

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction((event -> {

            deleteTextField.setVisible(true);
            deleteLabel.setVisible(true);
            deleteTextField.requestFocus();

        }));

        Button loadButton = new Button("Load from file");
        loadButton.setOnAction(event -> {

            ArrayList<Event> newEvents = External.loadNewEvents(stage);
            if(newEvents != null) {
                Manager.write(events);
                table.getItems().removeAll(events);
                table.getItems().addAll(newEvents);

                stage.sizeToScene();
                //stage.centerOnScreen();
            }
        });

        Button backupButton = new Button("Backup to file");
        backupButton.setOnAction(event -> {
            External.makeBackup(stage, events);
        });

        hBox.getChildren().addAll(newEventButton, editButton, deleteButton, loadButton, backupButton);
        // Buttons

        vBox.getChildren().addAll(hBox,deleteTextField, deleteLabel);
        return vBox;

    }
    public static Pane eventEditPane(){

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER); vBox.setSpacing(20); vBox.setPadding(new Insets(20));

        GridPane grid = new GridPane();
        grid.setVgap(20); grid.setHgap(20); grid.setPadding(new Insets(20)); grid.setAlignment(Pos.CENTER);

        Event currentEvent = table.getSelectionModel().getSelectedItem(); // Current item

        // date, start, end, desc, date created,

        String dateOrig = currentEvent.getDate().toString();
        TextField date = new TextField(dateOrig);

        String startOrig = currentEvent.getStart().toString();
        TextField start = new TextField(startOrig);

        String endOrig = currentEvent.getEnd().toString();
        TextField end = new TextField(endOrig);

        String descOrig = currentEvent.getDescription();
        TextArea desc = new TextArea(descOrig);

        grid.addRow(0, new Label("Date: "), date);
        grid.addRow(1, new Label("Start: "), start);
        grid.addRow(2, new Label("End: "), end);
        grid.addRow(3, new Label("Description"), desc);

        Label label = new Label("Edit Event");
        label.setFont(new Font("Consolas",20));

        Button acceptButton = new Button("Accept");
        acceptButton.setOnAction(event -> {

            String dateNew = date.getText();
            String startNew = start.getText();
            String endNew = end.getText();
            String descNew = desc.getText();

            if(!dateOrig.equals(dateNew)){
                try {
                    currentEvent.setDate(LocalDate.parse(dateNew));
                } catch (DateTimeParseException e){date.setText(dateOrig);}
            }
            if(!startOrig.equals(startNew)){
                currentEvent.setStart(parse(startNew));
            }
            if(!endOrig.equals(endNew)){
                currentEvent.setEnd(parse(endNew));
            }
            if(!descOrig.equals(descNew)){
                currentEvent.setDescription(descNew);
            }
            table.getItems().set(table.getSelectionModel().getSelectedIndex(), currentEvent);
            table.requestFocus();
            table.getSelectionModel().select(0);
            border.setLeft(eventDetailsPane(table.getSelectionModel().getSelectedItem()));
            stage.sizeToScene();
            //stage.centerOnScreen();
        });

        vBox.getChildren().addAll(label, grid, acceptButton);

        return vBox;

    }
    public static Pane severalSelectedItemsPane(Event[] events){

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER); vBox.setSpacing(20); vBox.setPadding(new Insets(20));
        vBox.setMaxWidth(500);

        Label label = new Label("Several Events");
        label.setFont(new Font("Consolas",20));

        GridPane eventsGrid = new GridPane();

        for(int i=0; i<events.length; i++){
            Event event = events[i];
            Duration d = event.getDuration();

            TextField date = new TextField(event.getDate().toString());
            date.setEditable(false);
            TextField time = new TextField(d.toHours()+" hours and "+((d.toMinutes())-(60*d.toHours()))+" minutes.");
            time.setEditable(false);
            //date.setPrefWidth(200);
            eventsGrid.addRow(i, date, time);
            //vBox.getChildren().add(new TextField(events[i].toString()));
        }


        //Duration total = events[1].getDuration();
        Duration d = Duration.ZERO;
        for(Event e : events){
            d = d.plus(e.getDuration());
        }

        TextField duration = new TextField(d.toHours()+" hours and "+((d.toMinutes())-(60*d.toHours()))+" minutes.");
        duration.setEditable(false);

        vBox.getChildren().addAll(label, eventsGrid, duration);

        return vBox;
    }

    private static void clearAll(TextInputControl ... items){

        for(TextInputControl item : items){
            item.clear();
        }
    }
    private static LocalTime parse(String time){

        try {
            if (time.contains(":")) {

                if(time.indexOf(":") == 1) time = "0"+time;

                return LocalTime.parse(time);
            }
            else {

                return LocalTime.of(Integer.parseInt(time),0);
            }
        }
        catch (DateTimeException | NumberFormatException ex){
            return LocalTime.MIN;
        }
    }
}