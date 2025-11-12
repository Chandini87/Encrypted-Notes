import java.io.*;
import java.util.*;

public class EncryptedNotesApp {

    private static final String NOTES_DIR = "notes";
    private static final String AUTH_DIR = "auth";
    private static final String KEY_DIR = "key";
    private static final String PASSWORD_FILE = AUTH_DIR + "/password.txt";

    private static String password = null;

    public static void main(String[] args) {
        createFolders();
        Scanner sc = new Scanner(System.in);

        if (!isPasswordSet()) {
            System.out.print("Set a password for the app: ");
            String newPassword = sc.nextLine();
            savePassword(newPassword);
            System.out.println("Password set successfully!");
        }

        while (true) {
            System.out.println("\n==== Encrypted Notes App ====");
            System.out.println("1. Add Note");
            System.out.println("2. View Note");
            System.out.println("3. Delete Note");
            System.out.println("4. Delete Line in Note");
            System.out.println("5. Edit Line in Note");
            System.out.println("6. Change Password");
            System.out.println("7. Exit");
            System.out.print("Choose an option: ");
            int choice = Integer.parseInt(sc.nextLine());

            switch (choice) {
                case 1: addNote(sc); break;
                case 2: viewNote(sc); break;
                case 3: deleteNote(sc); break;
                case 4: deleteLine(sc); break;
                case 5: editLine(sc); break;
                case 6: changePassword(sc); break;
                case 7: System.out.println("Exiting..."); return;
                default: System.out.println("Invalid option!");
            }
        }
    }

    // ================================
    // Core Features
    // ================================
    private static void addNote(Scanner sc) {
        listAllNotes();
        System.out.print("Enter filename: ");
        String filename = sc.nextLine();
        System.out.print("Enter note text: ");
        String text = sc.nextLine();

        String encrypted = simpleEncrypt(text);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(NOTES_DIR + "/" + filename + ".txt", true))) {
            bw.write(encrypted);
            bw.newLine();
            System.out.println("Note saved successfully!");
        } catch (IOException e) {
            System.out.println("Error saving note.");
        }
    }

    private static void viewNote(Scanner sc) {
        if (!checkPassword(sc)) return;
        String filename = chooseFile(sc);
        if (filename == null) return;

        try (BufferedReader br = new BufferedReader(new FileReader(NOTES_DIR + "/" + filename))) {
            String line;
            System.out.println("\n--- Note Content ---");
            while ((line = br.readLine()) != null) {
                System.out.println(simpleDecrypt(line));
            }
            System.out.println("--------------------");
        } catch (IOException e) {
            System.out.println("Error reading file.");
        }
    }

    private static void deleteNote(Scanner sc) {
        if (!checkPassword(sc)) return;
        String filename = chooseFile(sc);
        if (filename == null) return;

        File file = new File(NOTES_DIR + "/" + filename);
        if (file.delete()) {
            System.out.println("Note deleted successfully.");
        } else {
            System.out.println("Failed to delete note.");
        }
    }

    private static void deleteLine(Scanner sc) {
        if (!checkPassword(sc)) return;
        String filename = chooseFile(sc);
        if (filename == null) return;

        try {
            List<String> lines = readEncryptedLines(filename);

            System.out.println("\n--- Note Content ---");
            for (int i = 0; i < lines.size(); i++) {
                System.out.println((i + 1) + ": " + simpleDecrypt(lines.get(i)));
            }

            System.out.print("Enter line number to delete: ");
            int lineNum = Integer.parseInt(sc.nextLine());

            if (lineNum < 1 || lineNum > lines.size()) {
                System.out.println("Invalid line number.");
                return;
            }

            lines.remove(lineNum - 1);
            writeEncryptedLines(filename, lines);

            System.out.println("Line deleted successfully!");
        } catch (IOException e) {
            System.out.println("Error deleting line.");
        }
    }

    private static void editLine(Scanner sc) {
        if (!checkPassword(sc)) return;
        String filename = chooseFile(sc);
        if (filename == null) return;

        try {
            List<String> lines = readEncryptedLines(filename);

            System.out.println("\n--- Note Content ---");
            for (int i = 0; i < lines.size(); i++) {
                System.out.println((i + 1) + ": " + simpleDecrypt(lines.get(i)));
            }

            System.out.print("Enter line number to edit: ");
            int lineNum = Integer.parseInt(sc.nextLine());

            if (lineNum < 1 || lineNum > lines.size()) {
                System.out.println("Invalid line number.");
                return;
            }

            System.out.print("Enter new text: ");
            String newText = sc.nextLine();
            lines.set(lineNum - 1, simpleEncrypt(newText));

            writeEncryptedLines(filename, lines);

            System.out.println("Line updated successfully!");
        } catch (IOException e) {
            System.out.println("Error editing line.");
        }
    }

    private static void changePassword(Scanner sc) {
        if (!checkPassword(sc)) return;

        System.out.print("Enter old password: ");
        String oldPass = sc.nextLine();

        try (BufferedReader br = new BufferedReader(new FileReader(PASSWORD_FILE))) {
            String stored = br.readLine();
            if (stored == null || !simpleDecrypt(stored).equals(oldPass)) {
                System.out.println("Old password incorrect!");
                return;
            }
        } catch (IOException e) {
            System.out.println("Error reading password file.");
            return;
        }

        System.out.print("Enter new password: ");
        String newPassword = sc.nextLine();
        savePassword(newPassword);
        password = null; // force re-login
        System.out.println("Password changed successfully!");
    }

    // ================================
    // Utility Methods
    // ================================
    private static void createFolders() {
        new File(NOTES_DIR).mkdirs();
        new File(AUTH_DIR).mkdirs();
        new File(KEY_DIR).mkdirs();
    }

    private static boolean isPasswordSet() {
        return new File(PASSWORD_FILE).exists();
    }

    private static void savePassword(String pass) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(PASSWORD_FILE))) {
            bw.write(simpleEncrypt(pass));
        } catch (IOException e) {
            System.out.println("Error saving password.");
        }
    }

    private static boolean checkPassword(Scanner sc) {
        if (password == null) {
            System.out.print("Enter password: ");
            String input = sc.nextLine();
            try (BufferedReader br = new BufferedReader(new FileReader(PASSWORD_FILE))) {
                String stored = br.readLine();
                if (stored != null && simpleDecrypt(stored).equals(input)) {
                    password = input;
                    return true;
                } else {
                    System.out.println("Wrong password!");
                    return false;
                }
            } catch (IOException e) {
                System.out.println("Error reading password file.");
                return false;
            }
        }
        return true;
    }

    private static String chooseFile(Scanner sc) {
        listAllNotes();
        File folder = new File(NOTES_DIR);
        String[] files = folder.list((dir, name) -> name.endsWith(".txt"));

        if (files == null || files.length == 0) {
            return null;
        }

        System.out.print("Choose file number: ");
        int choice = Integer.parseInt(sc.nextLine());

        if (choice < 1 || choice > files.length) {
            System.out.println("Invalid choice.");
            return null;
        }
        return files[choice - 1];
    }

    private static void listAllNotes() {
        File folder = new File(NOTES_DIR);
        String[] files = folder.list((dir, name) -> name.endsWith(".txt"));

        if (files == null || files.length == 0) {
            System.out.println("No notes available.");
            return;
        }

        System.out.println("\nAvailable notes:");
        for (int i = 0; i < files.length; i++) {
            System.out.println((i + 1) + ". " + files[i]);
        }
    }

    private static List<String> readEncryptedLines(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(NOTES_DIR + "/" + filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static void writeEncryptedLines(String filename, List<String> lines) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(NOTES_DIR + "/" + filename))) {
            for (String l : lines) {
                bw.write(l);
                bw.newLine();
            }
        }
    }

    // ================================
    // Simple Encryption / Decryption
    // ================================
    private static String simpleEncrypt(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append((char) (c + 3)); // Caesar cipher
        }
        return sb.toString();
    }

    private static String simpleDecrypt(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append((char) (c - 3));
        }
        return sb.toString();
    }
}
