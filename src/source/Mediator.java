package source;

import data.Movie;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Acts as a mediator between different data sources and the application.
 */
public class Mediator {

    private JDBCClient jdbcClient;
    private OMDbClient omdbClient;
    private DBpediaClient dbpediaClient;
    // Ajout d'un logger pour contrôler les messages
    private static final Logger logger = Logger.getLogger(Mediator.class.getName());
    private static boolean verboseLogging = false; // Par défaut, logging réduit
    // Fichier de rejet pour les erreurs techniques
    private static final String REJECT_FILE = "rejected_records.txt";

    /**
     * Constructeur du médiateur.
     */
    public Mediator() {
        jdbcClient = new JDBCClient();
        omdbClient = new OMDbClient();
        dbpediaClient = new DBpediaClient();
        
        // Configurer le logger pour un niveau d'information minimal
        logger.setLevel(verboseLogging ? Level.INFO : Level.WARNING);
    }

    /**
     * Returns the singleton instance of the Mediator class.
     * @return The singleton instance of Mediator.
     */
    public static Mediator getInstance(){
        return new Mediator();
    }
    
    /**
     * Active ou désactive le mode verbeux pour les logs
     * @param verbose true pour activer les logs détaillés, false pour les désactiver
     */
    public static void setVerboseLogging(boolean verbose) {
        verboseLogging = verbose;
        Logger.getLogger(Mediator.class.getName()).setLevel(verbose ? Level.INFO : Level.WARNING);
    }


    /**
     * Retrieves movies by their title.
     * @param movieTitle The title of the movie to search for.
     * @param caseSensitive Whether the search should be case-sensitive.
     * @return An ArrayList of Movie objects matching the search criteria.
     */
    public ArrayList<Movie> getMoviesByMovieTitle(String movieTitle, boolean caseSensitive){

        // Arraylist of movies to return
        ArrayList<Movie> movies = new ArrayList<>();

        // In this part we extract films which corresponds to the movieTitle
        // and we create objects
        /******************** JDBC Part *****************************************************/
        ArrayList<ArrayList<Object>> filmsTable = jdbcClient.getMovieInfo(movieTitle);

        // Si aucun film n'est trouvé dans la base de données, on renvoie une liste vide
        if (filmsTable == null || filmsTable.isEmpty()) {
            logger.info("Aucun film trouvé dans la base de données pour le titre: " + movieTitle);
            return movies;
        }

        for (ArrayList<Object> filmTable : filmsTable) {
            if (filmTable.size() < 7) {
                logger.warning("Film ignoré (données incomplètes) : " + filmTable);
                continue;
            }
            try {
                Movie movie = new Movie();
                movie.setTitle((String)filmTable.get(0));
                movie.setReleaseDate((Date)filmTable.get(1));
                movie.setGenre((String)filmTable.get(2));
                movie.setDistributor((String)filmTable.get(3));
                movie.setBudget((double)filmTable.get(4));
                movie.setUsaRevenue((double)filmTable.get(5));
                movie.setWorldwideRevenue((double)filmTable.get(6));
                movies.add(movie);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erreur technique, enregistrement rejeté pour : " + filmTable, e);
                // Écrire dans le fichier de rejet
                try (FileWriter fw = new FileWriter(REJECT_FILE, true)) {
                    fw.write(filmTable + " -> " + e.getMessage() + "\n");
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "Impossible d'écrire dans le fichier de rejet", ioe);
                }
            }
        }

        // Si aucun film n'a pu être créé, on renvoie une liste vide
        if (movies.isEmpty()) {
            logger.info("Aucun film n'a pu être créé à partir des données de la base");
            return movies;
        }

        /******************** DBpedia Part *****************************************************/
        for (Movie movie : movies) {
            // Enrichissement DBpedia : seulement sur le titre du film
            ArrayList<ArrayList<Object>> details = dbpediaClient.getMoviesDetails(movie.getTitle());
            if (details.size() == 3) {
                movie.addDirectors(details.get(0));
                movie.addProducers(details.get(1));
                movie.addActors(details.get(2));
            }
        }

        /******************** OMDb API Part *****************************************************/
        for (Movie movie : movies) {
            try {
                String original = movie.getTitle();
                // Générer variantes pour la requête OMDb : espaces, tirets, suppression de ponctuation
                ArrayList<String> variants = new ArrayList<>();
                variants.add(original);
                if (original.contains("-")) variants.add(original.replace('-', ' '));
                if (original.contains(" ")) variants.add(original.replace(' ', '-'));
                String stripped = original.replaceAll("[^A-Za-z0-9 ]", " ").trim();
                if (!variants.contains(stripped)) variants.add(stripped);

                String plot = null;
                // Tester chaque variante uniquement sur le titre, sans année
                for (String var : variants) {
                    String fmt = var.replace(' ', '+');
                    plot = OMDbClient.getMovieResume(fmt, "");
                    if (plot != null && !plot.equals("<html>\n<p></p>\n</html>") && !plot.trim().isEmpty()) {
                        break;
                    }
                }
                movie.setSummary(plot);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erreur lors de la récupération du résumé du film: " + e.getMessage(), e);
                // Continue avec le prochain film
            }
        }

        // Filtrer les films sans aucune info DBpedia/OMDb (option par défaut)
        movies.removeIf(m -> m.getDirectors().isEmpty()
                && m.getProducers().isEmpty()
                && m.getActors().isEmpty()
                && (m.getSummary() == null || m.getSummary().trim().isEmpty()));

        return movies;
    }


    /**
     * Retrieves movies by actor name.
     * @param actorName The name of the actor to search for.
     * @param caseSensitive Whether the search should be case-sensitive.
     * @return An ArrayList of Movie objects featuring the specified actor.
     */
    public ArrayList<Movie> getMoviesByActorName(String actorName, boolean caseSensitive) {
        ArrayList<Movie> movies = new ArrayList<>();
        // Récupère liste des titres de films via SPARQL
        ArrayList<Object> actorResults = dbpediaClient.getMoviesByActor(actorName, caseSensitive, true);
        for (Object entry : actorResults) {
            String[] data = (String[]) entry;  // [0]=titre, [1]=année ou null
            String title = data[0].split("\\(")[0].trim();
            // Intersection avec la base locale : ne traiter que si présent en JDBC
            ArrayList<ArrayList<Object>> tbl = jdbcClient.getMovieInfo(title);
            if (tbl == null || tbl.isEmpty()) {
                // Pas dans la base locale, on ignore
                continue;
            }
            // Création de l'objet Movie à partir des données SQL
            ArrayList<Object> row = tbl.get(0);
            Movie movie = new Movie();
            movie.setTitle((String) row.get(0));
            movie.setReleaseDate((Date) row.get(1));
            movie.setGenre((String) row.get(2));
            movie.setDistributor((String) row.get(3));
            movie.setBudget((double) row.get(4));
            movie.setUsaRevenue((double) row.get(5));
            movie.setWorldwideRevenue((double) row.get(6));
            // Enrichissement DBpedia
            ArrayList<ArrayList<Object>> details = dbpediaClient.getMoviesDetails(title);
            if (details.size() == 3) {
                movie.addDirectors(details.get(0));
                movie.addProducers(details.get(1));
                movie.addActors(details.get(2));
            }
            // Enrichissement OMDb
            String plot = omdbClient.getMovieResume(title.replace(' ', '+'), "");
            movie.setSummary(plot);
            movies.add(movie);
        }
        return movies;
    }

    /**
     * Récupère des suggestions de titres de films basées sur une sous-chaîne.
     * 
     * @param subString La sous-chaîne à rechercher dans les titres de films.
     * @param limit Le nombre maximum de suggestions à retourner.
     * @return Une liste des titres de films contenant la sous-chaîne.
     */
    public ArrayList<String> getMovieTitleSuggestions(String subString, int limit) {
        // Cast explicite pour résoudre l'erreur de type
        return new ArrayList<>(jdbcClient.getMovieTitleSuggestions(subString, limit));
    }
    
    /**
     * Ferme proprement les connexions à la fin de l'application
     */
    public void closeConnections() {
        JDBCClient.closeConnection();
    }
}
