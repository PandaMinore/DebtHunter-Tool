package dataManager;

import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class FeaturesHandler {
	
	public static Instances Binarization(Instances data, Boolean removeFeature) throws Exception {
		
		// create new feature
		Add add = new Add();
		add.setAttributeIndex("last");
		add.setNominalLabels("SATD, WITHOUT_CLASSIFICATION");
		add.setAttributeName("BinaryClassification");
		add.setInputFormat(data);
		data = Filter.useFilter(data, add);
		

		// set BinaryClassification value into "SATD" if the label is one of {TEST,IMPLEMENTATION,DESIGN,DEFECT,DOCUMENTATION}
		for (int i = 0; i < data.numInstances(); i++) {

			// get original class
			Double SATDclass = data.instance(i).value(1);
			
			if (SATDclass == 2.0) {
				data.instance(i).setValue(data.numAttributes() - 1, "WITHOUT_CLASSIFICATION");
			} else {
				data.instance(i).setValue(data.numAttributes() - 1, "SATD");
			}
		}

		
		if (removeFeature) {
			// select the BinaryClassification attribute as a class target and not consider
			// Classification attribute
			Remove remove = new Remove();
			remove.setAttributeIndices("2");
			remove.setInputFormat(data);
			data = Filter.useFilter(data, remove);
			
		} 
		data.setClassIndex(data.numAttributes() - 1);
		
		return data;
	}
	
	public static Instances removeClass(Instances data) throws Exception {

		RemoveWithValues rm = new RemoveWithValues();
		rm.setInputFormat(data);
		String[] options = { "-C", "2", "-L", "3", "-H" }; // 2: 2nd column is the attribute for type of SATD.
															// 3:WITHOUT_CLASSIFICATION is the 3rd class
		rm.setOptions(options);
		data = Filter.useFilter(data, rm);

		return data;
	}
	
		
}
