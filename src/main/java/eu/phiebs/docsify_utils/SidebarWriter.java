package eu.phiebs.docsify_utils;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

public class SidebarWriter {

    public static String getSidebarText(Path inputPath) {
        File root = inputPath.toFile();

        File[] files = root.listFiles();
        if (files == null) {
            System.out.println("Verzeichnis darf nicht leer sein " + inputPath);
            return null;
        }

        StringBuilder output = new StringBuilder();

        // Alphabetisch sortieren
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        // Erst die Markdown-Dateien dieses Ordners
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".md")) {
                output.append("- [");
                output.append(stripExtension(file.getName()));
                output.append("](<");
                output.append(file.getName());
                output.append(">)");
                output.append(System.lineSeparator());
            }
        }

        // Danach rekursiv die Unterordner
        for (File file : files) {
            if (file.isDirectory()) {
                output.append(listMarkdownFiles(file, file.getName() + "/", 0));
            }
        }

        return output.toString();
    }

    private static String listMarkdownFiles(File dir, String relativePath, int depth) {
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }

        StringBuilder output = new StringBuilder();

        // Alphabetisch sortieren
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        // Zuerst prüfen: Gibt es überhaupt Markdown-Dateien oder Unterordner mit Markdown?
        boolean hasMarkdown = false;
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".md")) {
                hasMarkdown = true;
                break;
            } else if (file.isDirectory()) {
                if (containsMarkdown(file)) {
                    hasMarkdown = true;
                    break;
                }
            }
        }

        if (!hasMarkdown) {
            return null;
        }

        // Ordnername ausgeben
        output.append(indent(depth));
        output.append("- ");
        output.append(dir.getName());
        output.append(System.lineSeparator());

        // Erst die Markdown-Dateien dieses Ordners
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".md")) {
                output.append(indent(depth + 1));
                output.append("- [");
                output.append(stripExtension(file.getName()));
                output.append("](<");
                output.append(relativePath);
                output.append(file.getName());
                output.append(">)");
                output.append(System.lineSeparator());
            }
        }

        // Danach rekursiv die Unterordner
        for (File file : files) {
            if (file.isDirectory()) {
                output.append(listMarkdownFiles(file, relativePath + file.getName() + "/", depth + 1));
            }
        }
        return output.toString();
    }

    private static boolean containsMarkdown(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".md")) {
                return true;
            } else if (file.isDirectory()) {
                if (containsMarkdown(file)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String indent(int depth) {

        StringBuilder indentation = new StringBuilder();

        for (int i = 0; i < depth; i++) {
            indentation.append("  ");
        }

        return indentation.toString();
    }

    private static String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? filename : filename.substring(0, dotIndex);
    }
}
