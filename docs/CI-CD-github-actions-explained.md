## GitHub Actions CI/CD for This Project – Explained in Simple Terms

This document explains **what changed** in the codebase to set up CI/CD and **what each line** in the GitHub Actions file does, in plain, non‑technical language as much as possible.

If you open the file `.github/workflows/ci.yml`, that file is what tells GitHub **how to automatically build and test your project, and how to build a Docker image** when you push code.

Below is the same file, broken into sections, with an explanation for every part.

---

### 1. Workflow name and when it runs

```yaml
name: CI / CD - backend
```

- **What it means**: This is just the **title** of the automation.  
  When you look at the Actions tab in GitHub, this is the name you’ll see for this pipeline: “CI / CD - backend”.

```yaml
on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
```

- **What it means**:
  - `on:` says **when** this automation should run.
  - `push:` means: **whenever someone pushes code** (for example, using `git push`) to the repository:
    - `branches: [ main ]` means it will run **only when the push is to the `main` branch**, not for other branches.
  - `pull_request:` means: **whenever someone opens or updates a pull request** targeting the `main` branch, this automation also runs.

In simple words:  
> *Every time code changes on `main`, or a pull request is made to go into `main`, GitHub will automatically run this pipeline.*

---

### 2. First job – Build and Test the project

```yaml
jobs:
  build-and-test:
    name: Build & Test (Gradle)
    runs-on: ubuntu-latest
```

- **`jobs:`**: A workflow can have one or more **jobs**. Think of a job as a block of work that should be done.
- **`build-and-test:`**: This is the name of the **first job**. It will:
  - Download the code.
  - Set up Java.
  - Run your Gradle build and tests.
- **`name: Build & Test (Gradle)`**: A human‑friendly label for this job.
- **`runs-on: ubuntu-latest`**: GitHub will run this job on a fresh **Linux machine** (Ubuntu) in the cloud.

```yaml
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
```

- **`steps:`**: Inside a job, we have several **steps**, each doing one small thing.
- **`Checkout repository`**:
  - This step grabs **your code** from GitHub so the rest of the steps can work with it.
  - `uses: actions/checkout@v4` tells GitHub to use a **pre-made action** (a kind of plugin) called “checkout” in version 4.

```yaml
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: gradle
```

- **`Set up JDK 21`**:
  - Your project uses **Java 21**, so this step installs Java 21 on the machine.
  - `distribution: temurin` says which Java build to use (a common one).
  - `java-version: '21'` chooses Java 21 specifically.
  - `cache: gradle` tells GitHub to **cache Gradle dependencies**, so future builds are faster (it won’t re-download everything each time).

```yaml
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
```

- **`Grant execute permission for gradlew`**:
  - `gradlew` is the script that runs Gradle in your project.
  - On Linux, a file must have “execute permission” to be run like a program.
  - `chmod +x gradlew` gives that permission.

```yaml
      - name: Build and test with Gradle
        run: ./gradlew clean build --no-daemon
```

- **`Build and test with Gradle`**:
  - This is the actual command that **builds your project and runs tests**.
  - `./gradlew` runs Gradle using the wrapper that comes with your repo.
  - `clean` clears old build files.
  - `build` compiles the code and runs the tests.
  - `--no-daemon` tells Gradle not to keep a background process running (good for one-time CI jobs).

In simple words:  
> *This first job makes sure the code compiles and all tests pass on a clean machine every time code changes.*

---

### 3. Second job – Build and Publish the Docker Image

```yaml
  docker-image:
    name: Build & Publish Docker Image
    needs: build-and-test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
```

- **`docker-image:`**: This is the **second job**, which handles **Docker image creation and upload**.
- **`name: Build & Publish Docker Image`**: Human‑friendly name.
- **`needs: build-and-test`**:
  - This means: **do not run this job unless the `build-and-test` job finished successfully**.
  - So, if build or tests fail, Docker image **won’t** be built.
- **`if: github.ref == 'refs/heads/main'`**:
  - This is a condition: **only run this job if the code is on the `main` branch**.
  - So, pull requests or feature branches will run build/tests, but **will not** push Docker images.
- **`runs-on: ubuntu-latest`**: Same as before: use a Linux machine.

```yaml
    permissions:
      contents: read
      packages: write
```

- **`permissions`**:
  - `contents: read` means this job can **read the repository content**.
  - `packages: write` means it can **publish packages** (in this case, the Docker image) to GitHub’s package registry.

```yaml
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
```

- This is the same as in the first job: **get your code** so that Docker can build it.

```yaml
      - name: Set up QEMU
        uses: docker/setup-qemu-action@v3
```

- **`Set up QEMU`**:
  - QEMU helps build Docker images for **different CPU types** (for example, Intel and ARM).
  - This makes your image more portable (optional but useful).

```yaml
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
```

- **`Set up Docker Buildx`**:
  - Buildx is an improved Docker builder with more features.
  - This step turns it on so the next step can use it.

```yaml
      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
```

- **`Log in to GitHub Container Registry`**:
  - Before you can upload Docker images to GitHub’s registry, you must **log in**.
  - `registry: ghcr.io` is the address of **GitHub Container Registry**.
  - `username: ${{ github.actor }}` uses the GitHub user that triggered the workflow.
  - `password: ${{ secrets.GITHUB_TOKEN }}` uses a **built-in secret token** that GitHub generates for the workflow.  
    This token has permission (thanks to `packages: write` earlier) to push images.

```yaml
      - name: Build and push Docker image
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: |
            ghcr.io/prabhakaran77/codewish-backend:latest
            ghcr.io/prabhakaran77/codewish-backend:${{ github.sha }}
```

- **`Build and push Docker image`**:
  - This step actually builds the Docker image **from your repo’s `Dockerfile`** and pushes it to GitHub’s container registry.
  - `uses: docker/build-push-action@v6` is another ready-made action from Docker to handle building and pushing.
  - `context: .` means:
    - “Use the **current folder** (the whole repository) as the build context for Docker.”
  - `push: true` means:
    - “After building the image, **upload** (push) it to the registry.”
  - `tags:` defines **names for the image**:
    - `ghcr.io/prabhakaran77/codewish-backend:latest`
      - This is the “latest” version tag, easy to use when you always want the newest build.
    - `ghcr.io/prabhakaran77/codewish-backend:${{ github.sha }}`
      - This tag includes the **specific Git commit ID** (`github.sha`), which makes it easy to know exactly which version of the code the image came from.

In simple words:  
> *If the build and tests passed on the main branch, this job builds a Docker image and uploads it to GitHub’s container registry under a predictable name.*

---

## 4. What CI/CD Value You Get Now (Non‑Technical Summary)

- Every time someone pushes to `main` or opens a PR into `main`:
  - GitHub automatically:
    - **Compiles the code**.
    - **Runs all tests**.
    - **Fails fast** if something is broken.
- Every time code is pushed to `main` and passes tests:
  - GitHub also:
    - **Builds a fresh Docker image** using the `Dockerfile` in the repo.
    - **Publishes the image** to GitHub Container Registry, so it can be used by your deployment platform.

This means:
- You don’t have to build locally and manually upload images.
- You can configure your server (or Render, Kubernetes, etc.) to always pull:
  - `ghcr.io/prabhakaran77/codewish-backend:latest`  
  and get the newest successful version of your backend automatically.
Even if you’re not technical, you can think of this as:
> *“Whenever we change the code, a robot checks that it still works and then packages it up neatly so our servers can use the newest version without us doing anything manually.”*

---

## 5. Silly / “Lehman” Questions and Answers

This section is for the kind of questions a non‑technical person (or a very honest technical person) might ask.

### Q1. What happens if I delete this `.yml` file?

- If you delete `.github/workflows/ci.yml` and push that change:
  - GitHub will **stop running** these automatic checks and builds.
  - Your code will still exist and can still be run manually on your machine, but:
    - No more automatic build/test when you push.
    - No more automatic Docker image creation.

In short: the app doesn’t disappear, but your **safety net and automation** do.

---

### Q2. Can I change the `name: CI / CD - backend` to something funny?

```yaml
name: CI / CD - backend
```

- **Is it crucial?** No.
- You can change it to:
  - `name: My Super Cool Robot`
  - or anything you like.
- It only affects **what you see** in the GitHub Actions UI, not how it runs.

So yes, you can rename it freely; it won’t break the pipeline.

---

### Q3. What if my internet is off – does CI/CD still run?

- CI/CD runs on **GitHub’s servers in the cloud**, not on your laptop.
- If **you** are offline:
  - You simply can’t push code.
  - But as soon as you push (when you’re back online), GitHub picks it up and runs the workflow.
- CI/CD is like a robot in a remote workshop: it only reacts when you send it new code.

---

### Q4. Which parts are “must‑have” and which are just “nice names”?

Here’s a quick guide:

- **Crucial settings (don’t break them without knowing what you’re doing):**
  - `on:` block  
    - Controls **when** the workflow runs. If you remove or misconfigure it, the workflow might **never run**.
  - `jobs:` and each job’s **ID** (like `build-and-test`, `docker-image`)  
    - The IDs are used internally. They should:
      - Be unique inside this file.
      - Have no spaces.
  - `runs-on:`  
    - Tells GitHub what kind of machine to use (e.g., `ubuntu-latest`).  
    - If you put something invalid, the job will fail before it starts.
  - `uses:` vs `run:`  
    - `uses:` must point to real GitHub Actions (like `actions/checkout@v4`).  
    - `run:` must contain valid shell commands (like `./gradlew clean build`).
  - Docker `tags:` format and registry name  
    - Must be lowercase and a valid Docker image name, e.g. `ghcr.io/prabhakaran77/codewish-backend:latest`.

- **Flexible / cosmetic settings:**
  - Top‑level `name:` for the workflow.
  - `name:` for jobs and steps.
  - Exact wordings of step names (“Build & Test”, “Checkout repository”, etc.).

If in doubt:  
> Names are mostly cosmetic; IDs, machine types, commands, and image names are structural.

---

### Q5. What is `runs-on`, and what choices do I have?

```yaml
runs-on: ubuntu-latest
```

- **What it means**:
  - “Run this job on a fresh **Ubuntu Linux** machine that GitHub provides.”
- **Other common options:**
  - `windows-latest` – use a Windows machine.
  - `macos-latest` – use a macOS machine.
- **Self‑hosted runners:**
  - You can also set `runs-on: self-hosted` (optionally with extra labels).
  - That means:
    - Instead of using GitHub’s cloud machines, you provide your own computer/server.
    - GitHub sends the work there to run.

For this project:
- `ubuntu-latest` is:
  - Free (within GitHub’s limits).
  - Simple.
  - Well‑supported for Java and Docker builds.

---

### Q6. Why do we have two jobs instead of one big one?

- Job 1: **build-and-test**
  - Purpose: “Check that the code compiles and tests pass.”
- Job 2: **docker-image**
  - Purpose: “Only if Job 1 passed **and** we are on `main`, build & push Docker image.”

Benefits:
- We **separate responsibilities**:
  - First make sure the code is healthy.
  - Then, and only then, package it.
- If tests fail, we don’t waste time building and uploading a Docker image.

---

### Q7. What does `needs: build-and-test` really do?

```yaml
needs: build-and-test
```

- It tells GitHub:
  - “This job must **wait for** the `build-and-test` job to finish successfully.”
- If `build-and-test` fails:
  - The `docker-image` job is **skipped**.

Without `needs`, the jobs might run **in parallel** or in an order you didn’t expect.

---

### Q8. Why are there two Docker image tags?

```yaml
tags: |
  ghcr.io/prabhakaran77/codewish-backend:latest
  ghcr.io/prabhakaran77/codewish-backend:${{ github.sha }}
```

- `...:latest`:
  - Always points to **the newest successful build** from `main`.
  - Easy for deployments that just want “whatever is current”.
- `...:${{ github.sha }}`:
  - The long string (`github.sha`) is the **exact Git commit ID**.
  - This gives a **permanent, precise version**.
  - Useful when you want to know exactly which code a running container came from.

Think of it as:
- `latest` = “current book edition on the shelf”.
- `sha` tag = “the specific print run and page layout of one particular copy”.

---

### Q9. What happens if tests fail?

- If tests fail in **build-and-test**:
  - GitHub marks the workflow as **failed**.
  - The **docker-image job will not run** (because of `needs: build-and-test`).
  - No new Docker image is pushed.
- This is good:
  - You avoid deploying a broken version of the app.

---

### Q10. Can I add more steps, like sending a Slack message?

- Yes.
- You can add extra steps at the end of a job, for example:
  - To send a message to Slack or Teams.
  - To upload test reports.
  - To notify people by email.
- As long as:
  - The syntax is valid YAML.
  - You use real GitHub Actions or valid `run:` commands.

This workflow is a **starting point**. You can extend it later as your process grows more advanced.

---

## 6. Extra Concept Help – What is CI vs CD here?

Sometimes the terms are confusing, so here’s how they map to **this** file:

- **CI – Continuous Integration**
  - Means: *“Every time we integrate (push) code, automatically check that it still builds and tests pass.”*
  - In our workflow, this is the **`build-and-test` job**:
    - It runs on every push/PR to `main`.
    - It compiles the code and runs tests.

- **CD – Continuous Delivery (or Deployment)**
  - Means: *“Every time the code passes all checks, automatically prepare a version that is ready to be deployed.”*
  - In our workflow, this is the **`docker-image` job**:
    - It only runs on the `main` branch after tests pass.
    - It builds a Docker image and publishes it to a registry.
  - Whether that image is **automatically deployed** or used by a **separate deployment step** is up to your environment, but:
    - This job guarantees you **always have a fresh, ready-to-deploy image**.

So:
- CI = “automatic checking”
- CD = “automatic packaging (and possibly releasing)”

Our file does **both**: first check (CI), then package (CD).

---

## 7. Visual Diagrams (Mermaid)

### 7.1 High-Level Flow: From Code Push to Docker Image

```mermaid
flowchart TD
    A[Developer pushes code to main or opens PR] --> B[GitHub detects push/PR]
    B --> C[Run 'build-and-test' job]
    C -->|Build or tests fail| D[Mark workflow as FAILED\n(No Docker image built)]
    C -->|Build and tests pass| E[Run 'docker-image' job\n(only on main branch)]
    E --> F[Build Docker image from Dockerfile]
    F --> G[Push image to GitHub Container Registry\n(ghcr.io/prabhakaran77/codewish-backend)]
    G --> H[Image ready for deployment\n(e.g. server, Render, Kubernetes)]
```

**How to read this:**
- Start at the top: “Developer pushes code…”.
- Follow the arrows:
  - GitHub sees the change.
  - Runs build/tests.
  - If that fails, it stops.
  - If that succeeds (on `main`), it builds and uploads a Docker image.
- The final box shows the image is now ready for whatever deployment system you use.

---

### 7.2 Jobs and Their Relationship

```mermaid
flowchart LR
    A[build-and-test job\n(CI)] -->|needs| B[docker-image job\n(CD)]
```

**Explanation:**
- The `docker-image` job **depends on** (`needs`) the `build-and-test` job.
- This means:
  - `build-and-test` must run **first**, and succeed.
  - Only then is `docker-image` allowed to run.

---

### 7.3 Docker Image Tags Concept

```mermaid
flowchart TD
    A[Successful build on main] --> B[Docker image built]
    B --> C[Tag 1: 'latest']
    B --> D[Tag 2: '<commit SHA>']
    C --> E[Use when you always want\n"the newest version"]
    D --> F[Use when you want a\n"precise, traceable version"]
```

**In words:**
- Every successful main-branch build produces:
  - A “latest” tag for convenience.
  - A commit‑based tag for traceability and debugging.



