package CIandTtest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.inference.TTest;

public class StatisticsTests {

	//confidence intervals: compare CV distributions with equal test set
	public static void compareResult(double[][] fm, String[] classifierName, Boolean firstTime) throws IOException {

		List<Object> results = new ArrayList<>();

		for (int i = 1; i < fm.length; i++) {

			results = getTtest(fm[0], fm[i], classifierName[0] + "_vs_" + classifierName[i]);

			if (firstTime) {
				saveDataFirst(results, "Ttest_ConfidenceIntervals");
				firstTime = false;
			} else {
				saveData(results, "Ttest_ConfidenceIntervals");
			}

		}

	}
	
	//confidence intervals that compare distributions with different test set
	public static void compareResult2(double[] fm, double[] numInst, String[] classifierName, Boolean firstTime) throws IOException {
		
		List<Object> results = new ArrayList<>();

		for (int i = 1; i < fm.length; i++) {

			results = confidenceInterval2(fm[0], numInst[0], fm[i], numInst[i], classifierName[0] + "_vs_" + classifierName[i]);

			if (firstTime) {
				saveDataFirst(results, "Ttest_ConfidenceIntervals2");
				firstTime = false;
			} else {
				saveData(results, "Ttest_ConfidenceIntervals2");
			}

		}

	}
	
	
	// statistically compare the classifiers
	private static List<Object> getTtest(double[] sample1, double[] sample2, String combination) throws IOException {

		TTest test = new TTest();
		double tstatistic = test.pairedT(sample1, sample2);
		System.out.println();
		System.out.println(combination + ": The t statistic is " + Double.toString(tstatistic));
		double pvalue = test.pairedTTest(sample1, sample2) / 2; // pvalue/2 is for one-sided
		System.out.println("The p-value is " + Double.toString(pvalue));
		boolean result = test.pairedTTest(sample1, sample2, 0.05 * 2); // true iff the null hypothesis can be rejected with
																	// confidence 1-alpha
		System.out.println("The null hypothesis can be rejected? " + Boolean.toString(result)); // alpha is the
																								// significance level of
																								// the test (0.05)

		// calculate confidence intervals for decide which distribution is better
		double[] interval = confidenceInterval(sample1, sample2);

		boolean flag = false;
		String isBetter = "";
		if ((interval[0] <= 0.0) && (interval[1] >= 0.0)) {
			System.out.println("None distribution (classifier) is statistically better than other.");
			isBetter = "none better";
		} else if (interval[0] > 0) {
			System.out.println("second classifier is statistically better than first one.");
			flag = false;
		} else if (interval[1] < 0) {
			System.out.println("first classifier is statistically better than second one.");
			flag = true;
		}

		// results line
		List<Object> results = new ArrayList<>();
		results.add(combination);
		results.add(tstatistic);
		results.add(pvalue);
		results.add(result);
		results.add(interval[0]);
		results.add(interval[1]);

		if (isBetter == "none better") {
			results.add(isBetter);
		} else {
			results.add(flag);
		}

		return results;

	}
	
	
	private static double[] confidenceInterval(double[] sample1, double[] sample2) {
		// difference between sample1 and sample2 values
		double[] data = new double[10];
		double sum = 0.0;

		
		for (int i = 0; i < sample1.length; i++) {
			data[i] = sample1[i] - sample2[i];
			sum += data[i];
		}

		double averageDifference = sum / data.length;

		// standard deviation
		double squaredDifferenceSum = 0.0;
		for (double num : data) {
			squaredDifferenceSum += (num - averageDifference) * (num - averageDifference);
		}

		double variance = squaredDifferenceSum / (sample1.length * (sample1.length -1));
		double standardDeviation = Math.sqrt(variance);

		double confidenceLevel = 1.833; // value for 95% confidence interval (t) and 10-1 samples (one-sided)
		double tmp = confidenceLevel * standardDeviation;
		double[] interval = { averageDifference - tmp, averageDifference + tmp };

		System.out.println("Confidence intervals: the lower bound is " + Double.toString(interval[0])
				+ " and the upper bound is " + Double.toString(interval[1]));

		return interval;

	}
	
	private static List<Object> confidenceInterval2 (double firstF1, double numInst1, double secondF1, double numInst2, String combination) {
			
		double variance = ((firstF1 * (1 - firstF1)) / numInst1) + ((secondF1 * (1 - secondF1)) / numInst2);
		double standardDeviation = Math.sqrt(variance);
		
		double difference = firstF1 - secondF1;
		double z = 1.96; //with 95% of confidence level (following standard normal distribution)
		double tmp = z * standardDeviation;
		
		double[] interval = {difference - tmp, difference + tmp};

		System.out.println("Confidence intervals: the lower bound is " + Double.toString(interval[0])
				+ " and the upper bound is " + Double.toString(interval[1]));
		
		boolean flag = false;
		String isBetter = "";
		if ((interval[0] <= 0.0) && (interval[1] >= 0.0)) {
			System.out.println("None distribution (classifier) is statistically better than other.");
			isBetter = "none better";
		} else if (interval[0] > 0) {
			System.out.println("second classifier is statistically better than first one.");
			flag = false;
		} else if (interval[1] < 0) {
			System.out.println("first classifier is statistically better than second one.");
			flag = true;
		}
		
		// results line
		List<Object> results = new ArrayList<>();
		results.add(combination);
		results.add(interval[0]);
		results.add(interval[1]);

		if (isBetter == "none better") {
			results.add(isBetter);
		} else {
			results.add(flag);
		}
		
		return results;
		
	}
	
	private static void createHeader(List<String> header) {

		header.add("combination");
		header.add("t_statistic");
		header.add("p_value");
		header.add("result");
		header.add("lower_bound_interval");
		header.add("upper_bound_interval");
		header.add("classifier1_is_better");

	}
	
	private static void createHeader2(List<String> header) {

		header.add("combination");
		header.add("lower_bound_interval");
		header.add("upper_bound_interval");
		header.add("classifier1_is_better");

	}

	// save comparison data when csv is empty
	private static void saveDataFirst(List<Object> results, String fileName) throws IOException {

		List<String> header = new ArrayList<>();
		if (fileName == "Ttest_ConfidenceIntervals") {
			createHeader(header);
		} else {
			createHeader2(header);
		}
		

		BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName + ".csv"));
		CSVPrinter csvPrinter = new CSVPrinter(writer,
				CSVFormat.DEFAULT.withHeader(header.toArray(new String[header.size()])));

		csvPrinter.printRecord(results);
		csvPrinter.close();
		writer.close();

	}

	// save comparison data when csv has header and results yet
	private static void saveData(List<Object> results, String fileName) throws IOException {

		FileWriter csv = new FileWriter(fileName + ".csv", true);
		BufferedWriter writer = new BufferedWriter(csv);

		String tmp = "";
		for (Object element : results) {
			tmp += element + ",";
		}

		tmp = tmp.substring(0, tmp.length() - 1);

		writer.write(tmp);
		writer.newLine();
		writer.close();
	}

}

