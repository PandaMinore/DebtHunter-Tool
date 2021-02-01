package parsing;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class IssueTrackerProcessing {
	
	static String pattern_unico = "&[0-9a-z]*;";
	static String pattern_tag = "<[^>]*>";//"</*[0-9a-z]*/*>";
	static String pattern_spaces = "\\s+";
	static String pattern_html = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";//"<.*>"; TODO: Check if the href are eliminated with this!!


	@SuppressWarnings("unchecked")
	private static Map[] loadElements(String file) throws IOException{

		Map<String,String> simple_classes = new HashMap<>(); // <simple_class, class>
		Map<String,String> classes = new HashMap<>(); // <class, package>
		Map<String,String> packages = new HashMap<>(); //< package, top>

		Map<String,double[]> locs = new HashMap<>();
		
		Reader reader = Files.newBufferedReader(Paths.get(file));
		CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader()); //the file is a pure csv file, no strange or different format.

		for(CSVRecord r : csvParser){
			simple_classes.put(r.get("class").substring(r.get("class").lastIndexOf(".")+1), r.get("class"));
			classes.put(r.get("class"),r.get("package"));
			packages.put(r.get("package"),r.get("top_package"));
			
			double [] aux = locs.get(r.get("class"));
			if(aux == null){
				aux = new double[2];
				locs.put(r.get("class"), aux);
			}
			
			if(r.get("loc").length() > 0){
//				System.out.println(file+" class "+r.get("class")+" "+r.get("loc"));
				aux[0] += Double.parseDouble(r.get("loc"));
				aux[1] += 1;
				
				aux = locs.get(r.get("package"));
				if(aux == null){
					aux = new double[2];
					locs.put(r.get("package"), aux);
				}
				aux[0] += Double.parseDouble(r.get("loc"));
				aux[1] += 1;
			}			
		}

		csvParser.close();
		reader.close();

		return new Map[]{simple_classes,classes,packages,locs};
	}

	private static List<List<String>> getMappings(String text, Map[] elements) {
		Set<String> found = new HashSet<>();
		List<List<String>> maps = new ArrayList<>();

		Map<String,String> basic_class = elements[0];
		Map<String,String> clas_pack = elements[1];
		Map<String,String> pack_top = elements[2];
		Map<String,double[]> locs = elements[3];

		for(String bc : basic_class.keySet())
			if(!found.contains(bc) && text.matches(".*\\b"+bc+"\\b.*")){ 
				String c = basic_class.get(bc);
				String p = clas_pack.get(c);
				String t = pack_top.get(p);

				maps.add(Arrays.asList(new String[]{c,p,t,Double.toString(locs.get(c)[0]),Double.toString(locs.get(p)[0]/locs.get(p)[1])}));

				found.add(bc);found.add(c);
				found.add(p);found.add(t);
			}

		for(String c : clas_pack.keySet()){
			if(!found.contains(c) && text.matches(".*\\b"+c+"\\b.*")){
				String p = clas_pack.get(c);
				String t = pack_top.get(p);

				maps.add(Arrays.asList(new String[]{c,p,t,Double.toString(locs.get(c)[0]),Double.toString(locs.get(p)[0]/locs.get(p)[1])}));


				found.add(c);
				found.add(p);found.add(t);
			}
		}

		for(String p : pack_top.keySet()){
			if(!found.contains(p) && text.matches(".*\\b"+p+"\\b.*")){

				String t = pack_top.get(p);
				maps.add(Arrays.asList(new String[]{null,p,t,null,Double.toString(locs.get(p)[0]/locs.get(p)[1])}));


				found.add(p);found.add(t);
			}
		}

		return maps;
	}

	public static List<List<String>> processIssues(String path_xml, Map[] elements, Set<String> all_elements) throws JDOMException, IOException{

		List<List<String>> records = new ArrayList<>();

		SAXBuilder builder = new SAXBuilder();
		Document document = (Document) builder.build(path_xml);
		Element rootNode = document.getRootElement();

		Element issue = rootNode.getChild("channel").getChild("item");

		String key = issue.getChild("key").getText().trim();
		String type = issue.getChild("type").getText().trim();
		String priority = issue.getChild("priority").getText().trim();

		String summary = issue.getChild("summary").getText().replaceAll(pattern_unico, "").replaceAll(pattern_html,"").replaceAll(pattern_tag," ").replaceAll(pattern_spaces, " ").trim();
		
		List<List<String>> mappings = getMappings(summary, elements);
		
		String d = ""; //TODO: Classify summary
		if(mappings.isEmpty())
			records.add(Arrays.asList(new String[]{key,type,priority,"summary",summary,null,null,null,null,null,d}));
		else{ 
			for(List<String> mm : mappings){
				List<String> aux = new ArrayList<>();
				aux.add(key); aux.add(type);aux.add(priority);aux.add("summary");
				aux.add(summary);aux.addAll(mm); aux.add(d);
				records.add(aux);
			}
		}

		String description = issue.getChild("description").getText().replaceAll(pattern_unico, "").replaceAll(pattern_html,"").replaceAll(pattern_tag," ").replaceAll(pattern_spaces, " ").trim();
		if(description.length() > 0){
			d = ""; //TODO: Classify description
			mappings = getMappings(description, elements);
			if(mappings.isEmpty())
				records.add(Arrays.asList(new String[]{key,type,priority,"description",description,null,null,null,null,null,d}));
			else{
				for(List<String> mm : mappings){
					List<String> aux = new ArrayList<>();
					aux.add(key); aux.add(type);aux.add(priority);aux.add("description");
					aux.add(description);aux.addAll(mm); aux.add(d);
					records.add(aux);
				}
			}
		}
			

		Element comms = issue.getChild("comments");
		if(comms != null){
			List<Element> comments = comms.getChildren("comment");
			if(comments != null){
				for(Element c : comments){
					String cc = c.getText().replaceAll(pattern_unico, "").replaceAll(pattern_html,"").replaceAll(pattern_tag," ").replaceAll(pattern_spaces, " ").trim();
					if(cc.length() == 0)
						continue;

					mappings = getMappings(cc, elements);
					String val = ""; // TODO: classify comment
					if(mappings.isEmpty())
						records.add(Arrays.asList(new String[]{key,type,priority,"comment",cc,null,null,null,null,null,val}));
					else{
						for(List<String> mm : mappings){
							List<String> aux = new ArrayList<>();
							aux.add(key); aux.add(type);aux.add(priority);aux.add("comment");
							aux.add(cc);aux.addAll(mm); aux.add(d);
							records.add(aux);
						}
					}

				}

					
			}

		}



		return records;
	}
	
}
