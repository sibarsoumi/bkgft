package bkgft;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
/**
 * This class chooses some statements randomly from a knowledge graph given as ttl file.
 * P.S.: exclusively the statements that have rdfs:label as predicate.
 * @author Sibar
 *
 */
public class RandomlyChooser {
	/**
	 * Generates n random unrepeated numbers between 0 and max (max inclusive)
	 * @param n How many random numbers is required
	 * @param max Upperbound of the range from which the random numbers have to be chosen
	 * @return
	 * @throws Exception
	 */
	private static Set<Integer> getRandomNumbers(int n, int max) throws Exception {  
	    if (n>max) throw  new Exception ("Required random numbers are more than the upper allowed bound.");
	      
	   
	    Set<Integer> result = new HashSet<Integer>();
	    Set<Integer> used = new HashSet<Integer>();  
	    
	      // Repeat n times
	    for (int i = 0; i < n; i++) {
	    	// Generate a new random number till a number is generated that is not used before
	        int newRandom;  
	        do {  
	            newRandom = (int)(Math.random()*(max+1));  
	        } while (used.contains(newRandom));
	        // Add this number to the used and to the result
	        result.add(newRandom);
	        used.add(newRandom);  
	    }  
	    return result;  
	}  
/**
 * 
 * @param argv[0] The path of the ttl file of the given knowledge graph
 * @param argv[1] The path of output ttl file that contains the chosen statements
 * @param argv[2] How many statements must be chosen
 * @throws Exception
 */
public static void main (String [] argv) throws Exception
{
	// Some preperations
	int n=Integer.parseInt(argv[2]);
	String filename = argv[0];
	InputStream input = new FileInputStream(filename);
	Model model = Rio.parse(input, "", RDFFormat.TURTLE);
	ValueFactory vf = SimpleValueFactory.getInstance();
	
	// Exclude rdfs:label because they have no meaning in the evaluation
	IRI LabelUri = vf.createIRI("http://www.w3.org/2000/01/rdf-schema#label");
	model.remove(null,LabelUri,null);
	
	
	FileOutputStream out = new FileOutputStream(argv[1]);
	RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
	
	// Generate n random numbers 
	Set<Integer> arr = getRandomNumbers(n,model.size()-1);
	
	// Iterate over the statements of the given graph and choose only those whose index is in the generated array of random numbers
	int i=0;
	try { 
		  writer.startRDF();
		  for (Statement st: model)
				if (arr.contains(i++))
					 writer.handleStatement(st);
					
		 writer.endRDF();
		}
		catch (RDFHandlerException e) {
		
		}
		finally {
		  out.close();
		}
}
}
