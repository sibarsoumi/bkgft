package bkgft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * This class extracts the links from a given file having the format:
 * {link}{data}\n{link}{data}\n{link}{data}\n....{link}{data}\n,
 * replaces the links beginning in "http://enwikipedia" with "http://enwikipedia" and saves them as a list with the format:
 * {link}\n{link}\n{link}\n....{link}\n
 * @author Sibar
 *
 */
public class LinksExtractor {
	/**
	 * The main and only method of the class.
	 * @param argv[0] path of the file to be read from.
	 * @param argv[1] path of the output file.
	 * @throws IOException
	 */
public static void main (String [] argv) throws IOException
	{
	// Initialize reading buffer
	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(argv[0]),"UTF-16"));
	// Initialize writing buffer
	BufferedWriter writer = new BufferedWriter(new FileWriter(argv[1]));
	try {
		// Iterate over the the lines
	    String line = br.readLine();
	    int i=1;
	    while (line != null) {
	    	// Make sure the line starts with a link
	    	Matcher m = Pattern.compile("^http:[^\\s]+\\s").matcher(line);
	    	// If this holds, write the link to the buffer after fixing it by adding dot between 'en' and 'wikipedia'
	    	if (m.find())
	    	  writer.write((line.substring(0,m.end()-1).replace("http://enwikipedia", "http://en.wikipedia"))+System.lineSeparator());
	    	
	    	System.out.println("Line: "+i++);
	    	// read next
	        line = br.readLine();
	    }
	} finally {
	    br.close();
	    writer.close();
	}
	System.out.println("finished.\n ");
	}
}
