package dataManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVSaver;
import weka.core.converters.ArffLoader.ArffReader;
import weka.filters.Filter;
import weka.filters.supervised.instance.StratifiedRemoveFolds;
import weka.filters.unsupervised.instance.RemovePercentage;

public class DataHandler {
	
	public static Instances readFile(String path) throws IOException {
		
		if (path.endsWith(".csv")) {
			
			Map<String, String> comments = loadFile(path);
			Instances data = creatingArff(comments);
			
			return data;
			
		} else if (path.endsWith(".arff")) {
			
			BufferedReader reader = new BufferedReader(new FileReader(path));
			ArffReader arff = new ArffReader(reader);
			Instances data = arff.getData();
			
			return data;
			
		}
		return null;
		
	}
	// considered features: classification, comment. Not considered the project.
	// load file, generate map and clean data
	public static Map<String, String> loadFile(String path) throws IOException {

		Map<String, String> comments = new HashMap<>();

		Reader reader = Files.newBufferedReader(Paths.get(path));
		CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());

		String regex = "\\b([a-zA-Z][\\w_]*(\\.[a-zA-Z][\\w_]*)+)\\(\\)";

		for (CSVRecord csvRecord : csvParser) {

			String comment = csvRecord.get(2).replaceAll(regex, "JavaClassSATD").replaceAll("[0-9]", " ")
					.replaceAll("`", "\'")
					.replaceAll("won\'\\s*t", "will not").replaceAll("let\'\\s*s", "let us") // irregular contractions
					.replaceAll("n\'\\s*t", " not") //all "not" contractions
					.replaceAll("\'\\s*ll", " will").replaceAll("\'\\s*ve", " have").replaceAll("\'\\s*m", " am").replaceAll("\'\\s*re", " are").replaceAll("\'\\s*s", " is")  //most used contractions
					.replaceAll("gotta", "have got to").replaceAll("kinda", "kind of").replaceAll("wanna", "want to").replaceAll("gimme", "give me").replaceAll("lotta", "lot of") // slang
					.replaceAll("lemme", "let me").replaceAll("dunno", "do not know").replaceAll("prolly", "probably") // slang
					.replaceAll("\\p{Punct}", " ")
					.replaceAll("\\s+", " ");

			// remove all empty instances
			if (comment.contentEquals(" ")) {

				continue;

			}
			
			String classification = csvRecord.get(1);

			comments.put(comment, classification);
		}

		csvParser.close();

		return comments;
	}

	// generate instances from csv for generate at the end arff file.
	public static Instances creatingArff(Map<String, String> comments) {

		ArrayList<Attribute> attributes = new ArrayList<>();
		attributes.add(new Attribute("comment", (ArrayList<String>) null)); // a string attribute
		attributes.add(new Attribute("classification", new ArrayList<String>(new HashSet<String>(comments.values())))); // a nominal attribute with the definition of all possible values

		Instances instances = new Instances("comments", attributes, comments.size());
		instances.setClassIndex(1); // the target class is the column in position 1

		for (String k : comments.keySet()) {

			Instance i = new DenseInstance(attributes.size());
			i.setDataset(instances);
			i.setValue(0, k);
			i.setClassValue(comments.get(k));

			instances.add(i);
		}
		return instances;
	}
	
	// generate 10-folds
	public static Instances[] divideStratifiedInstances(Instances data, int fold) throws Exception {

		StratifiedRemoveFolds filter = new StratifiedRemoveFolds();
		filter.setInputFormat(data);

		filter.setOptions(new String[] { "-N", "10", "-S", "8", "-F", Integer.toString(fold) });
		Instances test = Filter.useFilter(data, filter);

		filter.setOptions(new String[] { "-N", "10", "-S", "8", "-F", Integer.toString(fold), "-V" });
		Instances training = Filter.useFilter(data, filter);

		return new Instances[] { training, test };
	}
	
	/*
	 * 70-30 dataset division for generate train-test select a percentage of data,
	 * regardless of the class of the instances
	 */
	public static Instances[] divideRandom(Instances data) throws Exception {

		Random random = new Random();
		random.setSeed(1);
		data.randomize(random);

		RemovePercentage filter = new RemovePercentage();
		filter.setInputFormat(data);
		filter.setPercentage(30.0); // percentage to remove (and put after in test set)

		Instances training = Filter.useFilter(data, filter);

		Instances test = new Instances(data, 0, data.numInstances() - training.numInstances());

		return new Instances[] { training, test };
	}

	public static void saveData(Instances training, String trainingName, Instances test, String testName)
			throws IOException {

		ArffSaver saver = new ArffSaver();
		saver.setInstances(training);
		saver.setFile(new File("C:/Users/irene/eclipse-workspace/SATD/datasets/" + trainingName + ".arff"));
		saver.writeBatch();

		saver.setInstances(test);
		saver.setFile(new File("C:/Users/irene/eclipse-workspace/SATD/datasets/" + testName + ".arff"));
		saver.writeBatch();

		// csv saver
		CSVSaver saverCSV = new CSVSaver();
		saverCSV.setInstances(training);
		saverCSV.setFile(new File("C:/Users/irene/eclipse-workspace/SATD/datasets/" + trainingName + ".csv"));
		saverCSV.writeBatch();

		saverCSV.setInstances(test);
		saverCSV.setFile(new File("C:/Users/irene/eclipse-workspace/SATD/datasets/" + testName + ".csv"));
		saverCSV.writeBatch();

	}
	
	public static void saveOneData(Instances data, String dataName) throws IOException {

		ArffSaver saver = new ArffSaver();
		saver.setInstances(data);
		saver.setFile(new File("C:/Users/irene/eclipse-workspace/SATD/datasets/" + dataName + ".arff"));
		saver.writeBatch();

		CSVSaver saverCSV = new CSVSaver();
		saverCSV.setInstances(data);
		saverCSV.setFile(new File("C:/Users/irene/eclipse-workspace/SATD/datasets/" + dataName + ".csv"));
		saverCSV.writeBatch();


	}

	
}
