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
			
			if (StringUtils.isNotEmpty(projectPath)) {
				// if the input data are java files
				try {
					JavaParsing.processDirectory(projectPath, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
				path = projectPath + "/comments.csv";
				
			} else if (StringUtils.isNotEmpty(jiraRepo)) {
				//TODO: JiraMiner.
			}
			
			// upload csv for data cleaning
			Map<String, String> comments = null;
			try {
				comments = DataHandler.loadFile(path);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			Instances test = DataHandler.creatingArff(comments);
			

			// remove not useful columns (i.e. project name, package name and top package or only project name)
			Instances newTest = null;
			while (test.numAttributes() > 2) {
				try {
					newTest = DataHandler.removeAttribute(test);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// if the user chooses the first use case and not provides a pre-trained model
			if((StringUtils.isEmpty(firstModelPath)) && (StringUtils.isEmpty(secondModelPath))) {
				
				try {
					UseCaseOne.debtHunterLabeling(newTest);
				} catch (Exception e) {
					e.printStackTrace();
				}
			
			// The user chooses the first use case and provides 2 pre-trained models in input
			} else if((StringUtils.isNotEmpty(firstModelPath)) && (StringUtils.isNotEmpty(secondModelPath))) {
				
				Boolean twoStep = true;
				
				String modelPath1 = firstModelPath; //"./preTrainedModels/DHbinaryClassifier.model"; 
				String modelPath2 = secondModelPath; //"./preTrainedModels/DHmultiClassifier.model";
				
				try {
					UseCaseOne.userClassifierLabeling(twoStep, newTest, modelPath1, modelPath2);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else if(((StringUtils.isEmpty(firstModelPath)) && (StringUtils.isNotEmpty(secondModelPath))) || ((StringUtils.isNotEmpty(firstModelPath)) && (StringUtils.isEmpty(secondModelPath)))) {
				System.out.println("You not provided one of two pre-trained models.");
				System.exit(0);
			}
			
			
			//TODO: formattare nel modo corretto il test set (rimettere i nomi delle classi, project...)
			if (StringUtils.isNotEmpty(projectPath))
				try {
					DataHandler.saveData(newTest, "comments", path);
				} catch (IOException e) {
					e.printStackTrace();
				}
			else
				try {
					DataHandler.saveData(newTest, "comments", path);
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
					training = DataHandler.removeAttribute(training);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			training.setClassIndex(training.numAttributes() - 1);

			System.out.println("The training data is ok!");
			
			// binarization
			Instances binTraining = null;
			try {
				binTraining = DataHandler.binarization(training);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// remove non-SATD instances
			Instances multiTraining = null;
			try {
				multiTraining = DataHandler.removeClass(training);
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
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
