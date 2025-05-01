/**
 * @file FacadeClient.java
 * @brief This file contains the FacadeClient class which acts as the GUI front-end for a movie search application.
 * @package client
 */

 package client;


 import javax.swing.*;
 import javax.swing.table.DefaultTableCellRenderer;
 import javax.swing.table.DefaultTableModel;
 
 import core.Movie;
 
 import java.awt.*;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import repository.MovieRepository;
import repository.MovieRepositoryConcrete;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


 public class FacadeClient{
     private static MovieRepository repository = new MovieRepositoryConcrete();
    
    
    
/**
 * Configure les logs pour l'application entière
 * @param verbose true pour activer les logs détaillés
 */
private static void configureLogging(boolean verbose) {
    Level level = verbose ? Level.INFO : Level.WARNING;

    // 1) Création et configuration du handler console
    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setLevel(Level.ALL);  // on laisse tout passer au handler
    consoleHandler.setFormatter(new SimpleFormatter() {
        @Override
        public String format(LogRecord record) {
            return String.format("[%1$-7s] %2$s - %3$s%n",
                record.getLevel(),
                record.getLoggerName(),
                record.getMessage()
            );
        }
    });

    // 2) Récupération du logger racine et setLevel
    Logger rootLogger = Logger.getLogger("");
    rootLogger.setLevel(level);

    // 3) On retire les handlers existants (souvent un default ConsoleHandler)
    for (Handler h : rootLogger.getHandlers()) {
        rootLogger.removeHandler(h);
    }

    // 4) On ajoute notre consoleHandler configuré
    rootLogger.addHandler(consoleHandler);

    // 5) Propagation du niveau aux classes qui ont leur propre setVerboseLogging
    //    Exemple pour votre repository
    MovieRepositoryConcrete.setVerboseLogging(verbose);
}
     
    public static void main(String[] args) {
        configureLogging(true);
         SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                 createAndShowGUI();
             }
         });
     }
 
     /**
      * @brief Creates and displays the main application window.
      *
      * This private static method sets up the main JFrame and its components including panels, labels, a text field,
      * a combo box for search options, and a button for initiating searches. It also configures a table to display
      * search results and handles the search action.
      */
     private static void createAndShowGUI() {
         JFrame frame = new JFrame("Application de recherche de films");
         frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         frame.setSize(800, 400);
 
         JPanel panel = new JPanel(new BorderLayout());
         frame.add(panel);
 
         JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
         JLabel searchLabel = new JLabel("Recherche par :");
         JComboBox<String> searchOptions = new JComboBox<>(new String[]{"Titre de film", "Nom d'acteur"});
         JTextField inputField = new JTextField(20);
         JButton searchButton = new JButton("Rechercher");
 
         searchPanel.add(searchLabel);
         searchPanel.add(searchOptions);
         searchPanel.add(inputField);
         searchPanel.add(searchButton);
 
         DefaultTableModel tableModel = new DefaultTableModel();
         tableModel.addColumn("Titre");
         tableModel.addColumn("Date de sortie");
         tableModel.addColumn("Genre");
         tableModel.addColumn("Distributeur");
         tableModel.addColumn("Budget");
         tableModel.addColumn("Revenus USA");
         tableModel.addColumn("Revenus mondiaux");
         tableModel.addColumn("Réalisateurs");
         tableModel.addColumn("Acteurs");
         tableModel.addColumn("Producteurs");
         tableModel.addColumn("Résumé");
 
         JTable resultTable = new JTable(tableModel);
         resultTable.setRowHeight(200); // Ajustez selon vos besoins
 
 
         // Utiliser un renderer pour rendre le texte multiligne
         DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
         renderer.setVerticalAlignment(SwingConstants.TOP); // Pour le rendu multiligne
         resultTable.setDefaultRenderer(Object.class, renderer);
 
         JScrollPane scrollPane = new JScrollPane(resultTable);
 
         searchButton.addActionListener(new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 String search = inputField.getText();
                 if (search == null || search.isEmpty()) {
                     JOptionPane.showMessageDialog(frame, "Veuillez entrer un terme de recherche.", "Alerte", JOptionPane.WARNING_MESSAGE);
                     return;
                 }
 
                 boolean caseSensitive = true;
                 ArrayList<Movie> movies = new ArrayList<>();
                 if (searchOptions.getSelectedIndex() == 0) {
                     movies = repository.getMoviesByTitle(search, caseSensitive);
                 } else {
                     movies = repository.getMoviesByActorName(search, caseSensitive);
                 }
 
                 tableModel.setRowCount(0); // Efface les lignes précédentes
 
                 if (movies.isEmpty()) {
                     JOptionPane.showMessageDialog(frame, "Aucun résultat trouvé pour \"" + search + "\".", "Information", JOptionPane.INFORMATION_MESSAGE);
                 } else {
                     for (Movie movie : movies) {
                    	Object[] rowData = { formatField(movie.getTitle(), movie.getStringTitle()),
                        formatField(movie.getReleaseDate(), movie.getStringReleaseDate()),
                        formatField(movie.getGenre(), movie.getStringGenre()),
                        formatField(movie.getDistributor(), movie.getStringDistributor()),
                        formatField(movie.getBudget() > 0 ? movie.getBudget() : null, movie.getStringBudget()),
                        formatField(movie.getUsaRevenue() > 0 ? movie.getUsaRevenue() : null, movie.getStringUsaRevenue()),
                        formatField(movie.getWorldwideRevenue() > 0 ? movie.getWorldwideRevenue() : null, movie.getStringWorldwideRevenue()),
                        formatField(!movie.getDirectors().isEmpty(), movie.getStringHTMLDirectors()),
                        formatField(!movie.getActors().isEmpty(), movie.getStringHTMLActors()),
                        formatField(!movie.getProducers().isEmpty(), movie.getStringHTMLProducers()),
                        formatField(movie.getSummary(), movie.getSummary()),
                        };
                         tableModel.addRow(rowData);
                     }
                 }
             }
         });
 
         panel.add(searchPanel, BorderLayout.NORTH);
         panel.add(scrollPane, BorderLayout.CENTER);
 
         frame.setVisible(true);
     }



     private static String formatField(Object value, String formattedValue) {
    	    // Cas 1: La valeur est null
    	    if (value == null) {
    	        return "<html><p style='color:#999999;'><i>n'est pas mentionné</i></p></html>";
    	    }
    	    
    	    // Cas 2: La valeur est une chaîne
    	    if (value instanceof String) {
    	        String stringValue = ((String)value).trim();
    	     // cas spécifiques pour résumé également => on gère ça dans le retour api 
    	        if (stringValue.isEmpty() || 
    	            stringValue.equals("N/A") || 
    	            stringValue.equals("null") ||
    	            stringValue.equals("<html>\n<p></p>\n</html>") ||
    	            stringValue.equals("<html>\n<p>No plot available.</p>\n</html>")) {
    	            return "<html><p style='color:#999999;'><i>n'est pas mentionné</i></p></html>";
    	        }
    	    }
    	    
    	    return formattedValue;
   }

private static String formatField(boolean hasValue, String formattedValue) {
    if (!hasValue) {
        return "<html><p style='color:#999999;'><i>n'est pas mentionné</i></p></html>";
    }
    return formattedValue;
	}
}