package tool;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;

public class UseCaseOne {
	
	public static Instances assignLabel(Instances test, FilteredClassifier fc1, FilteredClassifier fc2) throws Exception {
		
		// create new feature
		Add add = new Add();
		add.setAttributeIndex("last");
		add.setNominalLabels("TEST, IMPLEMENTATION, DESIGN, DEFECT, DOCUMENTATION, WITHOUT_CLASSIFICATION");
		add.setAttributeName("classification");
		add.setInputFormat(test);
		test = Filter.useFilter(test, add);
		
		// set Classification value into SATD types {TEST,IMPLEMENTATION,DESIGN,DEFECT,DOCUMENTATION} or non-SATD
		for (int i = 0; i < test.numInstances(); i++) {

			// get labels assigned by binary classifier
			Double tmp = fc1.classifyInstance(test.instance(i));
			
			if (tmp == 2.0) {
				test.instance(i).setValue(test.numAttributes() - 1, "WITHOUT_CLASSIFICATION");
				
			} else {
				
				// get labels assigned by multi-class classifier
				Double tmp2 = fc2.classifyInstance(test.instance(i));
				
				if (tmp2 == 0.0) {
					test.instance(i).setValue(test.numAttributes() - 1, "TEST");

				} else if (tmp2 == 1.0) {
					test.instance(i).setValue(test.numAttributes() - 1, "IMPLEMENTATION");

				} else if (tmp2 == 3.0) {
					test.instance(i).setValue(test.numAttributes() - 1, "DESIGN");

				} else if (tmp2 == 4.0) {
					test.instance(i).setValue(test.numAttributes() - 1, "DEFECT");

				} else if (tmp2 == 5.0) {
					test.instance(i).setValue(test.numAttributes() - 1, "DOCUMENTATION");

				}
				
			}
			
			System.out.println(test.instance(i) + " " + tmp);
		}
		
		return test;
		
	}
	
	public static Instances generateMultiTest (Instances test, FilteredClassifier fc) throws Exception {
		
		// remove non-SATD instances from test set
		for (int i = test.numInstances() - 1; i >= 0; i--) {

			// get original class
			Double tmp = fc.classifyInstance(test.instance(i));
			
			// 2.0 = non-SATD
			if (tmp == 2.0) {
				
				test.delete(i);
				
			} else {
				
				System.out.println(test.instance(i) + " " + tmp);
				
			}
		}
		
		
		return test;
	}

	public static Instances debtHunterLabeling (Instances test) throws Exception {
		
		System.out.println("You chose the DebtHunter pre-trained model. Good choice!");
		
		FilteredClassifier binaryClassifier = (FilteredClassifier) weka.core.SerializationHelper.read("/preTrainedModels/DHbinaryClassifier.model");
		FilteredClassifier multiClassifier = (FilteredClassifier) weka.core.SerializationHelper.read("/preTrainedModels/DHmultiClassifier.model");
		

		// assign labels to all comments
		test = assignLabel(test, binaryClassifier, multiClassifier);
		
		System.out.println("I labeled the comments! Now I save them!");
		
		return test;
		
	}

	public static Instances userClassifierLabeling (Boolean twoStep, Instances test, String modelPath1, String modelPath2) throws Exception {
		
		System.out.println("I'm using your pre-trained model.");
		
		if (twoStep) {
			
			//remove the first part of the path
			String target = "/preTrainedModels/";
			int startIndex = 0;
			int stopIndex = modelPath1.indexOf(target);
			String endPath1 = DataHandler.pathModifier(modelPath1, startIndex, stopIndex);
			
			stopIndex = modelPath2.indexOf(target);
			String endPath2 = DataHandler.pathModifier(modelPath2, startIndex, stopIndex);
			
			
			FilteredClassifier binaryClassifier = (FilteredClassifier) weka.core.SerializationHelper.read(endPath1);
			FilteredClassifier multiClassifier = (FilteredClassifier) weka.core.SerializationHelper.read(endPath2);
			
			// assign labels to all comments
			test = assignLabel(test, binaryClassifier, multiClassifier);

			System.out.println("I labeled the comments! Now I save them!");
	
		}
		return test;
	}

}
