package tool;

import weka.classifiers.meta.FilteredClassifier;

public class DebtHunter {

	public static void main(String[] args) throws Exception {
		
		String useCase = "second";
		
		
		if (useCase == "second") {
			
			Boolean userGivesClassifier = false;
			// if the user chooses the first use case and not provides a pre-trained model
			if(!userGivesClassifier) {
				
				UseCaseOne.debtHunterLabeling(args);
				
			}
			
			
			// if the user chooses the first use case and provides a pre-trained model in input
			//accept in input the user's pre-trained classifier
			FilteredClassifier userClassifier = new FilteredClassifier();
			Boolean twoStep = false;
			
			if(userGivesClassifier) {
				
				UseCaseOne.userClassifierLabeling(twoStep, args);
				
			}
			
		} else {
			
			//if the user chooses the second use case
			UseCaseTwo.trainModel(args);
			
		}
		
	}

}
