package bkgft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.json.JSONObject;
/**
 * This class is responsible for querying from DBPedia
 * @author Sibar
 *
 */
public class PropertiesQuerier {
/**
 * 
 * @param a
 * @param b
 * @return
 * @throws MalformedURLException
 * @throws IOException
 */
	
protected static List<String> getForEntityEntity (String a, String b) throws MalformedURLException, IOException 
	{
	
	String query="SELECT ?prop WHERE\r\n" + 
			"{\r\n<" + 
			a+"> ?prop <"+b+"> .\r\n" + 
			"}";

	JSONObject result=request(query);
	List<String> properties = new LinkedList<String>();
	for (Object i:(result.getJSONObject("results").getJSONArray("bindings")))
		properties.add(((JSONObject)i).getJSONObject("prop").getString("value"));

	return properties;

	}

protected static List<String> getForEntityLiteral (String a, String b) throws MalformedURLException, IOException 
	{
	
	String queryA="SELECT ?prop WHERE\r\n" + 
			"{\r\n<" + 
			a+"> ?prop \""+b+"\"@en .\r\n" + 
			"}";
	
	String queryB="SELECT ?prop WHERE\r\n" + 
			"{\r\n<" + 
			a+"> ?prop ?o .\r\n" + 
			"FILTER regex(?o, \".*"+b+".*\", \"i\")"+
			"}";
	
	JSONObject resultA=request(queryA);
	JSONObject resultB=request(queryB);
	
	List<String> properties = new LinkedList<String>();
	
	for (Object i:(resultA.getJSONObject("results").getJSONArray("bindings")))
		properties.add(((JSONObject)i).getJSONObject("prop").getString("value"));

	for (Object i:(resultB.getJSONObject("results").getJSONArray("bindings")))
		properties.add(((JSONObject)i).getJSONObject("prop").getString("value"));
	
	return properties;

	}

protected static List<List<String>> getProperties (String a) throws MalformedURLException, IOException 
{
String query="PREFIX dbr: <http://dbpedia.org/resource/>\r\n" + 
		"PREFIX dbo: <http://dbpedia.org/ontology/>\r\n" + 
		"SELECT DISTINCT ?o ?label WHERE\r\n" + 
		"{\r\n" + a+
		" ?o ?x .\r\n" + 
		"?o rdfs:label ?label. FILTER(lang(?label)=\"en\")\r\n" + 
		"}";

JSONObject result=request(query);
List<List<String>> properties = new ArrayList<>();

for (Object i:(result.getJSONObject("results").getJSONArray("bindings")))
	properties.add(new ArrayList<String>(){{add(((JSONObject)i).getJSONObject("o").getString("value"));add(((JSONObject)i).getJSONObject("label").getString("value"));}});
	


return properties;

}
/**
 * This function gets both the subject and the object as uris and tries to find possible relations between them in DBPedia.
 * @param subject 
 * @param predicate
 * @param object
 * @return
 * @throws MalformedURLException
 * @throws IOException
 */
protected static List<String> getPossibleRelationsEntityEntity (String subject, String predicate, String object) throws MalformedURLException, IOException 
{
	predicate=putSlashesBeforeRegexMetacharacters(predicate);
	
	// Prepare the SPARQL query according to our approach
String query="SELECT ?relation WHERE\r\n" + 
		"{\r\n" + 
		"<"+subject+"> a ?typeOfSubject.\r\n" + 
		"?relation rdfs:domain ?typeOfSubject.\r\n" + 
		"?relation rdfs:range ?typeOfObject.\r\n" + 
		"<"+object+"> a ?typeOfObject.\r\n" + 
		"?relation rdfs:label ?lab.\r\n" + 
		"FILTER(lang(?lab)=\"en\").\r\n" + 
		"FILTER regex(?lab, \"^"+predicate+"$\", \"i\" ).\r\n" + 
		"}";


JSONObject result=request(query);
List<String> relations = new LinkedList<String>();

for (Object i:(result.getJSONObject("results").getJSONArray("bindings")))
	relations.add(((JSONObject)i).getJSONObject("relation").getString("value"));
	
return relations;

}
/**
 * This function gets the subject as a uri and tries to find possible relations in DBPedia.
 * @param subject
 * @param predicate
 * @return
 * @throws MalformedURLException
 * @throws IOException
 */
protected static List<String> getPossibleRelationsEntityLiteral (String subject, String predicate) throws MalformedURLException, IOException 
{
	predicate=putSlashesBeforeRegexMetacharacters(predicate);
	
	// Prepare the SPARQL query according to our approach
String query="SELECT ?relation WHERE\r\n" + 
		"{\r\n <" + 
		subject+"> a ?typeOfSubject.\r\n" + 
		"?relation rdfs:domain ?typeOfSubject.\r\n" + 
		"?relation rdfs:range ?typeOfObject.\r\n" + 
		"?typeOfObject a rdfs:Datatype.\r\n" + 
		"?relation rdfs:label ?lab.\r\n" + 
		"FILTER(lang(?lab)=\"en\").\r\n" + 
		"FILTER regex(?lab, \"^"+predicate+"$\", \"i\" ).\r\n" + 
		"}";

JSONObject result=request(query);
List<String> relations = new LinkedList<String>();

for (Object i:(result.getJSONObject("results").getJSONArray("bindings")))
	relations.add(((JSONObject)i).getJSONObject("relation").getString("value"));
	
return relations;

}
/**
 * This function gets a string that can contain regex meta characters, prepares it for string matching by preceding the regex meta character with slashes 
 * @param str
 * @return
 */
private static String putSlashesBeforeRegexMetacharacters(String str)
	{
	str=str.replace("(", "\\(");	str=str.replace(")", "\\)");	str=str.replace("[", "\\[");
	str=str.replace("]", "\\]");	str=str.replace("{", "\\{");	str=str.replace("}", "\\}");
	str=str.replace("^", "\\^");	str=str.replace("$", "\\$"); str=str.replace("|", "\\|");
	str=str.replace("?", "\\?");	str=str.replace("*", "\\*"); str=str.replace("+", "\\+");
	str=str.replace(".", "\\.");	str=str.replace("<", "\\<"); str=str.replace(">", "\\>");
	str=str.replace("-", "\\-");	str=str.replace("=", "\\="); str=str.replace("!", "\\!");
	str=str.replace("\\", "\\\\");
	return str;
	}

private static String readAll(Reader rd) throws IOException
	{
	StringBuilder sb = new StringBuilder();
	int cp;
	while ((cp = rd.read()) != -1) {
		sb.append((char) cp);
	}
	return sb.toString();
	}
/**
 * This function gets a SPARQL query, sends it via HTTP to DBPedia and returns the results as JSON Object.
 * @param query The SPARQL query to be sent to DBPedia
 * @return
 * @throws MalformedURLException
 * @throws IOException
 */
private static JSONObject request(String query) throws MalformedURLException, IOException
	{
	String mainGraph="http://dbpedia.org";
	String sparqlUrl="https://dbpedia.org/sparql";
	String graphEncoded = URLEncoder.encode(mainGraph, "UTF-8");
    String formatEncoded = URLEncoder.encode("application/sparql-results+json", "UTF-8");
    String queryEncoded = URLEncoder.encode(query, "UTF-8");
    String url = sparqlUrl+"?"+"default-graph-uri="+graphEncoded+"&query="+queryEncoded+"&format="+formatEncoded+"&debug=on&timeout=";

	InputStream is = new URL(url).openStream();
	try {
		BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
		String jsonText = readAll(rd);
		return new JSONObject(jsonText);
	} finally {
		is.close();
	}

	}

}
