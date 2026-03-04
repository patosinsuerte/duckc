package org.pato.duckc.services;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class ImageCompressionService {

    public void compress(File source, String destinationDirPath, float quality) throws IOException {
        String extension = getFileExtension(source).toLowerCase();


        BufferedImage originalImage = ImageIO.read(source);


        if (originalImage == null) {
            throw new IOException("The file format is not supported or the file is corrupted.");
        }


        int targetWidth = originalImage.getWidth();
        int targetHeight = originalImage.getHeight();
        if (targetWidth > 1600) {
            double ratio = 1600.0 / targetWidth;
            targetWidth = 1600;
            targetHeight = (int) (targetHeight * ratio);
        }

        // TIPO DE IMAGEN: ARGB para PNG (transparencia), RGB para el resto
        int type = (extension.equals("png")) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, type);

        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        // GUARDADO
        File outputDir = new File(destinationDirPath);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create the destination folder.");
        }

        String formatName = (extension.equals("jpeg") || extension.equals("jpg")) ? "jpg" : extension;
        File outputFile = new File(outputDir, "duck_" + source.getName());

        if (formatName.equals("jpg")) {
            saveAsJpg(resizedImage, outputFile, quality);
        } else {
            // Si ImageIO.write devuelve false, significa que no sabe cómo escribir ese formato
            boolean success = ImageIO.write(resizedImage, formatName, outputFile);
            if (!success) {
                throw new IOException("No encoder found for format: " + formatName);
            }
        }
    }

    private void saveAsJpg(BufferedImage image, File outputFile, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        try (FileOutputStream os = new FileOutputStream(outputFile)) {
            writer.setOutput(ImageIO.createImageOutputStream(os));
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndex = name.lastIndexOf('.');
        return (lastIndex == -1) ? "" : name.substring(lastIndex + 1);
    }

}