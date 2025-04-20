/**
 * @file FacadeClient.java
 * @brief This file contains the FacadeClient class which acts as the GUI front-end for a movie search application.
 * @package client
 */

package client;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import data.Movie;
import source.Mediator;

/**
 * @class FacadeClient
 * @brief The FacadeClient class provides a graphical user interface to search for movies.
 *
 * This class uses Swing components to create a user-friendly environment for searching and displaying movies
 * based on various search criteria like movie title or actor name. The interaction with the database
 * and the business logic is handled by the Mediator class.
 */
public class FacadeClient {
    private static Mediator mediator = Mediator.getInstance();
    private static Color primaryColor = new Color(25, 42, 86); // Bleu marine plus foncé pour contraste
    private static Color secondaryColor = new Color(41, 128, 185); // Bleu brillant
    private static Color accentColor = new Color(231, 76, 60);
    private static Color successColor = new Color(39, 174, 96);
    private static Color backgroundColor = new Color(240, 243, 247); // Fond clair subtil
    private static Color textColor = new Color(44, 62, 80); // Texte foncé pour contraste
    private static Color headerBgColor = new Color(32, 47, 90); // Légèrement plus clair que primaryColor
    private static Color cardColor = Color.WHITE; // Couleur pour les "cartes" d'information

    private static Font titleFont = new Font("Segoe UI", Font.BOLD, 24);
    private static Font subtitleFont = new Font("Segoe UI", Font.ITALIC, 14);
    private static Font normalFont = new Font("Segoe UI", Font.PLAIN, 14);
    private static Font smallFont = new Font("Segoe UI", Font.PLAIN, 12);
    private static Font headerFont = new Font("Segoe UI", Font.BOLD, 13);

    private static JProgressBar progressBar;
    private static JFrame frame;
    private static Timer progressTimer;
    private static JTextField inputField;
    private static JPopupMenu suggestionsPopup;
    private static JButton searchButton;
    private static Timer suggestionsTimer;
    private static final int MAX_SUGGESTIONS = 10;

    /**
     * Interface pour l'ActionListener du timer avec des méthodes supplémentaires
     */
    private interface TimerActionListener extends ActionListener {
        void startSearching();
        void finishSearching();
    }

    public static void main(String[] args) {
        try {
            // Utiliser le look and feel du système
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Personnaliser certains éléments UI
            UIManager.put("Button.background", backgroundColor);
            UIManager.put("Button.font", normalFont);
            UIManager.put("Label.font", normalFont);
            UIManager.put("TextField.font", normalFont);
            UIManager.put("ComboBox.font", normalFont);
            UIManager.put("Table.font", smallFont);
            UIManager.put("TableHeader.font", headerFont);
            UIManager.put("OptionPane.messageFont", normalFont);
            UIManager.put("OptionPane.buttonFont", normalFont);
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        // Créer une fenêtre avec une belle icône
        frame = new JFrame("Cinémathèque - Recherche de Films");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 800);  // Fenêtre plus grande pour une meilleure expérience
        frame.setLocationRelativeTo(null);

        // Définition du style global
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(backgroundColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        frame.add(mainPanel);

        // Création d'un bandeau de titre élégant avec un dégradé
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Création d'un dégradé du haut vers le bas
                GradientPaint gp = new GradientPaint(
                    0, 0, primaryColor, 
                    0, getHeight(), headerBgColor
                );

                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel titleLabel = new JLabel("CINÉMATHÈQUE");
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Base de données cinématographique");
        subtitleLabel.setFont(subtitleFont);
        subtitleLabel.setForeground(new Color(255, 255, 255, 200));

        JPanel titleContainer = new JPanel(new GridLayout(2, 1));
        titleContainer.setOpaque(false);
        titleContainer.add(titleLabel);
        titleContainer.add(subtitleLabel);

        headerPanel.add(titleContainer, BorderLayout.WEST);

        // Ajout d'une option pour contrôler le mode verbeux
        JCheckBox verboseCheckBox = new JCheckBox("Mode verbeux");
        verboseCheckBox.setOpaque(false);
        verboseCheckBox.setForeground(Color.WHITE);
        verboseCheckBox.setFont(smallFont);
        verboseCheckBox.setSelected(false); // Désactivé par défaut
        verboseCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Mediator.setVerboseLogging(verboseCheckBox.isSelected());
            }
        });

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        optionsPanel.setOpaque(false);
        optionsPanel.add(verboseCheckBox);
        headerPanel.add(optionsPanel, BorderLayout.EAST);

        // Panneau de recherche avec effet d'ombre et coins arrondis
        JPanel searchPanel = new RoundedPanel(20, cardColor);
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel searchControlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        searchControlsPanel.setOpaque(false);

        JLabel searchLabel = new JLabel("Recherche par :");
        searchLabel.setFont(normalFont);
        searchLabel.setForeground(textColor);

        String[] searchOptionsText = {"Titre de film", "Nom d'acteur"};
        JComboBox<String> searchOptions = new JComboBox<>(searchOptionsText);
        searchOptions.setFont(normalFont);
        searchOptions.setPreferredSize(new Dimension(150, 40));
        searchOptions.setBackground(Color.WHITE);
        searchOptions.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        // Champ de recherche avec style moderne et suggestions
        inputField = new JTextField();
        inputField.setFont(normalFont);
        inputField.setPreferredSize(new Dimension(350, 40));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // Initialiser le popup de suggestions
        suggestionsPopup = new JPopupMenu();
        suggestionsPopup.setFocusable(false);
        suggestionsPopup.setBackground(Color.WHITE);
        suggestionsPopup.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Ajouter un listener pour détecter les changements dans le champ de recherche
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSuggestions();
            }
        });
        
        // Focus listener pour le champ de texte
        inputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Ne pas fermer si on clique dans le popup
                if (e.getOppositeComponent() != null && SwingUtilities.isDescendingFrom(e.getOppositeComponent(), suggestionsPopup)) {
                    return;
                }
                suggestionsPopup.setVisible(false);
            }
                
            @Override
            public void focusGained(FocusEvent e) {
                if (inputField.getText().length() >= 2) {
                    updateSuggestions();
                }
            }
        });

        // Création d'un bouton de recherche moderne avec un effet de survol
        searchButton = new RoundedButton("Rechercher", secondaryColor);
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchButton.setForeground(Color.WHITE);
        searchButton.setPreferredSize(new Dimension(150, 40));

        // Indicateur de recherche avec icône
        JPanel statusPanel = new JPanel(new BorderLayout(0, 10));
        statusPanel.setOpaque(false);
        JLabel statusLabel = new JLabel("");
        statusLabel.setFont(smallFont);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Ajout d'une barre de progression personnalisée
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(400, 8));
        progressBar.setBackground(new Color(230, 230, 230));
        progressBar.setForeground(secondaryColor);
        progressBar.setBorder(BorderFactory.createEmptyBorder());

        // Personnaliser la barre de progression avec un look plus moderne
        progressBar.setUI(new BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                // Fond arrondi
                g2d.setColor(progressBar.getBackground());
                g2d.fillRoundRect(0, 0, w, h, h, h);

                // Barre de progression arrondie
                if (progressBar.getValue() > 0) {
                    int x = (int) (progressBar.getPercentComplete() * w);
                    g2d.setColor(progressBar.getForeground());
                    g2d.fillRoundRect(0, 0, x, h, h, h);
                }
            }

            @Override
            protected void paintIndeterminate(Graphics g, JComponent c) {
                // Utiliser l'implémentation par défaut pour l'indeterminé
                super.paintIndeterminate(g, c);
            }
        });

        statusPanel.add(statusLabel, BorderLayout.NORTH);
        statusPanel.add(progressBar, BorderLayout.CENTER);

        searchControlsPanel.add(searchLabel);
        searchControlsPanel.add(searchOptions);
        searchControlsPanel.add(inputField);
        searchControlsPanel.add(searchButton);

        searchPanel.add(searchControlsPanel);
        searchPanel.add(Box.createVerticalStrut(15)); // Espacement
        searchPanel.add(statusPanel);

        // Animation du timer pour la barre de progression
        progressTimer = new Timer(50, new TimerActionListener() {
            private int currentValue = 0;
            private boolean isSearching = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                // Simuler une progression pendant la recherche
                if (isSearching) {
                    currentValue = Math.min(currentValue + 1, 95); // Max 95% pour l'animation
                    progressBar.setValue(currentValue);
                } else {
                    // Compléter rapidement la barre à 100% quand la recherche est finie
                    currentValue = Math.min(currentValue + 5, 100);
                    progressBar.setValue(currentValue);

                    if (currentValue >= 100) {
                        progressTimer.stop();
                        // Après 500ms, cacher la barre de progression
                        Timer hideTimer = new Timer(500, evt -> {
                            progressBar.setVisible(false);
                            ((Timer)evt.getSource()).stop();
                        });
                        hideTimer.setRepeats(false);
                        hideTimer.start();
                    }
                }
            }

            // Méthodes pour contrôler l'animation
            public void startSearching() {
                isSearching = true;
                currentValue = 0;
                progressBar.setValue(0);
                progressBar.setVisible(true);
                progressTimer.start();
            }

            public void finishSearching() {
                isSearching = false;
            }
        });

        // Ajouter un panneau pour les résultats de recherche avec un titre
        JPanel resultsPanel = new JPanel(new BorderLayout(0, 10));
        resultsPanel.setBackground(backgroundColor);
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Titre des résultats avec style amélioré
        JLabel resultsTitle = new JLabel("Résultats de recherche");
        resultsTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        resultsTitle.setForeground(primaryColor);
        resultsTitle.setBorder(BorderFactory.createEmptyBorder(5, 5, 15, 5));

        // Table de résultats avec un style moderne
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

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
        resultTable.setRowHeight(120);
        resultTable.setFont(smallFont);
        resultTable.setRowMargin(6);
        resultTable.setIntercellSpacing(new Dimension(10, 5));
        resultTable.setShowGrid(true);
        resultTable.setGridColor(new Color(220, 225, 230));
        
        // Restaurer la capacité de redimensionnement automatique des colonnes
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.setSelectionBackground(new Color(232, 242, 254));
        resultTable.setSelectionForeground(textColor);

        // Permettre le redimensionnement manuel des colonnes par l'utilisateur
        resultTable.getTableHeader().setResizingAllowed(true);
        resultTable.getTableHeader().setReorderingAllowed(false);

        // Configuration claire des en-têtes de colonnes
        JTableHeader tableHeader = resultTable.getTableHeader();
        tableHeader.setFont(headerFont);
        tableHeader.setBackground(headerBgColor);
        tableHeader.setForeground(Color.WHITE);
        tableHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, primaryColor));

        // Renderer personnalisé pour l'en-tête de table pour s'assurer que le texte est visible
        tableHeader.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                          boolean isSelected, boolean hasFocus,
                                                          int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(headerFont);
                c.setBackground(headerBgColor); 
                c.setForeground(Color.WHITE); // S'assurer que le texte est blanc sur fond foncé
                ((JLabel)c).setHorizontalAlignment(SwingConstants.CENTER);
                ((JLabel)c).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                return c;
            }
        });

        // Renderer personnalisé pour des cellules multiligne avec HTML et style amélioré
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                JLabel label = (JLabel) c;
                label.setVerticalAlignment(SwingConstants.TOP);
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(220, 225, 230)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));

                // Alterner les couleurs des lignes pour une meilleure lisibilité
                if (!isSelected) {
                    label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }

                return label;
            }
        };
        resultTable.setDefaultRenderer(Object.class, cellRenderer);

        // Configurer des largeurs de colonnes plus étroites
        TableColumnModel columnModel = resultTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(150); // Titre
        columnModel.getColumn(1).setPreferredWidth(80); // Date
        columnModel.getColumn(2).setPreferredWidth(100); // Genre
        columnModel.getColumn(3).setPreferredWidth(120); // Distributeur
        columnModel.getColumn(4).setPreferredWidth(80); // Budget
        columnModel.getColumn(5).setPreferredWidth(80); // Rev USA
        columnModel.getColumn(6).setPreferredWidth(80); // Rev Mondial
        columnModel.getColumn(7).setPreferredWidth(120); // Réalisateurs
        columnModel.getColumn(8).setPreferredWidth(120); // Acteurs
        columnModel.getColumn(9).setPreferredWidth(120); // Producteurs
        columnModel.getColumn(10).setPreferredWidth(200); // Résumé

        // Ajouter un tooltip pour montrer le contenu complet au survol
        resultTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                int row = resultTable.rowAtPoint(p);
                int col = resultTable.columnAtPoint(p);
                if (row >= 0 && col >= 0) {
                    Object value = resultTable.getValueAt(row, col);
                    if (value != null) {
                        String htmlText = value.toString();
                        // Extraire le texte du HTML si nécessaire
                        if (htmlText.startsWith("<html>")) {
                            htmlText = htmlText.replaceAll("<[^>]*>", "").trim();
                        }
                        resultTable.setToolTipText(htmlText);
                    } else {
                        resultTable.setToolTipText(null);
                    }
                }
            }
        });

        // Double-clic pour afficher les détails complets d'un film
        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = resultTable.getSelectedRow();
                    if (row >= 0) {
                        showMovieDetails(row, tableModel);
                    }
                }
            }
        });

        // Créer un JScrollPane avec une bordure élégante
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.getViewport().setBackground(backgroundColor);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                new DropShadowBorder(new Color(0, 0, 0, 100), 5, 0.2f, 10, false, true, true, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Configuration de la barre de défilement
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        resultsPanel.add(resultsTitle, BorderLayout.NORTH);
        resultsPanel.add(scrollPane, BorderLayout.CENTER);

        // Ajout des composants au panneau principal
        JPanel contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setBackground(backgroundColor);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.add(searchPanel, BorderLayout.NORTH);
        contentPanel.add(resultsPanel, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Référence à l'ActionListener du timer pour contrôler la barre de progression
        TimerActionListener progressController = (TimerActionListener) progressTimer.getActionListeners()[0];

        // Logique de recherche
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String search = inputField.getText().trim();
                if (search.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, 
                        "Veuillez entrer un terme de recherche.", 
                        "Champ requis", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Fermer le popup de suggestions
                suggestionsPopup.setVisible(false);
                
                // Effacer les résultats précédents immédiatement
                tableModel.setRowCount(0);

                // Afficher un message de chargement
                resultsTitle.setText("Recherche en cours pour : \"" + search + "\"");
                statusLabel.setText("Recherche en cours...");
                statusLabel.setForeground(secondaryColor);
                searchButton.setEnabled(false);

                // Démarrer l'animation de la barre de progression
                ((TimerActionListener)progressController).startSearching();

                // Utiliser SwingWorker pour exécuter la recherche en arrière-plan
                SwingWorker<List<Movie>, Void> worker = new SwingWorker<List<Movie>, Void>() {
                    @Override
                    protected List<Movie> doInBackground() throws Exception {
                        boolean caseSensitive = false; // Recherche insensible à la casse
                        if (searchOptions.getSelectedIndex() == 0) {
                            return mediator.getMoviesByMovieTitle(search, caseSensitive);
                        } else {
                            return mediator.getMoviesByActorName(search, caseSensitive);
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            List<Movie> movies = get();

                            // Indiquer que la recherche est terminée
                            ((TimerActionListener)progressController).finishSearching();

                            if (movies.isEmpty()) {
                                resultsTitle.setText("Aucun résultat pour : \"" + search + "\"");
                                statusLabel.setText("Aucun résultat trouvé.");
                                statusLabel.setForeground(accentColor);

                                JOptionPane.showMessageDialog(frame, 
                                    "Aucun résultat trouvé pour \"" + search + "\".", 
                                    "Recherche terminée", 
                                    JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                for (Movie movie : movies) {
                                    Object[] rowData = {
                                        movie.getStringTitle(),
                                        movie.getStringReleaseDate(),
                                        movie.getStringGenre(),
                                        movie.getStringDistributor(),
                                        movie.getStringBudget(),
                                        movie.getStringUsaRevenue(),
                                        movie.getStringWorldwideRevenue(),
                                        movie.getStringHTMLDirectors(),
                                        movie.getStringHTMLActors(),
                                        movie.getStringHTMLProducers(),
                                        movie.getSummary()
                                    };
                                    tableModel.addRow(rowData);
                                }
                                resultsTitle.setText(movies.size() + " film(s) trouvé(s) pour : \"" + search + "\"");
                                statusLabel.setText("Recherche terminée avec succès. Double-cliquez sur un film pour plus de détails.");
                                statusLabel.setForeground(successColor);
                            }
                        } catch (Exception ex) {
                            // Indiquer que la recherche est terminée en cas d'erreur
                            ((TimerActionListener)progressController).finishSearching();

                            resultsTitle.setText("Erreur de recherche");
                            statusLabel.setText("Erreur lors de la recherche: " + ex.getMessage());
                            statusLabel.setForeground(accentColor);
                            ex.printStackTrace();

                            JOptionPane.showMessageDialog(frame, 
                                "Une erreur est survenue : " + ex.getMessage(), 
                                "Erreur", 
                                JOptionPane.ERROR_MESSAGE);
                        } finally {
                            searchButton.setEnabled(true);
                        }
                    }
                };
                worker.execute();
            }
        });

        // Action sur touche Entrée
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && searchButton.isEnabled()) {
                    searchButton.doClick();
                }
            }
        });

        frame.setVisible(true);
    }
    
    /**
     * Met à jour les suggestions de films en fonction du texte saisi
     */
    private static void updateSuggestions() {
        String text = inputField.getText().trim();
        
        // Ne pas afficher de suggestions si le texte est trop court
        if (text.length() < 2) {
            suggestionsPopup.setVisible(false);
            return;
        }
        
        // Annuler le timer existant s'il est en cours
        if (suggestionsTimer != null && suggestionsTimer.isRunning()) {
            suggestionsTimer.stop();
        }
        
        // Créer un nouveau timer pour éviter les requêtes trop fréquentes
        suggestionsTimer = new Timer(300, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Vérifier si le texte est toujours valide
                String currentText = inputField.getText().trim();
                if (currentText.length() < 2) {
                    suggestionsPopup.setVisible(false);
                    return;
                }
                
                // Récupérer les suggestions de la base de données
                List<String> suggestions = mediator.getMovieTitleSuggestions(currentText, MAX_SUGGESTIONS);
                
                if (suggestions.isEmpty()) {
                    suggestionsPopup.setVisible(false);
                    return;
                }
                
                // Vider et reconstruire le popup de suggestions
                suggestionsPopup.removeAll();
                
                // Ajouter chaque suggestion au popup
                for (String suggestion : suggestions) {
                    JMenuItem item = new JMenuItem(suggestion);
                    item.setFont(normalFont);
                    item.setBackground(Color.WHITE);
                    
                    // Mettre en évidence la partie correspondante du texte
                    if (!currentText.isEmpty()) {
                        final String highlightText = currentText;
                        String safeHighlightText = Pattern.quote(highlightText);
                        item.setText("<html>" + suggestion.replaceAll("(?i)(" + safeHighlightText + ")", 
                                "<span style='background-color:#FFFF00;'>$1</span>") + "</html>");
                    }
                    
                    // Action lors de la sélection d'une suggestion
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            inputField.setText(suggestion);
                            suggestionsPopup.setVisible(false);
                            
                            // Donner le focus au champ de texte après sélection
                            inputField.requestFocusInWindow();
                            
                            // Lancer la recherche immédiatement quand une suggestion est sélectionnée
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    searchButton.doClick();
                                }
                            });
                        }
                    });
                    
                    // Effet de survol
                    item.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent evt) {
                            item.setBackground(new Color(240, 240, 240));
                        }
                        
                        @Override
                        public void mouseExited(MouseEvent evt) {
                            item.setBackground(Color.WHITE);
                        }
                    });
                    
                    suggestionsPopup.add(item);
                }
                
                // Positionner et afficher le popup seulement si le composant est visible
                if (inputField.isShowing()) {
                    Rectangle bounds = inputField.getBounds();
                    suggestionsPopup.show(inputField, 0, bounds.height);
                    
                    // Ajuster la taille du popup
                    suggestionsPopup.setPopupSize(inputField.getWidth(), Math.min(suggestions.size() * 35, 350));
                    
                    // IMPORTANT: Forcer le popup à rester visible
                    suggestionsPopup.setVisible(true);
                }
            }
        });
        suggestionsTimer.setRepeats(false);
        suggestionsTimer.start();
    }

    /**
     * Affiche une fenêtre pop-up avec les détails complets d'un film.
     * 
     * @param row L'index de la ligne sélectionnée dans la table
     * @param tableModel Le modèle de table contenant les données
     */
    private static void showMovieDetails(int row, DefaultTableModel tableModel) {
        JDialog detailsDialog = new JDialog(frame, "Détails du film", true);
        detailsDialog.setSize(900, 650);
        detailsDialog.setLocationRelativeTo(frame);

        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(backgroundColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // Titre du film en haut avec un style élégant
        String title = tableModel.getValueAt(row, 0).toString().replaceAll("<[^>]*>", "").trim();
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(primaryColor);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Séparateur stylisé
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(200, 200, 200));
        separator.setBackground(backgroundColor);

        JPanel titlePanel = new JPanel(new BorderLayout(0, 15));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(separator, BorderLayout.SOUTH);

        // Panneau d'informations avec un style de carte
        JPanel infoCard = new RoundedPanel(15, cardColor);
        infoCard.setLayout(new BorderLayout(15, 15));
        infoCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel infoGrid = new JPanel(new GridLayout(0, 2, 15, 15));
        infoGrid.setOpaque(false);

        // Date de sortie
        addDetailRow(infoGrid, "Date de sortie :", tableModel.getValueAt(row, 1));

        // Genre
        addDetailRow(infoGrid, "Genre :", tableModel.getValueAt(row, 2));

        // Distributeur
        addDetailRow(infoGrid, "Distributeur :", tableModel.getValueAt(row, 3));

        // Budget
        addDetailRow(infoGrid, "Budget :", tableModel.getValueAt(row, 4));

        // Revenus USA
        addDetailRow(infoGrid, "Revenus USA :", tableModel.getValueAt(row, 5));

        // Revenus mondiaux
        addDetailRow(infoGrid, "Revenus mondiaux :", tableModel.getValueAt(row, 6));

        // Réalisateurs
        addDetailRow(infoGrid, "Réalisateurs :", tableModel.getValueAt(row, 7));

        // Acteurs
        addDetailRow(infoGrid, "Acteurs :", tableModel.getValueAt(row, 8));

        // Producteurs
        addDetailRow(infoGrid, "Producteurs :", tableModel.getValueAt(row, 9));

        // Ajouter la grille d'informations à la carte
        infoCard.add(infoGrid, BorderLayout.CENTER);

        // Résumé dans une carte séparée
        JPanel summaryCard = new RoundedPanel(15, cardColor);
        summaryCard.setLayout(new BorderLayout(10, 10));
        summaryCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel summaryTitle = new JLabel("Résumé");
        summaryTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        summaryTitle.setForeground(primaryColor);
        summaryTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JTextPane summaryText = new JTextPane();
        summaryText.setContentType("text/html");
        summaryText.setText(tableModel.getValueAt(row, 10).toString());
        summaryText.setEditable(false);
        summaryText.setFont(normalFont);
        summaryText.setBackground(cardColor);
        summaryText.setBorder(null);

        JScrollPane summaryScroll = new JScrollPane(summaryText);
        summaryScroll.setPreferredSize(new Dimension(500, 150));
        summaryScroll.setBorder(null);

        summaryCard.add(summaryTitle, BorderLayout.NORTH);
        summaryCard.add(summaryScroll, BorderLayout.CENTER);

        // Bouton de fermeture avec style amélioré
        JButton closeButton = new RoundedButton("Fermer", secondaryColor);
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeButton.setForeground(Color.WHITE);
        closeButton.setPreferredSize(new Dimension(120, 40));
        closeButton.addActionListener(e -> detailsDialog.dispose());

        // Assemblage des panneaux
        JPanel centerPanel = new JPanel(new BorderLayout(20, 20));
        centerPanel.setOpaque(false);
        centerPanel.add(infoCard, BorderLayout.CENTER);
        centerPanel.add(summaryCard, BorderLayout.SOUTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeButton);

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        detailsDialog.add(mainPanel);
        detailsDialog.setVisible(true);
    }

    /**
     * Ajoute une ligne de détail au panneau d'informations avec style amélioré
     * 
     * @param panel Le panneau où ajouter la ligne
     * @param label Le libellé de la ligne
     * @param value La valeur à afficher
     */
    private static void addDetailRow(JPanel panel, String label, Object value) {
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelComponent.setForeground(primaryColor);

        JTextPane valueComponent = new JTextPane();
        valueComponent.setContentType("text/html");
        valueComponent.setText(value.toString());
        valueComponent.setEditable(false);
        valueComponent.setBackground(cardColor);
        valueComponent.setBorder(null);

        panel.add(labelComponent);
        panel.add(valueComponent);
    }

    /**
     * Classe pour créer un panneau avec coins arrondis
     */
    private static class RoundedPanel extends JPanel {
        private int radius;
        private Color backgroundColor;

        public RoundedPanel(int radius, Color bgColor) {
            super();
            this.radius = radius;
            this.backgroundColor = bgColor;
            setOpaque(false);
            setLayout(new BorderLayout());

            // Ajouter une ombre
            setBorder(new ShadowBorder(10, 0.2f));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g2.dispose();
        }
    }

    /**
     * Classe pour créer un bouton avec coins arrondis
     */
    private static class RoundedButton extends JButton {
        private Color bgColor;
        private Color hoverColor;
        private boolean isHover = false;

        public RoundedButton(String text, Color bgColor) {
            super(text);
            this.bgColor = bgColor;
            this.hoverColor = new Color(
                Math.max((int)(bgColor.getRed() * 0.8), 0),
                Math.max((int)(bgColor.getGreen() * 0.8), 0),
                Math.max((int)(bgColor.getBlue() * 0.8), 0)
            );

            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Dessiner le fond du bouton
            if (isEnabled()) {
                g2.setColor(isHover ? hoverColor : bgColor);
            } else {
                g2.setColor(new Color(180, 180, 180));
            }

            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

            // Ajouter un effet de lumière subtil
            if (isEnabled()) {
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(255, 255, 255, 50),
                    0, getHeight() / 2, new Color(255, 255, 255, 0)
                );
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight() / 2, 10, 10));
            }

            g2.dispose();

            // Dessiner le texte
            super.paintComponent(g);
        }
    }

    /**
     * Classe pour créer une bordure avec ombre
     */
    private static class ShadowBorder extends AbstractBorder {
        private int shadowSize;
        private float shadowOpacity;

        public ShadowBorder(int shadowSize, float shadowOpacity) {
            this.shadowSize = shadowSize;
            this.shadowOpacity = shadowOpacity;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Créer l'effet d'ombre
            for (int i = 0; i < shadowSize; i++) {
                float opacity = shadowOpacity * (shadowSize - i) / shadowSize;
                g2.setColor(new Color(0, 0, 0, (int)(opacity * 255)));

                // Dessiner le fond avec des bords arrondis
                RoundRectangle2D.Float shadow = new RoundRectangle2D.Float(
                    x + i, y + i,
                    width - i * 2, height - i * 2,
                    20, 20
                );
                g2.draw(shadow);
            }

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(shadowSize, shadowSize, shadowSize, shadowSize);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    /**
     * Classe utilitaire qui crée une bordure avec effet d'ombre
     */
    private static class DropShadowBorder extends AbstractBorder {
        private final Color shadowColor;
        private final int shadowSize;
        private final float shadowOpacity;
        private final int shadowSoftness;
        private final boolean showTopShadow;
        private final boolean showLeftShadow;
        private final boolean showBottomShadow;
        private final boolean showRightShadow;

        public DropShadowBorder(Color shadowColor, int shadowSize, float shadowOpacity, 
                                int shadowSoftness, boolean showTopShadow, 
                                boolean showLeftShadow, boolean showBottomShadow, 
                                boolean showRightShadow) {
            this.shadowColor = shadowColor;
            this.shadowSize = shadowSize;
            this.shadowOpacity = shadowOpacity;
            this.shadowSoftness = shadowSoftness;
            this.showTopShadow = showTopShadow;
            this.showLeftShadow = showLeftShadow;
            this.showBottomShadow = showBottomShadow;
            this.showRightShadow = showRightShadow;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Dessiner l'ombre
            int xOffset = (showLeftShadow ? shadowSize : 0);
            int yOffset = (showTopShadow ? shadowSize : 0);
            int shadowWidth = width - (showLeftShadow ? shadowSize : 0) - (showRightShadow ? shadowSize : 0);
            int shadowHeight = height - (showTopShadow ? shadowSize : 0) - (showBottomShadow ? shadowSize : 0);

            for (int i = 0; i < shadowSoftness; i++) {
                float opacity = shadowOpacity / shadowSoftness * (shadowSoftness - i);
                g2.setColor(new Color(shadowColor.getRed(), shadowColor.getGreen(), 
                                      shadowColor.getBlue(), (int)(255 * opacity)));

                // Dessiner les bords de l'ombre
                if (showBottomShadow) {
                    g2.fillRect(xOffset, y + height - shadowSize + i, shadowWidth, 1);
                }
                if (showRightShadow) {
                    g2.fillRect(x + width - shadowSize + i, yOffset, 1, shadowHeight);
                }
                if (showLeftShadow) {
                    g2.fillRect(x + i, yOffset, 1, shadowHeight);
                }
                if (showTopShadow) {
                    g2.fillRect(xOffset, y + i, shadowWidth, 1);
                }
            }

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(
                showTopShadow ? shadowSize : 0,
                showLeftShadow ? shadowSize : 0,
                showBottomShadow ? shadowSize : 0,
                showRightShadow ? shadowSize : 0
            );
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}