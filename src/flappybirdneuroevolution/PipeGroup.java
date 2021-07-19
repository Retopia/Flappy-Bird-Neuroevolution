package flappybirdneuroevolution;

import java.util.concurrent.ThreadLocalRandom;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author Preston Tang
 * 
 * This class represents both the top and bottom pipe
 * 
 */
public class PipeGroup extends Group {

    private double top, bottom;
    private Rectangle topPipe, bottomPipe;
    private double gap;
    private double updateAmount;

    public PipeGroup(double updateAmount, double gameWidth, double gameHeight, double gap, double width) {
        // I generate the opening location while taking account of the width of the opening
        // Then I just place the pipes where they make sense
        
        double openingLocation = ThreadLocalRandom.current().nextDouble(gameHeight * 0.1, (gameHeight * 0.9 - gap));

        topPipe = new Rectangle();
        topPipe.setTranslateY(0);
        topPipe.setWidth(width);
        topPipe.setHeight(openingLocation);
        topPipe.setCache(true);
        topPipe.setCacheHint(CacheHint.SPEED);
        topPipe.setFill(Color.rgb(30, 150, 30));

        bottomPipe = new Rectangle();
        bottomPipe.setTranslateY(openingLocation + gap);
        bottomPipe.setWidth(width);
        bottomPipe.setHeight(gameHeight - (openingLocation + gap));
        bottomPipe.setCache(true);
        bottomPipe.setCacheHint(CacheHint.SPEED);
        bottomPipe.setFill(Color.rgb(30, 150, 30));

        top = topPipe.getHeight();
        bottom = top + gap;

        this.updateAmount = updateAmount;
        this.gap = gap;

        super.getChildren().addAll(topPipe, bottomPipe);
        super.setTranslateX(gameWidth);
    }

    public void update() {
        super.setTranslateX(super.getTranslateX() - updateAmount);
    }

    public double getTop() {
        return top;
    }

    public double getBottom() {
        return bottom;
    }

    public Rectangle getTopPipe() {
        return topPipe;
    }

    public Rectangle getBottomPipe() {
        return bottomPipe;
    }

    public double getWidth() {
        return this.topPipe.getWidth();
    }

    public double getGap() {
        return gap;
    }
}
