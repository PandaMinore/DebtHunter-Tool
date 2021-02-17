package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Evolution {

	//summary de a una sola feature
	public static void makeEvolution(String path,String prefix,String feature) throws IOException{
	
		Map<String,Map<String,String>> values = new HashMap<>(); // <Paquete, <Version, value>>
		Set<String> versions = new HashSet<>();
		
		
		File f = new File(path);
		for(File ff : f.listFiles()){
			
			if(ff.getName().startsWith(prefix)){
				
				String ver = ff.getName().replace(".txt", "").replace(prefix, "");
				versions.add(ver);
				
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(ff)));

				String l = in.readLine();
				int index = findIndex(l,feature);
				l = in.readLine();
				
				while(l != null){
					String sp [] = l.split(";");
					
					Map<String,String> vv = values.get(sp[0]);
					if(vv == null){
						vv = new HashMap<>();
						values.put(sp[0], vv);
					}
					
					vv.put(ver,sp[index]);
					
					l = in.readLine();
				}
				
				in.close();
				
			}
		} //we can now save the file
		
		List<String> versions_sorted = new ArrayList<>(versions);
		versions_sorted.sort(new NaturalOrderComparator());
		
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getAbsolutePath()+File.separator+"EVOLUTION_"+prefix+feature+".txt", false), "UTF8"));
		out.write("package;"+versions_sorted.toString().substring(1).replace(", ", ";").replace("]", "")+System.lineSeparator());
		
		for(String p : values.keySet()){
			Map<String,String> vals = values.get(p);
			out.write(p);
			for(String v : versions_sorted){
				String fe = vals.get(v);
				if(fe != null)
					fe = fe.replace("NaN", "-");
				out.write(";"+fe);
			}
				
			out.write(System.lineSeparator());
		}
		
		out.close();
		
	}

	private static int findIndex(String l, String feature) {
		String [] sp = l.split(";");
		for(int i=0;i<sp.length;i++)
			if(sp[i].equals(feature))
				return i;
		return -1;
	}
	
	public static void main(String[] args) throws IOException {
		System.out.println("Hello World! "+Evolution.class.getCanonicalName());
		
		String path = "C:/Users/Anto/Desktop/apache-camel";
		String prefix = "COMMENTS_TOP-PACKAGE_ANALYSIS-";
		String feature = "avg_loc";
		
		makeEvolution(path, prefix, feature);
		
	}
	
}
