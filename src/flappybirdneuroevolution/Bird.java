package flappybirdneuroevolution;

import flappybirdneuroevolution.NeuralNetwork.ActivationFunction;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author Preston Tang
 * 
 * Represents the birds in the simulation
 * 
 */
public class Bird extends Rectangle {

    private double yVelocity;
    private double jumpStrength;
    private boolean alive;
    private NeuralNetwork brain;
    private double gameWidth, gameHeight;
    private double score;
    private double fitness;
    private double mutationRate;

    // Initializes the bird with a new brain
    public Bird(double mutationRate, double jumpStrength, double gameWidth, double gameHeight) {
        this.jumpStrength = jumpStrength;
        yVelocity = 0.0;
        alive = true;
        score = 0;
        fitness = 0;
        this.mutationRate = mutationRate;
        super.setTranslateY(gameHeight / 2);
        super.setTranslateX(gameWidth * 0.05);

        brain = new NeuralNetwork(4, 3, 2);
        brain.setActivationFunction(new ActivationFunction(Mat.SIGMOID, Mat.SIGMOID_DERIVATIVE));
        brain.randomizeBoth();

        this.gameWidth = gameWidth;
        this.gameHeight = gameHeight;
    }

    // Initializes the bird with a mutated copy of the old brain
    public Bird(NeuralNetwork brain, double mutationRate, double jumpStrength, double gameWidth, double gameHeight) {
        this.brain = brain.clone();
        this.mutationRate = mutationRate;

        // Mutation
        this.brain.mutateBiases(mutationRate);
        this.brain.mutateWeights(mutationRate);

        this.jumpStrength = jumpStrength;
        yVelocity = 0.0;
        alive = true;
        score = 0;
        fitness = 0;
        super.setTranslateY(gameHeight / 2);
        super.setTranslateX(gameWidth * 0.05);

        this.gameWidth = gameWidth;
        this.gameHeight = gameHeight;
    }

    public void jump() {
        yVelocity = 0;
        yVelocity -= jumpStrength;
    }

    // These are values I find tuned through trial and error
    public void update(double gravity) {
        yVelocity += gravity;

        // Max speed going down
        if (yVelocity > 5) {
            yVelocity = 5;
        }

        // Max speed going up
        if (yVelocity < -12.5) {
            yVelocity = -12.5;
        }

        this.setTranslateY(this.getTranslateY() + yVelocity);
    }

    // Could technically be done with just 1 output neuron
    // Professor Daniel Shiffman used 2 so I'm using 2 as well
    public void action(PipeGroup p) {
        // Inputs after normalized
        double[] inputs = {p.getTop() / gameHeight, p.getBottom() / gameHeight,
            (this.getTranslateY() + (this.getHeight() / 2)) / gameHeight, this.yVelocity / 12.5};

        double[] outputs = brain.process(inputs);

        if (outputs[0] > outputs[1]) {
            this.jump();
        }
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isAlive() {
        return alive;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public NeuralNetwork getBrain() {
        return brain;
    }

    public Bird mate(Bird b) {
        return new Bird(brain.quoteBreedUnquote(b.getBrain()), mutationRate, jumpStrength,
                gameWidth, gameHeight);
    }
    
    // Moves bird back to starting location
    public void resetPosition() {
        super.setTranslateY(gameHeight / 2);
        super.setTranslateX(gameWidth * 0.05);
        yVelocity = 0;
    }
}
