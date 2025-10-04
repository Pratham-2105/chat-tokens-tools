# 📜 Changelog — Chat Token Tools (Java)

All notable changes to this project will be documented here.

---

## [v0.1.0] - October 2025  
### 🚀 Initial CLI Release
- Implemented **TokenEstimator**:
  - Reads `.txt` files, cleans text (`unicode`, `ascii`, or `none` modes)
  - Estimates token, word, and character counts
  - Generates a model-fit report for GPT-4/5 variants
- Implemented **TokenChunker**:
  - Splits large text files into safe, model-sized chunks
  - Uses paragraph → sentence → hard-cut splitting
  - Adds overlap between chunks for continuity
  - Generates:
    - `chunk_plan.txt`
    - `summary_prompts.txt`
    - Chunked text files (`__partNN.txt`)
- Added sample inputs and outputs under `/examples`
- Added documentation:
  - `README.md` (user guide)
  - `LICENSE` (MIT)
  - `.gitignore`
  - `WARNING.md` (maintenance notice)

---

### 🧭 Notes
This release marks the completion of the **CLI phase (Phase 1)**.  
Further expansions like a web interface or AI summarizer are conceptual only — not in active development.

---

> _This changelog will only update if major changes are released. Minor documentation tweaks will not be tracked._
