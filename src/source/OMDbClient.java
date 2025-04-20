package source;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
 */
public class OMDbClient {
	public static final  String API_KEY = "69d448d7"; // clé api personnnalisée pour timsah


	/**
	 * Sends a HTTP GET request to the OMDb API and retrieves the response.
	 * @param movieTitle The title of the movie to search for.
	 * @param year The release year of the movie.
	 * @return A StringBuilder containing the response from the API.
	 */
	private static StringBuilder getResponse(String movieTitle, String year) {
		StringBuilder response = null;
        try {
            // Effectuer la requête HTTP GET vers l'API OMDb
            URL url = new URL("http://www.omdbapi.com/?t=" + movieTitle + "&y=" + year + "&type=movie&apikey=" + API_KEY + "&r=xml");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
			if (response.toString().equals("<root response=\"False\"><error>Movie not found!</error></root>")){
				url = new URL("http://www.omdbapi.com/?t=" + movieTitle + "&type=movie&apikey=" + API_KEY + "&r=xml");
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				BufferedReader reader2 = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				response.delete(0, response.length());
				while ((line = reader2.readLine()) != null) {
					response.append(line);
				}
			}

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
	}

	/**
	 * Evaluates an XPath expression on the XML response from the OMDb API to extract the movie plot.
	 * @param httpResponse The XML response from the OMDb API.
	 * @return The plot summary of the movie.
	 */
	private static String evaluateXPathExpression(StringBuilder httpResponse) {
		String plot = null;
		try {
			 // Convertir la réponse XML en arbre DOM
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(new InputSource(new StringReader(httpResponse.toString())));

	        // Créer un objet XPath pour naviguer dans l'arbre DOM
	        XPath xpath = XPathFactory.newInstance().newXPath();

	        // Définir l'expression XPath pour extraire le résumé (plot) du film
	        XPathExpression plotExpr = xpath.compile("/root/movie/@plot");

	        // Évaluer l'expression XPath pour récupérer le résumé du film
	        plot = (String) plotExpr.evaluate(doc, XPathConstants.STRING);
		} catch (Exception e) {
			e.printStackTrace();
		}
        return plot;   
	}

	/**
	 * Retrieves the plot summary of a movie from the OMDb API.
	 * @param movieTitle The title of the movie to search for.
	 * @param year The release year of the movie.
	 * @return The plot summary of the movie in HTML format.
	 */
	public static String getMovieResume(String movieTitle, String year) {
		StringBuilder response = getResponse(movieTitle, year);
		String plot = evaluateXPathExpression(response);
		plot = "<html>\n<p>" + plot + "</p>\n</html>";
		return plot;
	}
	
}
	