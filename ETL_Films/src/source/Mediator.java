package source;

import data.Movie;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.jena.rdfxml.xmlinput.ALiteral;


/**
 * Acts as a mediator between different data sources and the application.
 */
public class Mediator {

    // Eager Singleton
    private static Mediator instance = new Mediator();

    private Mediator(){
    }


    /**
     * Returns the singleton instance of the Mediator class.
     * @return The singleton instance of Mediator.
     */
    public static Mediator getInstance(){
        return instance;
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
        JDBCClient jdbcClient = new JDBCClient();
		ArrayList<ArrayList<Object>> filmsTable = jdbcClient.getMovieInfo(movieTitle);

        for (ArrayList<Object> filmTable : filmsTable){

            Movie movie = new Movie();
            //		System.out.println("Titre: " + filmTable.get(0) + "\nDate de sortie: " + filmTable.get(1)
            //		+ "\nDistributeur: " + filmTable.get(2) + "\nBudget: " + String.format("%.0f", filmTable.get(3))
            //		+ "\nRevenus USA: " + String.format("%.0f", filmTable.get(4)) + " $\nRevenus mondiaux: "
            //		+ String.format("%.0f", filmTable.get(5)) + " $");
            movie.setTitle((String)filmTable.get(0));
            movie.setReleaseDate((Date)filmTable.get(1));
            movie.setGenre((String)filmTable.get(2));
            movie.setDistributor((String)filmTable.get(3));
            movie.setBudget((double)filmTable.get(4));
            movie.setUsaRevenue((double)filmTable.get(5));
            movie.setWorldwideRevenue((double)filmTable.get(6));

            movies.add(movie);

        }

        // Pour cette partie je vais extraire l'année de sortie d'un Film avec son nom et les mettre en commun pour la
        // exacte dans SPARQL


        /******************** DBpedia Part *****************************************************/
        // DBPedia Part
        // With movies I'm gonna extract actors, directors, producers
        for (Movie movie : movies){
            String filmTitle = movie.getTitle();
            // Pour cette partie je vais extraire l'année de sortie d'un Film avec son nom et les mettre en
            // commun pour la exacte dans SPARQL
            String releaseYear = "" + (movie.getReleaseDate().getYear() + 1900) + "";

            String filmLabel = DBpediaClient.getMovie(filmTitle, releaseYear, caseSensitive);

            // Case sensitive research or not
            ArrayList<ArrayList<Object>> moviesDetails = DBpediaClient.getMoviesDetails(filmLabel/*, caseSensitive*/);
            if (moviesDetails.get(0).size() == 0 && moviesDetails.get(1).size() == 0 && moviesDetails.get(2).size() == 0){
                // Traitement des cas spéciaux
                if (filmTitle.toLowerCase().contains("the")){
                    filmTitle = filmTitle.replace("the", "").replace("The", "").trim();

                    filmLabel = DBpediaClient.getMovie(filmTitle, releaseYear, caseSensitive);
                    moviesDetails = DBpediaClient.getMoviesDetails(filmLabel/*, caseSensitive*/);
                }

            }
            ArrayList<Object> actors = moviesDetails.get(0);
            ArrayList<Object> directors = moviesDetails.get(1);
            ArrayList<Object> producers = moviesDetails.get(2);

            if (actors.size() > 0){
                movie.addActors(actors);
                System.out.println("les acteurs :"+actors);
            }
            if (directors.size() > 0){
                movie.addDirectors(directors);
                System.out.println("les directeurs :"+directors);

            }
            if (producers.size() > 0){
                movie.addProducers(producers);
                System.out.println("les producteurs :"+producers);

            }
            System.out.println("y a rien :"+producers);

        }


        /******************** OMDb API Part *****************************************************/
        // OMDb API Part
        for(Movie movie : movies) {
        	String movieTitleFormatted = movie.getTitle().replace(' ', '+');
            String releaseYear = "" + (movie.getReleaseDate().getYear() + 1900) + "";
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
     * Retrieves movies by actor name.
     * @param actorName The name of the actor to search for.
     * @param caseSensitive Whether the search should be case-sensitive.
     * @return An ArrayList of Movie objects featuring the specified actor.
     */
    public ArrayList<Movie> getMoviesByActorName(String actorName, boolean caseSensitive){

        ArrayList<Movie> movies = new ArrayList<Movie>();
        HashMap<String, Movie> moviesByLabels = new HashMap();

        // On récupère les films de l'acteur renseigné
        ArrayList<Object> moviesLabels = DBpediaClient.getMoviesByActor(actorName, caseSensitive, true);


        /******************** JDBC Part *****************************************************/


        // Ararylist des films qui n'existent pas
        ArrayList<Object> notFoundMoviesLabels = new ArrayList<>();

        // On croise les films sql avec les films dpbedia
        for(Object movieLabel : moviesLabels) {
            JDBCClient jdbcClient = new JDBCClient();
            String movieTitle = ((String[])movieLabel)[0].split("\\(")[0].trim();
            ArrayList<ArrayList<Object>> filmsTable = jdbcClient.getMovieInfo(movieTitle);
            if(!filmsTable.isEmpty()){
                for (ArrayList<Object> filmTable : filmsTable){
                    Movie movie = new Movie();

                    // Pour cette partie je vais extraire l'année de sortie d'un Film avec son nom et les mettre en
                    // commun pour la exacte dans SPARQL
                    String releaseYearSql = "" + (((Date)filmTable.get(1)).getYear() + 1900) + "";
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
        System.out.println("Films de "+ actorName +" non trouvés :\n");
        for (Object movieObject: notFoundMoviesLabels){
            String movieLabel = ((String[])movieObject)[0];
            System.out.println(movieLabel);
        }

        // Pour cette partie je vais extraire l'année de sortie d'un Film avec son nom et les mettre en commun pour la
        // exacte dans SPARQL


        /******************** DBpedia Part *****************************************************/
        // DBPedia Part
        // With movies I'm gonna extract actors, directors, producers
        for (Entry<String, Movie> movieByLabel : moviesByLabels.entrySet()){
//            String filmTitle = movie.getTitle();
            // Pour cette partie je vais extraire l'année de sortie d'un Film avec son nom et les mettre en
            // commun pour la exacte dans SPARQL
//            String releaseYear = "" + (movie.getReleaseDate().getYear() + 1900) + "";

            String filmLabel = movieByLabel.getKey();
            Movie movie = movieByLabel.getValue();

            // Case sensitive research or not
            ArrayList<ArrayList<Object>> moviesDetails = DBpediaClient.getMoviesDetails(filmLabel/*, caseSensitive*/);
//            if (moviesDetails.get(0).size() == 0 && moviesDetails.get(1).size() == 0 && moviesDetails.get(2).size() == 0){
//                // Traitement des cas spéciaux
//                if (filmTitle.toLowerCase().contains("the")){
//                    filmTitle = filmTitle.replace("the", "").replace("The", "").trim();
//
//                    filmLabel = DBpediaClient.getMovie(filmTitle, releaseYear, caseSensitive);
//                    moviesDetails = DBpediaClient.getMoviesDetails(filmLabel/*, caseSensitive*/);
//                }
//
//            }
            ArrayList<Object> actors = moviesDetails.get(0);
            ArrayList<Object> directors = moviesDetails.get(1);
            ArrayList<Object> producers = moviesDetails.get(2);
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
            String releaseYear = "" + (movie.getReleaseDate().getYear() + 1900) + "";
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

}
