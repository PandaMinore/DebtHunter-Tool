package tool;

import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;

public class UseCaseTwo {

	public static void trainModel(Instances binTraining, Instances multiTraining, String path) throws Exception {
		
		SMO binaryClassifier = new SMO();
		binaryClassifier.setRandomSeed(8);
		RBFKernel kernel = new RBFKernel();
		binaryClassifier.setKernel(kernel);
		
		SMO multiClassifier = binaryClassifier;
		
		//1st step: Binary
		binaryClassifier = (SMO) Classification.parametersOptimization(binTraining, binaryClassifier, "DebtHunter", "binary");
		FilteredClassifier binaryClass = Classification.train(binTraining, binaryClassifier, "binary"); 

		
		//2nd step: Multi-class
		multiClassifier = (SMO) Classification.parametersOptimization(multiTraining, multiClassifier, "DebtHunter", "multi");
		FilteredClassifier multiClass = Classification.train(multiTraining, multiClassifier, "multi");
		
		String target = "/datasets/";
		int startIndex = path.indexOf(target);
		int stopIndex = path.length();
		String initialPath = DataHandler.pathModifier(path, startIndex, stopIndex);
		
		//save pre-trained models
		weka.core.SerializationHelper.write(initialPath + "/preTrainedModels/binaryClassifier.model", binaryClass);
		weka.core.SerializationHelper.write(initialPath + "/preTrainedModels/multiClassifier.model", multiClass);
		

	}

}
