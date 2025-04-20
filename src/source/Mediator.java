package source;

import data.Movie;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;


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

        for (ArrayList<Object> filmTable : filmsTable){
            // Vérification que les données du film sont complètes
            if (filmTable.size() < 7) {
                logger.warning("Les données du film sont incomplètes, ignoré: " + filmTable);
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
                logger.log(Level.SEVERE, "Erreur lors de la création du film: " + e.getMessage(), e);
                // Continue avec le prochain film
            }
        }

        // Si aucun film n'a pu être créé, on renvoie une liste vide
        if (movies.isEmpty()) {
            logger.info("Aucun film n'a pu être créé à partir des données de la base");
            return movies;
        }

        /******************** DBpedia Part *****************************************************/
        for (Movie movie : movies) {
            // Récupération directe des informations depuis DBpedia (comme dans DBpediaClientTest)
            ArrayList<ArrayList<Object>> details = dbpediaClient.getMoviesDetails(movie.getTitle());
            if (details.size() == 3) {
                movie.addDirectors(details.get(0));
                movie.addProducers(details.get(1));
                movie.addActors(details.get(2));
            }
        }

        /******************** OMDb API Part *****************************************************/
        // OMDb API Part
        for(Movie movie : movies) {
            try {
                String movieTitleFormatted = movie.getTitle().replace(' ', '+');
                String releaseYear = String.valueOf(movie.getReleaseDate().toLocalDate().getYear());
                String plot = OMDbClient.getMovieResume(movieTitleFormatted, releaseYear);
                if (plot == null || plot.equals("<html>\n<p></p>\n</html>")){
                    if (movieTitleFormatted.toLowerCase().contains("the")){
                        movieTitleFormatted = movieTitleFormatted.replace("the", "").replace("The", "").trim();
                        plot = OMDbClient.getMovieResume(movieTitleFormatted, releaseYear);
                    }
                }

                movie.setSummary(plot);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erreur lors de la récupération du résumé du film: " + e.getMessage(), e);
                // Continue avec le prochain film
            }
        }

        return movies;
    }


    /**
     * Retrieves movies by actor name.
     * @param actorName The name of the actor to search for.
     * @param caseSensitive Whether the search should be case-sensitive.
     * @return An ArrayList of Movie objects featuring the specified actor.
     */
    public ArrayList<Movie> getMoviesByActorName(String actorName, boolean caseSensitive){

        ArrayList<Movie> movies = new ArrayList<Movie>();
        HashMap<String, Movie> moviesByLabels = new HashMap<>();

        // On récupère les films de l'acteur renseigné
        ArrayList<Object> moviesLabels = dbpediaClient.getMoviesByActor(actorName, caseSensitive, true);


        /******************** JDBC Part *****************************************************/


        // Ararylist des films qui n'existent pas
        ArrayList<Object> notFoundMoviesLabels = new ArrayList<>();

        // On croise les films sql avec les films dpbedia
        for(Object movieLabel : moviesLabels) {
            String movieTitle = ((String[])movieLabel)[0].split("\\(")[0].trim();
            ArrayList<ArrayList<Object>> filmsTable = jdbcClient.getMovieInfo(movieTitle);
            if(!filmsTable.isEmpty()){
                for (ArrayList<Object> filmTable : filmsTable){
                    Movie movie = new Movie();

                    // Pour cette partie je vais extraire l'année de sortie d'un Film avec son nom et les mettre en
                    // commun pour la exacte dans SPARQL
                    String releaseYearSql = String.valueOf(((Date)filmTable.get(1)).toLocalDate().getYear());
                    String releaseYearDbpedia = ((String[])movieLabel)[1];
                    if (releaseYearDbpedia.equals(releaseYearSql)){
                        movie.setTitle((String)filmTable.get(0));
                        movie.setReleaseDate((Date)filmTable.get(1));
                        movie.setGenre((String)filmTable.get(2));
                        movie.setDistributor((String)filmTable.get(3));
                        movie.setBudget((double)filmTable.get(4));
                        movie.setUsaRevenue((double)filmTable.get(5));
                        movie.setWorldwideRevenue((double)filmTable.get(6));

                        moviesByLabels.put(((String[])movieLabel)[0], movie);
                    }


                }
            }
            else {
                notFoundMoviesLabels.add(movieLabel);
            }

        }

        moviesLabels.removeAll(notFoundMoviesLabels);
        // Affichage des labels inexistants dans la bd sql
        if (verboseLogging) {
            logger.info("Films de "+ actorName +" non trouvés :");
            for (Object movieObject: notFoundMoviesLabels){
                String movieLabel = ((String[])movieObject)[0];
                logger.info(movieLabel);
            }
        }

        // Pour cette partie je vais extraire l'année de sortie d'un Film avec son nom et les mettre en commun pour la
        // exacte dans SPARQL


        /******************** DBpedia Part *****************************************************/
        // DBPedia Part
        // With movies I'm gonna extract actors, directors, producers
        for (Entry<String, Movie> movieByLabel : moviesByLabels.entrySet()){
            String filmLabel = movieByLabel.getKey();
            Movie movie = movieByLabel.getValue();

            // Case sensitive research or not
            ArrayList<ArrayList<Object>> moviesDetails = dbpediaClient.getMoviesDetails(filmLabel/*, caseSensitive*/);
            ArrayList<Object> directors = moviesDetails.get(0);
            ArrayList<Object> producers = moviesDetails.get(1);
            ArrayList<Object> actors = moviesDetails.get(2);
            if (actors.size() > 0){
                movie.addActors(actors);
            }
            if (directors.size() > 0){
                movie.addDirectors(directors);
            }
            if (producers.size() > 0){
                movie.addProducers(producers);
            }

            // Transfert des données
            movies.add(movie);

        }


        /******************** OMDb API Part *****************************************************/
        // OMDb API Part
        for(Movie movie : movies) {
            String movieTitleFormatted = movie.getTitle().replace(' ', '+');
            String releaseYear = String.valueOf(movie.getReleaseDate().toLocalDate().getYear());
            String plot = OMDbClient.getMovieResume(movieTitleFormatted, releaseYear);
            if (plot == null || plot.equals("<html>\n<p></p>\n</html>")){
                if (movieTitleFormatted.toLowerCase().contains("the")){
                    movieTitleFormatted = movieTitleFormatted.replace("the", "").replace("The", "").trim();
                    plot = OMDbClient.getMovieResume(movieTitleFormatted, releaseYear);
                }
            }

            movie.setSummary(plot);
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
