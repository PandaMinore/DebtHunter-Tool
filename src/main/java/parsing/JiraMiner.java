package parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JiraMiner {
	
	//https://issues.apache.org/jira/browse/CAMEL-5122?jql=project%20%3D%20CAMEL%20AND%20affectedVersion%20%3D%202.2.0%20ORDER%20BY%20created%20DESC

	
	private static Set<String> getIssuesVersion(String base_url,String project, String version, String component) throws IOException{
		Set<String> issues = new HashSet<String>();

		String params = null;
		if(component != null)
			params = URLEncoder.encode("project = " + project + " AND affectedVersion = " + version + " AND component = " + component + " ORDER BY created DESC", "UTF-8");
		else
			params = URLEncoder.encode("project = " + project + " AND affectedVersion = "+version+" ORDER BY created DESC", "UTF-8");
			
		String url = base_url + "/browse/" + project + "-1?jql=" + params;

		Document doc = Jsoup.connect(url).get();
		
//		System.out.println(doc);
		
		Element ee = doc.getElementsByClass("navigator-content").first().getElementsByClass("list-content").first();

		Elements iss = ee.getElementsByAttribute("data-id");
		for(Element i : iss)
			issues.add(i.attr("data-key"));

		return issues;
	}

	public static void downloadIssuesAffectingVersion(String base_url,String saveDir, String project, String version, String component) throws IOException{

		Set<String> issues = getIssuesVersion(base_url,project,version,component);
		
		System.out.println(issues.size());
//		System.exit(0);
		
		String url = base_url+"/si/jira.issueviews:issue-xml/";

		File ff = new File(saveDir+File.separator+version);
		ff.mkdir();

		for(String issue : issues){

			String path = ff.getAbsolutePath()+File.separator+issue+".xml";
			if(new File(path).exists())
				continue;

			System.out.println(issue+" "+new Date());
			
			try{
				Document doc = Jsoup.connect(url+issue+"/"+issue+".xml").get();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, false), "UTF8"));
				writer.write(doc.toString());
				writer.close();
			}catch(IOException e){
				System.out.println(" -- ERROR: "+issue+" "+e.getMessage());
			}
			
		}

	}

	//if we already downloaded everything, we can filter out those belonging to the version we are interested in
	public static void filterIssues(String dir, String version, String component) throws IOException, JDOMException{

		File d = new File(dir+File.separator+version);
		d.mkdir();
		
		SAXBuilder builder = new SAXBuilder();
		
		File f = new File(dir);
		for(File ff : f.listFiles()){
			if(ff.isFile() && ff.getName().endsWith(".xml")){
				
				System.out.println(ff.getName());
				
				//check whether the file needs to be added
				org.jdom2.Document document = (org.jdom2.Document) builder.build(ff.getAbsolutePath());
				org.jdom2.Element rootNode = document.getRootElement();
				
				boolean isVersion = false;
				List<org.jdom2.Element> elements = rootNode.getChild("channel").getChild("item").getChildren("version");
				if(elements != null)
					for(org.jdom2.Element e : elements)
						if(e.getText().equals(version)){
							isVersion = true;
							break;
						}
				if(!isVersion)
					continue;
				
				if(component == null){
					System.out.println("	Copying ... "+ff.getName());
					Files.copy(ff.toPath(), Paths.get(d+File.separator+ff.getName()), StandardCopyOption.REPLACE_EXISTING);
					continue;
				}
					
				elements = rootNode.getChild("channel").getChild("item").getChildren("component");
				if(elements != null)
					for(org.jdom2.Element e : elements)
						if(e.getText().equals(component)){
							System.out.println("	Copying ... "+ff.getName());
							Files.copy(ff.toPath(), Paths.get(d+File.separator+ff.getName()), StandardCopyOption.REPLACE_EXISTING);
							break;
					}
			}
		}
		
	}

	//downloads every issue there is
	public static void downloadIssues(String saveDir,int initial, int end, String project){

		//CAMEL-1/CAMEL-1.xml
		String base_url = "https://issues.apache.org/jira/si/jira.issueviews:issue-xml/"+project+"-";
		for(int i=initial;i<=end;i++){

			String path = saveDir+File.separator+project+"-"+i+".xml";
			if(new File(path).exists())
				continue;

						String url = base_url+i+"/"+project+"-"+i+".xml";
//						System.out.println(url+" "+new Date());
//						URL obj = new URL(url);
//						BufferedReader in = null;
//						try{
//							HttpURLConnection con = (HttpURLConnection) obj.openConnection();
//							
//						    in = new BufferedReader(
//						             new InputStreamReader(con.getInputStream()));
//						    
//						}catch(IOException e){
//							System.out.println("  ERROR -- "+e.getMessage());
//						}
//							
//						if(in == null)
//							continue;
//						 String inputLine;
//					     StringBuilder response = new StringBuilder();
//					     while ((inputLine = in.readLine()) != null) {
//					     	response.append(inputLine+System.lineSeparator());
//					     }
//					    
//					     //we have the xml 
//					     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, false), "UTF8"));
//					     writer.write(response.toString());
//					     writer.close();

			
			try {
				System.out.println(url);
				Document doc = Jsoup.connect(url).get();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, false), "UTF8"));
				writer.write(doc.toString());
				writer.close();
			} catch (IOException e) {
				System.out.println(" -- ERROR: "+url+" "+e.getMessage());
			}
			

		}	
	}
	
	public static void main(String[] args) throws Exception {

		String saveDir = "D:/apache-camel/issues";
		int initial = 1;
		int end = 15000;
		String project = "CAMEL";
		String version = "2.0.0";
		String component = "camel-core";
		String base_url = "https://issues.apache.org/jira";
		
//		downloadIssues(saveDir, initial, end, project);

		String [] versions = new String[]{"2.0.0","2.1.0","2.2.0","2.3.0",
										  "2.4.0","2.5.0","2.6.0","2.7.0",
										  "2.8.0","2.9.0","2.10.0","2.11.0",
										  "2.12.0","2.13.0","2.14.0","2.15.0",
								  "2.16.0","2.17.0","2.18.0","2.19.0","2.20.0"};
		
//		downloadIssuesAffectingVersion(saveDir,project,version,component);
		
//		saveDir = "C:/Users/Anto/Desktop/cxf/issues";
//		project = "CXF";
//		version = "2.7";
//		component = "Core";
//		downloadIssuesAffectingVersion(base_url,saveDir,project,version,component);
		
		saveDir = "C:/Users/Anto/Desktop/hibernate/issues";
		project = "HHH";
		version = "5.4.0";
		component = "hibernate-core";
		base_url = "https://hibernate.atlassian.net";
		downloadIssuesAffectingVersion(base_url,saveDir,project,version,component);
		
//		for(String v : versions)
//			filterIssues(saveDir, v,component);
			
//		filterIssues(saveDir, version,component);
		
//		System.out.println(getIssuesVersion(project, version));


	}

}
