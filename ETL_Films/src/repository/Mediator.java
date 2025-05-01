package repository;

import core.Movie;
import datasource.DBpediaDataSource;
import datasource.LocalDatabaseDataSource;
import datasource.OMDbDataSource;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implémentation du MovieRepository qui joue le rôle de médiateur entre les différentes sources de données.
 * Centralise l'accès aux sources de données (JDBC, DBpedia, OMDb) et coordonne leur utilisation.
*/

public class Mediator implements MovieRepository {

    private final LocalDatabaseDataSource dbDataSource;
    private final OMDbDataSource omdbDataSource;
    private final DBpediaDataSource dbpediaDataSource;
    
    private static final Logger logger = Logger.getLogger(Mediator.class.getName());
    private static boolean verboseLogging = false; 
    
    private static final String REJECT_FILE = "rejected_records.txt";

  
    public Mediator() {
        dbDataSource = new LocalDatabaseDataSource();
        omdbDataSource = new OMDbDataSource();
        dbpediaDataSource = new DBpediaDataSource();
        
        // Configurer le logger pour un niveau d'information minimal
        logger.setLevel(verboseLogging ? Level.INFO : Level.WARNING);
    }

 
    public static Mediator getInstance() {
        return new Mediator();
    }
    
    
    public static void setVerboseLogging(boolean verbose) {
        verboseLogging = verbose;
        Logger.getLogger(Mediator.class.getName()).setLevel(verbose ? Level.INFO : Level.WARNING);
        
        Level level = verbose ? Level.INFO : Level.WARNING;
        new DBpediaDataSource().setLogLevel(level);
        new OMDbDataSource().setLogLevel(level);
    }


    @Override
    public ArrayList<Movie> getMoviesByTitle(String movieTitle, boolean caseSensitive) {
        // Vérification de la validité du titre
        logger.info("Recherche de films par titre: " + movieTitle);
        ArrayList<Movie> movies = new ArrayList<>();

        ArrayList<Object> localResults = dbDataSource.getMovieByTitle(movieTitle, caseSensitive);

        if (localResults == null || localResults.isEmpty()) {
            logger.info("Aucun film trouvé dans la base de données pour le titre: " + movieTitle);
            return movies;
        }

        for (Object result : localResults) {
            ArrayList<Object> filmData = (ArrayList<Object>) result;
            if (filmData.size() < 7) {
                logger.warning("Film ignoré (données incomplètes) : " + filmData);
                continue;
            }
            
            try {
                Movie movie = createMovieFromData(filmData);
                movies.add(movie);
            } catch (Exception e) {
                handleDataError(filmData, e);
            }
        }

        if (movies.isEmpty()) {
            logger.info("Aucun film n'a pu être créé à partir des données de la base");
            return movies;
        }

        enrichMoviesWithExternalData(movies);
        
        //filterMoviesWithoutEnrichment(movies);

        return movies;
    }

    

    @Override
    public ArrayList<Movie> getMoviesByActorName(String actorName, boolean caseSensitive) {
        logger.info("Recherche de films par acteur: " + actorName);
        ArrayList<Movie> movies = new ArrayList<>();
        Map<String, Movie> uniqueMovies = new HashMap<>();  
        
        ArrayList<Object> actorResults = dbpediaDataSource.getMoviesByActor(actorName, caseSensitive);
        
        if (actorResults.isEmpty()) {
            logger.info("Aucun film trouvé pour l'acteur: " + actorName);
            return movies;
        }
        
        for (Object entry : actorResults) {

            HashMap<String, Object> movieData = (HashMap<String, Object>) entry;
            String title = (String) movieData.get("title");
            
            ArrayList<Object> localResults = dbDataSource.getMovieByTitle(title, false);
            if (localResults == null || localResults.isEmpty()) {
                logger.fine("Film non trouvé dans la base locale: " + title);
                continue;
            }
            
            for (Object localObj : localResults) {
                ArrayList<Object> row = (ArrayList<Object>) localObj;
                if (row.size() < 7) continue;
                
                try {
                    Movie movie = createMovieFromData(row);
                    
                    ArrayList<Object> actors = (ArrayList<Object>) movieData.get("actors");
                    if (actors != null && !actors.isEmpty()) {
                        movie.addActors(actors);
                    }
                    
                    ArrayList<Object> directors = (ArrayList<Object>) movieData.get("directors");
                    ArrayList<Object> producers = (ArrayList<Object>) movieData.get("producers");
                    
                    if (directors != null && !directors.isEmpty()) {
                        movie.addDirectors(directors);
                    }
                    
                    if (producers != null && !producers.isEmpty()) {
                        movie.addProducers(producers);
                    }
                    
                    boolean actorFound = false;
                    for (Object actor : movie.getActors()) {
                        String actorStr = (String) actor;
                        if (actorStr.toLowerCase().contains(actorName.toLowerCase())) {
                            actorFound = true;
                            break;
                        }
                    }
                    
                    if (!actorFound && !movie.getActors().isEmpty()) {
                        movie.addActors(actors);
                    }
                    
                    addMovieSynopsis(movie);
                    
                    String movieKey = movie.getTitle();
                    if (movie.getReleaseDate() != null) {
                        Date releaseDate = movie.getReleaseDate();
                        String filmYear = String.valueOf(releaseDate.getYear() + 1900);
                        movieKey += "_" + filmYear;
                    }
                    
                    uniqueMovies.put(movieKey, movie);
                    
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Erreur lors du traitement du film: " + title, e);
                }
            }
        }
        
        movies.addAll(uniqueMovies.values());
        
        // filterMoviesWithoutEnrichment(movies);
        
        return movies;
    }

  
    public ArrayList<String> getMovieTitleSuggestions(String subString, int limit) {
        logger.info("Recherche de suggestions pour: " + subString);
        ArrayList<String> suggestions = new ArrayList<>();
        
     
        ArrayList<Object> results = dbDataSource.getMovieByTitle(subString, false);
        
        int count = 0;
        for (Object result : results) {
            if (count >= limit) break;
            
            ArrayList<Object> movieData = (ArrayList<Object>) result;
            if (movieData.size() > 0) {
                suggestions.add((String) movieData.get(0));
                count++;
            }
        }
        
        return suggestions;
    }
    
   
    public void closeConnections() {
        logger.info("Fermeture des connexions");
        if (dbDataSource != null) {
            dbDataSource.closeConnection();
        }
    }
    


    // ============= MÉTHODES PRIVÉES UTILITAIRES =============
    
   
    private Movie createMovieFromData(ArrayList<Object> data) {
        Movie movie = new Movie();
        movie.setTitle((String) data.get(0));
        movie.setReleaseDate((Date) data.get(1));
        movie.setGenre((String) data.get(2));
        movie.setDistributor((String) data.get(3));
        movie.setBudget((double) data.get(4));
        movie.setUsaRevenue((double) data.get(5));
        movie.setWorldwideRevenue((double) data.get(6));
        return movie;
    }
    




    private void enrichMoviesWithExternalData(ArrayList<Movie> movies) {
        for (Movie movie : movies) {
            ArrayList<Object> dbpediaResults = dbpediaDataSource.getMovieByTitle(movie.getTitle(), false);
            
            for (Object result : dbpediaResults) {
                HashMap<String, Object> movieData = (HashMap<String, Object>) result;
                
                ArrayList<Object> actors = (ArrayList<Object>) movieData.get("actors");
                if (actors != null && !actors.isEmpty()) {
                    movie.addActors(actors);
                }
                
                ArrayList<Object> directors = (ArrayList<Object>) movieData.get("directors");
                if (directors != null && !directors.isEmpty()) {
                    movie.addDirectors(directors);
                }
                
                ArrayList<Object> producers = (ArrayList<Object>) movieData.get("producers");
                if (producers != null && !producers.isEmpty()) {
                    movie.addProducers(producers);
                }
            }
            
            addMovieSynopsis(movie);
        }
    }
    
   



    private void addMovieSynopsis(Movie movie) {
        try {
            String title = movie.getTitle();
            
            ArrayList<String> variants = generateTitleVariants(title);
            
            for (String variant : variants) {
                String formattedTitle = variant.replace(' ', '+');
                
                ArrayList<Object> omdbResults = omdbDataSource.getMovieByTitle(formattedTitle, false);
                if (!omdbResults.isEmpty()) {
                    String plot = (String) omdbResults.get(0);
                    if (plot != null && !plot.equals("<html>\n<p></p>\n</html>") && !plot.trim().isEmpty()) {
                        movie.setSummary(plot);
                        logger.fine("Synopsis trouvé pour: " + title);
                        break;
                    }
                }
            }
            
            if (movie.getSummary() == null || movie.getSummary().trim().isEmpty()) {
                logger.fine("Aucun synopsis trouvé pour: " + title);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de la récupération du synopsis pour: " + movie.getTitle(), e);
        }
    }
    
   
    private ArrayList<String> generateTitleVariants(String title) {
        ArrayList<String> variants = new ArrayList<>();
        variants.add(title);
        
        if (title.contains("-")) {
            variants.add(title.replace('-', ' '));
        }
        
        if (title.contains(" ")) {
            variants.add(title.replace(' ', '-'));
        }
        
        String stripped = title.replaceAll("[^A-Za-z0-9 ]", " ").trim();
        if (!variants.contains(stripped)) {
            variants.add(stripped);
        }
        
        return variants;
    }
    
   

    private void filterMoviesWithoutEnrichment(ArrayList<Movie> movies) {
        int beforeSize = movies.size();
        
        movies.removeIf(m -> m.getDirectors().isEmpty()
                && m.getProducers().isEmpty()
                && m.getActors().isEmpty()
                && (m.getSummary() == null || m.getSummary().trim().isEmpty()));
        
        int afterSize = movies.size();
        if (beforeSize > afterSize) {
            logger.fine(String.format("Filtrage: %d films supprimés car sans information enrichie", beforeSize - afterSize));
        }
    }
    
  
    private void handleDataError(ArrayList<Object> data, Exception e) {
        logger.log(Level.SEVERE, "Erreur technique, enregistrement rejeté pour : " + data, e);
        
        try (FileWriter fw = new FileWriter(REJECT_FILE, true)) {
            fw.write(data + " -> " + e.getMessage() + "\n");
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Impossible d'écrire dans le fichier de rejet", ioe);
        }
    }
}