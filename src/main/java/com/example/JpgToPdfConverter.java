package com.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

public class JpgToPdfConverter {

    // A4 page size with margins (in points, 1 inch = 72 points)
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 36; // 0.5 inch margin
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float CONTENT_HEIGHT = PAGE_HEIGHT - 2 * MARGIN;

    // Supported image formats
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp"
    );

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -jar jpg-to-pdf-converter.jar <output.pdf> <input1.jpg> [input2.jpg] ...");
            System.out.println("   Or: java -jar jpg-to-pdf-converter.jar <output.pdf> <directory>");
            System.exit(1);
        }

        String outputPath = args[0];
        List<String> inputPaths = new ArrayList<>();

        // Collect all input paths (files or directories)
        for (int i = 1; i < args.length; i++) {
            String path = args[i];
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("Error: Path not found: " + path);
                System.exit(1);
            }
            if (file.isDirectory()) {
                inputPaths.addAll(collectImagesFromDirectory(file));
            } else {
                inputPaths.add(path);
            }
        }

        if (inputPaths.isEmpty()) {
            System.err.println("Error: No image files found to convert.");
            System.exit(1);
        }

        // Validate and convert
        List<String> validImages = new ArrayList<>();
        for (String path : inputPaths) {
            if (isSupportedImage(path)) {
                validImages.add(path);
            } else {
                System.err.println("Skipping unsupported file: " + path);
            }
        }

        if (validImages.isEmpty()) {
            System.err.println("Error: No valid image files to convert.");
            System.exit(1);
        }

        try {
            convertImagesToPdf(validImages, outputPath);
            System.out.println("Successfully converted " + validImages.size() + " image(s) to " + outputPath);
        } catch (IOException e) {
            System.err.println("Error creating PDF: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<String> collectImagesFromDirectory(File directory) {
        List<String> images = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.sort(files); // Sort for consistent ordering
            for (File file : files) {
                if (file.isFile() && isSupportedImage(file.getName())) {
                    images.add(file.getAbsolutePath());
                }
            }
        }
        return images;
    }

    private static boolean isSupportedImage(String filename) {
        String extension = getExtension(filename).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    private static String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    public static void convertImagesToPdf(List<String> inputPaths, String outputPath) throws IOException {
        // Ensure output directory exists
        Path outputDir = Paths.get(outputPath).getParent();
        if (outputDir != null && !Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        try (PDDocument document = new PDDocument()) {
            for (String inputPath : inputPaths) {
                addImageToDocument(document, inputPath);
            }
            document.save(outputPath);
        }
    }

    private static void addImageToDocument(PDDocument document, String inputPath) throws IOException {
        File imageFile = new File(inputPath);
        if (!imageFile.exists()) {
            throw new IOException("File not found: " + inputPath);
        }

        // Load the image
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("Could not read image (unsupported format?): " + inputPath);
        }

        // Create a new page
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        // Calculate scaled dimensions to fit page while maintaining aspect ratio
        float[] dimensions = calculateScaledDimensions(
            image.getWidth(), image.getHeight(),
            CONTENT_WIDTH, CONTENT_HEIGHT
        );

        float scaledWidth = dimensions[0];
        float scaledHeight = dimensions[1];

        // Center the image on the page
        float xOffset = MARGIN + (CONTENT_WIDTH - scaledWidth) / 2;
        float yOffset = MARGIN + (CONTENT_HEIGHT - scaledHeight) / 2;

        // Convert and embed the image
        PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);

        // Draw the image
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.drawImage(pdImage, xOffset, yOffset, scaledWidth, scaledHeight);
        }
    }

    private static float[] calculateScaledDimensions(
            float imgWidth, float imgHeight,
            float maxWidth, float maxHeight) {
        float scale;

        // Determine scale based on which dimension needs more scaling
        if (imgWidth / maxWidth > imgHeight / maxHeight) {
            // Width is the limiting factor
            scale = maxWidth / imgWidth;
        } else {
            // Height is the limiting factor
            scale = maxHeight / imgHeight;
        }

        return new float[] { imgWidth * scale, imgHeight * scale };
    }
}
