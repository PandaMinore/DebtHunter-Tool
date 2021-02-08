package tool;

import java.io.File;
import java.io.IOException;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class DataHandler {
	
	//required full path
	public static void saveData(Instances data, String dataName, String path) throws IOException {

		ArffSaver saver = new ArffSaver();
		saver.setInstances(data);
		saver.setFile(new File(path + dataName + ".arff"));
		saver.writeBatch();

		CSVSaver saverCSV = new CSVSaver();
		saverCSV.setInstances(data);
		saverCSV.setFile(new File(path + dataName + ".csv"));
		saverCSV.writeBatch();


	}
	
	public static Instances binarization(Instances data) throws Exception {
		
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

		
		// select the BinaryClassification attribute as a class target and not consider
		// Classification attribute
		Remove remove = new Remove();
		remove.setAttributeIndices("2");
		remove.setInputFormat(data);
		data = Filter.useFilter(data, remove);
			
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
	
	// delete the piece of string that starts from index "start" and finish to "end"
	public static String pathModifier(String path, int start, int end) {
		
		StringBuilder builder = new StringBuilder(path);
		builder.delete(start, end);
		String newPath = builder.toString();
		
		return newPath;
		
	}
	
}
