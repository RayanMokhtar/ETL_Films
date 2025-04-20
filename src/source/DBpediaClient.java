package source;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SPARQL client for DBpedia using HTTP CSV format (pas de dépendance Jena).
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

    private static final String ENDPOINT = "https://dbpedia.org/sparql?format=text/csv&query=";
    private static final Logger logger = Logger.getLogger(DBpediaClient.class.getName());
    static { logger.setLevel(Level.WARNING); }

    // Exécute une requête SPARQL retournant du CSV, renvoie les lignes (sans entête)
    private ArrayList<String[]> executeQuery(String sparql) {
        ArrayList<String[]> rows = new ArrayList<>();
        try {
            String urlStr = ENDPOINT + URLEncoder.encode(sparql, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "text/csv");
            if (conn.getResponseCode() != 200) return rows;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // Split CSV en tenant compte des guillemets
                    rows.add(line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1));
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "executeQuery failed", e);
        }
        if (!rows.isEmpty()) rows.remove(0); // supprimer l'entête
        return rows;
    }

    public ArrayList<ArrayList<Object>> getMoviesDetails(String title) {
        // Requête SPARQL unique avec GROUP_CONCAT, full-text sur le titre via bif:contains
        String sparql = PREFIXES +
            "SELECT (GROUP_CONCAT(DISTINCT ?dirLabel; SEPARATOR=\", \" ) AS ?Directors) " +
            "(GROUP_CONCAT(DISTINCT ?prodLabel; SEPARATOR=\", \" ) AS ?Producers) " +
            "(GROUP_CONCAT(DISTINCT ?actorLabel; SEPARATOR=\", \" ) AS ?Actors) WHERE {" +
            "  ?film a dbo:Film; rdfs:label ?title. " +
            "  FILTER(lang(?title)='en'). " +
            "  ?title bif:contains '\"" + title.replace("'","\\'") + "\"'." +
            "  OPTIONAL { ?film (dbo:director|dbp:director) ?dir. ?dir rdfs:label ?dirLabel FILTER(lang(?dirLabel)='en') }." +
            "  OPTIONAL { ?film (dbo:producer|dbp:producer|dbp:producers) ?prod. ?prod rdfs:label ?prodLabel FILTER(lang(?prodLabel)='en') }." +
            "  OPTIONAL { ?film (dbo:starring|dbp:starring) ?actor. ?actor rdfs:label ?actorLabel FILTER(lang(?actorLabel)='en') }." +
            "} GROUP BY ?film ?title LIMIT 1";
        ArrayList<String[]> rows = executeQuery(sparql);
        ArrayList<ArrayList<Object>> out = new ArrayList<>();
        if (rows.isEmpty()) return out;
        String[] data = rows.get(0);
        // data[0]=Directors CSV, [1]=Producers CSV, [2]=Actors CSV
        // Split puis nettoyer chaque élément des éventuelles guillemets
        ArrayList<Object> listDirs = new ArrayList<>();
        for (String d : data[0].isEmpty() ? new String[0] : data[0].split(",\\s*")) {
            listDirs.add(d.replaceAll("^\"|\"$", "").trim());
        }
        ArrayList<Object> listProds = new ArrayList<>();
        for (String p : data[1].isEmpty() ? new String[0] : data[1].split(",\\s*")) {
            listProds.add(p.replaceAll("^\"|\"$", "").trim());
        }
        ArrayList<Object> listActors = new ArrayList<>();
        for (String a : data[2].isEmpty() ? new String[0] : data[2].split(",\\s*")) {
            listActors.add(a.replaceAll("^\"|\"$", "").trim());
        }
        out.add(listDirs);
        out.add(listProds);
        out.add(listActors);
        return out;
    }

    private ArrayList<Object> fetchRole(String uri, String props) {
        ArrayList<Object> list = new ArrayList<>();
        String sparql = PREFIXES +
            "SELECT DISTINCT ?label WHERE { <" + uri + "> (" + props + ") ?p. ?p rdfs:label ?label FILTER(lang(?label)='en') }";
        for (String[] row : executeQuery(sparql)) {
            String v = row[0].replaceAll("^\"|\"$","").trim();
            list.add(v);
        }
        return list;
    }

    public ArrayList<Object> getMoviesByActor(String actorName, boolean caseSensitive, boolean includeYear) {
        ArrayList<Object> results = new ArrayList<>();
        String esc = actorName.replace("'","\\'");
        String sparql = PREFIXES +
            "SELECT ?movieLabel ?year WHERE { ?actor rdfs:label ?actorLabel; <http://www.openlinksw.com/schemas/virtrdf#contains> '" + esc + "'. FILTER(lang(?actorLabel)='en') " +
            "?movie a dbo:Film; dbo:starring ?actor; rdfs:label ?movieLabel; dbo:releaseDate ?date. FILTER(lang(?movieLabel)='en') BIND(YEAR(?date) AS ?year) } ORDER BY ?movieLabel LIMIT 100";
        for (String[] row : executeQuery(sparql)) {
            results.add(new String[]{ row[0], row[1] });
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
            "  ?movie a dbo:Film; rdfs:label ?movieLabel." +
            "  ?movie <http://www.openlinksw.com/schemas/virtrdf#contains> '" + esc + "'." +
            "  FILTER(lang(?movieLabel)='en')" +
            "} LIMIT 1";
        ArrayList<String[]> rows = executeQuery(sparql);
        return rows.isEmpty() ? null : rows.get(0)[0].replaceAll("^\"|\"$","").trim();
    }

    public static void setLogLevel(Level lvl) {
        logger.setLevel(lvl);
    }
}
