package org.example;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.*;

public class MarkdownFileManager {

    private Map<String, String> fileLinksMap = new HashMap<>();
    private Path sourceRootPath = Paths.get("C:\\Users\\Philip Fahrer\\Documents\\docsify.test\\docs");
    private Path targetRootPath = Paths.get("C:\\Users\\Philip Fahrer\\Documents\\docsify.test\\rendered");
    private Map<String,String> calloutTitleMap = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(MarkdownFileManager.class.getName());

    public MarkdownFileManager(Path origin, Path destination) {
        this.sourceRootPath = origin;
        this.targetRootPath = destination;
    }

    /**
     * @throws InvalidPathException if the path is invalid
     * @throws IOException if an I/O error occurs
     */
    public void generateFileStructure() throws InvalidPathException, IOException {
        LOGGER.info("Processing path: <" + sourceRootPath + ">");

        if (targetRootPath.toFile().exists()) {
            FileUtils.cleanDirectory(targetRootPath.toFile());
            LOGGER.info(("Cleared /rendered directory at " + targetRootPath.toString()));
        } else {
            Files.createDirectories(targetRootPath);
            LOGGER.info(("Created new /rendered directory at " + targetRootPath.toString()));
        }
        recursiveCopy(sourceRootPath);

        LOGGER.fine("Finished copying source to /rendered");
        updateMarkdownFiles(targetRootPath);



        try {
            FileWriter myWriter = new FileWriter(targetRootPath + "\\_sidebar.md");
            myWriter.write(SidebarWriter.getSidebarText(targetRootPath));
            myWriter.close();
            LOGGER.fine("Successfully wrote sidebar file");
        } catch (IOException e) {
            LOGGER.warning("Could not write sidebar file");
            e.printStackTrace();
        }
    }
    private boolean shouldCopyFile(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.endsWith(".md") || fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".html");
    }

    private void copyFile(Path file) throws IOException {
        if (!shouldCopyFile(file)) {
            LOGGER.warning("File not copied: " + file.toString());
            return;
        }

        Path targetPath = targetRootPath.resolve(sourceRootPath.relativize(file));

        Files.createDirectories(targetPath.getParent());
        Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
        Files.setLastModifiedTime(targetPath, Files.getLastModifiedTime(file));

        String fileName = file.getFileName().toString();
        String relativePath = sourceRootPath.relativize(file).toString().replace("\\", "/");
        fileLinksMap.put(fileName.substring(0, fileName.lastIndexOf('.')), "</" + relativePath + ">");

        LOGGER.fine("Copied file from <" + file.toString() + "> to <" + targetPath.toString() + ">");
    }

    private void updateMarkdownFiles(Path directoryPath) throws IOException {
        Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".md") || file.toString().endsWith(".qmd")) {
                    LOGGER.fine("Editing file: " + file.toString());
                    editMarkdownFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void editMarkdownFile(Path file) {
        String inputFilePath = file.toString();
        String outputFilePath = file.getParent().toString() + "/temp_" + file.getFileName();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            boolean isInTags = false;
            boolean madeChanges = false;
            boolean hasSeenTags = false;
            int calloutDepth = 0;
            String line;

            /* Functionality for adding dates is currently deprecated
            writer.write("---");
            writer.newLine();
            writer.write("date: " + Files.getLastModifiedTime(file).toString().substring(0,10));
            writer.newLine();
            //If file is empty then this is necessary
            if (!reader.ready()) {
                writer.write("---");
            }
            */

            while ((line = reader.readLine()) != null) {
                if (line.equals("<!--IGNORED_FILE-->")) {
                    LOGGER.info("File ignored");
                    return;
                }
                if (line.equals("<!--TAGS-->")) {
                    isInTags = true;
                    hasSeenTags = true;
                    reader.readLine();
                    LOGGER.info("Started reading tags");
                    continue;
                }
                if (line.equals("<!--/TAGS-->")) {
                    isInTags = false;
                    LOGGER.info("Done reading tags");
                    continue;
                }
                if (!hasSeenTags) {
                    while (line.isEmpty() && reader.ready() && (line = reader.readLine()).isEmpty()) {
                    }
                    writer.write("---");
                    writer.newLine();
                    hasSeenTags = true;
                }

                if (line.equals("TARGET DECK")) {
                    madeChanges = applyTargetDeckTemplate(reader, writer);
                    continue;
                }

                if (isInTags) {
                    writer.write(line);
                    writer.newLine();
                    continue;
                }

                String modifiedLine = replaceMarkdownLinks(line);

                Pattern calloutPattern = Pattern.compile("\\s*>\\s*\\[!(\\w+)]\\s*(.*)");
                Pattern nestedCalloutPattern = Pattern.compile("\\s*>\\s*>\\s*\\[!(\\w+)]\\s*(.*)");

                // Prüfen, ob die Zeile ein Callout beginnt
                Matcher matcher = calloutPattern.matcher(modifiedLine);
                Matcher nestedMatcher = nestedCalloutPattern.matcher(modifiedLine);

                if (nestedMatcher.find()) {
                    // Verschachtelter Callout gefunden
                    String calloutType = nestedMatcher.group(1);  // z.B. "info" oder "warning"
                    calloutType = quartoCalloutTitle(calloutType);
                    String title = matcher.group(2).trim();  // Titel des Callouts
                    modifiedLine = "::: {.callout-"+calloutType+" title=\""+title+"\"}";
                    calloutDepth = 2;
                    writer.write(modifiedLine);
                    writer.newLine();
                    continue;
                }
                else if (matcher.find()) {
                    // Einfache Callout-Zeile gefunden
                    String calloutType = matcher.group(1);  // z.B. "info" oder "warning"
                    calloutType = quartoCalloutTitle(calloutType);
                    String title = matcher.group(2).trim();  // Titel des Callouts
                    modifiedLine = "::: {.callout-"+calloutType+" title=\""+title+"\"}";
                    calloutDepth = 1;
                    writer.write(modifiedLine);
                    writer.newLine();
                    continue;
                }
                int geCharCount = modifiedLine.length() - modifiedLine.replace(">", "").length();
                if (geCharCount < calloutDepth) {
                    for (int i = 0; i < calloutDepth - geCharCount; i++) {
                        writer.write(":::");
                        writer.newLine();
                        writer.newLine();
                    }
                    calloutDepth = geCharCount;
                }
                if (calloutDepth > 0) {
                    modifiedLine = modifiedLine.replace(">", "");
                }

                writer.write(modifiedLine);
                writer.newLine();
            }

            if (madeChanges) {
                LOGGER.fine("Made changes to " + file.toString());
            } else {
                LOGGER.fine("Made no changes to " + file.toString());
            }
            reader.close();
            writer.close();
            replaceOriginalFile(file, outputFilePath);

        } catch (IOException e) {
            LOGGER.warning("Error editing file: " + file.toString());
            e.printStackTrace();
        }
    }

    private boolean applyTargetDeckTemplate(BufferedReader reader, BufferedWriter writer) throws IOException {
        reader.readLine();
        reader.readLine();
        String line = reader.readLine();
        line = "## " + line;
        writer.write(line);
        writer.newLine();
        writer.newLine();
        reader.readLine();
        LOGGER.info("Applied TARGET DECK template changes");
        return true;
    }

    private String replaceMarkdownLinks(String line) {
        Pattern pattern = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
        Matcher matcher = pattern.matcher(line);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String linkText = matcher.group(1);
            if (fileLinksMap.containsKey(linkText)) {
                String replacement = "[" + linkText + "](" + fileLinksMap.get(linkText) + ")";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else if (linkText.endsWith(".png") || linkText.endsWith(".jpg")) {
                String replacement = "<img src=\"" + linkText + "\" alt=\"\">" + System.lineSeparator();
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private void replaceOriginalFile(Path originalFile, String tempFilePath) throws IOException {
        Path tempPath = Path.of(tempFilePath);

        // Check if the original file is writable
        if (Files.isWritable(originalFile)) {
            Files.move(tempPath, originalFile, StandardCopyOption.REPLACE_EXISTING);
        } else {
            LOGGER.warning("The original file is not (over)writable: " + originalFile.toString());
        }
    }

    private String quartoCalloutTitle(String input) {
        if (this.calloutTitleMap.containsKey(input.toLowerCase())) {
            return calloutTitleMap.get(input);
        }
        return input;
    }

    private void recursiveCopy(Path source) throws IOException {
        File[] listedFiles = source.toFile().listFiles();
        if (listedFiles == null) {
            return;
        }
        for (File file : listedFiles) {
            if (file.isDirectory()) {
                recursiveCopy(file.toPath());
            } else {
                copyFile(file.toPath());
            }
        }

    }
}
