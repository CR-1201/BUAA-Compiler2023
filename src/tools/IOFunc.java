package tools;

import config.Config;

import java.io.*;
import java.util.Scanner;
import java.util.StringJoiner;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IOFunc {
    public static String input(String filename) throws IOException {
        InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(filename)));
        Scanner scanner = new Scanner(in);
        StringJoiner stringJoiner = new StringJoiner("\n");
        while (scanner.hasNextLine()) {
            stringJoiner.add(scanner.nextLine());
        }
        scanner.close();
        in.close();
        return stringJoiner.toString();
    }
    public static void output(String content, String filename) {
        File outputFile = new File(filename);
        try (FileWriter writer = new FileWriter(outputFile, true)) {
            writer.write(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void output(String content) {
        output(content, Config.fileOutPutPath);
    }
    public static void delete(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
    }

    public static void clear(String filename) {
        delete(filename);
        output("", filename);
    }
}
