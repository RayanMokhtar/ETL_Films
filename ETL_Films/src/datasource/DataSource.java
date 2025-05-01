package datasource;

import java.util.ArrayList;

/**
 * Interface pour les sources de données qui fournissent des informations sur les films.
 */
public interface DataSource {

    ArrayList<Object> getMovieByTitle(String movieTitle, boolean caseSensitive);
    ArrayList<Object> getMoviesByActor(String actorName, boolean caseSensitive);
}