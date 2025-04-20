package source;

import java.util.ArrayList;

/**
 * Test console pour DBpediaClient : affiche URI, réalisateur, producteurs et acteurs pour "Inception".
 */
public class DBpediaClientTest {
    public static void main(String[] args) {
        DBpediaClient client = new DBpediaClient();
        System.out.println("Test SPARQL getMoviesDetails pour 'Inception':");
        ArrayList<ArrayList<Object>> details = client.getMoviesDetails("Inception");
        if (details.size() == 3) {
            System.out.println("Réalisateurs : " + details.get(0));
            System.out.println("Producteurs  : " + details.get(1));
            System.out.println("Acteurs      : " + details.get(2));
        } else {
            System.out.println("Aucun résultat getMoviesDetails pour 'Inception'.");
        }
    }
}