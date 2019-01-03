package bkgft;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.json.JSONArray;

import com.github.slugify.Slugify;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
/**
 * This class takes a table and extracts relations from it.
 * @author Sibar
 *
 */
public class RelationExtractor {
	/**
	 * This is the main and only function of the class that has to be called to generate relations from the passed table.
	 * @param table The given table
	 * @return
	 * @throws UnirestException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static Model calculate (List<List<String>> table) throws UnirestException, MalformedURLException, IOException
	{
	// converting <List<List<String>> to 2D Array
		String[][] mytable = table.stream()
			    .map((l) -> l.toArray(new String[l.size()]))
			    .collect(Collectors.toList())
			    .toArray(new String[table.size()][]);
	
	// Initialize a new model
		ModelBuilder builder = new ModelBuilder();
	// Prefix for our namespace
		builder.setNamespace("my", "http://bkgft.upb.de/");
	
		ValueFactory vf = SimpleValueFactory.getInstance();
		
		// Named entity disambiguation
		//		Iterate over all cells in the table
		for (int i=1;i<mytable.length;i++)
		{
			for (int j=0;j<mytable[i].length;j++)
				{
				// Disambiguate using the AGDISTIS API
				HttpResponse<String> response = Unirest.post("http://akswnc9.informatik.uni-leipzig.de:8113/AGDISTIS")
				  .header("content-type", "application/x-www-form-urlencoded")
				  .body("text='<entity>"+mytable[i][j]+"</entity>'")
				  .asString();
				String responseString =new JSONArray(response.getBody()).getJSONObject(0).getString("disambiguatedURL");
				// If disambiguated successfully, replace the text of the cell with the uri
				mytable[i][j]=(responseString.contains("http://aksw.org/notInWiki/"))?mytable[i][j]:responseString;
				
				}
		}
		
		// Identify the subject/object column
		int j=-1;
		for (int i=0;i<mytable[0].length;i++)
			if (Pattern.compile("[a-zA-Z]").matcher(mytable[1][i]).find())
			{
				j=i;
				break;
			}
		// if j!=-1, it is now the index of the first column that is neither a number nor a date, namely the subject/object column
		
		
		if (j!=-1)
		// Iterate over the rows of the table
		for (int i=1;i<mytable.length;i++)
		{		// Check a cell from the subject/object column pairwise with all other cells in the current row (i.e. with all other columns)
				for (int k=0;k<mytable[i].length;k++)
					if (j!=k)
						// If both cells are linked to uris (Entity-Entity check)
					if (mytable[i][k].contains("http://dbpedia.org/") && mytable[i][j].contains("http://dbpedia.org/"))
					{	
						// Try to find possible relations between the both cells considering the cell in the table header as a predicate
						List<String> relations_j2k=PropertiesQuerier.getPossibleRelationsEntityEntity(mytable[i][j], mytable[0][k], mytable[i][k]);
						List<String> relations_k2j=PropertiesQuerier.getPossibleRelationsEntityEntity(mytable[i][k], mytable[0][j], mytable[i][j]);
						
						// If relations found, add them to the model. Otherwise, add them under our namespace.
						if (!relations_j2k.isEmpty())
							for (String rel:relations_j2k)
								builder.subject(mytable[i][j]).add(rel, vf.createIRI(mytable[i][k]));
						else if (Pattern.compile("[a-zA-Z]").matcher(mytable[0][k]).find())
								{builder.subject("my:"+new Slugify().slugify(mytable[0][k])).add(RDFS.LABEL, vf.createLiteral((mytable[0][k]),"en"));
								builder.subject(mytable[i][j]).add("my:"+new Slugify().slugify(mytable[0][k]), vf.createIRI(mytable[i][k]));}
						
						// If relations found, add them to the model. Otherwise, add them under our namespace.
						if (!relations_k2j.isEmpty())
							for (String rel:relations_k2j)
								builder.subject(mytable[i][k]).add(rel, vf.createIRI(mytable[i][j]));
						else if (Pattern.compile("[a-zA-Z]").matcher(mytable[0][j]).find())
								{builder.subject("my:"+new Slugify().slugify(mytable[0][j])).add(RDFS.LABEL, vf.createLiteral(mytable[0][j],"en"));
								builder.subject(mytable[i][k]).add("my:"+new Slugify().slugify(mytable[0][j]), vf.createIRI(mytable[i][j]));}
					}
						// If the cell not in the subject/object column is not linked to a uri (Entity-Literal check)
					else if ((!mytable[i][k].contains("http://dbpedia.org/")) && mytable[i][j].contains("http://dbpedia.org/"))
					{	
						// Try to find possible relations between the both cells considering the cell in the table header as a predicate
						List<String> relations_j2k=PropertiesQuerier.getPossibleRelationsEntityLiteral(mytable[i][j], mytable[0][k]);
						
						// If relations found, add them to the model. Otherwise, add them under our namespace.
						if (!relations_j2k.isEmpty())
							for (String rel:relations_j2k)
								builder.subject(mytable[i][j]).add(rel, mytable[i][k]);
						else if (Pattern.compile("[a-zA-Z]").matcher(mytable[0][k]).find())
								{builder.subject("my:"+new Slugify().slugify(mytable[0][k])).add(RDFS.LABEL, vf.createLiteral(mytable[0][k],"en"));
								builder.subject(mytable[i][j]).add("my:"+new Slugify().slugify(mytable[0][k]), mytable[i][k]);}		
					}
		}
		return builder.build();
	}
}
