package source;

import org.apache.jena.query.*;
import java.util.ArrayList;

public class DBpediaClient_V1 {

    /**
     * Méthode générique pour récupérer une propriété d'un film depuis DBpedia.
     * @param movieLabel     Le label du film.
     * @param propertyGroup  Le groupe de propriétés (ex: "dbo:director|dbp:director|dbo:directors|dbp:directors").
     * @param namePropertyGroup  Le groupe de propriétés servant à récupérer le nom (ex: "foaf:name|rdfs:label|dbp:name").
     * @return Une ArrayList contenant les valeurs récupérées.
     */
    public static ArrayList<Object> getMovieProperty(String movieLabel, String propertyGroup, String namePropertyGroup) {
        String sparqlQuery = String.format(
              "PREFIX dbo: <http://dbpedia.org/ontology/>\n"
            + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX dbp: <http://dbpedia.org/property/>\n"
            + "SELECT DISTINCT ?value WHERE {\n"
            + "  ?film a dbo:Film .\n"
            + "  { ?film rdfs:label \"%s\"@en } UNION { ?film rdfs:label \"%s\"@fr } .\n"
            + "  { { ?film %s ?v } UNION { ?film %s ?x . ?x %s ?v } } .\n"
            + "  FILTER (lang(?v) = \"en\") .\n"
            + "  BIND(STR(?v) AS ?value)\n"
            + "}", movieLabel, movieLabel, propertyGroup, propertyGroup, namePropertyGroup);

        return executeQueryAndGetResults(sparqlQuery, false);
    }

    /**
     * Exécute une requête SPARQL et retourne les résultats sous forme d'une liste d'objets.
     * @param queryString La chaîne de la requête SPARQL.
     * @param year        Si true, traite également l'année (non utilisé ici).
     * @return Une ArrayList contenant les résultats de la requête.
     */
    private static ArrayList<Object> executeQueryAndGetResults(String queryString, boolean year) {
        ArrayList<Object> resultsList = new ArrayList<>();
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String value = soln.get("?value").toString();
                if (value != null && !value.isEmpty() && !resultsList.contains(value)) {
                    resultsList.add(value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultsList;
    }

    /**
     * Récupère les détails d'un film (acteurs, réalisateurs, producteurs) en appelant la méthode générique.
     * @param movieLabel Le label du film à rechercher.
     * @return Une ArrayList contenant trois sous-listes (acteurs, réalisateurs, producteurs).
     */
    public static ArrayList<ArrayList<Object>> getMoviesDetails(String movieLabel) {
        ArrayList<ArrayList<Object>> moviesDetails = new ArrayList<>();
        
        // Acteurs
        ArrayList<Object> actors = getMovieProperty(movieLabel,
                "dbo:starring|dbp:starring",
                "foaf:name|rdfs:label|dbp:name");

        // Réalisateurs
        ArrayList<Object> directors = getMovieProperty(movieLabel,
                "dbo:director|dbp:director|dbo:directors|dbp:directors",
                "foaf:name|rdfs:label|dbp:name");

        // Producteurs
        ArrayList<Object> producers = getMovieProperty(movieLabel,
                "dbp:producers|dbo:producers|dbp:producer|dbo:producer",
                "foaf:name|rdfs:label|dbp:name");

        moviesDetails.add(actors);
        moviesDetails.add(directors);
        moviesDetails.add(producers);

        return moviesDetails;
    }

    // Méthode main pour tester les requêtes généralistes
    public static void main(String[] args) {
        String movieLabel = "Inception";
        ArrayList<ArrayList<Object>> details = getMoviesDetails(movieLabel);

        System.out.println("Acteurs : " + details.get(0));
        System.out.println("Réalisateurs : " + details.get(1));
        System.out.println("Producteurs : " + details.get(2));
    }
}
