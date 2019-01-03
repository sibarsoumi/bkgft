package bkgft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Semaphore;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import com.google.gson.Gson;
import com.mashape.unirest.http.exceptions.UnirestException;
/**
 * This class takes reads tables that are stored as JSON Strings and generates a knowledge graph.
 * @author Sibar
 *
 */
public class GraphGenerator extends Thread{
	// RDF4J Repository where the knowledge graph will be stored.
	private static Repository repo;
	// Semaphore to restrict the number of simultaneously running threads to avoid RAM overload.
	private static Semaphore pool_limiter = new Semaphore(40);
	// Semaphore works as a mutex to ensure mutual exclusion when writing to the repository.
	private static Semaphore repo_guard = new Semaphore(1);
	private File file;
	/**
	 * Consructor sets the passed file.
	 * @param file
	 */
	private GraphGenerator(File file)
	{
		this.file=file;
		
	}
	
	/**
	 * It reads the file, iterates over all the tables found in this file, generates possible relations and stores them in the repository.
	 */
	@Override
	public void run()
	{
		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line = br.readLine();
			// Iterate over the lines in the given file (each line corresponds a table coded as JSON)
			while (line != null) {
				// Convert the read JSON to List<List<String>>
				List<List<String>> Table=new Gson().fromJson(line,List.class);
				Model model = RelationExtractor.calculate(Table);
				
				try {
					// Acquire Mutex of repository
					repo_guard.acquire();
					// Connect to the repository
					RepositoryConnection con = repo.getConnection();
					// Write the model to the repository
					try {
						// Iterate over the statements that have been extracted from the table and write them to the repository
					 for (Statement st: model) 
						 con.add(st);  
					   }
					   finally {
					      con.close();
					      // Release the mutex when writing finished
					      repo_guard.release();
					   }
					}
					catch (RDF4JException e) {
					   // handle exception
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
				
				line = br.readLine();
				    }
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnirestException e) {
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
	 * @param argv[0] path to the folder of the files that contain the tables (the output of {@link Reader}).
	 * @param argv[1] path to the folder of the output repository of the knowledge graph.
	 * @throws IOException
	 */
public static void main (String [] argv) throws IOException
{
	final File folder = new File(argv[0]);
	File dataDir = new File(argv[1]);
	repo = new SailRepository(new NativeStore(dataDir)); // Initialize an RDF4J Repository in the given path
	repo.initialize();
	int i=1;
	try {
		// Iterate over the files in the given folder
	for (final File fileEntry : folder.listFiles()) {
        if (!fileEntry.isDirectory())
        {
        	// Create a thread to handle the read tables in this file, after acquiring the semaphore
			pool_limiter.acquire();
    		new GraphGenerator(fileEntry).start();
    		System.out.println("File: "+i++);

        }
    }
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
}
