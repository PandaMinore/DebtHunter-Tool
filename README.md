# DebtHunter Tool

DebtHunter help developers to manage Self-Admitted Technical Debt (SATD) more effectively. DebtHunter can not only mine source code comments in Java projects, but also associated issue trackers, to capture debt at an architecture level. The classification model implemented can be updated with already labeled data.

## How to use
1. Download DebtHunter-tool.jar
2. Open cmd in the fold where you download the jar

### Label your data with DebtHunter model or your pre-trained classifier

```
java -jar DebtHunter-tool.jar first
```

### Train your classifier

```
java -jar DebtHunter-tool.jar second
```

### Requirements
- Weka 3.8.0 or above
