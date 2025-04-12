package source;

import org.apache.jena.query.*;

import java.sql.*;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * Provides methods to interact with a MySQL database using JDBC.
 */
public class JDBCClient {
    private static final String HOST = "mysql-museevasion.alwaysdata.net";
    private static final String BASE = "museevasion_movies_budgets";
    private static final String USER = "332768";
//    private static final String USER = "root";
    private static final String PASSWORD = "Pmlpmlpmlk0+";
//    private static final String PASSWORD = "";

    private Connection connection;


    /**
     * Constructs a new JDBCClient object and establishes a connection to the database.
     */
    public JDBCClient(){
        // Create a connection
        createConnection();

    }


    /**
     * Creates a JDBC connection to the MySQL database.
     */
    private void createConnection(){
        Connection connection = null;
        // Construire l'URL de connexion JDBC
        String url = "jdbc:mysql://" + HOST + "/" + BASE;
        try {
            // Établir la connexion à la base de données
            connection = DriverManager.getConnection(url, USER, PASSWORD);
            System.out.println("Connexion réussie à la base de données!");
        } catch (SQLException e) {
            // Gérer les erreurs liées à la connexion
            System.err.println("Erreur lors de la connexion à la base de données : " + e.getMessage());
        }

        this.connection = connection;
    }


    /**
     * Creates a JDBC statement.
     * @param connection The JDBC connection.
     * @return A Statement object.
     */
    private static Statement createStatement(Connection connection){
        Statement statement = null;
        try {
            statement = connection.createStatement();
        } catch (SQLException e) {
            System.out.println("Error creating statement");
            e.printStackTrace();
        } catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return statement;
    }

    /**
     * Retrieves movie information from the database based on the search term.
     * @param search The search term.
     * @return An ArrayList of ArrayLists containing movie information.
     */
    public ArrayList<ArrayList<Object>> getMovieInfo(String search){
        Statement statement = createStatement(this.connection);

        String query = "SELECT * FROM film" +
                " WHERE film.title LIKE \"%" + search +"%\" ";

        ArrayList<ArrayList<Object>> filmsTable = new ArrayList<>();
        try {
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                // Récupérer les données par nom de colonne
                String title = resultSet.getString("title");
                Date releaseDate = resultSet.getDate("release_date");
                String genre = resultSet.getString("genre");
                String distributor = resultSet.getString("distributor");
                double budget = resultSet.getDouble("budget");
                double usaRevenue = resultSet.getDouble("usa_revenue");
                double worldwideRevenue = resultSet.getDouble("worldwide_revenue");
                ArrayList<Object> filmTable = new ArrayList<>(6);
                filmTable.add(title);
                filmTable.add(releaseDate);
                filmTable.add(genre);
                filmTable.add(distributor);
                filmTable.add(budget);
                filmTable.add(usaRevenue);
                filmTable.add(worldwideRevenue);

                filmsTable.add(filmTable);

                // Afficher les résultats
//                System.out.println("Titre: " + title + "\nDate de sortie: " + releaseDate
//                        + "\nDistributeur: " + distributor + "\nBudget: " + String.format("%.0f", budget)
//                        + "\nRevenus USA: " + String.format("%.0f", usaRevenue) + " $\nRevenus mondiaux: "
//                        + String.format("%.0f", worldwideRevenue) + " $");
            }
        } catch (SQLException e) {
            System.out.println("Error executing query");
            e.printStackTrace();
        }

        return filmsTable;

    }

}
