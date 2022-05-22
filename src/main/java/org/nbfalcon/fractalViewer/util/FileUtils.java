package org.nbfalcon.fractalViewer.util;

import java.io.File;

public class FileUtils {
    public static String getExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');

        return dot != -1 && dot + 1 < name.length() ? name.substring(dot + 1) : null;
    }

    public static File addExtension(File file, String addMe) {
        String ext = getExtension(file);
        return addMe.equalsIgnoreCase(ext) ? file
                : new File(file.getPath() + "." + addMe);
    }
}
