package flappybirdneuroevolution;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 *
 * @author Preston Tang
 */
public class Main extends Application {

    // Arrays to keep track of birds & pipes, dead birds go to backup
    private ArrayList<PipeGroup> pipes = new ArrayList<>();
    private ArrayList<Bird> birds = new ArrayList<>();
    private ArrayList<Bird> birdsBackup = new ArrayList<>();

    // To prevent concurrent modification exception
    private ArrayList<Bird> toRemove = new ArrayList<>();

    private final Rectangle2D bounds = Screen.getPrimary().getBounds();

    private double sceneWidth, sceneHeight;
    private double gameWidth, gameHeight;
    private double uiWidth, uiHeight;

    private Pane gameRoot, uiRoot;

    // Default dimensions of each pipe
    private double pipeWidth = 50;
    private double pipeGapMin = 130;
    private double pipeGapMax = 130;

    private double gravity = 0.3;

    private int populationSize = 1000;

    private int score = 0;
    private int highscore = 0;
    private int generation = 1;

    private double mutationRate = 0.05;

    // How much of the screen the pipes moves every frame
    private double pipeUpdatePercentage = 0.002;

    // What percentage of the population will be chosen as parents
    // Deprecated
    private double parentPercentage = 0.1;

    private Bird bestBird;

    private Label info, highestScoreLabel;
    private JFXToggleButton trainingMode;

    @Override
    public void start(Stage stage) {
        // Setting up the various UI dimensions
        // I have the scene as the base, then game and ui
        // are drawn ontop of it
        sceneWidth = bounds.getWidth() * 0.85;
        sceneHeight = bounds.getHeight() * 0.8;

        gameWidth = sceneWidth;
        gameHeight = sceneHeight * 0.85;

        uiWidth = sceneWidth;
        uiHeight = sceneHeight * 0.15;

        Pane root = new Pane();
        Scene scene = new Scene(root, sceneWidth, sceneHeight);

        stage.setTitle("Solving Flappy Bird through Neuro-Evolution "
                + "(Neural Network with Genetic Algorithms) - "
                + "Inspired by Daniel Shiffman - "
                + "Created in June 2021");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();

        gameRoot = new Pane();
        gameRoot.setMaxSize(gameWidth, gameHeight);
        gameRoot.setPrefSize(gameWidth, gameHeight);

        uiRoot = new Pane();
        uiRoot.setTranslateY(sceneHeight * 0.85);
        uiRoot.setMaxSize(sceneWidth, sceneHeight * 0.15);
        uiRoot.setPrefSize(sceneWidth, sceneHeight * 0.15);

        // Everything below here till the game loop is just
        // setting up and initializing the UI elements
        Separator sep = new Separator();
        sep.setPrefWidth(sceneWidth);
        sep.setTranslateY(sceneHeight * 0.85);

        VBox base = new VBox(-20);
        base.setAlignment(Pos.CENTER_LEFT);

        HBox box = new HBox(uiWidth * 0.02);
        box.setAlignment(Pos.CENTER_LEFT);

        HBox box2 = new HBox(uiWidth * 0.02);
        box2.setAlignment(Pos.CENTER_LEFT);

        base.getChildren().addAll(box, box2);

        Slider speedControl = new Slider(0, 5, 1);
        speedControl.setPrefWidth(sceneWidth * 0.1);
        speedControl.setMajorTickUnit(10);
        speedControl.setMajorTickUnit(1);
        speedControl.setShowTickMarks(true);
        speedControl.setShowTickLabels(true);
        speedControl.setSnapToTicks(true);
        speedControl.setTranslateX(5);
        speedControl.valueProperty().addListener((obs, oldval, newVal)
                -> speedControl.setValue(Math.round(newVal.doubleValue())));

        JFXCheckBox maxSpeed = new JFXCheckBox();
        maxSpeed.setText("30x Speed");
        maxSpeed.setOnAction(event -> {
            if (maxSpeed.isSelected()) {
                speedControl.setDisable(true);
            } else {
                speedControl.setDisable(false);
            }
        });

        info = new Label();

        JFXToggleButton displayGame = new JFXToggleButton();
        displayGame.setText("Display Game");
        displayGame.setSelected(true);
        displayGame.setOnAction(event -> {
            if (displayGame.isSelected()) {
                if (!root.getChildren().contains(gameRoot)) {
                    root.getChildren().add(gameRoot);
                }
            } else {
                if (root.getChildren().contains(gameRoot)) {
                    root.getChildren().remove(gameRoot);
                }
            }
        });

        JFXButton nextGeneration = new JFXButton("Next Generation");
        nextGeneration.setStyle("-fx-border-color: black;");
        nextGeneration.setOnAction(event -> {
            resetAndRepopulate();
        });

        highestScoreLabel = new Label("Highest Score: ");

        JFXTextField setGapMin = new JFXTextField("" + pipeGapMin);
        setGapMin.setPromptText("Pipe Gap Min");
        setGapMin.setLabelFloat(true);

        JFXTextField setGapMax = new JFXTextField("" + pipeGapMax);
        setGapMax.setPromptText("Pipe Gap Max");
        setGapMax.setLabelFloat(true);

        trainingMode = new JFXToggleButton();
        trainingMode.setText("Training Mode");
        trainingMode.setSelected(true);
        trainingMode.setOnAction(event -> {
            if (trainingMode.isSelected()) {
                //Default training mode
                if (gameRoot.getChildren().contains(bestBird)) {
                    gameRoot.getChildren().remove(bestBird);
                }

                loadBackup();
            } else {
                //Best Bird mode
                gameRoot.getChildren().clear();
                pipes.clear();
                addStartingPipes();
                gameRoot.getChildren().add(bestBird);
                score = 0;

                bestBird.resetPosition();
                bestBird.setAlive(true);
            }
        });

        JFXButton save = new JFXButton("Save");
        save.setStyle("-fx-border-color: black;");
        save.setOnAction(event -> {
            if (setGapMin.getText().chars().allMatch(Character::isDigit)
                    && Double.parseDouble(setGapMin.getText()) <= gameHeight
                    && Double.parseDouble(setGapMin.getText()) <= pipeGapMax) {
                pipeGapMin = Double.parseDouble(setGapMin.getText());
            }

            if (setGapMax.getText().chars().allMatch(Character::isDigit)
                    && Double.parseDouble(setGapMax.getText()) <= gameHeight
                    && Double.parseDouble(setGapMax.getText()) >= pipeGapMin) {
                pipeGapMax = Double.parseDouble(setGapMax.getText());
            }
        });

        JFXButton resetBestBird = new JFXButton("Reset Best");
        resetBestBird.setStyle("-fx-border-color: black;");
        resetBestBird.setRipplerFill(Color.RED);
        resetBestBird.setOnAction(event -> {
            highscore = 0;
            bestBird = null;
            highestScoreLabel.setText("Highest Score: 0");
        });

        JFXButton saveBest = new JFXButton("Save Best Bird");
        saveBest.setStyle("-fx-border-color: black;");
        saveBest.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Best Bird");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Desktop"));
            fileChooser.setInitialFileName("bird.data");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try {
                    bestBird.getBrain().writeTo(file);
                } catch (Exception ex) {
                }
            }
        });

        JFXButton loadBest = new JFXButton("Load Bird");
        loadBest.setStyle("-fx-border-color: black;");
        loadBest.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Best Bird");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Desktop"));
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                bestBird = new Bird(mutationRate, 5.0, gameWidth, gameHeight);
                bestBird.setScore(9999999);
                bestBird.setWidth(20);
                bestBird.setHeight(20);
                bestBird.setFill(Color.rgb(0, 0, 0, 0.15));
                try {
                    bestBird.getBrain().readFrom(file);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        JFXButton derive = new JFXButton("Derive From Best");
        derive.setStyle("-fx-border-color: black;");
        derive.setOnAction(event -> {
            deriveFromBest();
        });

        box.getChildren().addAll(speedControl, maxSpeed, setGapMin, setGapMax, save, displayGame, nextGeneration, derive);
        box2.getChildren().addAll(trainingMode,
                saveBest, resetBestBird, loadBest, info, highestScoreLabel);

        uiRoot.getChildren().add(base);

        root.getChildren().addAll(gameRoot, sep, uiRoot);

        addStartingPipes();

        for (int i = 0; i < populationSize; i++) {
            Bird b = new Bird(mutationRate, 5.0, gameWidth, gameHeight);
            b.setWidth(20);
            b.setHeight(20);
            b.setFill(Color.rgb(0, 0, 0, 0.15));
            birds.add(b);
            gameRoot.getChildren().add(b);
        }

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // For loop is used to handle the game speed
                for (int a = 0;
                        a < (maxSpeed.isSelected() ? 31 : Math.round(speedControl.getValue()));
                        a++) {
                    if (!trainingMode.isSelected()) {
                        if (bestBird.isAlive()) {
                            score++;

                            // I don't call this every frame to save memory
                            if (score % 10 == 0) {
                                updateStatus();
                            }

                            pipeManagement();

                            bestBird.update(gravity);
                            bestBird.setScore(score);

                            // Dies when out of bounds or hits pipe
                            if (bestBird.getTranslateY() < 0 || bestBird.getTranslateY() + bestBird.getHeight() > gameHeight
                                    || isBirdInPipeGroup(bestBird, getClosestPipe(bestBird))) {
                                bestBird.setAlive(false);
                                gameRoot.getChildren().remove(bestBird);
                            }

                            // Move
                            bestBird.action(getClosestPipe(bestBird));

                        } else {
                            gameRoot.getChildren().remove(bestBird);
                            loadBackup();
                            trainingMode.setSelected(true);

                            // Temporary Bug Fix
                            // For some reason after it dies the loaded
                            // bird gets deleted
                            // Probably applies to birds that aren't loaded but
                            // generated in the current instance as well
                            bestBird.setScore(999999);
                        }
                    } else {
                        // If there are still birds
                        if (!birds.isEmpty()) {
                            score++;

                            if (score % 10 == 0) {
                                updateStatus();
                                newBestBird();
                            }
                            pipeManagement();

                            // Loop through birds
                            for (int i = 0; i < birds.size(); i++) {
                                Bird b = birds.get(i);

                                if (b.isAlive()) {
                                    b.update(gravity);
                                    b.setScore(score);

                                    // Dies when out of bounds or hits pipe
                                    if (b.getTranslateY() < 0 || b.getTranslateY() + b.getHeight() > gameHeight
                                            || isBirdInPipeGroup(b, getClosestPipe(b))) {
                                        b.setAlive(false);
                                        gameRoot.getChildren().remove(b);
                                        toRemove.add(b);
                                    }

                                    // Move
                                    b.action(getClosestPipe(b));
                                }
                            }

                            // Remove birds
                            for (Bird b : toRemove) {
                                birds.remove(b);
                                birdsBackup.add(b);
                            }
                            toRemove.clear();

                        } else {
                            // They've all died, time for new gen
                            resetAndRepopulate();
                        }
                    }
                }
            }
        };
        timer.start();
    }

    // Finds a new best bird if there is one (comparison of score)
    private void newBestBird() {
        if (bestBird == null) {
            bestBird = birds.size() != 1 ? birds.get(
                    ThreadLocalRandom.current().nextInt(0,
                            birds.size() - 1)) : birds.get(0);
        } else {
            if (score > bestBird.getScore()) {
                bestBird = birds.size() != 1 ? birds.get(
                        ThreadLocalRandom.current().nextInt(0,
                                birds.size() - 1)) : birds.get(0);
            }
        }
    }

    // Updates some UI elements
    private void updateStatus() {
        highscore = score > highscore ? score : highscore;

        highestScoreLabel.setText("Highest Score: " + highscore);

        info.setText(String.format("%s %10s %12s %14s %12s",
                "Pipes: " + pipes.size(),
                " | Birds: " + birds.size(),
                " | Backup: " + birdsBackup.size(),
                " | Score: " + score,
                " | Generation: " + generation));
    }

    // Handles pipe display
    private void pipeManagement() {
        // Add pipes
        if (gameWidth - pipes.get(pipes.size() - 1).getTranslateX() > gameWidth / 4) {
            PipeGroup p = new PipeGroup(gameWidth * pipeUpdatePercentage,
                    gameWidth, gameHeight,
                    pipeGapMin == pipeGapMax ? pipeGapMin
                            : ThreadLocalRandom.current().nextDouble(pipeGapMin,
                                    pipeGapMax), pipeWidth);
            gameRoot.getChildren().add(p);
            pipes.add(p);
        }

        // Remove pipes when out of screen
        for (Iterator<PipeGroup> iterator = pipes.iterator(); iterator.hasNext();) {
            PipeGroup p = iterator.next();

            if (p.getTranslateX() + pipeWidth < 0) {
                iterator.remove();
                gameRoot.getChildren().remove(p);
            }

            // Move the pipes to the left of the screen
            p.update();
        }
    }

    // Used when switching between best bird mode and training mode
    private void loadBackup() {
        gameRoot.getChildren().clear();
        pipes.clear();

        //Create the first 2 pipes
        addStartingPipes();

        birds.addAll(birdsBackup);

        for (Bird b : birds) {
            b.setAlive(true);
            b.setScore(0);
            gameRoot.getChildren().add(b);
        }

        birdsBackup.clear();
    }

    // Creates the next generation as clones of the current saved best bird
    private void deriveFromBest() {
        trainingMode.setSelected(true);

        birds.clear();
        birdsBackup.clear();

        bestBird.setWidth(20);
        bestBird.setHeight(20);
        bestBird.setFill(Color.rgb(0, 0, 0, 0.15));

        birdsBackup.add(bestBird);

        bestBird.resetPosition();

        for (int i = 0; i < populationSize - 1; i++) {
            Bird b = new Bird(bestBird.getBrain(), mutationRate, 5.0, gameWidth, gameHeight);
            b.setWidth(20);
            b.setHeight(20);
            b.setFill(Color.rgb(0, 0, 0, 0.15));
            birdsBackup.add(b);
        }

        loadBackup();
    }

    private void resetAndRepopulate() {
        gameRoot.getChildren().clear();
        pipes.clear();

        // Create the first 2 pipes
        addStartingPipes();

        birdsBackup.addAll(birds);
        birds.clear();
        generation++;
        // If it passed the first 2 pipes
        if (score > 270) {
            // Set fitness values to between 0 and 1
            normalizeFitness();

            // Generate next generation
            generate();

            // Draw new generation
            for (Bird b : birds) {
                b.setAlive(true);
                b.setWidth(20);
                b.setHeight(20);
                b.setFill(Color.rgb(0, 0, 0, 0.15));
                gameRoot.getChildren().add(b);
            }
            birdsBackup.clear();

            // If it doesn't even pass the first 2 pipes just wipe everything and restart
        } else {
            generation = 1;

            for (int i = 0; i < populationSize; i++) {
                Bird b = new Bird(mutationRate, 5.0, gameWidth, gameHeight);
                b.setWidth(20);
                b.setHeight(20);
                b.setFill(Color.rgb(0, 0, 0, 0.15));
                birds.add(b);
                gameRoot.getChildren().add(b);
            }

            birdsBackup.clear();
        }

        score = 0;
    }

//    private void generate() {
//        Bird[] parents = new Bird[(int) (populationSize * parentPercentage)];
//
//        for (int i = 0; i < parents.length; i++) {
//            parents[i] = poolSelection();
//        }
//
//        for (int i = 0; i < (int) parents.length; i += 2) {
//            for (int a = 0;
//                    a < ((populationSize) / (parents.length / 2));
//                    a++) {
//                birds.add(parents[i].mate(parents[i + 1]));
//            }
//        }
//    }
    
    private void generate() {
        // They won't be similar cause I remove them in poolSelection()
        Bird parent1 = poolSelection();
        Bird parent2 = poolSelection();

        for (int i = 0; i < populationSize; i++) {
            birds.add(parent1.mate(parent2));
        }

//        birds.add(parent1);
//        birds.add(parent2);
    }

    // From coding train, translated to Java
    private void normalizeFitness() {
        // Make fitness score exponentially better
        for (int i = 0; i < birdsBackup.size(); i++) {
            birdsBackup.get(i).setFitness(Math.pow(birdsBackup.get(i).getScore(), 2));
        }

        // Add up all the fitness scores
        double sum = 0;
        for (int i = 0; i < birdsBackup.size(); i++) {
            sum += birdsBackup.get(i).getFitness();
        }
        // Divide by the sum
        for (int i = 0; i < birdsBackup.size(); i++) {
            birdsBackup.get(i).setFitness(birdsBackup.get(i).getFitness() / sum);
        }
    }

    // From coding train, translated to Java
    // Picks 1 bird from an arraylist based on fitness
    private Bird poolSelection() {
        // Start at 0
        int index = 0;

        // Pick a random number between 0 and 1
        double r = Math.random();

        // Keep subtracting probabilities until you get less than zero
        // Higher probabilities will be more likely to be fixed since they will
        // subtract a larger number towards zero
        while (r > 0) {
            r -= birdsBackup.get(index).getFitness();
            // And move on to the next
            index += 1;
        }

        // Go back one
        index -= 1;

        // Make sure it's a copy!
        // (this includes mutation)
        Bird result = birdsBackup.get(index);

//        birdsBackup.remove(result);
        return result;
    }

    // Handles collision detection
    private boolean isBirdInPipeGroup(Bird b, PipeGroup p) {
        boolean result = false;

        Rectangle top = p.getTopPipe();
        Rectangle bottom = p.getBottomPipe();

        //If within the x region
        //Check if in top or bottom pipe
        if (b.getBoundsInParent().intersects(p.getTranslateX(), p.getTranslateY(),
                top.getWidth(), top.getHeight())
                || b.getBoundsInParent().intersects(p.getTranslateX(),
                        top.getHeight() + p.getGap(),
                        bottom.getWidth(), bottom.getHeight())) {
            result = true;
        }

        return result;
    }

    private void addStartingPipes() {
        PipeGroup pipe = new PipeGroup(gameWidth * pipeUpdatePercentage, gameWidth, gameHeight, pipeGapMin == pipeGapMax ? pipeGapMin
                : ThreadLocalRandom.current().nextDouble(pipeGapMin,
                        pipeGapMax), pipeWidth);
        gameRoot.getChildren().add(pipe);
        pipes.add(pipe);
        pipe.setTranslateX(gameWidth / 2.0);

        PipeGroup pipe1 = new PipeGroup(gameWidth * pipeUpdatePercentage, gameWidth, gameHeight, pipeGapMin == pipeGapMax ? pipeGapMin
                : ThreadLocalRandom.current().nextDouble(pipeGapMin,
                        pipeGapMax), pipeWidth);
        gameRoot.getChildren().add(pipe1);
        pipes.add(pipe1);
        pipe1.setTranslateX(gameWidth - (gameWidth / 4.0));
    }

    private PipeGroup getClosestPipe(Bird b) {
        PipeGroup closest = null;

        double dist = Double.MAX_VALUE;

        for (PipeGroup p : pipes) {
            if (p.getTranslateX() + p.getWidth() > b.getTranslateX()
                    && p.getTranslateX() - b.getTranslateX() < dist) {
                closest = p;
                dist = p.getTranslateX() - b.getTranslateX();
            }
        }

        return closest;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
