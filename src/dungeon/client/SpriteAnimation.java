package dungeon.client;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class SpriteAnimation extends Transition {
    private final ImageView imageView;
    private final int count;
    private final int columns;
    private int offsetX;
    private int offsetY; // شيلنا كلمة final عشان نقدر نغيره
    private final int width;
    private final int height;
    private int lastIndex;

    public SpriteAnimation(ImageView imageView, Duration duration, int count, int columns,
                           int offsetX, int offsetY, int width, int height) {
        this.imageView = imageView;
        this.count = count;
        this.columns = columns;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;

        setCycleDuration(duration);
        setInterpolator(Interpolator.LINEAR);
    }

    // الدالتين دول جداد عشان نغير الاتجاه
    public void setOffsetY(int y) { this.offsetY = y; }
    public int getOffsetY() { return this.offsetY; }

    @Override
    protected void interpolate(double k) {
        final int index = Math.min((int) Math.floor(k * count), count - 1);
        if (index != lastIndex) {
            final int x = (index % columns) * width + offsetX;
            final int y = (index / columns) * height + offsetY;

            imageView.setViewport(new Rectangle2D(x, y, width, height));
            lastIndex = index;
        }
    }
}