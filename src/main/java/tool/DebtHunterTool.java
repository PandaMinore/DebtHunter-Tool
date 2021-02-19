package tool;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;
import parsing.JavaParsing;

@Command(name = "DebtHunter", mixinStandardHelpOptions = true, version = "DebtHunter 1.0.0",
description = "DebtHunter detect the Self-Admitted in your project or in issue tracker.")
public class DebtHunterTool implements Runnable {
	
	@Option(names = {"-u", "--usecase"}, description = "first, second.")
    private String useCase = "second";
	
	// test set
	@Option(names = {"-p", "--project"}, description = "The java project path.")
    private String projectPath = "";
	
	// test set
	@Option(names = {"-j", "--jira"}, description = "The Jira issue tracker repository.")
    private String jiraRepo = "";
	@Option(names = {"-m1", "--model1"}, description = "The path of your binary model.")
    private String firstModelPath = "";
	@Option(names = {"-m2", "--model2"}, description = "The path of your multi-class model.")
    private String secondModelPath = "";
	
	//training set
    @Option(names = {"-l", "--labeled"}, description = "The path of your labeled data.")
    private String LabeledPath = "";


	@Override
	public void run() {
		
		System.out.println("Let's start!");
		
		
		if (useCase.equals("first")) {
			
			System.out.println("You selected the first use case!");
			String path = "";
			Instances test = null;
			
			// if the input data is java files
			if (StringUtils.isNotEmpty(projectPath)) {
				try {
					test = JavaParsing.processDirectory(projectPath, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
				path = projectPath + "/comments.csv";
			
			// if the input data is issue tracker
			} else if (StringUtils.isNotEmpty(jiraRepo)) {
				//TODO: JiraMiner.
			}
			
			
/*			// upload csv for data cleaning
			Map<String, String> comments = null;
			try {
				comments = DataHandler.loadFile(path);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			Instances test = DataHandler.creatingArff(comments);
*/			
			// remove not useful columns (i.e. project name, package name and top package or only project name)
			Instances onlyComments = test;
			while (onlyComments.numAttributes() > 1) {
				try {
					onlyComments = DataHandler.removeAttribute(onlyComments, "first");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			System.out.println("AAAAAAAAAAAAAAAAAAAA " + test.instance(0));
			System.out.println("AAAAAAAAAAAAAAAAAAAA " + onlyComments.instance(0));
			
			// if the user chooses the first use case and not provides a pre-trained model
			if((StringUtils.isEmpty(firstModelPath)) && (StringUtils.isEmpty(secondModelPath))) {
				
				try {
					UseCaseOne.debtHunterLabeling(onlyComments);
				} catch (Exception e) {
					e.printStackTrace();
				}
			
			// The user provides 2 pre-trained models in input
			} else if((StringUtils.isNotEmpty(firstModelPath)) && (StringUtils.isNotEmpty(secondModelPath))) {
				
				String modelPath1 = firstModelPath; //"./preTrainedModels/DHbinaryClassifier.model"; 
				String modelPath2 = secondModelPath; //"./preTrainedModels/DHmultiClassifier.model";
				
				try {
					UseCaseOne.userClassifierLabeling(onlyComments, modelPath1, modelPath2);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else if(((StringUtils.isEmpty(firstModelPath)) && (StringUtils.isNotEmpty(secondModelPath))) || ((StringUtils.isNotEmpty(firstModelPath)) && (StringUtils.isEmpty(secondModelPath)))) {
				System.out.println("You not provided one of two pre-trained models.");
				System.exit(0);
			}
			
			
			//create output file and save it
			if (StringUtils.isNotEmpty(projectPath))
				try {
					onlyComments = DataHandler.removeAttribute(onlyComments, "first");
					onlyComments = DataHandler.mergeRecords(test, onlyComments);
					DataHandler.saveData(onlyComments, "commentsLabeled", path);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			else
				try {
					DataHandler.saveData(onlyComments, "comments", projectPath);
				} catch (IOException e) {
					e.printStackTrace();
				}
		
			
			
		// The user provides labeled data (training set).
		} else if (useCase.equals("second")) {
			
			System.out.println("You selected the second use case!");

			String endPath = LabeledPath.substring(1);
			
			ArffLoader loader = new ArffLoader();
			try {
				loader.setSource(new File(endPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			// last part of training set path
			Instances training = null;
			try {
				training = loader.getDataSet();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// remove the not useful columns (i.e. project name, package name and top package or only project name)
			while (training.numAttributes() > 2) {
				try {
					training = DataHandler.removeAttribute(training, "first");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			training.setClassIndex(training.numAttributes() - 1);

			System.out.println("The training data is ok!");
			
			Instances multiTraining = null;
			Instances binTraining = null;
			
			try {
				// binarization
				binTraining = DataHandler.binarization(training);

				// remove non-SATD instances
				multiTraining = DataHandler.removeClass(training);
				UseCaseTwo.trainModel(binTraining, multiTraining, LabeledPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		int exitCode = new CommandLine(new DebtHunterTool()).execute(args); 
        System.exit(exitCode);
			
		
	}

}
