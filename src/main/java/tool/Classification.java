package tool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import dataManager.DataHandler;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.meta.GridSearch;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.stemmers.LovinsStemmer;
import weka.core.stopwords.WordsFromFile;
import weka.core.tokenizers.WordTokenizer;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class Classification {

	public static double[] tenFoldCV(Instances training, Classifier classifier, String classifierName, String type)
			throws Exception {

		double[] fMeasure = new double[11];

		for (int i = 1; i <= 10; i++) {

			// divide training into 10-fold
			Instances[] fold = DataHandler.divideStratifiedInstances(training, i);
			FilteredClassifier CVClassifier = train(fold[0], classifier, type);

			Evaluation eval = new Evaluation(fold[0]);
			eval.evaluateModel(CVClassifier, fold[1]);
			fMeasure[i - 1] = eval.weightedFMeasure();

		}

		fMeasure[10] = (fMeasure[0] + fMeasure[1] + fMeasure[2] + fMeasure[3] + fMeasure[4] + fMeasure[5] + fMeasure[6]
				+ fMeasure[7] + fMeasure[8] + fMeasure[9]) / 10;
		System.out.println(type + " classification with " + classifierName
				+ ": the mean weighted Fmeasure for the 10 fold CV is " + fMeasure[10]);
		return fMeasure;	
		
	}
	
	
	public static Classifier parametersOptimization(Instances training, Classifier classifier, String classifierName,
			String type) throws Exception {

		WordsFromFile stopwords = new WordsFromFile();
		stopwords.setStopwords(new File("/dic/stopWords.txt"));

		// preprocessing
		StringToWordVector stw = new StringToWordVector(100000);
		stw.setInputFormat(training);
		stw.setLowerCaseTokens(true);
		stw.setStemmer(new LovinsStemmer());
		stw.setTokenizer(new WordTokenizer());
		stw.setStopwordsHandler(stopwords);
		stw.setOutputWordCounts(true);
		stw.setTFTransform(true);
		stw.setIDFTransform(true);

		training = Filter.useFilter(training, stw);


		if (type == "binary") {

			// resample
			SpreadSubsample resample = new SpreadSubsample();
			resample.setInputFormat(training);
			resample.setDistributionSpread(10.0);
			resample.setRandomSeed(8);

			training = Filter.useFilter(training, resample);
		
			// feature selection: info gain
			AttributeSelection attSelection = new AttributeSelection();
			Ranker ranker = new Ranker();
			ranker.setNumToSelect(750);
			InfoGainAttributeEval ifg = new InfoGainAttributeEval();
			attSelection.setEvaluator(ifg);
			attSelection.setSearch(ranker);
			attSelection.setInputFormat(training);

			training = Filter.useFilter(training, attSelection);

		} if (type == "multi") {
			
			String[] classToExpand = { "1", "2", "4", "5" }; // apply SMOTE on minority classes, one at time

			for (String i : classToExpand) {
				SMOTE smote = new SMOTE();
				smote.setInputFormat(training);
				smote.setClassValue(i);
				smote.setPercentage(100.0);

				training = Filter.useFilter(training, smote);
				
			}
			
			// feature selection: info gain
			AttributeSelection attSelection = new AttributeSelection();
			Ranker ranker = new Ranker();
			ranker.setNumToSelect(3000);
			InfoGainAttributeEval ifg = new InfoGainAttributeEval();
			attSelection.setEvaluator(ifg);
			attSelection.setSearch(ranker);
			attSelection.setInputFormat(training);

			training = Filter.useFilter(training, attSelection);
				
		} 
		

		// gridSearch
		GridSearch grid = new GridSearch();
		grid.setEvaluation(new SelectedTag(GridSearch.EVALUATION_WAUC, GridSearch.TAGS_EVALUATION));

		LibSVM lib = new LibSVM();
		SMO smo = new SMO();

		Map<String, String> map = new HashMap<String, String>();

		if (lib.getClass().isInstance(classifier)) {
			map.put("YProperty", "gamma");
			map.put("YMin", "-4.0");
			map.put("YMax", "1.0");
			map.put("YStep", "0.5"); // 0.2
			map.put("YBase", "10.0");
			map.put("YExpression", "pow(BASE,I)");
			map.put("XProperty", "cost");
			map.put("XMin", "-1.0");
			map.put("XMax", "4.0");
			map.put("XStep", "0.5"); // 0.2
			map.put("XBase", "10.0");
			map.put("XExpression", "pow(BASE,I)");
		} else if (smo.getClass().isInstance(classifier)) {
			map.put("YProperty", "kernel.gamma");
			map.put("YMin", "-5.0");
			map.put("YMax", "2.0");
			map.put("YStep", "1");
			map.put("YBase", "10.0");
			map.put("YExpression", "pow(BASE,I)");
			map.put("XProperty", "c");
			map.put("XMin", "1");
			map.put("XMax", "16");
			map.put("XStep", "1");
			map.put("XExpression", "I");
		}

		// gamma 10^-3, 10^-2, ... , 10^3
		grid.setYProperty(map.get("YProperty"));
		grid.setYMin(Double.parseDouble(map.get("YMin")));
		grid.setYMax(Double.parseDouble(map.get("YMax")));
		grid.setYStep(Double.parseDouble(map.get("YStep")));
		grid.setYBase(Double.parseDouble(map.get("YBase")));
		grid.setYExpression(map.get("YExpression"));

		// cost 10^-3, 10^-2, ... , 10^3
		grid.setXProperty(map.get("XProperty"));
		grid.setXMin(Double.parseDouble(map.get("YMin")));
		grid.setXMax(Double.parseDouble(map.get("YMax")));
		grid.setXStep(Double.parseDouble(map.get("YStep")));
		grid.setXBase(Double.parseDouble(map.get("YBase")));
		grid.setXExpression(map.get("XExpression"));

		grid.setNumExecutionSlots(5);
		grid.setSeed(8);

		grid.setClassifier(classifier);
		grid.buildClassifier(training);

		classifier = grid.getBestClassifier();

		System.out.println(" grid search with " + classifierName + " " + type
				+ ": the optimal gamma and the optimal cost are " + grid.getValues());

		return classifier;

	}
	
	public static FilteredClassifier train(Instances trainSet, Classifier classifier, String type) throws Exception {

		WordsFromFile stopwords = new WordsFromFile();
		stopwords.setStopwords(new File("/dic/stopWords.txt"));

		StringToWordVector stw = new StringToWordVector(100000); // 100000: words to keep
		stw.setInputFormat(trainSet);
		stw.setLowerCaseTokens(true);
		stw.setStemmer(new LovinsStemmer());
		stw.setTokenizer(new WordTokenizer());
		stw.setStopwordsHandler(stopwords);
		stw.setOutputWordCounts(true);
		stw.setIDFTransform(true);
		stw.setTFTransform(true);

		SpreadSubsample resample = new SpreadSubsample();
		SMOTE[] smote = { new SMOTE(), new SMOTE(), new SMOTE(), new SMOTE() };
		AttributeSelection attSelection = new AttributeSelection();

		if (type == "binary") {
			// resample
			resample.setInputFormat(trainSet);
			resample.setDistributionSpread(10.0);
			resample.setRandomSeed(8);
			
			// feature selection: info gain
			Ranker ranker = new Ranker();
			ranker.setNumToSelect(750);
			InfoGainAttributeEval ifg = new InfoGainAttributeEval();
			attSelection.setEvaluator(ifg);
			attSelection.setSearch(ranker);	

		} else if (type == "multi") {
			
			// SMOTE
			String[] classToExpand = { "1", "2", "4", "5" }; // apply SMOTE on minority classes, one at time
			int j = 0;
			for (String i : classToExpand) {
				smote[j].setInputFormat(trainSet);
				smote[j].setClassValue(i);
				smote[j].setPercentage(100.0);
				j++;
			}
			
			// feature selection: info gain
			Ranker ranker = new Ranker();
			ranker.setNumToSelect(3000);
			InfoGainAttributeEval ifg = new InfoGainAttributeEval();
			attSelection.setEvaluator(ifg);
			attSelection.setSearch(ranker);
			
		}

		MultiFilter mf = new MultiFilter();
		if (type == "binary") {
			mf.setFilters(new Filter[] { stw, resample, attSelection });
		}
		if (type == "multi") {
			mf.setFilters(new Filter[] { stw, attSelection, smote[0], smote[1], smote[2], smote[3] });
		}

		FilteredClassifier fc = new FilteredClassifier();
		fc.setFilter(mf);
		fc.setClassifier(classifier);
		fc.buildClassifier(trainSet);

		return fc;
	}

	
	public static void evaluatorTwoStepFirst(Evaluation eval, Instances instances, List<String> headerBinary,
			List<Object> resultsBinary, double[] MultifMeasure) throws Exception {

		List<Object> classes = Collections.list(instances.classAttribute().enumerateValues());

		List<String> header = new ArrayList<>();
		header.addAll(headerBinary);
		createHeader(header, classes);

		List<Object> results = new ArrayList<>();
		results.addAll(resultsBinary);
		createResults(results, eval, classes, MultifMeasure);

		BufferedWriter writer = Files.newBufferedWriter(Paths.get("output_DebtHunter.csv"));
		CSVPrinter csvPrinter = new CSVPrinter(writer,
				CSVFormat.DEFAULT.withHeader(header.toArray(new String[header.size()])));

		csvPrinter.printRecord(results);
		csvPrinter.close();
		writer.close();

	}
	
	public static void evaluatorTwoStep(Evaluation eval, Instances instances, List<String> headerBinary, 
			List<Object> resultsBinary, double[] MultifMeasure) throws Exception {

		FileWriter csv = new FileWriter("output_DebtHunter.csv", true);
		BufferedWriter writer = new BufferedWriter(csv);

		List<Object> classes = Collections.list(instances.classAttribute().enumerateValues());

		List<Object> results = new ArrayList<>();
		results.addAll(resultsBinary);
		createResults(results, eval, classes, MultifMeasure);

		String tmp = "";
		for (Object element : results) {
			tmp += element + ",";
		}

		tmp = tmp.substring(0, tmp.length() - 1);

		writer.write(tmp);
		writer.newLine();
		writer.close();

	}
	
	public static void createHeader(List<String> header, List<Object> classes) {

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

		for (Object cc : classes) {
			header.add(cc + "_TP Rate");
			header.add(cc + "_FP Rate");
			header.add(cc + "_Precision");
			header.add(cc + "_Recall");
			header.add(cc + "_F-Measure");
			header.add(cc + "_ROC Area");
			header.add(cc + "_PRC Area");
		}

		header.add("Mean_Weighted_F-Measure");

	}

	public static void createResults(List<Object> results, Evaluation eval, List<Object> classes, double[] fm) {

		results.add(eval.correct());
		results.add(eval.correct() * 100 / eval.numInstances());
		results.add(eval.incorrect());
		results.add(eval.incorrect() * 100 / eval.numInstances());
		results.add(eval.weightedTruePositiveRate());
		results.add(eval.weightedFalsePositiveRate());
		results.add(eval.weightedPrecision());
		results.add(eval.weightedRecall());
		results.add(eval.weightedFMeasure());
		results.add(eval.weightedAreaUnderROC());
		results.add(eval.weightedAreaUnderPRC());

		for (int i = 0; i < classes.size(); i++) {
			results.add(eval.truePositiveRate(i));
			results.add(eval.falsePositiveRate(i));
			results.add(eval.precision(i));
			results.add(eval.recall(i));
			results.add(eval.fMeasure(i));
			results.add(eval.areaUnderROC(i));
			results.add(eval.areaUnderPRC(i));
		}

		results.add(fm[10]);

	}
	
	
	
}
