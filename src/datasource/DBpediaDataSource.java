package datasource;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import datasource.DataSource;

/**
 * Provides methods to interact with the DBpedia SPARQL endpoint to retrieve movie details.
 * Implements the DataSource interface for consistent integration with other components.
 */
public class DBpediaDataSource implements DataSource {
    // SPARQL endpoint configuration
    private static final String ENDPOINT = "https://dbpedia.org/sparql";
    
    // Common SPARQL prefixes used in queries
    private static final String PREFIXES =
        "PREFIX text: <http://jena.apache.org/text#>\n" +
        "PREFIX dbo:  <http://dbpedia.org/ontology/>\n" +
        "PREFIX dbp:  <http://dbpedia.org/property/>\n" +
        "PREFIX dbr:  <http://dbpedia.org/resource/>\n" +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n";
    
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
        QueryExecution qexec = QueryExecutionFactory.sparqlService(ENDPOINT, query);
        try {
            return qexec.execSelect();
        } catch (Exception e) {
            logger.warning("SPARQL query failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            qexec.close();
        }
    }

    public ArrayList<Object> getMovieByTitle(String movieTitle, boolean caseSensitive) {
        ArrayList<Object> results = new ArrayList<>();
        String esc = movieTitle.replace("\\", "\\\\").replace("\"", "\\\"");
        String sparql =
            "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX dbo:  <http://dbpedia.org/ontology/>\n" +
            "SELECT DISTINCT ?movieLabel WHERE {" +
            "  BIND(\"" + esc + "\" AS ?searchTerm)" +
            "  ?movie a dbo:Film ; rdfs:label ?movieLabel ." +
            "  BIND(lcase(replace(str(?movieLabel), '[^A-Za-z0-9]', '')) AS ?normLabel)" +
            "  BIND(lcase(replace(str(?searchTerm), '[^A-Za-z0-9]', '')) AS ?normSearch)" +
            "  FILTER(STRSTARTS(?normLabel, ?normSearch))" +
            "  FILTER(lang(?movieLabel)='en')" +
            "} ORDER BY DESC(STRSTARTS(?normLabel, ?normSearch)) ASC(strlen(str(?movieLabel))) LIMIT 50";
        ResultSet rs = executeSparqlQuery(sparql);
        if (rs == null) return results;
        while (rs.hasNext()) {
            String label = rs.nextSolution().getLiteral("movieLabel").getString();
            HashMap<String, Object> map = new HashMap<>();
            map.put("title", label);
            map.put("actors", getMovieDetails(label).get(0));
            map.put("directors", getMovieDetails(label).get(1));
            map.put("producers", getMovieDetails(label).get(2));
            results.add(map);
        }
        return results;
    }
    
    public ArrayList<Object> getMoviesByActor(String actorName, boolean caseSensitive) {
        ArrayList<Object> results = new ArrayList<>();
        // conversion du nom de l'acteur en IRI DBpedia (espace -> underscore)
        String resource = actorName.trim().replace(" ", "_");
        // requête SPARQL fixe avec BIND sur dbr:Resource et UNION pour dbo:starring et dbp:starring
        String sparql = PREFIXES +
            "SELECT DISTINCT ?film ?filmLabel WHERE {" +
            "  BIND(dbr:" + resource + " AS ?actor)" +
            "  ?film a dbo:Film; rdfs:label ?filmLabel ." +
            "  FILTER(lang(?filmLabel) = 'en')" +
            "  { ?film dbo:starring ?actor } UNION { ?film dbp:starring ?actor }" +
            "} ORDER BY ?filmLabel";
        ResultSet rs = executeSparqlQuery(sparql);
        if (rs == null) return results;
        while (rs.hasNext()) {
            // on récupère uniquement le label pour le titre
            String raw = rs.nextSolution().getLiteral("filmLabel").getString();
            String label = raw.replaceAll("\\s*\\(\\d{4}\\)$", "");
            HashMap<String,Object> map = new HashMap<>();
            map.put("title", label);
            map.put("actors", getMovieDetails(label).get(0));
            map.put("directors", getMovieDetails(label).get(1));
            map.put("producers", getMovieDetails(label).get(2));
            results.add(map);
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