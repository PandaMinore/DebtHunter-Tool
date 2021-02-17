package parsing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;

import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.mauricioaniche.ck.util.SourceCodeLineCounter;

import utils.FileIterator;


public class JavaParsing {

	private static Map<String,List<String>> parseClass(InputStream in) throws IOException{

		// parse the file
		CompilationUnit cu = null;

		Map<String,List<String>> elements = new HashMap<>();

		try{
			cu = JavaParser.parse(in);
		}catch(ParseProblemException e){
			System.out.println("PARSING ERROR!");
		}

		if(cu == null)
			return elements;

		String package_declaration = null;

		if(cu.getChildNodesByType(PackageDeclaration.class).size() == 0)
			package_declaration = "default";
		else
			package_declaration = cu.getChildNodesByType(PackageDeclaration.class).get(0).getNameAsString();

		List<ClassOrInterfaceDeclaration> classes = cu.getChildNodesByType(ClassOrInterfaceDeclaration.class);
		for(ClassOrInterfaceDeclaration ci : classes){ 

			String class_name = getQualifiedName(cu,ci,package_declaration);

			List<String> comments = ci.getAllContainedComments().stream().map(c -> c.getContent().trim().replaceAll("//|\\*","").replaceAll("\\s+", " ")).collect(Collectors.toList());

			elements.put(class_name, comments);

		}
		return elements;
	}


	static String getQualifiedName(CompilationUnit cu, ClassOrInterfaceDeclaration ci, String package_declaration) {

		StringBuilder sb = new StringBuilder();
		sb.append(package_declaration + ".");
		if(!ci.isNestedType())
			return sb.toString()+ci.getNameAsString();

		Optional<ClassOrInterfaceDeclaration> ancestor = ci.getAncestorOfType(ClassOrInterfaceDeclaration.class);
		while(ancestor.isPresent()){
			sb.append(ancestor.get().getNameAsString() + "#");
			ancestor = ancestor.get().getAncestorOfType(ClassOrInterfaceDeclaration.class);
		}

		sb.append(ci.getNameAsString());

		return sb.toString();
	}

	class MethodCallVisitor extends VoidVisitorAdapter<Void> {

		StringBuilder sb;

		MethodCallVisitor(StringBuilder s){
			sb = s;
		}

		@Override
		public void visit(MethodCallExpr n, Void arg) {
			// Found a method call
			if(n.getScope().isPresent())
				//	    		sb.append(n.getScope().get() + "." + n.getName()+" ");
				sb.append(n.getName() + " ");
			// Don't forget to call super, it may find more method calls inside the arguments of this method call, for example.
			super.visit(n, arg);
		}
	}
	
	public static void processDirectory(String path, boolean process) throws IOException{
		
		Boolean firstTime = true;
		
		File f = new File(path);
		for(File ff : f.listFiles()){
			String full_path = null;
			if(ff.isDirectory())
				full_path = ff.getAbsolutePath();
			else
				if(ff.isFile() && (ff.getName().endsWith(".jar") || ff.getName().endsWith(".zip") || ff.getName().endsWith("tag.gz")))
					full_path = ff.getAbsolutePath().substring(0,ff.getAbsolutePath().lastIndexOf("."));
			
			System.out.println(full_path);
			
			if(full_path == null)
				continue;
			
			if(process)
				processSourceCode(full_path);
			else
				saveComments(full_path, path, firstTime);
				firstTime = false;
		}
		
	}
	
	public static void saveComments(String full_path, String path, Boolean firstTime) throws IOException{
		
		System.out.println("Getting comments... " + full_path + " " + new Date());
		
		FileIterator it = FileIterator.getIterator(full_path);

		Map<String,List<String>> comments = new HashMap<>();
		Set<String> packages = new HashSet<>();
		InputStream clas = it.nextStream();
		while(clas != null){
			
			Map<String,List<String>> aux = parseClass(clas);
			if(aux.size() > 0){
				comments.putAll(aux);
				String cc = findClass(aux.keySet());
				if(cc != null)
					packages.add(cc.substring(0,cc.lastIndexOf(".")));
			}
				
			clas = it.nextStream();
		}
		
		if (firstTime ) {
			
			//save file
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(path + "/comments.csv"));
	
			Set<String> top_levels = getTopLevelPackages(packages);	
			
			//projectname		package		top_package		comment
	        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("projectname","package","top_package", "comment"));
	        String projectname = new File(full_path).getName();
	        for(String cla : comments.keySet()){
	        	
	        	String pack = cla.substring(0, cla.lastIndexOf("."));
	        	String top = getTop(top_levels, pack);
	        	
	        	List<String> comms = comments.get(cla);
	        	for(String c : comms){
	        		csvPrinter.printRecord(projectname, pack, top, c);
	        	}
	        }
	        
	        csvPrinter.flush();  
	        csvPrinter.close();
	        
		} else {
			FileWriter csv = new FileWriter(path + "/comments.csv", true);
			BufferedWriter writer = new BufferedWriter(csv);
			
			Set<String> top_levels = getTopLevelPackages(packages);	
			
			String projectname = new File(full_path).getName();
	        for(String cla : comments.keySet()){
	        	
	        	String pack = cla.substring(0, cla.lastIndexOf("."));
	        	String top = getTop(top_levels, pack);
	        	
	        	List<String> comms = comments.get(cla);
	        	for(String c : comms){
	        		String line = projectname + ", " + pack + ", " + top + ", " + c;
	        		writer.write(line);
	        		writer.newLine();
	        	}
	        }
			
			writer.close();
			
		}
	}
	
	public static void processSourceCode(String path) throws IOException{
		
		System.out.println("Processing... " + path + " " + new Date());
		
		FileIterator it = FileIterator.getIterator(path);

		Map<String,List<String>> comments = new HashMap<>();
		Map<String,Integer> loc = new HashMap<>(); 
		
		InputStream clas = it.nextStream();
		while(clas != null){

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			IOUtils.copy(clas, baos);
			
			Map<String,List<String>> aux = parseClass(new ByteArrayInputStream(baos.toByteArray()));
			if(aux.size() > 0){

				comments.putAll(aux);
				int locc = SourceCodeLineCounter.getNumberOfLines(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()))));
				String cc = findClass(aux.keySet());
				
				System.out.println(locc);
				System.out.println(cc);
				
				if(cc != null)
					loc.put(cc,locc);
			}
			
			clas = it.nextStream();
			
			for (String key: aux.keySet()) {
	            System.out.println("Key number : " + key);
	            System.out.println("Strings List : " +  aux.get(key));
	        }
			
		}
		
		
		
		
	}

	private static String getTop(Set<String> tops, String p) {
		String top = "";
		for(String t : tops)
			if(p.startsWith(t)){
				if(t.length() >= top.length())
					top = t;
			}
	
		return top;
	}

	private static String findClass(Set<String> keySet) {
		for(String k : keySet)
			if(!k.contains("#") && !k.contains("$"))
				return k;
		return "";
	}

	public static Set<String> getTopLevelPackages(Set<String> allPackage) {
		
		String highLevelRoot = org.apache.commons.lang3.StringUtils.getCommonPrefix(allPackage.toArray(new String[]{}));
		
		if(highLevelRoot.endsWith("."))
			highLevelRoot = highLevelRoot.substring(0, highLevelRoot.length()-1);

		if(highLevelRoot.length() > 0){
			Set<String> naive_packages = getNaivePackages(highLevelRoot,allPackage);
			if(highLevelRoot.contains("."))
				return naive_packages;
			
			Set<String> aux_packages = new HashSet<>();
			for(String np : naive_packages){
				
				Set<String> aux = getNaivePackages(np, allPackage);
					
				if(aux.size() == 1)
					aux_packages.addAll(aux);
				else
					aux_packages.add(np);
				
			}

			Set<String> packages = new HashSet<>();
			if(aux_packages.size() > 0){
				for(String ap : aux_packages){
					System.out.println(ap+" -- "+getNaivePackages(ap, allPackage));
					packages.addAll(getNaivePackages(ap, allPackage));
				}
					
				return packages;
			}
		}
		
		//we have no common root - we need to find the potential commons!
		Set<String> packages = new HashSet<>();
		Map<String,Set<String>> potentialRoots = new HashMap<>();
		for(String a : allPackage){
			int index = a.indexOf(".");
			String a_edited = a;
			if(index > 0)
				a_edited = a.substring(0,index);

			Set<String> aux = potentialRoots.get(a_edited);
			if(aux == null){
				aux = new HashSet<>();
				potentialRoots.put(a_edited, aux);
			}

			aux.add(a);
		}
			
		for(String pr : potentialRoots.keySet()){
			
//			highLevelRoot = org.apache.commons.lang3.StringUtils.getCommonPrefix(potentialRoots.get(pr).toArray(new String[]{}));
//			packages.addAll(getNaivePackages(highLevelRoot,potentialRoots.get(pr)));	
			packages.addAll(getTopLevelPackages(potentialRoots.get(pr)));
//			System.out.println(pr+" :: "+packages);
		}
		
		return packages;
	}

	private static Set<String> getNaivePackages(String topLevel,Set<String> packagesToSearch) {
		Set<String> packages = new HashSet<>();
				
		if(topLevel.endsWith("."))
			topLevel = topLevel.substring(0,topLevel.length()-1);
		
		for(String a : packagesToSearch){ //for every package we got here

			if(!a.startsWith(topLevel))
				continue;
			
			a = a.replace(topLevel, "");
		
			if(a.length() == 0)
				packages.add(topLevel);
			else{
				if(a.startsWith("."))
					a = a.substring(1);
					
				int index = a.indexOf(".");
				if(index > 0)
					a = a.substring(0,index);
					
				if(!a.contains("impl."))
					if(topLevel.length() > 0)
						packages.add(topLevel+"."+a);
					else
						packages.add(a);
				
//				else
//					packages.add(topLevel); //no need to check it, if there was a parent, we may have already added it
			
			}

		}
		return packages;
	}
	
	public static void main(String[] args) throws IOException {
		
		String path = args[0];
		processDirectory(path, false);

	}

}
