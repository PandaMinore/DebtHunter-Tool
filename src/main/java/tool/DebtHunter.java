package tool;

import java.io.File;

import weka.core.Instances;
import weka.core.converters.ArffLoader;

public class DebtHunter {

	public static void main(String[] args) throws Exception {
		
		
		// args[0] = "first" or "second" use case
		String useCase = "second";
		
		// args[1] = test set path, args[2] = 1st model (binary) path, args[3] = 2nd model (multi-class) path
		if (useCase == "first") {
			
			Boolean userGivesClassifiers = false;
			
			// TODO: parse user data. For now I use an example test set
			// TODO: creare il test set ad hoc per testare il tool
			
			
			String TestPath = "C:/Users/irene/eclipse-workspace/SATD/datasets/testData.arff"; //args[1]
			String target = "/datasets/";
			int startIndex = 0;
			int stopIndex = TestPath.indexOf(target);
			String endPath = DataHandler.pathModifier(TestPath, startIndex, stopIndex);
			
			
			ArffLoader loader = new ArffLoader();
			loader.setSource(new File(endPath)); // last part of test set path
			Instances test = loader.getDataSet();
			
			
			// if the user chooses the first use case and not provides a pre-trained model
			if(!userGivesClassifiers) {
				
				UseCaseOne.debtHunterLabeling(TestPath, test);
			
			// The user chooses the first use case and provides 2 pre-trained models in input
			} else if(userGivesClassifiers) {
				
				Boolean twoStep = true;
				
				String modelPath1 = "C:/Users/irene/eclipse-workspace/SATD/preTrainedModels/DHbinaryClassifier.model"; //args[2];
				String modelPath2 = "C:/Users/irene/eclipse-workspace/SATD/preTrainedModels/DHmultiClassifier.model"; //args[3];
				
				UseCaseOne.userClassifierLabeling(twoStep, TestPath, test, modelPath1, modelPath2);
				
			}
			
		// The user provides labeled data (training set).
		// args[1] = training set path
		} else if (useCase == "second") {
			
			// example training set path
			String TrainingPath = "C:/Users/irene/eclipse-workspace/SATD/datasets/training.arff"; //args[1]

			String target = "/datasets/";
			int startIndex = 0;
			int stopIndex = TrainingPath.indexOf(target);
			String endPath = DataHandler.pathModifier(TrainingPath, startIndex, stopIndex);
			
			ArffLoader loader = new ArffLoader();
			loader.setSource(new File(endPath)); // last part of training set path
			Instances training = loader.getDataSet();
			training.setClassIndex(training.numAttributes() - 1);

			// binarization
			Instances binTraining = DataHandler.binarization(training);

			// remove non-SATD instances
			Instances multiTraining = DataHandler.removeClass(training);

			UseCaseTwo.trainModel(binTraining, multiTraining, TrainingPath);
			
			
		}
		
	}

}
