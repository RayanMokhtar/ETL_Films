package repository;

import core.Movie;
import java.util.ArrayList;

/**
 * Interface repository pour l'accès aux données des films.
 */
public interface MovieRepository {
    /*
    Titre des films
     */
    ArrayList<Movie> getMoviesByTitle(String movieTitle, boolean caseSensitive);
    
    /**
     Movies par nom d'acteur
     */
    ArrayList<Movie> getMoviesByActorName(String actorName, boolean caseSensitive);
}