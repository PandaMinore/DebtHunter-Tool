# DebtHunter Tool

DebtHunter helps developers to manage Self-Admitted Technical Debt (SATD) more effectively. DebtHunter can not only mine source code comments in Java projects but also associated issue trackers, to capture debt at an architecture level. The classification model implemented can be updated with already labeled data.

## How to use
1. Download DebtHunter-tool.jar
2. Create in the same fold a directory named output
3. Open cmd in the fold where you download the jar
4. choose one of two tool use cases form:

### Label your Java projects comments with DebtHunter model or your pre-trained classifier
In the *Resources* directory is possible to find a Java project example (*sampleDirectory*) to be analysed. You can also use the pre-trained models in the *preTrainedModels* directory for testing the tool using custom pre-trained models.

5. for example, you can execute in cmd:

    - Using DebtHunter pre-trained models:
    ```
    java -jar DebtHunter-tool.jar -u first -p ./SampleDirectory -o ./output
    ```
    - Using your pre-trained models:
    ```
    java -jar DebtHunter-tool.jar -u first -p ./SampleDirectory -m1 ./preTrainedModels/DHbinaryClassifier.model -m2 ./preTrainedModels/DHmultiClassifier.model -o ./output
    ```

### Label your issue (from Jira) with DebtHunter model or your pre-trained classifier

5. for example, you can execute in cmd:

    - Using DebtHunter pre-trained models:
    ```
    java -jar DebtHunter-tool.jar -u first -pv 2.0.0 -jp CAMEL -c camel-core -bu https://issues.apache.org/jira -o ./output
    ```
    - Using your pre-trained models:
    ```
    java -jar DebtHunter-tool.jar -u first -pv 2.0.0 -jp CAMEL -c camel-core -bu https://issues.apache.org/jira -m1 ./binaryModel/path -m2 ./multi-classModel/path -o ./output
    ```

### Train your classifier
In the *datasets* folder you can find example labeled data to provide in the input (you can download *trining.arff*). Please use as training data a .arff file.

5. for example, you can execute in cmd:
```
java -jar DebtHunter-tool.jar -u second -l ./datasets/labeledComments.arff -o ./output
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

## Dependency:
-Java jdk 15.0.1
