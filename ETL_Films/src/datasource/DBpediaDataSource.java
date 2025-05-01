package datasource;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides methods to interact with the DBpedia SPARQL endpoint to retrieve movie details.
 * Implements the DataSource interface for consistent integration with other components.
 */
public class DBpediaDataSource{
    // SPARQL endpoint configuration
    private static final String ENDPOINT = "https://dbpedia.org/sparql";
    
    // Common SPARQL prefixes used in queries
    private static final String PREFIXES =
        "PREFIX owl:    <http://www.w3.org/2002/07/owl#>\n" +
        "PREFIX xsd:    <http://www.w3.org/2001/XMLSchema#>\n" +
        "PREFIX rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
        "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#>\n" +
        "PREFIX foaf:   <http://xmlns.com/foaf/0.1/>\n" +
        "PREFIX dc:     <http://purl.org/dc/elements/1.1/>\n" +
        "PREFIX dbo:    <http://dbpedia.org/ontology/>\n" +
        "PREFIX dbp:    <http://dbpedia.org/property/>\n" +
        "PREFIX dbr:    <http://dbpedia.org/resource/>\n" +
        "PREFIX skos:   <http://www.w3.org/2004/02/skos/core#>\n" +
        "PREFIX bif:    <http://www.openlinksw.com/schemas/bif#>\n";
    
    // Logger for error handling and debugging
    private static final Logger logger = Logger.getLogger(DBpediaDataSource.class.getName());
    
    /**
     * Constructor for DBpediaDataSource.
     * Initializes the logger level.
     */
    public DBpediaDataSource() {
        logger.setLevel(Level.WARNING);
    }
    
    /**
     * Executes a SPARQL query on the DBpedia endpoint and returns the result set.
     * 
     * @param sparqlQuery The SPARQL query to execute
     * @return The ResultSet containing query results, or null if there was an error
     */
    private ResultSet executeSparqlQuery(String sparqlQuery) {
        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution queryExecution = QueryExecutionFactory.sparqlService(ENDPOINT, query)) {
            return queryExecution.execSelect();
        } catch (Exception e) {
            logger.warning("SPARQL query failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    
    @Override
    public ArrayList<Object> getMovieByTitle(String movieTitle, boolean caseSensitive) {
        ArrayList<Object> results = new ArrayList<>();
        
        String escapedTitle = movieTitle.replace("'", "\\'");
        
        // Construct the SPARQL query
        String sparqlQuery = PREFIXES +
            "SELECT DISTINCT ?movieLabel WHERE {" +
            "  ?movie a dbo:Film; rdfs:label ?movieLabel. " +
            "  FILTER(lang(?movieLabel)='en' && ";
            
        if (caseSensitive) {
            sparqlQuery += "regex(?movieLabel, '" + escapedTitle + "'))";
        } else {
            sparqlQuery += "regex(?movieLabel, '" + escapedTitle + "', 'i'))";
        }
        
        sparqlQuery += "} LIMIT 50";
        
        ResultSet resultSet = executeSparqlQuery(sparqlQuery);
        if (resultSet == null) {
            return results;
        }
        

        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            String label = solution.getLiteral("movieLabel").getString();
            
            // For each found movie, get its details
            ArrayList<ArrayList<Object>> movieDetails = getMovieDetails(label);
            HashMap<String, Object> movieData = new HashMap<>();
            movieData.put("title", label);
            movieData.put("actors", movieDetails.get(0));
            movieData.put("directors", movieDetails.get(1));
            movieData.put("producers", movieDetails.get(2));
            
            results.add(movieData);
        }
        
        return results;
    }
    
    
    @Override
    public ArrayList<Object> getMoviesByActor(String actorName, boolean caseSensitive) {
        ArrayList<Object> results = new ArrayList<>();
        
        // Escape special characters in the actor name
        String escapedName = actorName.replace("'", "\\'");
        
        // Construct the SPARQL query with appropriate case sensitivity
        String sparqlQuery = PREFIXES +
            "SELECT DISTINCT ?movieLabel WHERE {" +
            "  ?actor rdfs:label ?actorLabel. ";
            
        if (caseSensitive) {
            sparqlQuery += "  FILTER(lang(?actorLabel)='en' && regex(?actorLabel, '" + escapedName + "')). ";
        } else {
            sparqlQuery += "  FILTER(lang(?actorLabel)='en' && regex(?actorLabel, '" + escapedName + "', 'i')). ";
        }
        
        sparqlQuery += 
            "  ?movie a dbo:Film; (dbo:starring|dbp:starring) ?actor; rdfs:label ?movieLabel. " +
            "  FILTER(lang(?movieLabel)='en')." +
            "} ORDER BY ?movieLabel LIMIT 100";
        
        // Execute the query and process results
        ResultSet resultSet = executeSparqlQuery(sparqlQuery);
        if (resultSet == null) {
            logger.warning("No results for actor query, possible SPARQL endpoint error");
            return results;
        }
        
        // Process each result into a data structure
        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            String movieLabel = solution.getLiteral("movieLabel").getString();
            
            // Extract year from movie title if present (e.g., "Movie Title (2020)")
            String year = "";
            Pattern yearPattern = Pattern.compile("\\((\\d{4})\\)");
            Matcher matcher = yearPattern.matcher(movieLabel);
            if (matcher.find()) {
                year = matcher.group(1);
            }
            
            // For each found movie, create a data structure
            HashMap<String, Object> movieData = new HashMap<>();
            movieData.put("title", movieLabel);
            movieData.put("year", year);
            
            // Optionally get more details about the movie (directors, actors, etc.)
            ArrayList<ArrayList<Object>> details = getMovieDetails(movieLabel);
            movieData.put("actors", details.get(0));
            movieData.put("directors", details.get(1));
            movieData.put("producers", details.get(2));
            
            results.add(movieData);
        }
        
        return results;
    }
    
   
    public ArrayList<ArrayList<Object>> getMovieDetails(String movieLabel) {
        ArrayList<ArrayList<Object>> moviesDetails = new ArrayList<>();
        moviesDetails.add(new ArrayList<>()); // Actors
        moviesDetails.add(new ArrayList<>()); // Directors
        moviesDetails.add(new ArrayList<>()); // Producers
        
        // Escape special characters in movie label
        String escapedLabel = movieLabel.replace("'", "\\'");
        
        // Construct optimized SPARQL query for movie details
        String sparqlQuery = PREFIXES +
            "SELECT (GROUP_CONCAT(DISTINCT ?dirLabel; SEPARATOR=\", \" ) AS ?Directors) " +
            "(GROUP_CONCAT(DISTINCT ?prodLabel; SEPARATOR=\", \" ) AS ?Producers) " +
            "(GROUP_CONCAT(DISTINCT ?actorLabel; SEPARATOR=\", \" ) AS ?Actors) WHERE {" +
            "  ?film a dbo:Film; rdfs:label ?titleVar. " +
            "  FILTER(lang(?titleVar)='en' && ?titleVar = \"" + escapedLabel + "\"@en). " +
            "  OPTIONAL { ?film (dbo:director|dbp:director) ?dir. ?dir rdfs:label ?dirLabel FILTER(lang(?dirLabel)='en') }. " +
            "  OPTIONAL { ?film (dbo:producer|dbp:producer|dbp:producers) ?prod. ?prod rdfs:label ?prodLabel FILTER(lang(?prodLabel)='en') }. " +
            "  OPTIONAL { ?film (dbo:starring|dbp:starring) ?actor. ?actor rdfs:label ?actorLabel FILTER(lang(?actorLabel)='en') }. " +
            "} GROUP BY ?film ?titleVar LIMIT 1";
        
        // Execute the query
        ResultSet resultSet = executeSparqlQuery(sparqlQuery);
        if (resultSet == null || !resultSet.hasNext()) {
            return moviesDetails;
        }
        
        // Process the single result with combined data
        QuerySolution solution = resultSet.nextSolution();
        String[] categories = {"Actors", "Directors", "Producers"};
        
        for (int i = 0; i < categories.length; i++) {
            ArrayList<Object> categoryList = moviesDetails.get(i);
            Literal literal = solution.getLiteral(categories[i]);
            
            if (literal != null) {
                String names = literal.getString();
                for (String name : names.split(",\\s*")) {
                    if (!name.trim().isEmpty()) {
                        categoryList.add(name.trim());
                    }
                }
            }
        }
        
        return moviesDetails;
    }
    
  
    public void setLogLevel(Level level) {
        logger.setLevel(level);
    }
}