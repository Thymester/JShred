package JShred.Operations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class Shredder {

    public void shredFile(Scanner scanner) {
        File file;
        while (true) {
            System.out.print("Enter the full path of the file to shred: ");
            String filePath = scanner.nextLine().trim();
            file = new File(filePath);

            if (file.isFile()) {
                break;
            }

            if (file.isDirectory()) {
                System.out.println("That path is a directory. Enter the image filename too.");
                System.out.println("Files in that directory:");
                File[] files = file.listFiles(File::isFile);
                if (files != null) {
                    for (File directoryFile : files) {
                        System.out.println(" - " + directoryFile.getName());
                    }
                }
            } else {
                System.out.println("File not found. Enter the full path, including the filename and extension.");
            }
        }

        System.out.print("Enter the number of overwrite passes: ");
        if (!scanner.hasNextInt()) {
            System.out.println("Error: Passes must be a positive whole number.");
            return;
        }

        int passes = scanner.nextInt();
        if (passes < 1) {
            System.out.println("Error: Passes must be a positive whole number.");
            return;
        }

        System.out.println("Shredding " + file.getPath() + "...");
        if (shredFile(file, passes)) {
            System.out.println("File shredded successfully.");
        } else {
            System.out.println("File could not be shredded.");
        }
    }

    public static boolean shredFile(File file, int passes) {
        if (!file.exists() || !file.isFile() || passes < 1) {
            System.out.println("Error: File does not exist, is a directory, or has invalid passes.");
            return false;
        }

        long fileLength = file.length();
        
        try (FileOutputStream out = new FileOutputStream(file)) {
            Random random = new Random();
            byte[] buffer = new byte[4096]; // 4KB buffer to ensure there is efficient writing

            for (int currentPass = 1; currentPass <= passes; currentPass++) {
                long bytesWritten = 0;

                while (bytesWritten < fileLength) {
                    // The following will decide what to write based on the currentPass
                    if (currentPass == 1) {
                        // Pass 1: Writes all Zeros
                        java.util.Arrays.fill(buffer, (byte) 0);
                    } else if (currentPass == 2) {
                        // Pass 2: Writes all Ones
                        java.util.Arrays.fill(buffer, (byte) 0xFF);
                    } else {
                        // Pass 3 and up: Writes a Cryptographic or/and Random Noise to remove all traces of that file
                        random.nextBytes(buffer);
                    }

                    int bytesToWrite = (int) Math.min(buffer.length, fileLength - bytesWritten);
                    out.write(buffer, 0, bytesToWrite);
                    bytesWritten += bytesToWrite;
                }
                out.getFD().sync(); // Force the write to physical storage on device
            }
        } catch (IOException e) {
            System.out.println("Failed to overwrite file: " + e.getMessage());
            return false;
        }

        // The Final Step: Delete the wiped file entry from the device without a trace
        return file.delete();
    }
}
