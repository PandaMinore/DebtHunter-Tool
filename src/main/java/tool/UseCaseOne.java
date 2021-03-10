package tool;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;

public class UseCaseOne {
	
	private static Instances assignLabel(Instances onlyComments, Instances test, FilteredClassifier fc1, FilteredClassifier fc2) throws Exception {
		
		// create new feature
		Add add = new Add();
		add.setAttributeIndex("last");
		add.setNominalLabels("TEST, IMPLEMENTATION, DESIGN, DEFECT, DOCUMENTATION, WITHOUT_CLASSIFICATION");
		add.setAttributeName("classification");
		add.setInputFormat(test);
		test = Filter.useFilter(test, add);
		
		add = new Add();
		add.setAttributeIndex("last");
		add.setNominalLabels("TEST, IMPLEMENTATION, DESIGN, DEFECT, DOCUMENTATION, WITHOUT_CLASSIFICATION");
		add.setAttributeName("classification");
		add.setInputFormat(onlyComments);
		onlyComments = Filter.useFilter(onlyComments, add);
		onlyComments.setClassIndex(onlyComments.numAttributes() - 1);
		
		// set Classification value into SATD types {TEST,IMPLEMENTATION,DESIGN,DEFECT,DOCUMENTATION} or non-SATD
		for (int i = 0; i < test.numInstances(); i++) {

			// get labels assigned by binary classifier
			Double tmp = fc1.classifyInstance(onlyComments.instance(i));
			
			if (tmp == 1.0) {
				test.instance(i).setValue(test.numAttributes() - 1, "WITHOUT_CLASSIFICATION");
				
			} else {
				
				// get labels assigned by multi-class classifier
				Double tmp2 = fc2.classifyInstance(onlyComments.instance(i));
				
				if (tmp2 == 0.0) {
					test.instance(i).setValue(test.numAttributes() - 1, "TEST");

				} else if (tmp2 == 1.0) {
					test.instance(i).setValue(test.numAttributes() - 1, "IMPLEMENTATION");

				} else if (tmp2 == 2.0) {
					test.instance(i).setValue(test.numAttributes() - 1, "DESIGN");

				} else if (tmp2 == 3.0) {
					test.instance(i).setValue(test.numAttributes() - 1, "DEFECT");

				} else if (tmp2 == 4.0) {
					test.instance(i).setValue(test.numAttributes() - 1, "DOCUMENTATION");

				}
				
			}
			
		}
		
		return test;
		
	}
	
	public static Instances debtHunterLabeling (Instances onlyComments, Instances test) throws Exception {
		
		System.out.println("You chose the DebtHunter pre-trained models. Good choice!");
		
		FilteredClassifier binaryClassifier = (FilteredClassifier) weka.core.SerializationHelper.read("./preTrainedModels/DHbinaryClassifier.model");
		FilteredClassifier multiClassifier = (FilteredClassifier) weka.core.SerializationHelper.read("./preTrainedModels/DHmultiClassifier.model");
		

		// assign labels to all comments
		test = assignLabel(onlyComments, test, binaryClassifier, multiClassifier);
		
		System.out.println("I labeled the comments! Now I save them!");
		
		return test;
		
	}

	public static Instances userClassifierLabeling (Instances onlyComments, Instances test, String modelPath1, String modelPath2) throws Exception {
		
		System.out.println("I'm using your pre-trained models.");
			
		FilteredClassifier binaryClassifier = (FilteredClassifier) weka.core.SerializationHelper.read(modelPath1);
		FilteredClassifier multiClassifier = (FilteredClassifier) weka.core.SerializationHelper.read(modelPath2);
		System.out.println("Done!");
		
		// assign labels to all comments
		test = assignLabel(onlyComments, test, binaryClassifier, multiClassifier);

		System.out.println("I labeled the comments! Now I save them!");
	
		return test;
	}

}
