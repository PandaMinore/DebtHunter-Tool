package parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import tool.DataHandler;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class IssueTrackerProcessing {
	
	static String pattern_unico = "&[0-9a-z]*;";
	static String pattern_tag = "<[^>]*>";//"</*[0-9a-z]*/*>";
	static String pattern_spaces = "\\s+";
	static String pattern_html = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";//"<.*>"; TODO: Check if the href are eliminated with this!!

	private static void addInstance (Instances data, String projectName, String key, String type, String priority, String textType, String text) {
		
		// text cleaning
		text = DataHandler.cleanComment(text);
		
		// do not add to data empty text
		if (text.contentEquals(" ")) {

			return;

		}
		
		Instance inst  = new DenseInstance(6);
		inst.setDataset(data);
		inst.setValue(0, projectName);
		inst.setValue(1 , key);
		inst.setValue(2 , type);
		inst.setValue(3 , priority);
		inst.setValue(4 , textType);
		inst.setValue(5 , text);
		data.add(inst);
		
	}
	
	private static Instances processIssues(String full_path, String path, String outputPath, Boolean firstTime, Instances data, String projectName) throws JDOMException, IOException{ // , Map[] elements, Set<String> all_elements

		System.out.println("Getting comments... " + full_path + " " + new Date());

		SAXBuilder builder = new SAXBuilder();
		Document document = (Document) builder.build(full_path);
		Element rootNode = document.getRootElement();

		Element issue = rootNode.getChild("channel").getChild("item");

		String key = issue.getChild("key").getText().trim();
		String type = issue.getChild("type").getText().trim();
		String priority = issue.getChild("priority").getText().trim();
		
		String summary = issue.getChild("summary").getText().replaceAll(pattern_unico, "").replaceAll(pattern_html,"").replaceAll(pattern_tag," ").replaceAll(pattern_spaces, " ").trim();
		String description = issue.getChild("description").getText().replaceAll(pattern_unico, "").replaceAll(pattern_html,"").replaceAll(pattern_tag," ").replaceAll(pattern_spaces, " ").trim();

		//save file
		BufferedWriter writer = null;
		
		
		// SUMMARY
		if(firstTime) {
			
			//save file
			writer = Files.newBufferedWriter(Paths.get(outputPath + "/comments.csv"));
			
			CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("projectname", "key", "type", "priority", "textType", "comment"));
    		csvPrinter.printRecord(projectName, key, type, priority, "summary", summary);
    		
    		csvPrinter.flush();  
	        csvPrinter.close();
    		
		} else {
			
			FileWriter csv = new FileWriter(outputPath + "/comments.csv", true);
			writer = new BufferedWriter(csv);
			
			String line = projectName + ", " + key + ", " + type + ", " + priority + ", summary, " + summary;
    		writer.write(line);
    		writer.newLine();
    		
    		writer.close();
		}
		addInstance(data, projectName, key, type, priority, "summary", summary);

		
		FileWriter csv = new FileWriter(outputPath + "/comments.csv", true);
		writer = new BufferedWriter(csv);		
		
		
    	//DESCRIPTION
		if(description.length() > 0){

			String line = projectName + ", " + key + ", " + type + ", " + priority + ", description, " + description;
			writer.write(line);
    		writer.newLine();
    		
		}
		addInstance(data, projectName, key, type, priority, "description", description);
		
		
		//COMMENTS
		Element comms = issue.getChild("comments");
		if(comms != null){
			List<Element> comments = comms.getChildren("comment");
			if(comments != null){
				for(Element c : comments){
					String cc = c.getText().replaceAll(pattern_unico, "").replaceAll(pattern_html,"").replaceAll(pattern_tag," ").replaceAll(pattern_spaces, " ").trim();
					if(cc.length() == 0)
						continue;
					
					String line = projectName + ", " + key + ", " + type + ", " + priority + ", comment, " + cc;
					writer.write(line);
		    		writer.newLine();
					
					addInstance(data, projectName, key, type, priority, "comment", cc);

				}
					
			}

		}
		
		writer.close();
		
		return data;
	}
	
	
	public static Instances processDirectory(String projectPath, String outputPath, String projectName) throws Exception{
		
		Boolean firstTime = true;
		
		// key (e.g. CAMEL 2226), type (e.g. Improvement), priority (e.g. Minor), type of text (i.e. summary, description or comment), text
		ArrayList<Attribute> attributes = new ArrayList<>();
		attributes.add(new Attribute("projectname", (ArrayList<String>) null));
		attributes.add(new Attribute("key", (ArrayList<String>) null));
		attributes.add(new Attribute("type", (ArrayList<String>) null));
		attributes.add(new Attribute("textType", (ArrayList<String>) null));
		attributes.add(new Attribute("priority", (ArrayList<String>) null));
		attributes.add(new Attribute("text", (ArrayList<String>) null));
        Instances data = new Instances("comments", attributes, 1);
        		
		File f = new File(projectPath);
		
		for(File ff : f.listFiles()){
			if (!ff.getName().endsWith(".xml")) {
				continue;
			}

			String full_path = ff.getAbsolutePath().substring(0,ff.getAbsolutePath().lastIndexOf(".")) + ".xml";
			data = processIssues(full_path, projectPath, outputPath, firstTime, data, projectName);

			firstTime = false;
			
		}
		
		return data;
		
	}
	
	
}
