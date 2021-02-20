package tool;

import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;

public class UseCaseTwo {

	// TODO: passare come argomento il percorso d'output da richiedere all'utente
	public static void trainModel(Instances binTraining, Instances multiTraining, String path, String outputPath) throws Exception {
		
		SMO binaryClassifier = new SMO();
		binaryClassifier.setRandomSeed(8);
		RBFKernel kernel = new RBFKernel();
		binaryClassifier.setKernel(kernel);
		
		SMO multiClassifier = binaryClassifier;
		
		//1st step: Binary
		System.out.println("1st step: Paramenters optimization started.");

		binaryClassifier = (SMO) Classification.parametersOptimization(binTraining, binaryClassifier, "DebtHunter", "binary");
		
		System.out.println("1st step: Training phase started.");

		FilteredClassifier binaryClass = Classification.train(binTraining, binaryClassifier, "binary"); 

		
		//2nd step: Multi-class
		System.out.println("2nd step: Paramenters optimization started.");

		multiClassifier = (SMO) Classification.parametersOptimization(multiTraining, multiClassifier, "DebtHunter", "multi");
		
		System.out.println("2nd step: Training phase started.");

		FilteredClassifier multiClass = Classification.train(multiTraining, multiClassifier, "multi");
		
		String binaryPath = outputPath + "/binaryClassifier.model";
		String multiPath = outputPath + "/multiClassifier.model";

		//save pre-trained models
		weka.core.SerializationHelper.write(binaryPath, binaryClass);
		weka.core.SerializationHelper.write(multiPath, multiClass);
		
		System.out.println("Models training ended!");

	}

}
