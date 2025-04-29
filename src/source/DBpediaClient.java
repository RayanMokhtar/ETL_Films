package source;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SPARQL client for DBpedia using Jena API.
 */
public class DBpediaClient {

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
        "PREFIX bif:   <http://www.openlinksw.com/schemas/bif#>\n";

    private static final String ENDPOINT = "https://dbpedia.org/sparql";
    private static final Logger logger = Logger.getLogger(DBpediaClient.class.getName());
    static { logger.setLevel(Level.WARNING); }

    // Exécute une requête SPARQL via Jena et retourne le ResultSet
    private ResultSet executeQuery(String sparql) {
        Query query = QueryFactory.create(sparql);
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(ENDPOINT, query)) {
            return qexec.execSelect();
        } catch (Exception e) {
            logger.log(Level.WARNING, "SPARQL query failed: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Récupère les réalisateurs, producteurs et acteurs d'un film DBpedia en se basant sur son titre.
     * @param title Titre du film recherché
     * @return Trois listes : réalisateurs, producteurs, acteurs
     */
    public ArrayList<ArrayList<Object>> getMoviesDetails(String title) {
        String esc = title.replace("'","\\'");
        String sparql = PREFIXES +
            "SELECT (GROUP_CONCAT(DISTINCT ?dirLabel; SEPARATOR=\", \" ) AS ?Directors) " +
            "(GROUP_CONCAT(DISTINCT ?prodLabel; SEPARATOR=\", \" ) AS ?Producers) " +
            "(GROUP_CONCAT(DISTINCT ?actorLabel; SEPARATOR=\", \" ) AS ?Actors) WHERE {" +
            "  ?film a dbo:Film; rdfs:label ?titleVar. " +
            "  FILTER(lang(?titleVar)='en'). " +
            "  ?titleVar bif:contains '\"" + esc + "\"'." +
            "  OPTIONAL { ?film (dbo:director|dbp:director) ?dir. ?dir rdfs:label ?dirLabel FILTER(lang(?dirLabel)='en') }." +
            "  OPTIONAL { ?film (dbo:producer|dbp:producer|dbp:producers) ?prod. ?prod rdfs:label ?prodLabel FILTER(lang(?prodLabel)='en') }." +
            "  OPTIONAL { ?film (dbo:starring|dbp:starring) ?actor. ?actor rdfs:label ?actorLabel FILTER(lang(?actorLabel)='en') }." +
            "} GROUP BY ?film ?titleVar LIMIT 1";
        ResultSet rs = executeQuery(sparql);
        ArrayList<ArrayList<Object>> out = new ArrayList<>();
        if (rs == null || !rs.hasNext()) return out;
        QuerySolution sol = rs.nextSolution();
        String[] vars = {"Directors","Producers","Actors"};
        for (String v : vars) {
            ArrayList<Object> list = new ArrayList<>();
            Literal lit = sol.getLiteral(v);
            if (lit != null) {
                for (String e : lit.getString().split(",\\s*")) list.add(e.trim());
            }
            out.add(list);
        }
        return out;
    }

    private ArrayList<Object> fetchRole(String uri, String props) {
        ArrayList<Object> list = new ArrayList<>();
        String sparql = PREFIXES +
            "SELECT DISTINCT ?label WHERE { <" + uri + "> (" + props + ") ?p. ?p rdfs:label ?label FILTER(lang(?label)='en') }";
        ResultSet rs = executeQuery(sparql);
        while (rs.hasNext()) {
            QuerySolution sol = rs.nextSolution();
            String v = sol.getLiteral("label").getString().trim();
            list.add(v);
        }
        return list;
    }

    /**
     * Recherche la liste des titres de films dans lesquels un acteur donné a joué.
     * Utilise bif:contains pour matcher l'acteur et ne retourne que `movieLabel`.
     * @param actorName Nom de l'acteur
     */
    public ArrayList<Object> getMoviesByActor(String actorName, boolean caseSensitive, boolean includeYear) {
        ArrayList<Object> results = new ArrayList<>();
        String esc = actorName.replace("'","\\'");
        String sparql = PREFIXES +
            "SELECT DISTINCT ?movieLabel WHERE {" +
            "  ?actor rdfs:label ?actorLabel. " +
            "  FILTER(lang(?actorLabel)='en' && bif:contains(?actorLabel, '\"" + esc + "\"')). " +
            "  ?movie a dbo:Film; (dbo:starring|dbp:starring) ?actor; rdfs:label ?movieLabel. " +
            "  FILTER(lang(?movieLabel)='en')." +
            "} ORDER BY ?movieLabel LIMIT 100";
        ResultSet rs = executeQuery(sparql);
        if (rs == null) {
            logger.warning("No results for actor query, possible SPARQL endpoint error");
            return results;
        }
        while (rs.hasNext()) {
            QuerySolution sol = rs.nextSolution();
            String movieLabel = sol.getLiteral("movieLabel").getString();
            results.add(new String[]{ movieLabel });
        }
        return results;
    }

    /**
     * Recherche un film dans DBpedia par son titre uniquement.
     * @param movieTitle Titre du film à rechercher
     * @return Label du film trouvé ou null si aucun résultat
     */
    public String getMovie(String movieTitle, String unusedYear, boolean unusedCase) {
        String esc = movieTitle.replace("'","\\'");
        String sparql = PREFIXES +
            "SELECT DISTINCT ?movieLabel WHERE {" +
            "  ?movie a dbo:Film; rdfs:label ?movieLabel. FILTER(lang(?movieLabel)='en'&& regex(?movieLabel,'"+esc+"','i'))" +
            "} LIMIT 1";
        ResultSet rs = executeQuery(sparql);
        if (!rs.hasNext()) return null;
        return rs.nextSolution().getLiteral("movieLabel").getString();
    }

    public static void setLogLevel(Level lvl) {
        logger.setLevel(lvl);
    }
}
