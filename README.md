# DebtHunter Tool

DebtHunter help developers to manage Self-Admitted Technical Debt (SATD) more effectively. DebtHunter can not only mine source code comments in Java projects, but also associated issue trackers, to capture debt at an architecture level. The classification model implemented can be updated with already labeled data.

## How to use
1. Download DebtHunter-tool.jar
2. Open cmd in the fold where you download the jar
3. choose one of two tool use cases form:

### Label your Java projects comments with DebtHunter model or your pre-trained classifier
It is advisable to put the chosen project folder in the same folder as DebtHunter-tool.jar. In *Resources* directory is possible to find a project example (*sampleDirectory*) to be anlysed.

4. Download *preTrainedModels* directory (if you wanna use DebtHunter pre-trained models) in the same folder as DebtHunter-tool.jar.

5. for example, you can execute in cmd:

    - Using DebtHunter pre-trained models:
    ```
    java -jar DebtHunter-tool.jar -u first -p ./JavaProject/path
    ```
    - Using your pre-trained models:
    ```
    java -jar DebtHunter-tool.jar -u first -p ./JavaProject/path -m1 ./binaryModel/path -m2 ./multi-classModel/path
    ```

### Label your issue (from Jira) with DebtHunter model or your pre-trained classifier

4. for example, you can execute in cmd:

    - Using DebtHunter pre-trained models:
    ```
    java -jar DebtHunter-tool.jar -u first -j    
    ```
    - Using your pre-trained models:
    ```
    java -jar DebtHunter-tool.jar -u first -j    -m1 ./binaryModel/path -m2 ./multi-classModel/path
    ```

### Train your classifier
4. Download *datasets* folder in the same folder as DebtHunter-tool.jar.
5. Put your training data into *datasets* folder.
6. for example, you can execute in cmd:
```
java -jar DebtHunter-tool.jar -u second -l ./datasets/training.arff
```
----------------------------------------------------------------
You can see all commands with the help command:

```
java -jar DebtHunter-tool.jar -h
```
or
```
java -jar DebtHunter-tool.jar -help
```

### Requirements
- Weka 3.8.0 or above
