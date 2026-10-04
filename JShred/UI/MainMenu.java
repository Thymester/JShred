package JShred.UI;

import JShred.Operations.Shredder;
import JShred.Operations.DeDuplicater;
import java.util.Scanner;

public class MainMenu {
    public void mainMenu() {
        Scanner scanner = new Scanner(System.in);

        boolean correctChoice = false;

        System.out.println("-=-=-=Title=-=-=-");
        System.out.println("Wlecome to JShred!\n\nThis is an application that shreds files from your drive permanently.");
        System.out.println("-=-=-=-=-=-=-=-=-");
        System.out.println("\n\nWhat do you wish to do?\n1. Shred a file\n2. De-Duplicate a directory\n3. Exit");

        while (true) {
            if (!scanner.hasNextInt()) {
                System.out.println("That is not a valid option...");
                scanner.next();
                continue;
            }

            int userChoice = scanner.nextInt();
            if (userChoice < 1 || userChoice > 3) {
                System.out.println("That is not a valid option...");
                continue;
            }

            if (userChoice >= 1 && userChoice <= 3) {
                correctChoice = true;
            }

            while (correctChoice == true) {
                switch (userChoice) {
                    case 1:
                        Shredder shredder = new Shredder();
                        scanner.nextLine();
                        shredder.shredFile(scanner);
                        correctChoice = false;
                        break;
                    case 2:
                        DeDuplicater deDuplicater = new DeDuplicater();
                        scanner.nextLine();
                        deDuplicater.removeDuplicates(scanner);
                        correctChoice = false;
                        break;
                    case 3:
                        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
                        System.out.println("Thank you for using JShred!");
                        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
                        correctChoice = false;
                        break;
                }
            }
            break;
        }

        scanner.close();
    }
}
