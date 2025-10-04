# 🧠 Chat Token Tools (Java)

> Lightweight Java CLI utilities to estimate tokens and split large text files into model-safe chunks for GPT-style LLM workflows.

---

## 📸 Demo (to be added later)
<img width="1365" height="776" alt="image" src="https://github.com/user-attachments/assets/4252ac15-703b-4649-890a-05a4d3444b1a" />
<img width="1478" height="1054" alt="Screenshot 2025-10-04 161243" src="https://github.com/user-attachments/assets/7adb60ad-fd5f-4bc3-80da-5bd16aa53c04" />
<img width="1473" height="983" alt="Screenshot 2025-10-04 161158" src="https://github.com/user-attachments/assets/c5cb37db-2450-440f-88d8-fcfe4dfaffd7" />

---

## 🎯 Why This Exists

When working with huge chat logs, documents, or study notes, AI models eventually lose context because of limited token windows.  
For example:  
- GPT-4 → ~8k tokens  
- GPT-5 → ~40k tokens  

These tools fix that problem locally and simply.  

They help you:  
1. Know how large your files really are in tokens.  
2. Split them into safe, overlapping chunks.  
3. Prepare summaries for each chunk without breaking continuity.  

---

## 🧩 Tools Overview

1. **TokenEstimator** → Estimates token, word, and character counts for any text file and shows which GPT models can handle it.  
2. **TokenChunker** → Splits large files into model-safe chunks with paragraph and sentence awareness, plus configurable overlap.  

---

## ⚙️ Setup & Installation

### 1. Clone the Repository
```bash
git clone https://github.com/Pratham-2105/chat-token-tools.git
cd chat-token-tools
```

### 2. Compile the Java Files

If you’re using Eclipse:  
- Open Eclipse → File → Open Projects from File System...  
- Choose the `chat-token-tools` folder.  
- Eclipse compiles automatically.  

If you prefer the command line:  
```bash
cd src
javac Main.java TokenEstimator.java TokenChunker.java
```

---

## 🧮 TokenEstimator

**Purpose**  
Estimates how many tokens your text file approximately uses and whether it fits within GPT model limits.  

**Usage**  
```bash
java TokenEstimator <path-to-text-file> [--clean=unicode|ascii|none] [--summary]
```

**Options**  
- `--clean` : Cleaning mode. Values = unicode (default), ascii, none.  
- `--summary` : Writes a text summary file next to your input.  

**Example**  
```bash
java TokenEstimator myfile.txt --clean=unicode --summary
```

---

## ✂️ TokenChunker

**Purpose**  
Splits a large text file into safe chunks based on a selected model’s token limit and adds overlaps so context isn’t lost.  

**Usage**  
```bash
java TokenChunker <input.txt> <modelKey> [outDir] [--overlap=200] [--max=OVERRIDE] [--headroom=0.15] [--clean=unicode|ascii|none] [--dryrun]
```

**Model Keys**  
- gpt4-8k → 8000 tokens  
- gpt4-32k → 32000 tokens  
- gpt5-40k → 40000 tokens  
- gpt4turbo-128k → 128000 tokens  
- plus-gpt4 / plus-gpt5 / go-gpt5 → aliases  

**Examples**  
- Dry-run preview (no files written):  
```bash
java TokenChunker myfile.txt gpt5-40k --dryrun
```

- Actual run (creates chunks):  
```bash
java TokenChunker myfile.txt gpt5-40k ./chunks --overlap=250 --headroom=0.1
```

---

## ⚙️ Internal Design

**TokenEstimator Flow**  
1. Read file into memory.  
2. Clean text (optional modes).  
3. Count characters and words.  
4. Estimate tokens (chars / 4).  
5. Print model compatibility check.  

**TokenChunker Flow**  
1. Read text file.  
2. Determine token budget = maxTokens * (1 - headroom).  
3. Split text at paragraph boundaries, sentence boundaries, or hard cut if needed.  
4. Add overlap (about 200 tokens ≈ 800 chars).  
5. Write outputs:  
   - partXX.txt chunks  
   - chunk_plan.txt summary  
   - chunk_metadata.json machine-readable plan  
   - summary_prompts.txt for AI summarization later  

---

## 🧩 Example Use Cases

- Long conversation logs → split safely for AI summarization  
- Research notes → manage large academic text files  
- Book drafts → process chapters individually  
- ChatGPT archives → keep long chats intact with overlap  

---

## 📂 File & Folder Overview

```
chat-token-tools/
├── src/
│   ├── Main.java
│   ├── TokenEstimator.java
│   └── TokenChunker.java
│
├── LICENSE
├── README.md
├── .gitignore
└── CONTRIBUTING.md
```

---

## 🧰 Troubleshooting

- "File not found" → Path missing or incorrect. Use full path or move file into project folder.  
- "Unknown modelKey" → Argument misspelled. Run without args to see valid keys.  
- "Too many open files" → Input file is extremely large. Increase Java heap or pre-chunk manually.  
- Encoding issues → Use `--clean=ascii`.  

---

---

📎 **Note:** This project is stable but not actively maintained.  
See [WARNING.md](./WARNING.md) for full maintenance notice.

---

## 🧾 License

Licensed under the MIT License.  
See [LICENSE](./LICENSE) for details.  

---

## 👤 Maintainer

Pratham Srivastava  
📧 prathamsri.21@gmail.com  
🔗 GitHub: [Pratham-2105](https://github.com/Pratham-2105)  

---

## 💬 Contributing

1. Fork the repository.  
2. Create a branch: `feature/your-feature`.  
3. Commit your changes.  
4. Open a Pull Request.  

See [CONTRIBUTING.md](./CONTRIBUTING.md) for details.  

---
