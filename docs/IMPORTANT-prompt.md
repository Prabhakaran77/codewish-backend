## Generic Learning & Documentation Prompt

Use this prompt whenever you want Cursor to **analyze some part of your project** and then **create a clear, beginner‑friendly doc** explaining it (concepts, code, configs, flows, etc.).

Copy, paste, and then fill in the ALL‑CAPS placeholders.

---

### The Prompt

> I want to deeply understand **TOPIC_OR_FEATURE** in this project.  
> 1. **Analyze the existing code and configuration** related to this (especially files like `RELEVANT_FILES_OR_PATHS` if they exist).  
> 2. **Create or update a markdown document** at `docs/DOC_FILENAME.md` (create the `docs` folder if it doesn’t exist) that explains this topic end‑to‑end in clear, simple language, assuming the reader is not a developer.  
> 3. In that doc, please:  
>    - Start with a **plain‑English overview** of what this is and why it exists in this project.  
>    - Show the **key code/config snippets** and explain them **line by line** (or section by section), clearly pointing out which parts are **critical for correctness** and which are **optional / customizable**.  
>    - Describe how this fits into the **overall flow of the application** (who/what calls it, what it depends on, what it returns or changes).  
>    - Add a **FAQ / “silly questions” section** with common layman questions and straightforward answers.  
>    - Call out important **choices, options, and trade‑offs** (e.g. different ways to implement/use this, and when you’d pick each).  
>    - If diagrams help, include **Mermaid diagrams only** to visualize flows, relationships between components, or lifecycle steps.  
> 4. Make the doc **self‑contained**, so someone can read just this file and understand the concept and how it works in this codebase, without needing to ask follow‑up questions.

---

### When to Use This Prompt

Use this prompt when:
- You are trying to **learn or revise a concept** (e.g. CI/CD, transactions, caching, authentication, microservices, etc.) **as it is actually used in this codebase**.
- You want a **new doc** in `docs/` that:
  - Explains both **theory and practice** (concept + actual project code).
  - Is written for **non‑technical or junior** readers.
  - Includes **line‑by‑line explanations** of important code or config.
  - Includes **FAQs** and **Mermaid diagrams** for better understanding.
- You want something you can **share with teammates** so they can quickly understand a feature without walking through the whole repo.

Avoid this prompt when:
- You just need a **quick one‑line answer** or a short explanation.
- You are asking about something **outside this project** (e.g. pure theory not tied to your code). In that case, you can simplify the prompt and skip the “analyze the existing code” part.

