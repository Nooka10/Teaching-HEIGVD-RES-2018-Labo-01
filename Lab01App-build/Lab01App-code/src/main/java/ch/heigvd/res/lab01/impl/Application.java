package ch.heigvd.res.lab01.impl;

import ch.heigvd.res.lab01.impl.explorers.DFSFileExplorer;
import ch.heigvd.res.lab01.impl.transformers.CompleteFileTransformer;
import ch.heigvd.res.lab01.interfaces.IApplication;
import ch.heigvd.res.lab01.interfaces.IFileExplorer;
import ch.heigvd.res.lab01.interfaces.IFileVisitor;
import ch.heigvd.res.lab01.quotes.Quote;
import ch.heigvd.res.lab01.quotes.QuoteClient;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Olivier Liechti
 */
public class Application implements IApplication {
	
	/**
	 * This constant defines where the quotes will be stored. The path is relative to where the Java application is invoked.
	 */
	public static String WORKSPACE_DIRECTORY = "./workspace/quotes";
	
	private static final Logger LOG = Logger.getLogger(Application.class.getName());
	
	public static void main(String[] args) {
		
		/*
		 * I prefer to have LOG output on a single line, it's easier to read. Being able
		 * to change the formatting of console outputs is one of the reasons why it is
		 * better to use a Logger rather than using System.out.println
		 */
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%6$s%n");
		
		
		int numberOfQuotes = 0;
		try {
			numberOfQuotes = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.err.println("The command accepts a single numeric argument (number of quotes to fetch)");
			System.exit(-1);
		}
		
		Application app = new Application();
		try {
			/*
			 * Step 1 : clear the output directory
			 */
			app.clearOutputDirectory();
			
			/*
			 * Step 2 : use the QuotesClient to fetch quotes; store each quote in a file
			 */
			app.fetchAndStoreQuotes(numberOfQuotes);
			
			/*
			 * Step 3 : use a file explorer to traverse the file system; print the name of each directory and file
			 */
			Writer writer = new StringWriter(); // we create a special writer that will send characters into a string (memory)
			app.printFileNames(writer);         // we hand over this writer to the printFileNames method
			LOG.info(writer.toString());       // we dump the whole result on the console
			
			/*
			 * Step 4 : process the quote files, by applying 2 transformations to their content
			 *          (convert to uppercase and add line numbers)
			 */
			app.processQuoteFiles();
			
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not fetch quotes. {0}", ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	@Override
	public void fetchAndStoreQuotes(int numberOfQuotes) throws IOException {
		clearOutputDirectory();
		QuoteClient client = new QuoteClient();
		for (int i = 0; i < numberOfQuotes; i++) {
			Quote quote = client.fetchQuote();
			/* There is a missing piece here!
			 * As you can see, this method handles the first part of the lab. It uses the web service
			 * client to fetch quotes. We have removed a single line from this method. It is a call to
			 * one method provided by this class, which is responsible for storing the content of the
			 * quote in a text file (and for generating the directories based on the tags).
			 */
			storeQuote(quote, "quote-" + i + ".utf8");
			LOG.info("Received a new joke with " + quote.getTags().size() + " tags.");
			for (String tag : quote.getTags()) {
				LOG.info("> " + tag);
			}
		}
	}
	
	/**
	 * This method deletes the WORKSPACE_DIRECTORY and its content. It uses the apache commons-io library. You should call this method in the main method.
	 *
	 * @throws IOException
	 */
	void clearOutputDirectory() throws IOException {
		FileUtils.deleteDirectory(new File(WORKSPACE_DIRECTORY));
	}
	
	/**
	 * This method stores the content of a quote in the local file system. It has 2 responsibilities:
	 * <p>
	 * - with quote.getTags(), it gets a list of tags and uses it to create sub-folders (for instance, if a quote has three tags "A", "B" and "C", it will be stored in
	 * /quotes/A/B/C/quotes-n.utf8.
	 * <p>
	 * - with quote.getQuote(), it has access to the text of the quote. It stores this text in UTF-8 file.
	 *
	 * @param quote
	 * 		the quote object, with tags and text
	 * @param filename
	 * 		the name of the file to create and where to store the quote text
	 *
	 * @throws IOException
	 */
	void storeQuote(Quote quote, String filename) throws IOException {
		String fileSeparator = File.separator; // contains the system-dependent default name-separator character
		StringBuilder path = new StringBuilder(WORKSPACE_DIRECTORY + fileSeparator);
		List<String> tags = quote.getTags(); // gets all the tags of the quote.
		
		for (String tag : tags) { // create the path depending on the tags of this quote.
			path.append(tag).append(fileSeparator); // gives "./workspace/quotes/A/B/C/" for quote with tags A, B and C.
		}
		
		File dir = new File(path.toString());
		if (!dir.exists()) { // if the directory does not exist, we try to create it
			dir.mkdirs();
		}
		
		File file = new File(path.append(filename).toString()); // We create the file
		
		BufferedWriter writer = new BufferedWriter(new PrintWriter(file));
		writer.write(quote.getQuote()); // we write the quote in the file, then we flush and close the file.
		writer.flush();
		writer.close();
	}
	
	/**
	 * This method uses a IFileExplorer to explore the file system and prints the name of each encountered file and directory.
	 */
	void printFileNames(final Writer writer) {
		final IFileExplorer explorer = new DFSFileExplorer();
		explorer.explore(new File(WORKSPACE_DIRECTORY), new IFileVisitor() {
			@Override
			public void visit(File file) {
				/*
				 * There is a missing piece here. Notice how we use an anonymous class here. We provide the implementation
				 * of the the IFileVisitor interface inline. You just have to add the body of the visit method, which should
				 * be pretty easy (we want to write the filename, including the path, to the writer passed in argument).
				 */
				try {
					writer.write(file.getPath() + "\n");
				} catch (IOException e) {
					Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, e);
				}
			}
		});
	}
	
	@Override
	public String getAuthorEmail() {
		return "benoit.schopfer@heig-vd.ch";
	}
	
	@Override
	public void processQuoteFiles() throws IOException {
		IFileExplorer explorer = new DFSFileExplorer();
		explorer.explore(new File(WORKSPACE_DIRECTORY), new CompleteFileTransformer());
	}
	
}
