package example1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.CSVSaver;
import weka.core.stemmers.SnowballStemmer;
import weka.filters.Filter;
import weka.filters.supervised.instance.StratifiedRemoveFolds;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.filters.unsupervised.instance.RemovePercentage;

public class WekaExample {

	//project,classification,comment --> in this example I am not distinguishing between the files corresponding to the different projects
	//I return a Map<Comment,Classification>
	public static Map<String,String> loadCommentsFile(String path) throws IOException{
		
		Map<String,String> comments = new HashMap<>();
		
		Reader reader = Files.newBufferedReader(Paths.get(path));
		CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader()); //the file is a pure csv file, no strange or different format.
		
		 for (CSVRecord csvRecord : csvParser){
			 String comment = csvRecord.get(2).replaceAll("\\s", " "); //replace all new lines and tabs
			 String classification = csvRecord.get(1);
			  
			 comments.put(comment, classification);
		 }
		
		csvParser.close();
		
		return comments;
	}

	/*
	 * this is an example of how to create the processing unit for Weka, and arff
	 * This arff is a collection of Instance. Each Instance corresponds to a comment and the class.
	 * Note that this format (one feature with all text) cannot be used for the classification, as no algorithm supports String data.
	 * 
	 * Weka provides al alternative for directly loading the cvs into an Instances, however, they still have some problems with the quotation marks.
	 * Hence, for this specific dataset, I do not recommend to try it.
	 * 
	 * We could have directly created the Instances from the csv records.
	 */
	public static Instances creatingArff(Map<String,String> comments){
		
		ArrayList<Attribute> attributes = new ArrayList<>();
		attributes.add(new Attribute("comment",(ArrayList<String>)null)); //an string attribute
		attributes.add(new Attribute("classification",new ArrayList<String>(new HashSet<String>(comments.values())))); //a nominal attribute with the definition of all possible values
		
		Instances instances = new Instances("comments", attributes, comments.size());
		instances.setClassIndex(1); //for us, the class is the classification attribute
		
		for(String k : comments.keySet()){
			
			Instance i = new DenseInstance(attributes.size()); // number of attributes
			i.setDataset(instances);
			i.setValue(0, k);
			i.setClassValue(comments.get(k));
			
			instances.add(i);
		}
		return instances;
	}
	
	public static Instances[] divideStratifiedInstances(Instances data) throws Exception{
		
		StratifiedRemoveFolds filter = new StratifiedRemoveFolds(); 
		filter.setNumFolds(10); //each fold will have a 10% of the instances
		filter.setInputFormat(data); //we set the data over which the filter is going to be applied
	
		filter.setFold(1); //in this case, one fold goes for test and 9 for training. Hence we have a 90%-10% ratio, for a 80%-20% select 5 folds
		
		// apply filter for test data here
		Instances test = Filter.useFilter(data, filter);

		//  prepare and apply filter for training data here
		filter.setInvertSelection(true);     // invert the selection to get other data 
		Instances training = Filter.useFilter(data, filter);
		
		
		return new Instances[]{training,test};
	}
	
	// in this case we simply select a percentage of data, regardsless of the class of the instances
	public static Instances [] divideRandom(Instances data) throws Exception{
		
		data.randomize(new Random());
		
		RemovePercentage filter = new RemovePercentage();
		filter.setInputFormat(data);
		filter.setPercentage(30.0); // percentage we want to remove
		
		Instances training = Filter.useFilter(data, filter);
			
		Instances test = new Instances(data,0,data.numInstances()-training.numInstances());
			
		return new Instances[]{training,test};
	}
		
	/*
	 * In this example I am going to simply transform the comment into different features using the StringToWordVector, 
	 * similar to what the CountVectorizer of sklearn does.
	 * 
	 *  I am using the filter and the classifier together to avoid having to apply it to the data, as the classifier does it
	 *  alone
	 *  
	 */
	public static void simpleClassificationExample(Instances training,Instances test) throws Exception{
		
		StringToWordVector filter = new StringToWordVector();
		SnowballStemmer stemmer = new SnowballStemmer();
        stemmer.setStemmer("porter");
        filter.setStemmer(stemmer);
	    filter.setInputFormat(training);
	    filter.setIDFTransform(true);
	    
	    FilteredClassifier fc = new FilteredClassifier();
	    fc.setFilter(filter);
	    fc.setClassifier(new ZeroR()); // a majority class classifier, only to see results fast
	    fc.buildClassifier(training);
	    
	    Evaluation eval = new Evaluation(training);
	    eval.evaluateModel(fc, test);
	    
	    System.out.println(eval.toSummaryString());
	    System.out.println(eval.toClassDetailsString());
	    
	    simpleEvaluationResultsSaving(fc,eval,training); // The new method
	   
	}
	
	/*
	 * We are going to create a csv file with the header corresponding to the different Weighted metrics and metrics per class.
	 * of course you can choose any other organization for the file.
	 * 
	 * In this case, as I am performing a single classification, the output file will have only one row (besides the header), but the idea is to 
	 * keep adding rows and have all results together.
	 */
	public static void simpleEvaluationResultsSaving(Classifier c,Evaluation eval,Instances instances) throws Exception {
		
		List<Object> classes = Collections.list(instances.classAttribute().enumerateValues());
		
		List<String> header = new ArrayList<>(); //we create the header that our file will have
		header.add("combination");
		header.add("Correctly Classified Instances");
		header.add("% Correctly Classified Instances");
		header.add("Incorrectly Classified Instances");
		header.add("% Incorrectly Classified Instances");
		
		header.add("Weighted_TP Rate");
		header.add("Weighted_FP Rate");
		header.add("Weighted_Precision");
		header.add("Weighted_Recall");
		header.add("Weighted_F-Measure");
		header.add("Weighted_ROC Area");
		header.add("Weighted_PRC Area");
		
		for(Object cc : classes) {
			header.add(cc+"_TP Rate");
			header.add(cc+"_FP Rate");
			header.add(cc+"_Precision");
			header.add(cc+"_Recall");
			header.add(cc+"_F-Measure");
			header.add(cc+"_ROC Area");
			header.add(cc+"_PRC Area");
		}
		
		List<Object> values = new ArrayList<>(); //each row will be represented by a List with the values corresponding to the items in header
		
		values.add("combination XX"); //you should define this similarly as you defined the names of the pictures
		
		values.add(eval.correct());
		values.add(eval.correct()*100/eval.numInstances());
		values.add(eval.incorrect());
		values.add(eval.incorrect()*100/eval.numInstances());
		values.add(eval.weightedTruePositiveRate());
		values.add(eval.weightedFalsePositiveRate());
		values.add(eval.weightedPrecision());
		values.add(eval.weightedRecall());
		values.add(eval.weightedFMeasure());
		values.add(eval.weightedAreaUnderROC());
		values.add(eval.weightedAreaUnderPRC());
		
		for(int i=0;i<classes.size();i++) {
			values.add(eval.truePositiveRate(i));
			values.add(eval.falseNegativeRate(i));
			values.add(eval.precision(i));
			values.add(eval.recall(i));
			values.add(eval.fMeasure(i));
			values.add(eval.areaUnderROC(i));
			values.add(eval.areaUnderPRC(i));
		}
		
		BufferedWriter writer = Files.newBufferedWriter(Paths.get("output_example.csv"));
		CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(header.toArray(new String[header.size()]))); //we create the file with the header
		
		csvPrinter.printRecord(values); //we print the corresponding row

		csvPrinter.close();
		writer.close();
	}
	
	public static void main(String[] args) throws Exception {
		
		String path = "technical_debt_dataset.csv";
		
		Map<String,String> comments = loadCommentsFile(path);
		Instances instances = creatingArff(comments);
		
//		Instances [] tt = divideStratifiedInstances(instances);
		Instances [] tt = divideRandom(instances);
		
		System.out.println(instances.numInstances()+" "+tt[0].numInstances()+" "+tt[1].numInstances());
		
		//Instances can be saved as an arff or as a csv
		
//		//arff saver
//		ArffSaver saver = new ArffSaver();
//		saver.setInstances(tt[0]);
//		saver.setFile(new File("training.arff"));
//		saver.writeBatch();
//		
//		saver.setInstances(tt[1]);
//		saver.setFile(new File("test.arff"));
//		saver.writeBatch();
//		
//		//csv saver -- I recommend having everything saved as csv so, in case you need it, you can use it in any other place 
//		 
//		CSVSaver saverCSV = new CSVSaver();
//		saverCSV.setInstances(tt[0]);
//		saverCSV.setFile(new File("training.csv"));
//		saverCSV.writeBatch();
//		
//		saverCSV.setInstances(tt[1]);
//		saverCSV.setFile(new File("test.csv"));
//		saverCSV.writeBatch();
//		
//		//these csvs can be loaded with the Weka loader
//		CSVLoader ss = new CSVLoader();
//		ss.setSource(new File("training.csv"));
//		ss.setStringAttributes("1");
////		System.out.println(ss.getDataSet());
		
		simpleClassificationExample(tt[0],tt[1]);
		
	}
	
}

