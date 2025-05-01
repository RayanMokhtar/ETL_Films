package datasource;

import java.sql.*;
import java.util.ArrayList;

/**
 * Provides methods to interact with a MySQL database using JDBC.
 * This class handles database connections and movie data retrieval operations.
 * It implements the DataSource interface for consistent integration with other components.
 */
public class LocalDatabaseDataSource{
    // Database connection configuration
    private static final String HOST = "mysql-mokhtari-rayan.alwaysdata.net";
    private static final String BASE = "mokhtari-rayan_etl";
    private static final String USER ="327071";
    private static final String PASSWORD = "AZERTYUIO2";

    // Active database connection
    private Connection connection;

    public LocalDatabaseDataSource() {
        establishDatabaseConnection();
    }

    //etablisssement connexion
    private void establishDatabaseConnection() {
        Connection dbConnection = null;
        // Build the JDBC connection URL
        String jdbcUrl = "jdbc:mysql://" + HOST + "/" + BASE;
        
        try {
            // Establish connection to the database
            dbConnection = DriverManager.getConnection(jdbcUrl, USER, PASSWORD);
            System.out.println("Successfully connected to the database!");
        } catch (SQLException e) {
            // Handle connection errors
            System.err.println("Error connecting to database: " + e.getMessage());
            e.printStackTrace();
        }

        this.connection = dbConnection;
    }

    //statement pour les querySql 
    private Statement createDatabaseStatement(Connection dbConnection) {
        Statement sqlStatement = null;
        try {
            sqlStatement = dbConnection.createStatement();
        } catch (SQLException e) {
            System.err.println("Error creating SQL statement: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }

        return sqlStatement;
    }

    

    // deux fonctions pour recherche de films par titre et acteur 
    @Override
    public ArrayList<Object> getMovieByTitle(String movieTitle, boolean sensibleCasse) {
        return searchMoviesInDatabase(movieTitle, sensibleCasse);
    }
    
    //mm quand acteur , on a pas besoin de changer le nom de la fonction => reste un titre , mais on garde le nom de la fonction  pour plus de clarté 
    @Override
    public ArrayList<Object> getMoviesByActor(String titleFromActorNameRetriever , boolean sensibleCasse) {
        return searchMoviesInDatabase(titleFromActorNameRetriever, sensibleCasse);
    }

 


    // fonction de calcul
    private ArrayList<Object> searchMoviesInDatabase(String searchTerm, boolean caseSensitive) {
        Statement statement = createDatabaseStatement(this.connection);
        
        // Construct appropriate query based on case sensitivity
        String searchQuery;
        if (caseSensitive) {
            searchQuery = "SELECT * FROM Film AS f WHERE f.movie LIKE \"%" + searchTerm + "%\"";
        } else {
            searchQuery = "SELECT * FROM Film AS f WHERE LOWER(f.movie) LIKE LOWER(\"%" + searchTerm + "%\")";
        }

        ArrayList<ArrayList<Object>> filmsDataCollection = new ArrayList<>();
        
        try {
            // Execute the query and get results
            ResultSet resultSet = statement.executeQuery(searchQuery);
            filmsDataCollection = extractMovieDataFromResultSet(resultSet);
            
            // Clean up resources
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error executing movie search query: " + e.getMessage());
            e.printStackTrace();
        }

        // Convert to the expected return type
        ArrayList<Object> results = new ArrayList<Object>();
        for (ArrayList<Object> film : filmsDataCollection) {
            results.add(film);
        }
        return results;
    }
    


    // récupération des données de film
    private ArrayList<ArrayList<Object>> extractMovieDataFromResultSet(ResultSet resultSet) throws SQLException {
        ArrayList<ArrayList<Object>> moviesCollection = new ArrayList<>();
        
        while (resultSet.next()) {
            // Extract movie data fields by column name
            String title = resultSet.getString("movie");
            Date releaseDate = resultSet.getDate("release_date");
            String genre = resultSet.getString("genre");
            String distributor = resultSet.getString("distributeur");
            double productionBudget = resultSet.getDouble("production_budget");
            double domesticGross = resultSet.getDouble("domestic_gross");
            double worldwideRevenue = resultSet.getDouble("worldwide_gross");
            
            // Create a structured movie data object (represented as ArrayList)
            
            ArrayList<Object> movieData = new ArrayList<>(7); // 7 fields
            movieData.add(title);
            movieData.add(releaseDate);
            movieData.add(genre);
            movieData.add(distributor);
            movieData.add(productionBudget);
            movieData.add(domesticGross);
            movieData.add(worldwideRevenue);

            // Add this movie to the collection
            moviesCollection.add(movieData);
        }
        
        return moviesCollection;
    }
    

    //fermeture connexion
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed successfully");
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
}