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

// Classe médiatrice entre la BDD locale, DBpedia et OMDb
public class Mediator implements MovieRepository {

    // sources de données
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

    // recherche par titre
    @Override
    public ArrayList<Movie> getMoviesByTitle(String movieTitle, boolean caseSensitive) {
        ArrayList<Movie> movies = new ArrayList<>();
        ArrayList<Object> localResults = dbDataSource.getMovieByTitle(movieTitle, caseSensitive);
        if (localResults == null || localResults.isEmpty()) return movies;
        for (Object result : localResults) {
            ArrayList<Object> row = (ArrayList<Object>) result;
            if (row.size() < 7) continue;
            try {
                movies.add(createMovieFromData(row));
            } catch (Exception e) {}
        }
        // ajout des détails externes
        enrichMoviesWithExternalData(movies);
        return movies;
    }

    // recherche par acteur
    @Override
    public ArrayList<Movie> getMoviesByActorName(String actorName, boolean caseSensitive) {
        ArrayList<Movie> movies = new ArrayList<>();
        Map<String, Movie> uniqueMovies = new HashMap<>();
        ArrayList<Object> actorResults = dbpediaDataSource.getMoviesByActor(actorName, caseSensitive);
        if (actorResults.isEmpty()) return movies;
        for (Object entry : actorResults) {
            @SuppressWarnings("unchecked") HashMap<String, Object> movieData = (HashMap<String, Object>) entry;
            String rawTitle = (String) movieData.get("title");
            String title = rawTitle.replaceAll("\\s*\\(\\d{4}\\)$", "");
            ArrayList<Object> localResults = dbDataSource.getMovieByTitle(title, false);
            if (localResults == null || localResults.isEmpty()) {
                continue;
            }
            for (Object localObj : localResults) {
                ArrayList<Object> row = (ArrayList<Object>) localObj;
                if (row.size() < 7) continue;
                try {
                    Movie movie = createMovieFromData(row);
                    movie.addActors((ArrayList<Object>) movieData.get("actors"));
                    movie.addDirectors((ArrayList<Object>) movieData.get("directors"));
                    movie.addProducers((ArrayList<Object>) movieData.get("producers"));
                    addMovieSynopsis(movie);
                    String key = movie.getTitle() + "_" + movie.getReleaseDate().toLocalDate().getYear();
                    uniqueMovies.put(key, movie);
                } catch (Exception e) {}
            }
        }
        movies.addAll(uniqueMovies.values());
        enrichMoviesWithExternalData(movies);
        return movies;
    }

    // suggestions de titre
    public ArrayList<String> getMovieTitleSuggestions(String subString, int limit) {
        ArrayList<String> suggestions = new ArrayList<>();
        ArrayList<Object> results = dbDataSource.getMovieByTitle(subString, false);
        for (Object obj : results) {
            suggestions.add((String) ((ArrayList<Object>) obj).get(0));
            if (suggestions.size() >= limit) break;
        }
        return suggestions;
    }

    // fermeture
    public void closeConnections() {
        dbDataSource.closeConnection();
    }

    // création d'objet Movie à partir des données brutes
    private Movie createMovieFromData(ArrayList<Object> data) {
        Movie m = new Movie();
        m.setTitle((String) data.get(0));
        m.setReleaseDate((Date) data.get(1));
        m.setGenre((String) data.get(2));
        m.setDistributor((String) data.get(3));
        m.setBudget((double) data.get(4));
        m.setUsaRevenue((double) data.get(5));
        m.setWorldwideRevenue((double) data.get(6));
        return m;
    }

    // enrichissement des films
    private void enrichMoviesWithExternalData(ArrayList<Movie> movies) {
        for (Movie movie : movies) {
            ArrayList<HashMap<String,Object>> dbpResults = (ArrayList) dbpediaDataSource.getMovieByTitle(movie.getTitle(), false);
            if (!dbpResults.isEmpty()) {
                HashMap<String, Object> data = dbpResults.get(0);
                movie.addActors((ArrayList<Object>) data.get("actors"));
                movie.addDirectors((ArrayList<Object>) data.get("directors"));
                movie.addProducers((ArrayList<Object>) data.get("producers"));
            }
            addMovieSynopsis(movie);
        }
    }

    // récupération du synopsis via OMDb
    private void addMovieSynopsis(Movie movie) {
        for (String variant : generateTitleVariants(movie.getTitle())) {
            String qt = variant.replace(' ', '+');
            ArrayList<Object> omdb = omdbDataSource.getMovieByTitle(qt, false);
            if (!omdb.isEmpty()) { movie.setSummary((String) omdb.get(0)); break; }
        }
    }

    // variantes de titre
    private ArrayList<String> generateTitleVariants(String title) {
        ArrayList<String> v = new ArrayList<>();
        v.add(title);
        v.add(title.replace('-', ' '));
        v.add(title.replace(' ', '-'));
        v.add(title.replaceAll("[^A-Za-z0-9 ]", " ").trim());
        return v;
    }

    // filtrage optionnel (non utilisé)
    private void filterMoviesWithoutEnrichment(ArrayList<Movie> movies) {
        movies.removeIf(m -> m.getDirectors().isEmpty() && m.getProducers().isEmpty() && m.getActors().isEmpty() && (m.getSummary() == null || m.getSummary().trim().isEmpty()));
    }

    // gestion basique des erreurs
    private void handleDataError(ArrayList<Object> data, Exception e) {
        try (FileWriter fw = new FileWriter(REJECT_FILE, true)) {
            fw.write(data + " -> " + e.getMessage() + "\n");
        } catch (IOException ioe) {}
    }
}