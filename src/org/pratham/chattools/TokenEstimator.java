package org.pratham.chattools;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * TokenEstimator
 * Small utility to estimate token counts for text files.
 *
 * Usage:
 *  - As a library: TokenEstimator.run(Path)
 *  - As a standalone: java TokenEstimator <file> [--clean=unicode|ascii|none]
 *
 * Approximation: tokens ≈ cleanedChars / 4
 */
public class TokenEstimator {

    private static final String DEFAULT_CLEAN = "unicode"; // unicode|ascii|none

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java TokenEstimator <file> [--clean=unicode|ascii|none]");
            return;
        }
        Path p = Path.of(args[0]);
        String cleanMode = DEFAULT_CLEAN;
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--clean=")) {
                cleanMode = args[i].substring("--clean=".length()).toLowerCase(Locale.ROOT);
            }
        }
        run(p, cleanMode);
    }

    // convenience overload used by Main() earlier
    public static void run(Path file) {
        run(file, DEFAULT_CLEAN);
    }

    public static void run(Path file, String cleanMode) {
        if (!Files.isRegularFile(file)) {
            System.err.println("File not found: " + file);
            return;
        }
        if (!cleanMode.equals("unicode") && !cleanMode.equals("ascii") && !cleanMode.equals("none")) {
            System.err.println("Unknown clean mode: " + cleanMode);
            return;
        }

        try {
            String raw = Files.readString(file);

            String cleanedForCount = switch (cleanMode) {
                case "ascii" -> raw.replaceAll("[^\\x00-\\x7F]", "");
                case "none" -> raw;
                default -> raw.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", ""); // unicode: keep letters/numbers/punct/whitespace
            };

            int charCount = cleanedForCount.length();
            int approxTokens = Math.max(1, charCount / 4);

            int wordCount = countWords(cleanedForCount);
            Map<String, Integer> topWords = topNWords(cleanedForCount, 10);

            System.out.println("=== TokenEstimator ===");
            System.out.println("File: " + file.toAbsolutePath());
            System.out.println("Clean mode: " + cleanMode);
            System.out.println("Cleaned chars: " + NumberFormat.getIntegerInstance().format(charCount));
            System.out.println("Approx tokens (chars/4): " + NumberFormat.getIntegerInstance().format(approxTokens));
            System.out.println("Word count (approx): " + NumberFormat.getIntegerInstance().format(wordCount));
            System.out.println();
            System.out.println("Top words:");
            topWords.forEach((w, c) -> System.out.printf("  %-15s %5d%n", w, c));

            // Simple distribution hints
            System.out.println();
            System.out.println("Hints:");
            System.out.println(" - If approx tokens > model limit, consider chunking (TokenChunker).");
            System.out.println(" - Results are approximate; for precise tokenization, integrate a tokenizer library.");

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static int countWords(String s) {
        // A simple word splitter: consider words as sequences of letters/numbers/apostrophes
        Matcher m = Pattern.compile("[\\p{L}\\p{N}']+").matcher(s);
        int c = 0;
        while (m.find()) c++;
        return c;
    }

    private static Map<String, Integer> topNWords(String s, int n) {
        // normalize to lower, remove punctuation except apostrophes inside words
        String normalized = s.toLowerCase(Locale.ROOT);
        Matcher m = Pattern.compile("[\\p{L}\\p{N}']+").matcher(normalized);
        Map<String, Integer> freq = new HashMap<>();
        while (m.find()) {
            String w = m.group();
            // ignore short tokens (optional)
            if (w.length() <= 2) continue;
            freq.merge(w, 1, Integer::sum);
        }
        // take top n
        return freq.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(n)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a,b) -> a,
                        LinkedHashMap::new
                ));
    }
}
