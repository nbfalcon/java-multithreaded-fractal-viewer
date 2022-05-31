package org.nbfalcon.fractalViewer.ui.components;

import org.nbfalcon.fractalViewer.util.FileUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ImageIOFileChooser extends JFileChooser {
    // WEBP isn't supported on Java OOTB
    private static final List<FileTypeFilter> imageIOChoosers = FileTypeFilter.getSupportedFormats(
            "PNG", "JPEG", "WEBP", "GIF", "TIFF", "BMP", "WBMP");

    public ImageIOFileChooser() {
        for (FileTypeFilter chooser : imageIOChoosers) {
            addChoosableFileFilter(chooser);
        }
    }

    private FileTypeFilter getSelectedImageFormat() {
        FileFilter selected = getFileFilter();
        return selected instanceof FileTypeFilter ? (FileTypeFilter) selected : null;
    }

    public String getImageIOFormat() {
        FileTypeFilter format = getSelectedImageFormat();
        if (format != null) return format.formatName;

        File selected = getSelectedFile();
        if (selected != null) {
            String ext = FileUtils.getExtension(selected);
            if (ext != null) {
                Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix(ext);
                if (iter.hasNext()) {
                    String[] formats = iter.next().getOriginatingProvider().getFormatNames();
                    if (formats.length > 0) {
                        return formats[0];
                    }
                }
            }
        }

        return null;
    }

    public File getSelectedFileWithExtension() {
        File selectedFile = getSelectedFile();
        if (selectedFile == null) return null;

        FileTypeFilter format = getSelectedImageFormat();
        // if format == null, we just go by the extension; no need to fix the file
        if (format != null && format.primarySuffix != null) {
            String ext = FileUtils.getExtension(selectedFile);
            if (ext != null && !format.extensions.contains(ext.toLowerCase())) {
                return new File(selectedFile.getPath() + "." + format.primarySuffix);
            }
        }
        return selectedFile;
    }

    public static class FileTypeFilter extends FileFilter {
        public final String formatName;
        public final String primarySuffix;
        final Set<String> extensions;
        private final String description;

        public FileTypeFilter(String formatName, String... extensions) {
            this.formatName = formatName;
            this.primarySuffix = extensions.length == 0 ? null : extensions[0];

            this.extensions = Set.of(extensions);

            String extensionsS = Arrays.stream(extensions).map(ext -> "." + ext).collect(Collectors.joining(", "));
            this.description = formatName + " (" + extensionsS + ")";
        }

        public static List<FileTypeFilter> getSupportedFormats(String... formats) {
            List<FileTypeFilter> result = new ArrayList<>();
            for (String format : formats) {
                ImageIO.getImageWritersByFormatName(format).forEachRemaining(writer -> {
                    String[] suffixes = writer.getOriginatingProvider().getFileSuffixes();
                    result.add(new FileTypeFilter(format, suffixes));
                });
            }
            return result;
        }

        @Override
        public boolean accept(File file) {
            String extension = FileUtils.getExtension(file);
            return extension != null && this.extensions.contains(extension);
        }

        @Override
        public String getDescription() {
            return this.description;
        }
    }
}
