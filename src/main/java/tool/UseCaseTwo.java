package tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import dataManager.DataHandler;
import dataManager.FeaturesHandler;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;

public class UseCaseTwo {

	public static void trainModel(String[] args) throws Exception {
		// accept in input the user data
		String path = args[0];
		
		Map<String, String> comments = DataHandler.loadFile(path);
		Instances instances = DataHandler.creatingArff(comments);

		// divide 70-30 with instances chooses at random
		Instances[] data = DataHandler.divideRandom(instances);
		Instances training = data[0];
		Instances test = data[1];
		System.out.println(instances.numInstances() + " " + training.numInstances() + " " + test.numInstances());
		
		
		// transform data in binary-class
		Instances binTraining = FeaturesHandler.Binarization(training, true);
		Instances binTest = FeaturesHandler.Binarization(test, true);
		
		// multi-class classification: remove records in WITHOUT_CLASSIFICATION class
		Instances multiTraining = FeaturesHandler.removeClass(training);
		Instances multiTest = FeaturesHandler.removeClass(test);
		
	
		SMO classifier = new SMO();
		classifier.setRandomSeed(8);
		RBFKernel kernel = new RBFKernel();
		classifier.setKernel(kernel);
		
		//1st step: Binary
		//grid search
		classifier = (SMO) Classification.parametersOptimization(binTraining, classifier, "DebtHunter", "binary");
		
		//10 fold Cross Validation
		double[] binaryfMeasure = Classification.tenFoldCV(binTraining, classifier, "DebtHunter", "binary");
		
		FilteredClassifier binaryClass = Classification.train(binTraining, classifier, "binary"); 
		Evaluation eval1 = new Evaluation(binTraining); 
		eval1.evaluateModel(binaryClass, binTest);
		System.out.println(eval1.toSummaryString());
		System.out.println(eval1.toClassDetailsString());
		
		//save binalry classifier statistics
		List<Object> classes = Collections.list(binTraining.classAttribute().enumerateValues());
		List<String> header = new ArrayList<>();
		header.add("classifier");
		Classification.createHeader(header, classes);
		List<Object> results = new ArrayList<>();
		results.add("DebtHunter");
		Classification.createResults(results, eval1, classes, binaryfMeasure);

		
		//2nd step: Multi-class
		//grid search
		classifier = (SMO) Classification.parametersOptimization(multiTraining, classifier, "DebtHunter", "multi");
		
		//10 fold Cross Validation
		double[] multifMeasure = Classification.tenFoldCV(multiTraining, classifier, "DebtHunter", "multi");
		
		FilteredClassifier multiClass = Classification.train(multiTraining, classifier, "multi");
		Evaluation eval2 = new Evaluation(multiTraining);
		eval2.evaluateModel(multiClass, multiTest);
		System.out.println(eval2.toSummaryString());
		System.out.println(eval2.toClassDetailsString());
		
		
		
		File file = new File("/output_twoSteps.csv");
		boolean exists = file.exists(); // true if the csv does not already exists
		
		if (!exists) {
			Classification.evaluatorTwoStepFirst(eval2, multiTraining, header, results, multifMeasure);
		} else {
			Classification.evaluatorTwoStep(eval2, multiTraining, header, results, multifMeasure);
		}
		
		
		
		//save pre-trained models
		weka.core.SerializationHelper.write("/preTrainedModels/binaryClassifier.model", binaryClass);
		weka.core.SerializationHelper.write("/preTrainedModels/multiClassifier.model", multiClass);
		

	}

}
