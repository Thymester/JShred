package JShred.Operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

public class DeDuplicater {
    private static final int SHRED_PASSES = 6;
    private static final Pattern DUPLICATE_SUFFIX =
            Pattern.compile("(?:\\s+\\((?:copy(?:\\s+\\d+)?|\\d+)\\))+"
                    + "(?=\\.[^.]+$|$)",
                    Pattern.CASE_INSENSITIVE);

    public void removeDuplicates(Scanner scanner) {
        System.out.println("Enter the directory path:");
        File directory = new File(scanner.nextLine());
        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Invalid directory path.");
            return;
        }

        java.util.Map<String, java.util.List<File>> fileMap = new java.util.HashMap<>();
        int totalFiles = countFiles(directory);
        ProgressBar scanProgress = new ProgressBar("Scanning", totalFiles);
        scanDirectory(directory, fileMap, scanProgress);
        scanProgress.complete();

        ProgressBar duplicateProgress = new ProgressBar("Checking duplicates", totalFiles);
        for (java.util.List<File> files : fileMap.values()) {
            if (files.size() > 1) {
                files.sort(Comparator
                        .comparing((File file) -> !isCleanFileName(file))
                        .thenComparing(File::getPath));

                List<File> uniqueFiles = new ArrayList<>();
                File retainedFile = null;
                for (File file : files) {
                    try {
                        File matchingFile = findMatchingFile(file, uniqueFiles);
                        if (matchingFile == null) {
                            uniqueFiles.add(file);
                            if (retainedFile == null) {
                                retainedFile = file;
                            }
                        } else if (!Shredder.shredFile(file, SHRED_PASSES)) {
                            System.err.println("Could not remove duplicate file: "
                                    + file.getPath());
                        }
                    } catch (IOException e) {
                        System.err.println("Error comparing file: " + file.getPath());
                    }
                    duplicateProgress.update();
                }

                if (retainedFile != null) {
                    normalizeFileName(retainedFile);
                }
            } else {
                duplicateProgress.update(files.size());
            }
        }
        duplicateProgress.complete();
    }

    private File findMatchingFile(File file, List<File> candidates) throws IOException {
        for (File candidate : candidates) {
            if (areFilesIdentical(file, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean areFilesIdentical(File first, File second) throws IOException {
        if (first.length() != second.length()) {
            return false;
        }

        try (FileInputStream firstStream = new FileInputStream(first);
                FileInputStream secondStream = new FileInputStream(second)) {
            byte[] firstBuffer = new byte[8192];
            byte[] secondBuffer = new byte[8192];
            int firstRead;
            while ((firstRead = firstStream.read(firstBuffer)) != -1) {
                int secondRead = readFully(secondStream, secondBuffer, firstRead);
                if (secondRead != firstRead) {
                    return false;
                }

                for (int i = 0; i < firstRead; i++) {
                    if (firstBuffer[i] != secondBuffer[i]) {
                        return false;
                    }
                }
            }
            return secondStream.read() == -1;
        }
    }

    private int readFully(FileInputStream stream, byte[] buffer, int length)
            throws IOException {
        int totalRead = 0;
        while (totalRead < length) {
            int bytesRead = stream.read(buffer, totalRead, length - totalRead);
            if (bytesRead == -1) {
                break;
            }
            totalRead += bytesRead;
        }
        return totalRead;
    }

    private boolean isCleanFileName(File file) {
        return !DUPLICATE_SUFFIX.matcher(file.getName()).find();
    }

    private void normalizeFileName(File file) {
        String normalizedName = DUPLICATE_SUFFIX.matcher(file.getName()).replaceFirst("");
        if (normalizedName.equals(file.getName())) {
            return;
        }

        File normalizedFile = new File(file.getParentFile(), normalizedName);
        if (normalizedFile.exists()) {
            System.err.println("Could not rename duplicate file because the target exists: "
                    + normalizedFile.getPath());
            return;
        }

        try {
            Files.move(file.toPath(), normalizedFile.toPath());
        } catch (IOException e) {
            System.err.println("Could not rename duplicate file: " + file.getPath());
        }
    }

    private int countFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }

        int count = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                count += countFiles(file);
            } else if (file.isFile()) {
                count++;
            }
        }
        return count;
    }

    private void scanDirectory(File directory, Map<String, List<File>> fileMap,
            ProgressBar progress) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, fileMap, progress);
            } else if (file.isFile()) {
                try {
                    String hash = getFileHash(file);
                    fileMap.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
                } catch (IOException | NoSuchAlgorithmException e) {
                    System.err.println("Error while processing file: " + file.getName());
                }
                progress.update();
            }
        }
    }

    private static class ProgressBar {
        private static final int WIDTH = 30;
        private final String label;
        private final int total;
        private int current;

        ProgressBar(String label, int total) {
            this.label = label;
            this.total = total;
        }

        void update() {
            update(1);
        }

        void update(int amount) {
            current += amount;
            int completed = total == 0 ? WIDTH : current * WIDTH / total;
            int percent = total == 0 ? 100 : current * 100 / total;
            StringBuilder bar = new StringBuilder("[");
            for (int i = 0; i < WIDTH; i++) {
                bar.append(i < completed ? '=' : ' ');
            }
            bar.append(']');
            System.out.printf("\r%s %s %3d%%", label, bar, percent);
        }

        void complete() {
            if (current < total) {
                update(total - current);
            } else if (total == 0) {
                update(0);
            }
            System.out.println();
        }
    }

    private String getFileHash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] dataBytes = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, bytesRead);
            }
        }
        byte[] hashBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
