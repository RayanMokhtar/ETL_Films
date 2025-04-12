package source;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides methods to interact with the DBpedia SPARQL endpoint to retrieve movie details.
 */
public class DBpediaClient {

    /**
     * Retrieves movie details such as actors, directors, and producers based on the movie label.
     * @param movieLabel The label of the movie to search for.
     * @return An ArrayList containing ArrayLists of movie details: actors, directors, and producers.
     */
    public static ArrayList<ArrayList<Object>> getMoviesDetails(String movieLabel/*, boolean caseSensitive*/) {
        ArrayList<ArrayList<Object>> moviesDetails = new ArrayList<>(3);

        // Requête SPARQL pour récupérer les réalisateurs du film
        String queryStringDirectors = "PREFIX dbo: <http://dbpedia.org/ontology/>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX dbp: <http://dbpedia.org/property/>\n"
                + "SELECT DISTINCT ?value WHERE {\n"
                + "  ?film a dbo:Film .\n"
                + "  { ?film rdfs:label \"" + movieLabel + "\"@en } UNION { ?film rdfs:label \"" + movieLabel + "\"@fr } .\n"
                + "  { { ?film dbo:director|dbp:director|dbo:directors|dbp:directors ?directorName } \n"
                + "    UNION \n"
                + "    { ?film dbo:director|dbp:director|dbo:directors|dbp:directors ?d .\n"
                + "      ?d foaf:name|rdfs:label|dbp:name ?directorName }\n"
                + "  } .\n"
                + "  FILTER (lang(?directorName) = 'en') .\n"
                + "  BIND(STR(?directorName) AS ?value)\n"
                + "}";

        // Requête SPARQL pour récupérer les acteurs du film
        String queryStringActors = "PREFIX dbo: <http://dbpedia.org/ontology/>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX dbp: <http://dbpedia.org/property/>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "SELECT DISTINCT ?value WHERE {\n"
                + "  ?film a dbo:Film .\n"
                + "  { ?film rdfs:label \"" + movieLabel + "\"@en } UNION { ?film rdfs:label \"" + movieLabel + "\"@fr } .\n"
                + "  { { ?film dbo:starring|dbp:starring ?actorName } \n"
                + "    UNION \n"
                + "    { ?film dbo:starring|dbp:starring ?a .\n"
                + "      ?a foaf:name|rdfs:label|dbp:name ?actorName }\n"
                + "  } .\n"
                + "  FILTER (lang(?actorName) = 'en') .\n"
                + "  BIND(STR(?actorName) AS ?value)\n"
                + "}";

        // Requête SPARQL pour récupérer les producteurs du film
        String queryStringProducers = "PREFIX dbo: <http://dbpedia.org/ontology/>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX dbp: <http://dbpedia.org/property/>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "SELECT DISTINCT ?value WHERE {\n"
                + "  ?film a dbo:Film .\n"
                + "  { ?film rdfs:label \"" + movieLabel + "\"@en } UNION { ?film rdfs:label \"" + movieLabel + "\"@fr } .\n"
                + "  { { ?film dbp:producers|dbo:producers|dbp:producer|dbo:producer ?producerName } \n"
                + "    UNION \n"
                + "    { ?film dbp:producers|dbo:producers|dbp:producer|dbo:producer ?p .\n"
                + "      ?p foaf:name|rdfs:label|dbp:name ?producerName }\n"
                + "  } .\n"
                + "  FILTER (lang(?producerName) = 'en') .\n"
                + "  BIND(STR(?producerName) AS ?value)\n"
                + "}";

        // Exécuter les requêtes SPARQL et récupérer les résultats
        ArrayList<Object> directors = executeQueryAndGetResults(queryStringDirectors, false);
        ArrayList<Object> actors = executeQueryAndGetResults(queryStringActors, false);
        ArrayList<Object> producers = executeQueryAndGetResults(queryStringProducers, false);

        // Ajouter les détails du film à la liste
        moviesDetails.add(actors);
        moviesDetails.add(directors);
        moviesDetails.add(producers);

        return moviesDetails;
    }

    /**
     * Retrieves movies associated with a particular actor.
     * @param actorName The name of the actor to search for.
     * @param caseSensitive Whether the search should be case-sensitive.
     * @param year Whether to include the release year of the movies.
     * @return An ArrayList of movies associated with the specified actor.
     */
    public static ArrayList<Object> getMoviesByActor(String actorName, boolean caseSensitive, boolean year) {

        // Construire la requête SPARQL pour récupérer les films selon l'acteur
        String queryStringActorMovies = "PREFIX dbo: <http://dbpedia.org/ontology/>\n"
                + "PREFIX dbp: <http://dbpedia.org/property/>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "SELECT DISTINCT ?value ?comment WHERE {\n"
                + "  ?film a dbo:Film ;\n"
                + "        rdfs:comment ?comment .\n"
                + "  { ?film dbo:starring|dbp:starring ?actor } UNION { ?film dbo:starring|dbp:starring ?a . ?a foaf:name|rdfs:label ?actor } .\n";
        if (caseSensitive) {
            queryStringActorMovies += "FILTER (LCASE(str(?actor)) = \"" + actorName.toLowerCase() + "\")\n";
        } else {
            queryStringActorMovies += "FILTER (str(?actor) = \"" + actorName + "\"@en)\n";
        }

        queryStringActorMovies += "  ?film rdfs:label ?filmLabel .\n"
                + "  FILTER ((lang(?filmLabel) = 'en') || (lang(?filmLabel) = 'fr'))\n"
                + "  FILTER (lang(?comment) = 'en')\n"
                + "  BIND (STR(?filmLabel) AS ?value)\n"
                + "}";
//        System.out.println(queryStringActorMovies);
        // Exécuter la requête SPARQL et récupérer les résultats
        ArrayList<Object> actorMovies = executeQueryAndGetResults(queryStringActorMovies, year);
        
        return actorMovies;
    }

    /**
     * Retrieves a movie based on its title and optionally its release year.
     * @param movieTitle The title of the movie to search for.
     * @param year The release year of the movie.
     * @param caseSensitive Whether the search should be case-sensitive.
     * @return The movie title matching the search criteria.
     */
    public static String getMovie(String movieTitle, String year, boolean caseSensitive) {

        // Requête SPARQL pour récupérer les réalisateurs du film
        String queryStringMovie = "PREFIX dbo: <http://dbpedia.org/ontology/>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "SELECT DISTINCT ?comment ?labelValue WHERE {\n"
                + "  ?film a dbo:Film ;\n"
                + "        rdfs:comment ?comment ;\n"
                + "        rdfs:label ?label .\n";

        if (caseSensitive) {
            queryStringMovie += "  FILTER (CONTAINS(LCASE(?label), \"" + movieTitle.toLowerCase() + "\"))\n";
        } else {
            queryStringMovie += "  foaf:name \"" + movieTitle + "\"@en.\n";
        }
        queryStringMovie += "  FILTER (lang(?comment) = 'en') \n"
                + "  FILTER ((lang(?label) = 'en') || (lang(?label) = 'fr')) \n"
                + "  BIND (STR(?label) AS ?labelValue)\n"
                + "}";

//        System.out.println(queryStringMovie);

        HashMap<String, String> movieSummary = new HashMap<>();

        // Créer un objet Query
        Query query = QueryFactory.create(queryStringMovie);
        System.out.println(query);

        // Créer un objet QueryExecution pour exécuter la requête SPARQL sur DBpedia
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query)) {
            // Exécuter la requête et obtenir le ResultSet
            ResultSet results = qexec.execSelect();

            // Parcourir les résultats et ajouter les valeurs à la liste
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String label = soln.get("?labelValue").toString();
                String comment = soln.get("?comment").toString();
                if (label != null && !label.equals("")
                        && comment != null && !comment.equals("")) {
                    movieSummary.put(label, comment);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Afficher les détails du film à la liste
        if(movieSummary.entrySet().size() == 1){
            return movieSummary.entrySet().iterator().next().getKey();
        }

        for (Entry<String, String> entry : movieSummary.entrySet()) {
            // Création du motif pour matcher une année
            Pattern pattern = Pattern.compile("\\b\\d{4}\\b");
            Matcher matcher = pattern.matcher(entry.getValue());

            // Vérification de la présence d'une année dans le texte
            if (matcher.find()) {
//                System.out.println("La première année trouvée est : " + matcher.group());
                if (matcher.group().equals(year)) {
                    String searchedMovieTitle = entry.getKey().split("\\(")[0].trim();
                    String[] words = searchedMovieTitle.split("\\s+");
                    String[] wordsSearch = movieTitle.split("\\s+");
                    if (words.length == wordsSearch.length)
                        return entry.getKey();
                }
            } else {
                System.out.println("Aucune année trouvée dans le texte.");
            }
        }
        return null;
    }

    /**
     * Executes a SPARQL query and returns the results as a list of strings or string arrays.
     * @param queryString The SPARQL query string to execute.
     * @param year Whether to include the release year of the movies.
     * @return An ArrayList containing the results of the SPARQL query.
     */
    private static ArrayList<Object> executeQueryAndGetResults(String queryString, boolean year) {
        ArrayList<Object> resultsList = new ArrayList<>();

        // Créer un objet Query
        Query query = QueryFactory.create(queryString);
        System.out.println(query);
        // Créer un objet QueryExecution pour exécuter la requête SPARQL sur DBpedia
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query)) {
            // Exécuter la requête et obtenir le ResultSet
            ResultSet results = qexec.execSelect();

            // Parcourir les résultats et ajouter les valeurs à la liste
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String value = soln.get("?value").toString();
                if (!year) {
                    if (value != null && !value.equals("") && !resultsList.contains(value)) {
                        resultsList.add(value);
                    }
                } else {
                    String comment = soln.get("?comment").toString();
                    if (value != null && !value.equals("")
                            && comment != null && !comment.equals("")) {
                        // Extraction de l'année
                        String valueYear = "";
                        Pattern pattern = Pattern.compile("\\b\\d{4}\\b");
                        Matcher matcher = pattern.matcher(comment);
                        if (matcher.find()) {
                            valueYear = matcher.group();
                        } else {
                            System.out.println("Aucune année trouvée dans le texte.");
                        }
                        resultsList.add(new String[]{value, valueYear});
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultsList;
    }

}
