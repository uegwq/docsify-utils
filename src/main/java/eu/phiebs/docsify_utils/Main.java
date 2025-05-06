package eu.phiebs.docsify_utils;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {

        Path origin = Path.of(args[0]);
        Path destination = Path.of(args[1]);

        MarkdownFileManager manager = new MarkdownFileManager(origin, destination);
        try {
            manager.generateFileStructure();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
