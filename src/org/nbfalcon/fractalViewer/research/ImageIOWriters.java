package org.nbfalcon.fractalViewer.research;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.util.*;
import java.util.stream.Collectors;

public class ImageIOWriters {
    private static int countUppers(String s) {
        return (int) s.chars().filter(Character::isUpperCase).count();
    }

    public static void main(String[] args) {
//        for (String extension : ImageIO.getWriterFileSuffixes()) {
//            ImageIO.getImageWritersBySuffix(extension).forEachRemaining((writer) -> {
//                writer.getOriginatingProvider().getFormatNames();
//            });
//        }
//        for (String writer : ImageIO.getWriterFormatNames()) {
//            System.out.println(writer);
//            ImageIO.getImageWritersByFormatName(writer).forEachRemaining((f) -> {
//                for (String formatName : f.getOriginatingProvider().getFormatNames()) {
//                    System.out.println("\t" + formatName);
//                }
//            });
//        }
        Map<Class<?>, ImageWriter> writers = new HashMap<>();
        for (String wFormat : ImageIO.getWriterFormatNames()) {
            ImageIO.getImageWritersByFormatName(wFormat)
                    .forEachRemaining(writer -> writers.put(writer.getClass(), writer));
        }
        writers.forEach((ignored, writer) -> {
            Optional<String> longest = Arrays.stream(writer.getOriginatingProvider().getFormatNames())
                    .max(Comparator
                            .comparingInt(String::length)
                            .thenComparingInt(ImageIOWriters::countUppers));
            if (longest.isPresent()) {
                String suffixes = Arrays.stream(writer.getOriginatingProvider().getFileSuffixes()).map(ext -> "." + ext).collect(Collectors.joining(", "));
                System.out.println(longest.get() + " (" + suffixes + ")");
            }
        });
        System.out.println(ImageIO.getImageWritersBySuffix("PNg").next());
    }
}
