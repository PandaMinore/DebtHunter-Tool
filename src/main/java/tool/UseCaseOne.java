package tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import dataManager.DataHandler;
import dataManager.FeaturesHandler;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

public class UseCaseOne {

	public static void debtHunterLabeling (String[] args) throws Exception {
			
		FilteredClassifier binaryClassifier = (FilteredClassifier) weka.core.SerializationHelper.read("/preTrainedModels/DebtHunterBinaryClassifier.model");
		FilteredClassifier multiClassifier = (FilteredClassifier) weka.core.SerializationHelper.read("/DebtHunterMultiClassifier/multiClassifier.model");
		
		//TODO: add path where the user download DebtHunter
		String userPath = "";

		//create training sets and than test sets from user's data
		String path = userPath + "/DebtHunter/datasets/technical_debt_dataset.csv";

		Map<String, String> comments = DataHandler.loadFile(path);
		Instances instances = DataHandler.creatingArff(comments);

		// it divides 70-30 with instances chooses at random
		Instances[] data = DataHandler.divideRandom(instances);
		System.out.println(instances.numInstances() + " " + data[0].numInstances() + " " + data[1].numInstances()); // data, training, test
		Instances training = data[0];

		Instances binTraining = FeaturesHandler.Binarization(training, true);
		Instances multiTraining = FeaturesHandler.removeClass(training);
		
		
		String testPath = args[0];
		
		Instances test = DataHandler.readFile(testPath);
		test.setClassIndex(test.numAttributes() - 1);
		Instances binTest = FeaturesHandler.Binarization(test, true);
		Instances multiTest = FeaturesHandler.removeClass(test);
		
		// 1st STEP
		Evaluation eval1 = new Evaluation(binTraining); 
		eval1.evaluateModel(binaryClassifier, binTest);
		System.out.println(eval1.toSummaryString());
		System.out.println(eval1.toClassDetailsString());
		
		//10 fold CV is not performed
		double[] binaryfMeasure = new double[10];
		
		//save binary classifier statistics
		List<Object> classes = Collections.list(binTraining.classAttribute().enumerateValues());
		List<String> header = new ArrayList<>();
		header.add("classifier");
		Classification.createHeader(header, classes);
		List<Object> results = new ArrayList<>();
		results.add("YourPreTrainerModel");
		Classification.createResults(results, eval1, classes, binaryfMeasure);
		
		
		// 2nd STEP
		Evaluation eval2 = new Evaluation(multiTraining);
		eval2.evaluateModel(multiClassifier, multiTest);
		System.out.println(eval2.toSummaryString());
		System.out.println(eval2.toClassDetailsString());
		
		//10 fold CV is not performed
		double[] multifMeasure = new double[10];
		
		
		
		File file = new File("/output_twoSteps.csv");
		boolean exists = file.exists(); // true if the csv does not already exists
		
		if (!exists) {
			Classification.evaluatorTwoStepFirst(eval2, multiTraining, header, results, multifMeasure);
		} else {
			Classification.evaluatorTwoStep(eval2, multiTraining, header, results, multifMeasure);
		}
		
		//TODO: save the comments with their labels
		
	}

	// test set path is args[0],  user pre-trained models path is args[1] and args[2], training sets path is args[3] and args[4]
	public static void userClassifierLabeling (Boolean twoStep, String[] args) throws Exception {
		
		String testPath = args[0];
		String modelPath1 = args[1];
		String modelPath2 = args[2];
		String binTrainingPath = args[3];
		String multiTrainingPath = args[4];
		
		
		Instances test = DataHandler.readFile(testPath);
		test.setClassIndex(test.numAttributes() - 1);
		Instances binTest = FeaturesHandler.Binarization(test, true);
		Instances multiTest = FeaturesHandler.removeClass(test);
		
		Instances binTraining = DataHandler.readFile(binTrainingPath);
		binTraining.setClassIndex(test.numAttributes() - 1);
		
		Instances multiTraining = DataHandler.readFile(multiTrainingPath);
		multiTraining.setClassIndex(test.numAttributes() - 1);
		
		FilteredClassifier userClassifier1 = (FilteredClassifier) weka.core.SerializationHelper.read(modelPath1);
		FilteredClassifier userClassifier2 = (FilteredClassifier) weka.core.SerializationHelper.read(modelPath2);

		
		if (twoStep) {
			
			// 1st STEP
			Evaluation eval1 = new Evaluation(binTraining);
			eval1.evaluateModel(userClassifier1, binTest);
			System.out.println(eval1.toSummaryString());
			System.out.println(eval1.toClassDetailsString());
			
			//10 fold CV is not performed
			double[] binaryfMeasure = new double[10];
			
			//save binary classifier statistics
			List<Object> classes = Collections.list(binTraining.classAttribute().enumerateValues());
			List<String> header = new ArrayList<>();
			header.add("classifier");
			Classification.createHeader(header, classes);
			List<Object> results = new ArrayList<>();
			results.add("YourPreTrainerModel");
			Classification.createResults(results, eval1, classes, binaryfMeasure);
			
			
			//2nd STEP
			Evaluation eval2 = new Evaluation(multiTraining);
			eval2.evaluateModel(userClassifier2, multiTest);
			System.out.println(eval2.toSummaryString());
			System.out.println(eval2.toClassDetailsString());
			
			//10 fold CV is not performed
			double[] multifMeasure = new double[10];
			
			
			
			File file = new File("/output_twoSteps.csv");
			boolean exists = file.exists(); // true if the csv does not already exists
			
			if (!exists) {
				Classification.evaluatorTwoStepFirst(eval2, multiTraining, header, results, multifMeasure);
			} else {
				Classification.evaluatorTwoStep(eval2, multiTraining, header, results, multifMeasure);
			}
			
			//TODO: save the comments with their labels
			
			
		}
	}

}
