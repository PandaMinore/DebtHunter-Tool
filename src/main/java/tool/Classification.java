package tool;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
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

		SMO smo = new SMO();

		Map<String, String> map = new HashMap<String, String>();

		if (smo.getClass().isInstance(classifier)) {
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

		System.out.println("Grid search with " + classifierName + " " + type
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

	
}
