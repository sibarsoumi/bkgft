package bkgft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
/**
 * This class converts an RDF4J repository to a knowledge graph as file written in turtle format
 * @author Sibar
 *
 */
public class RDFRepoToTTLConverter {
	/**
	 * 
	 * @param argv[0] path of the repository
	 * @param argv[1] path of output ttl file
	 * @throws IOException
	 */
public static void main (String [] argv) throws IOException
{
	File dataDir = new File(argv[0]);
	Repository db = new SailRepository(new NativeStore(dataDir));
	db.initialize();
	RepositoryConnection conn = db.getConnection();
	RepositoryResult<Statement> statements=conn.getStatements(null, null, null, true);
	FileOutputStream out = new FileOutputStream(argv[1]);
	RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
	try {
		  writer.startRDF();
		  while (statements.hasNext())
		  {
		  Statement st= statements.next() ;
		  writer.handleStatement(st);
		  }
		  writer.endRDF();
		}
		catch (RDFHandlerException e) {
		 
		}
		finally {
		  out.close();
		}
}
}
