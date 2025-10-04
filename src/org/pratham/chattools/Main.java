package org.pratham.chattools;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        String cmd = args[0].toLowerCase(Locale.ROOT);
        String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);

        try {
            switch (cmd) {
                case "estimate":
                    if (cmdArgs.length < 1) {
                        System.err.println("estimate requires a file path");
                        printHelp();
                        break;
                    }
                    Path p = Paths.get(cmdArgs[0]);
                    // allow optional --clean=... as second arg
                    String cleanMode = "unicode";
                    for (int i = 1; i < cmdArgs.length; i++) {
                        if (cmdArgs[i].startsWith("--clean=")) cleanMode = cmdArgs[i].substring("--clean=".length());
                    }
                    TokenEstimator.run(p, cleanMode);
                    break;

                case "chunk":
                    // pass the remaining args to TokenChunker by reconstructing an args array:
                    // TokenChunker expects: <input.txt> <modelKey> [outDir] [--overlap=200] ...
                    if (cmdArgs.length < 2) {
                        System.err.println("chunk requires: <input.txt> <modelKey> [outDir] [--overlap=200] ...");
                        printHelp();
                        break;
                    }
                    // Build args array for TokenChunker main (it already expects the input and model key at indexes 0/1)
                    TokenChunker.main(cmdArgs);
                    break;

                case "-h":
                case "--help":
                case "help":
                    printHelp();
                    break;

                default:
                    System.err.println("Unknown command: " + cmd);
                    printHelp();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printHelp() {
        System.out.println("Chat Token Tools CLI");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  estimate <file> [--clean=unicode|ascii|none]");
        System.out.println("      Estimate token/word counts for a file.");
        System.out.println();
        System.out.println("  chunk <file> <modelKey> [outDir] [--overlap=200] [--max=OVERRIDE] [--headroom=0.15] [--clean=unicode|ascii|none]");
        System.out.println("      Split a large file into chunks sized for the given model.");
        System.out.println("      Model keys: gpt4-8k, gpt4-32k, gpt5-40k, gpt4turbo-128k, go-gpt5, plus-gpt4, plus-gpt5");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar chat-token-tools.jar estimate notes.txt --clean=unicode");
        System.out.println("  java -jar chat-token-tools.jar chunk book.txt gpt4-32k ./book__chunks --overlap=200");
    }
}
