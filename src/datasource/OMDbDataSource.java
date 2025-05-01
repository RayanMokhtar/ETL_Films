package datasource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Provides methods to interact with the OMDb API to retrieve movie information.
 * Implements the DataSource interface for consistent integration with other components.
 */
public class OMDbDataSource{
    private static final String API_KEY = "69d448d7"; // clé api personnnalisée pour timsah
    
    private static final Logger logger = Logger.getLogger(OMDbDataSource.class.getName());

    
    public OMDbDataSource() {
        logger.setLevel(Level.WARNING); // sur la console directement le logger
    }

   
	private StringBuilder getApiResponse(String movieTitle, String year) {
        StringBuilder response = new StringBuilder();
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        
        try {
            String encodedTitle = URLEncoder.encode(movieTitle, StandardCharsets.UTF_8.toString());
            
            String apiUrl = "http://www.omdbapi.com/?t=" + encodedTitle + 
                            (year.isEmpty() ? "" : "&y=" + year) + 
                            "&type=movie&apikey=" + API_KEY + "&r=xml";
            
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            // Read the response
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            // If movie not found and year was specified, try without year
            if (response.toString().contains("Movie not found!") && !year.isEmpty()) {
                logger.info("Movie not found with year, trying without year");
                
                // Close previous resources
                reader.close();
                conn.disconnect();
                
                // Retry without year
                apiUrl = "http://www.omdbapi.com/?t=" + encodedTitle + "&type=movie&apikey=" + API_KEY + "&r=xml";
                url = new URL(apiUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                // Clear previous response and read new one
                response.setLength(0);
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
        } catch (Exception e) {
            logger.warning("Error making API request: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            //appliquer la fermeture du flux 
            try {
                if (reader != null) reader.close();
                if (conn != null) conn.disconnect();
            } catch (Exception e) {
                logger.warning("Error closing resources: " + e.getMessage());
            }
        }
        
        return response;
    }



    public ArrayList<Object> getMovieByTitle(String movieTitle, boolean caseSensitive) {
        ArrayList<Object> results = new ArrayList<>();
        
        try {
            String plot = getMovieResume(movieTitle, "");
            if (plot != null && !plot.isEmpty()) {
                results.add(plot);
            }
        } catch (Exception e) {
            logger.warning("Error retrieving movie by title: " + e.getMessage());
            e.printStackTrace();
        }
        
        return results;
    }
    
	
   
    public ArrayList<Object> getMoviesByActor(String actorName, boolean caseSensitive) {
        logger.info("Searching by actor name is not supported in OMDb API");
        return new ArrayList<>();
    }


   

    private String extractResumeXml(StringBuilder xmlResponse) {
        if (xmlResponse == null || xmlResponse.length() == 0) {
            return null;
        }
        
        String resume = null;
        try {
            // Parse XML response into DOM document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entity processing for security
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse.toString())));

            // --- Extraction du synopsis via XPath sur le XML renvoyé par l'API OMDb ---
            XPath xpath = XPathFactory.newInstance().newXPath();

            // Define XPath expression to extract the plot
            XPathExpression plotExpr = xpath.compile("/root/movie/@plot");

            // Evaluate XPath and get the plot text
            resume = (String) plotExpr.evaluate(doc, XPathConstants.STRING);
        } catch (Exception e) {
            logger.warning("Error extracting plot from XML: " + e.getMessage());
            e.printStackTrace();
        }
        
        return resume;   
    }

   
    public String getMovieApiCall(String movieTitle, String year) {
        if (movieTitle == null || movieTitle.trim().isEmpty()) {
            logger.warning("Movie title cannot be null or empty");
            return null;
        }
        
        StringBuilder response = getApiResponse(movieTitle, year);
        if (response == null) {
            return null;
        }
        
        //cas extract => 
        String plot = extractResumeXml(response);
        //handle cas not null => 
        if (plot != null && !plot.isEmpty() && !plot.equals("N/A")) {
            plot = "<html>\n<p>" + plot + "</p>\n</html>";
        } else {
            logger.info("Pas de résumé pour le film: " + movieTitle);
            plot = null;
        }
        
        return plot;
    }
    
   
    public static String getMovieResume(String movieTitle, String year) {
        return new OMDbDataSource().getMovieApiCall(movieTitle, year);
    }
    
    
    public void setLogLevel(Level level) {
        logger.setLevel(level);
    }
}