package bkgft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.gson.Gson;

/**
 * This class takes reads tables from wikipedia articles, preprocesses them and stores them as JSON strings in files.
 * @author Sibar
 *
 */
public class Reader extends Thread {
	// Semaphore to restrict the number of simultaneously running threads to avoid RAM overload.
	private static Semaphore pool_limiter = new Semaphore(40);
	private static String output_path;
	private String url;
	/**
	 * Constructor sets the passed url.
	 * @param url
	 */
	private Reader (String url)
	{
		this.url=url;
	}
	
	/**
	 * It connects to wikipedia, fetches the article, preprocesses all of its tables and store them as JSON (each table is a line) in a text file in the given folder.
	 */
	@Override
	public void run()
	{
		
		try {	
			Matcher m = Pattern.compile("^http://en.wikipedia.org/wiki/").matcher(url);
	    	if (m.find())
	    	{	 
	    		BufferedWriter writer = null;

	    		try {
	    		Document doc = Jsoup.connect(url).get();
	    		// Select tables from class 'table.wikitable'
	    		Elements listOfTables=doc.select("table.wikitable");
	    		// Iterate over all found tables
	    		for (Element table:listOfTables)
	    		{
	    		// Read the table and dissolve colspans and rowspans
	    				final List<List<String>> result = table.select("table tr")
	    			    .stream()
	    			    // Select all <td> tags in single row
	    			    .map(tr -> tr.select("td"))
	    			    // Repeat n-times those <td> that have colspan="n" attribute
	    			    .map(rows -> rows.stream()
	    			        .map(td -> Collections.nCopies(td.hasAttr("colspan") ? Integer.valueOf(td.attr("colspan")) : 1, td))
	    			        .flatMap(Collection::stream)
	    			        .collect(Collectors.toList())
	    			    )
	    			    // Fold final structure to 2D List<List<Element>>
	    			    .reduce(new ArrayList<List<Element>>(), (acc, row) -> {
	    			        // First iteration - just add current row to a final structure
	    			        if (acc.isEmpty()) {
	    			            acc.add(row);
	    			            return acc;
	    			        }

	    			        // If last array in 2D array does not contain element with `rowspan` - append current
	    			        // row and skip to next iteration step
	    			        final List<Element> last = acc.get(acc.size() - 1);
	    			        if (last.stream().noneMatch(td -> td.hasAttr("rowspan"))) {
	    			            acc.add(row);
	    			            return acc;
	    			        }

	    			        // In this case last array in 2D array contains an element with `rowspan` - we are going to
	    			        // add this element n-times to current rows where n == rowspan - 1
	    			        final AtomicInteger index = new AtomicInteger(0);
	    			        last.stream()
	    			            // Map to a helper list of (index in array, rowspan value or 0 if not present, Jsoup element)
	    			            .map(td -> Arrays.asList(index.getAndIncrement(), Integer.valueOf(td.hasAttr("rowspan") ? td.attr("rowspan") : "0"), td))
	    			            // Filter out all elements without rowspan
	    			            .filter(it -> ((int) it.get(1)) > 1)
	    			            // Add all elements with rowspan to current row at the index they are present 
	    			            // (add them with `rowspan="n-1"`)
	    			            .forEach(it -> {
	    			                final int idx = (int) it.get(0);
	    			                final int rowspan = (int) it.get(1);
	    			                final Element td = (Element) it.get(2);

	    			                row.add(idx, rowspan - 1 == 0 ? (Element) td.removeAttr("rowspan") : td.attr("rowspan", String.valueOf(rowspan - 1)));
	    			            });

	    			        acc.add(row);
	    			        return acc;
	    			    }, (a, b) -> a)
	    			    .stream()
	    			    // Extract inner HTML text from Jsoup elements in 2D array
	    			    .map(tr -> tr.stream()
	    			        .map(Element::text)
	    			        .collect(Collectors.toList())
	    			    )
	    			    .collect(Collectors.toList());
	    		
	    		// Select header of the table
	    		Elements header = table.getElementsByTag("th");
	    		// Add header as first row in the table
	    		if (!header.isEmpty()) 
	    		{
	    			List<String> headers=new LinkedList<String>();
	    			for (Element e:header)
	    				headers.add(e.ownText());
	    			result.add(0, headers);
	    		
	    		}
	    		// Remove empty rows or rows
	    		result.removeIf(p -> p.isEmpty());
	    		
	    		// Compute the length of longest row
	    		int longest=result.get(0).size();
	    		for (List r:result)
	    			if (r.size()>longest)
	    				longest=r.size();
	    		
	    		// Remove all rows that have less than this length
	    		for (int i=0;i<result.size();i++)
	    			if (result.get(i).size()<longest)
	    				result.remove(i--);
	    		
	    		// Make sure the table has 2 rows at least
	    		if (result.size()<2) break;
	    		
	    		// Initialize writing buffer if not yet initialized.
	    		if (writer==null) writer=new BufferedWriter(new FileWriter(output_path+url.substring(m.end(), url.length())));
	    		
	    		// Write the table as JSON String as a line
	    		writer.write(new Gson().toJson(result)+System.lineSeparator());
		
	    		}
	    		
	    		}
	    		finally
	    		{
	    			if (writer!=null) writer.close();
	    		}
	    		
	    	}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			// Finally release the semaphore.
			pool_limiter.release();
		}
		
	}
	/**
	 * 
	 * @param argv[0] path to the file that contains the list of URLs.
	 * @param argv[1] path to the folder of output files.
	 * @throws Exception
	 */
public static void main (String [] argv) throws Exception
{
	// Initialize reading buffer
	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(argv[0])));
	// Setting the output path
	output_path=argv[1];
	try {
		// Iterate over the the lines
		String line = br.readLine();
		int i=1;
		while (line != null) {
					// Create a thread to handle the read URL, after acquiring the semaphore
					pool_limiter.acquire();
			    	new Reader(line).start();
			    	System.out.println("Line: "+i++);
			    	// read next
			        line = br.readLine();
			    }
	}
	finally
	{
		br.close();
	}
}
}
