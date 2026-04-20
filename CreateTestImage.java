import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CreateTestImage {
    public static void main(String[] args) throws IOException {
        // Create 3 test images
        createImage("test1.jpg", "Test Image 1", Color.BLUE);
        createImage("test2.jpg", "Test Image 2", Color.GREEN);
        createImage("test3.jpg", "Test Image 3", Color.RED);
        System.out.println("Created test1.jpg, test2.jpg, test3.jpg");
    }

    static void createImage(String filename, String text, Color color) throws IOException {
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        
        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 800, 600);
        
        // Colored box
        g.setColor(color);
        g.fillRect(200, 150, 400, 300);
        
        // Text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g.drawString(text, (800 - textWidth) / 2, 310);
        
        g.dispose();
        ImageIO.write(img, "JPEG", new File(filename));
    }
}
