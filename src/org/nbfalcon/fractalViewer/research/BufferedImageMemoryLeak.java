package org.nbfalcon.fractalViewer.research;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

public class BufferedImageMemoryLeak {
    public static void main(String[] args) {
        JLabel label = new JLabel();
        JFrame jf = new JFrame("Buffered image memory leak check?");
        jf.setSize(800, 600);
        jf.add(label);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setVisible(true);
        for (int i = 0; i < 10000; i++) {
            SwingUtilities.invokeLater(() -> {
                label.setIcon(new ImageIcon(genImage(8000, 6000)));
                System.gc();
            });
        }
        System.out.println("Done");
    }

    @SuppressWarnings("SameParameterValue")
    private static BufferedImage genImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int gray = ThreadLocalRandom.current().nextInt();
        image.getRaster().setPixel(width - 1, height - 1, new int[]{gray, gray, gray});
        image.getRaster().setPixel(0, 0, new int[]{gray, gray, gray});
        return image;
    }
}
