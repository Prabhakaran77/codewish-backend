## Machine Learning – Notes (DeepLearning.AI)

### 1. What is Machine Learning?

**Definition (Arthur Samuel)**  
Machine Learning is the field of study that gives computers the ability to learn without being explicitly programmed.

Instead of writing exact rules for every situation, we let the computer **learn patterns from data**.

**Simple idea**  
Machine Learning learns a mapping from **input (X)** to **output (Y)**.

**Examples**

- **Input (X) → Output (Y)**
  - Email → Spam / Not Spam
  - Audio → Text
  - House Size → Price
  - Image → Object label

---

### 2. Real-World Applications of Machine Learning

Machine learning is used everywhere:

- **Search Engines**
  - Google, Bing, Baidu use ML to rank web pages.
- **Image Recognition**
  - Instagram / Snapchat detect and tag people in photos.
- **Recommendation Systems**
  - Netflix / streaming platforms recommend similar movies or shows.
- **Speech Recognition**
  - Voice assistants like:
    - Siri
    - Google Assistant  
    convert speech → text or commands.
- **Spam Detection**
  - Email services detect spam emails automatically.

**Industrial Applications**

- **Climate & Energy** – Optimize wind turbine power generation.
- **Healthcare** – Help doctors with accurate diagnoses.
- **Manufacturing** – Computer vision detects defects in products.

---

### 3. Why Machine Learning is Important

Some tasks are too complex to program manually, for example:

- Web search
- Speech recognition
- Self-driving cars
- Medical diagnosis
- Image recognition

So instead of programming rules, we let machines **learn from data**.

---

### 4. Economic Impact of Machine Learning

According to McKinsey, AI and Machine Learning could create **\$13 trillion in value annually by 2030**.

Industries impacted include:

- Retail
- Healthcare
- Transportation
- Manufacturing
- Agriculture
- E-commerce

---

### 5. Artificial General Intelligence (AGI)

**AGI** = Machines as intelligent as humans, with abilities like:

- Reasoning
- Learning
- Understanding

Researchers believe:

- AGI is still far away (maybe decades or centuries).
- Current progress mainly comes from **machine learning algorithms**, not true AGI.

---

### 6. Main Types of Machine Learning

Two main types:

1. **Supervised Learning**
2. **Unsupervised Learning**

Later, we also include:

- Recommender Systems
- Reinforcement Learning

---

### 7. Supervised Learning

**Definition**  
Supervised learning learns **input → output mappings (X → Y)** using **labeled data**.

The dataset contains:

- Input (X)
- Correct Output (Y)

**Examples**

| Input  | Output          |
| ------ | --------------- |
| Email  | Spam / Not Spam |
| Audio  | Text transcript |
| English text | Translated text |
| Image  | Object label    |

The model learns from these examples.

After training:

- New Input (X) → Predict Output (Y)

---

### 8. Types of Supervised Learning

There are two main types.

#### 8.1 Regression

Regression predicts **continuous numeric values**.

**Examples**

- House price prediction
- Temperature prediction
- Sales forecasting

Example problem:

- Input: House size  
- Output: House price

Possible outputs:

- \$150,000
- \$175,500
- \$210,000

Numbers can be **any value**, so there are **infinite possibilities**.

#### 8.2 Classification

Classification predicts **categories or classes**.

**Example: Breast cancer detection**

- Inputs:
  - Tumor size
  - Patient age
- Output:
  - Benign (0)
  - Malignant (1)

Possible outputs are **limited categories**.

**More classification examples**

- Spam / Not Spam
- Cat / Dog
- Fraud / Not Fraud
- Cancer type

**Multiple Inputs (Features)**

Models can use many features.

Example (cancer detection):

- Tumor size
- Patient age
- Cell shape
- Cell thickness
- Gene expression

More features → **better predictions** (if they are relevant).

---

### 9. Unsupervised Learning

**Definition**  
Unsupervised learning uses data **without labels**.

The dataset contains:

- Input (X) only  
- No output (Y)

Goal:

- Find **patterns or structure** in the data.

---

### 10. Clustering (Unsupervised Learning)

Clustering groups **similar data points** together.

**Example: Google News**

- Groups articles about the same topic into one cluster.

Example cluster:

- Panda birth
- Panda twins
- Zoo news

The algorithm automatically groups related articles.

**DNA Analysis Example**

- Clustering can group people based on gene activity.
- This helps researchers discover:
  - Different genetic groups
  - Disease patterns

**Market Segmentation**

Companies cluster customers based on behavior.

Example customer groups:

1. Learning new skills  
2. Career growth  
3. Staying updated with AI  

Businesses use this to **target customers better**.

---

### 11. Other Types of Unsupervised Learning

Besides clustering, two important techniques are:

1. **Anomaly Detection**
2. **Dimensionality Reduction**

#### 11.1 Anomaly Detection

Detects **unusual patterns**.

Used in:

- Fraud detection
- Cybersecurity
- System failure detection

Example:

- Unusual credit card transaction → possible fraud.

#### 11.2 Dimensionality Reduction

Reduces the number of features while keeping important information.

**Purpose**

- Simplify data
- Speed up algorithms
- Help with visualization

Example:

- Convert 1000 features → 10 features while preserving patterns.

---

### 12. Tools Used in Machine Learning

#### Jupyter Notebook

Most common environment used by:

- ML engineers
- Data scientists
- Researchers

**Features**

- Write Python code
- Run experiments
- Visualize results

**Two Types of Cells**

1. **Markdown Cell**
   - Contains text and explanations.
   - Example:

```markdown
# Linear Regression
This algorithm predicts numeric values.
```

2. **Code Cell**
   - Contains Python code.
   - Example:

```python
x = 5
print(x)
```

- Run cell with: **Shift + Enter**

---

### 13. Types of Labs in the Course

**Optional Labs**

- No coding required
- Just run the code
- Help understand ML concepts

**Practice Labs**

- You write code yourself
- Implement ML algorithms

---

### Quick Summary (Revision)

- **Machine Learning** = computers learn from data instead of explicit programming.

- **Supervised Learning**
  - Uses labeled data \((X, Y)\).
  - Types:
    - **Regression** → predicts numbers.
    - **Classification** → predicts categories.
  - Examples:
    - Spam detection
    - Speech recognition
    - House price prediction

- **Unsupervised Learning**
  - Uses unlabeled data \((X \text{ only})\).
  - Goal → find patterns.
  - Types:
    - **Clustering**
    - **Anomaly Detection**
    - **Dimensionality Reduction**

- **Common ML Tool**
  - **Jupyter Notebook**:
    - Used for running Python code and experimenting with ML algorithms.

