package source;

import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides methods to interact with a MySQL database using JDBC.
 */
public class JDBCClient {
    private static final Logger logger = Logger.getLogger(JDBCClient.class.getName());
    
    private static final String HOST = "mysql-museevasion.alwaysdata.net";
    private static final String BASE = "museevasion_movies_budgets";
    private static final String USER = "332768";
    private static final String PASSWORD = "Pmlpmlpmlk0+";

    // Connection unique partagée
    private static Connection sharedConnection = null;

    private Connection connection;

    /**
     * Constructs a new JDBCClient object and establishes a connection to the database.
     */
    public JDBCClient() {
        // Utiliser la connexion partagée ou en créer une nouvelle
        this.connection = getConnection();
    }

    /**
     * Creates a JDBC connection to the MySQL database.
     */
    public static synchronized Connection getConnection() {
        if (sharedConnection == null || isConnectionClosed(sharedConnection)) {
            try {
                // Charger le driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                // Construire l'URL de connexion JDBC avec les paramètres optimaux
                String url = "jdbc:mysql://" + HOST + "/" + BASE + 
                            "?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
                
                // Établir la connexion à la base de données
                sharedConnection = DriverManager.getConnection(url, USER, PASSWORD);
                logger.info("JDBC: connecté à " + url);
            } catch (ClassNotFoundException e) {
                logger.log(Level.SEVERE, "Driver JDBC manquant", e);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Impossible de se connecter à la base", e);
                sharedConnection = null;
            }
        }
        return sharedConnection;
    }

    /**
     * Vérifie si une connexion est fermée
     */
    private static boolean isConnectionClosed(Connection conn) {
        try {
            return conn.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

    /**
     * Creates a JDBC statement.
     * @return A Statement object.
     */
    private Statement createStatement() {
        Statement statement = null;
        try {
            statement = this.connection.createStatement();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la création du statement", e);
        }
        return statement;
    }

    /**
     * Ferme proprement la connexion (à appeler au shutdown de l'app).
     */
    public static synchronized void closeConnection() {
        if (sharedConnection != null) {
            try {
                sharedConnection.close();
                logger.info("JDBC: connexion fermée");
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Erreur à la fermeture JDBC", e);
            }
            sharedConnection = null;
        }
    }

    /**
     * Test rapide : exécute SELECT 1 pour valider la connexion.
     */
    public static boolean testConnection() {
        Connection c = getConnection();
        if (c == null) return false;
        try (Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next() && rs.getInt(1) == 1;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Test de connexion échoué", e);
            return false;
        }
    }

    /**
     * Retrieves movie information from the database based on the search term.
     * @param search The search term.
     * @return An ArrayList of ArrayLists containing movie information.
     */
    public ArrayList<ArrayList<Object>> getMovieInfo(String search) {
        if (this.connection == null) {
            this.connection = getConnection();
            if (this.connection == null) {
                return new ArrayList<>();
            }
        }
        
        Statement statement = createStatement();
        ArrayList<ArrayList<Object>> filmsTable = new ArrayList<>();
        
        if (statement == null) {
            return filmsTable;
        }

        String query = "SELECT * FROM film WHERE film.title LIKE \"%" + search + "%\"";

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
                
                ArrayList<Object> filmTable = new ArrayList<>(7);
                filmTable.add(title);
                filmTable.add(releaseDate);
                filmTable.add(genre);
                filmTable.add(distributor);
                filmTable.add(budget);
                filmTable.add(usaRevenue);
                filmTable.add(worldwideRevenue);

                filmsTable.add(filmTable);
            }
            resultSet.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de l'exécution de la requête", e);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Erreur lors de la fermeture du statement", e);
            }
        }

        return filmsTable;
    }

    /**
     * Récupère des suggestions de titres de films basées sur une sous-chaîne.
     * 
     * @param movieTitle La sous-chaîne à rechercher dans les titres de films.
     * @param limit Le nombre maximum de suggestions à retourner.
     * @return Une liste des titres de films contenant la sous-chaîne.
     */
    public ArrayList<String> getMovieTitleSuggestions(String movieTitle, int limit) {
        ArrayList<String> suggestions = new ArrayList<>();
        if (this.connection == null) {
            this.connection = getConnection();
            if (this.connection == null) {
                return suggestions;
            }
        }

        String sql = "SELECT DISTINCT title FROM film WHERE title LIKE ? ORDER BY title LIMIT ?";
        try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setString(1, "%" + movieTitle + "%");
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    suggestions.add(rs.getString("title"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur getMovieTitleSuggestions", e);
        }
        return suggestions;
    }

    /**
     * Exemple d'utilisation dans main() pour valider tout le setup JDBC.
     */
    public static void main(String[] args) {
        System.out.println("Test JDBC ping: " + (testConnection() ? "OK" : "ÉCHEC"));
        ArrayList<String> list = new JDBCClient().getMovieTitleSuggestions("Inception", 5);
        System.out.println("Suggestions pour 'Inception' : " + list);
        closeConnection();
    }
}
