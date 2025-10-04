package org.pratham.chattools;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * TokenChunker
 * Splits a large text file into model-sized chunks by approximate token budget.
 *
 * Approx tokens = cleanedCharCount / 4.
 * Splitting strategy: paragraphs -> sentences -> hard cut if still too large.
 * Adds overlap between chunks (in tokens, approximated as overlapTokens*4 chars).
 *
 * Usage:
 *   java TokenChunker <input.txt> <modelKey> [outDir] [--overlap=200] [--max=OVERRIDE] [--headroom=0.15] [--clean=unicode|ascii|none]
 *
 * Model keys:
 *   gpt4-8k (8000), gpt4-32k (32000), gpt5-40k (40000),
 *   gpt4turbo-128k (128000), go-gpt5 (40000), plus-gpt4 (32000), plus-gpt5 (40000)
 *
 * Outputs:
 *   <basename>__chunks/
 *     - <basename>__part01.txt, ...
 *     - chunk_plan.txt
 *     - summary_prompts.txt
 */
public class TokenChunker {

    // Defaults
    private static final int DEFAULT_OVERLAP_TOKENS = 200;        // ~ context carry
    private static final double DEFAULT_HEADROOM = 0.15;          // keep 15% safety buffer
    private static final String DEFAULT_CLEAN = "unicode";        // unicode (strip emojis etc), "ascii", or "none"

    private static final Map<String, Integer> MODEL_LIMITS = Map.ofEntries(
            Map.entry("gpt4-8k", 8000),
            Map.entry("gpt4-32k", 32000),
            Map.entry("gpt5-40k", 40000),
            Map.entry("gpt4turbo-128k", 128000),
            // Friendly aliases
            Map.entry("go-gpt5", 40000),
            Map.entry("plus-gpt4", 32000),
            Map.entry("plus-gpt5", 40000)
    );

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java TokenChunker <input.txt> <modelKey> [outDir] [--overlap=200] [--max=OVERRIDE] [--headroom=0.15] [--clean=unicode|ascii|none]");
            System.out.println("Model keys: " + MODEL_LIMITS.keySet());
            return;
        }

        Path input = Paths.get(args[0]);
        if (!Files.isRegularFile(input)) {
            System.err.println("Input file not found: " + input);
            return;
        }

        String modelKey = args[1].toLowerCase(Locale.ROOT);
        Integer modelMax = MODEL_LIMITS.get(modelKey);
        if (modelMax == null) {
            System.err.println("Unknown modelKey: " + modelKey + " ; supported: " + MODEL_LIMITS.keySet());
            return;
        }

        // Defaults
        Path outDir = input.getParent() == null
                ? Paths.get(input.getFileName().toString().replaceAll("\\.txt$", "") + "__chunks")
                : input.getParent().resolve(input.getFileName().toString().replaceAll("\\.txt$", "") + "__chunks");
        int overlapTokens = DEFAULT_OVERLAP_TOKENS;
        double headroom = DEFAULT_HEADROOM;
        String cleanMode = DEFAULT_CLEAN; // unicode|ascii|none
        Integer overrideMax = null;

        // Parse optional args
        for (int i = 2; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--overlap=")) {
                overlapTokens = parseInt(a.substring("--overlap=".length()), DEFAULT_OVERLAP_TOKENS);
            } else if (a.startsWith("--max=")) {
                overrideMax = parseInt(a.substring("--max=".length()), modelMax);
            } else if (a.startsWith("--headroom=")) {
                headroom = parseDouble(a.substring("--headroom=".length()), DEFAULT_HEADROOM);
            } else if (a.startsWith("--clean=")) {
                cleanMode = a.substring("--clean=".length()).toLowerCase(Locale.ROOT);
                if (!cleanMode.equals("unicode") && !cleanMode.equals("ascii") && !cleanMode.equals("none")) {
                    System.err.println("Unknown --clean mode: " + cleanMode + " (use unicode|ascii|none)");
                    return;
                }
            } else if (a.startsWith("--")) {
                System.err.println("Unknown option: " + a);
                return;
            } else {
                // treat as outDir if provided without flag
                outDir = Paths.get(a);
            }
        }

        int maxTokens = (overrideMax != null) ? overrideMax : modelMax;
        int budgetTokens = (int) Math.floor(maxTokens * (1.0 - headroom));
        int overlapChars = overlapTokens * 4;

        try {
            Files.createDirectories(outDir);

            String raw = Files.readString(input);
            String cleanedForCount = switch (cleanMode) {
                case "ascii" -> raw.replaceAll("[^\\x00-\\x7F]", "");
                case "none" -> raw;
                default /* unicode */ -> raw.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "");
            };

            int totalChars = cleanedForCount.length();
            int approxTokens = totalChars / 4;

            System.out.println("=== TokenChunker ===");
            System.out.println("Input: " + input.toAbsolutePath());
            System.out.println("Model: " + modelKey + " (max=" + maxTokens + " tokens), headroom=" + (int)(headroom*100) + "%");
            System.out.println("Budget (per chunk): ~" + budgetTokens + " tokens");
            System.out.println("Overlap: " + overlapTokens + " tokens (~" + overlapChars + " chars)");
            System.out.println("Clean mode: " + cleanMode);
            System.out.println("Total cleaned chars: " + totalChars + "  (~" + approxTokens + " tokens)");
            System.out.println("Output dir: " + outDir.toAbsolutePath());
            System.out.println();

            // We will split on the original raw text to preserve content. For estimating chunk size, use char budget = tokens*4.
            int perChunkCharBudget = budgetTokens * 4;

            List<String> chunks = splitSmart(raw, perChunkCharBudget, overlapChars);

            // Write chunks
            String baseName = stripExt(input.getFileName().toString());
            List<Integer> chunkTokenEstimates = new ArrayList<>();
            int idx = 1;
            for (String chunk : chunks) {
                String fileName = baseName + "__part" + String.format("%02d", idx) + ".txt";
                Path out = outDir.resolve(fileName);
                Files.writeString(out, chunk);

                int tokens = estimateTokens(chunk, cleanMode);
                chunkTokenEstimates.add(tokens);
                System.out.printf("Wrote %-40s ~%d tokens%n", fileName, tokens);
                idx++;
            }

            // Plan file
            Path plan = outDir.resolve("chunk_plan.txt");
            try (BufferedWriter bw = Files.newBufferedWriter(plan)) {
                bw.write("Model: " + modelKey + " (max " + maxTokens + " tokens), budget ~" + budgetTokens + " tokens, overlap " + overlapTokens + " tokens\n");
                bw.write("Total chunks: " + chunks.size() + "\n\n");

                int c = 1; int sum = 0;
                for (Integer t : chunkTokenEstimates) {
                    sum += t;
                    bw.write(String.format("part%02d\t~%d tokens%s\n", c, t, (t <= maxTokens ? "" : "  [>MAX!]")));
                    c++;
                }
                bw.write("\nApprox combined tokens (without overlap dedup): ~" + sum + "\n");
                bw.write("Note: Each chunk is intended to be processed independently.\n");
            }

            // Summary prompts file
            Path prompts = outDir.resolve("summary_prompts.txt");
            try (BufferedWriter bw = Files.newBufferedWriter(prompts)) {
                bw.write("=== Per-Chunk Summary Prompt (copy for each part) ===\n");
                bw.write("""
                        Please summarize this chunk as a **Checkpoint Summary** with:
                        1) Context Recap (why this exists; what's inside),
                        2) Key Insights & Decisions (detailed, not superficial),
                        3) My Personal Experiences & Feelings (preserve the nuance),
                        4) Open Questions / Pending Work,
                        5) Continuation Instructions (what the next stage should assume).
                        Keep it faithful and specific; do not drop personal reflections.\n
                        """);

                bw.write("\n=== Master Summary Merge Prompt (after all chunks are summarized) ===\n");
                bw.write("""
                        I have multiple checkpoint summaries from a long conversation.
                        Please combine them into a **Master Summary** that:
                        - Preserves the logical flow and chronology,
                        - Keeps all important decisions and insights,
                        - Retains my personal experiences and emotional context,
                        - Lists all open questions/pending tasks,
                        - Ends with clear Continuation Instructions.
                        Target length: 1,500–2,500 words.\n
                        """);
            }

            System.out.println("\nDone. Files written to: " + outDir.toAbsolutePath());
            System.out.println(" - chunk_plan.txt");
            System.out.println(" - summary_prompts.txt");
            System.out.println(" - " + chunks.size() + " chunk files");

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // --- Splitting strategy ---

    private static List<String> splitSmart(String text, int perChunkCharBudget, int overlapChars) {
        List<String> result = new ArrayList<>();
        int n = text.length();
        int pos = 0;

        while (pos < n) {
            int targetEnd = Math.min(n, pos + perChunkCharBudget);

            // Try to extend to nearest paragraph end <= targetEnd
            int end = findBoundaryBack(text, pos, targetEnd, "\n\n"); // paragraph boundary
            if (end < pos + perChunkCharBudget / 2) {
                // If found boundary is too far back, try sentence boundary
                end = findSentenceBoundaryBack(text, pos, targetEnd);
            }
            if (end < pos + perChunkCharBudget / 2) {
                // Still too far? just cut at targetEnd
                end = targetEnd;
            }

            String slice = text.substring(pos, end);

            // Add overlap from previous end if not first chunk
            if (!result.isEmpty() && overlapChars > 0) {
                int overlapStart = Math.max(0, pos - overlapChars);
                String overlap = text.substring(overlapStart, pos);
                slice = overlap + slice;
            }

            result.add(slice);

            pos = end;
        }
        return result;
    }

    private static int findBoundaryBack(String text, int start, int targetEnd, String delimiter) {
        int idx = text.lastIndexOf(delimiter, targetEnd);
        if (idx < start) return start - 1; // indicate "not found use other strategy"
        return idx + delimiter.length();
    }

    // Try to find a sentence boundary before targetEnd (., !, ? followed by whitespace)
    private static int findSentenceBoundaryBack(String text, int start, int targetEnd) {
        int scanFrom = Math.min(text.length(), targetEnd);
        for (int i = scanFrom - 1; i >= start; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                // move forward over closing quotes/brackets if any
                int j = i + 1;
                while (j < text.length()) {
                    char c2 = text.charAt(j);
                    if (Character.isWhitespace(c2) || c2 == '"' || c2 == '\'' || c2 == ')' || c2 == ']' || c2 == '»') {
                        j++;
                    } else break;
                }
                return j;
            }
        }
        return start - 1; // not found
    }

    private static int estimateTokens(String s, String cleanMode) {
        String cleaned = switch (cleanMode) {
            case "ascii" -> s.replaceAll("[^\\x00-\\x7F]", "");
            case "none" -> s;
            default -> s.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "");
        };
        return Math.max(1, cleaned.length() / 4);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
    private static double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }
    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? name : name.substring(0, i);
    }
}
