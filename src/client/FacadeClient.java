package client;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.regex.Pattern;
import core.Movie;
import repository.Mediator;

public class FacadeClient {
    private static Mediator mediator = Mediator.getInstance();
    private static Color primaryColor = new Color(25, 42, 86);
    private static Color secondaryColor = new Color(41, 128, 185);
    private static Color accentColor = new Color(231, 76, 60);
    private static Color successColor = new Color(39, 174, 96);
    private static Color backgroundColor = new Color(240, 243, 247);
    private static Color textColor = new Color(44, 62, 80);
    private static Color headerBgColor = new Color(32, 47, 90);
    private static Color cardColor = Color.WHITE;

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

    private interface TimerActionListener extends ActionListener {
        void startSearching();
        void finishSearching();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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

        SwingUtilities.invokeLater(FacadeClient::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        frame = new JFrame("Cinémathèque - Recherche de Films");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 800);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(backgroundColor);
        frame.add(mainPanel);

        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, primaryColor, 0, getHeight(), headerBgColor);
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

        // Panneau de recherche
        JPanel searchPanel = new RoundedPanel(20, cardColor);
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel searchControlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        searchControlsPanel.setOpaque(false);
        JLabel searchLabel = new JLabel("Recherche par :");
        searchLabel.setFont(normalFont);
        searchLabel.setForeground(textColor);
        JComboBox<String> searchOptions = new JComboBox<>(new String[]{"Titre de film", "Nom d'acteur"});
        searchOptions.setFont(normalFont);
        searchOptions.setPreferredSize(new Dimension(150, 40));
        searchOptions.setBackground(Color.WHITE);
        searchOptions.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        inputField = new JTextField();
        inputField.setFont(normalFont);
        inputField.setPreferredSize(new Dimension(350, 40));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        suggestionsPopup = new JPopupMenu();
        suggestionsPopup.setFocusable(false);
        suggestionsPopup.setBackground(Color.WHITE);
        suggestionsPopup.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateSuggestions(); }
            @Override public void removeUpdate(DocumentEvent e) { updateSuggestions(); }
            @Override public void changedUpdate(DocumentEvent e) { updateSuggestions(); }
        });
        inputField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                if (e.getOppositeComponent()!=null && SwingUtilities.isDescendingFrom(e.getOppositeComponent(), suggestionsPopup)) return;
                suggestionsPopup.setVisible(false);
            }
            @Override public void focusGained(FocusEvent e) {
                if (inputField.getText().length()>=2) updateSuggestions();
            }
        });
        searchButton = new RoundedButton("Rechercher", secondaryColor);
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchButton.setForeground(Color.WHITE);
        searchButton.setPreferredSize(new Dimension(150, 40));

        JPanel statusPanel = new JPanel(new BorderLayout(0,10)); statusPanel.setOpaque(false);
        JLabel statusLabel = new JLabel(""); statusLabel.setFont(smallFont); statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        progressBar = new JProgressBar();
        progressBar.setVisible(false); progressBar.setPreferredSize(new Dimension(400,8));
        progressBar.setBackground(new Color(230,230,230)); progressBar.setForeground(secondaryColor);
        progressBar.setUI(new BasicProgressBarUI(){
            @Override protected void paintDeterminate(Graphics g,JComponent c){
                Graphics2D g2d=(Graphics2D)g; g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int w=c.getWidth(),h=c.getHeight(); g2d.setColor(progressBar.getBackground());
                g2d.fillRoundRect(0,0,w,h,h,h); if(progressBar.getValue()>0){
                    int x=(int)(progressBar.getPercentComplete()*w);
                    g2d.setColor(progressBar.getForeground());
                    g2d.fillRoundRect(0,0,x,h,h,h);
                }
            }
        });
        statusPanel.add(statusLabel,BorderLayout.NORTH); statusPanel.add(progressBar,BorderLayout.CENTER);
        searchControlsPanel.add(searchLabel); searchControlsPanel.add(searchOptions); searchControlsPanel.add(inputField); searchControlsPanel.add(searchButton);
        searchPanel.add(searchControlsPanel); searchPanel.add(Box.createVerticalStrut(15)); searchPanel.add(statusPanel);

        // Initialisation du Timer pour animer la barre de progression
        progressTimer = new Timer(50, new TimerActionListener() {
            private int currentValue = 0;
            private boolean isSearching = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isSearching) {
                    currentValue = Math.min(currentValue + 1, 95);
                    progressBar.setValue(currentValue);
                } else {
                    currentValue = Math.min(currentValue + 5, 100);
                    progressBar.setValue(currentValue);

                    if (currentValue >= 100) {
                        progressTimer.stop();
                        new Timer(500, evt -> {
                            progressBar.setVisible(false);
                            ((Timer) evt.getSource()).stop();
                        }).start();
                    }
                }
            }

            @Override
            public void startSearching() {
                isSearching = true;
                currentValue = 0;
                progressBar.setValue(0);
                progressBar.setVisible(true);
                progressTimer.start();
            }

            @Override
            public void finishSearching() {
                isSearching = false;
            }
        });

        JPanel resultsPanel = new JPanel(new BorderLayout(0,10)); resultsPanel.setBackground(backgroundColor);
        JLabel resultsTitle = new JLabel("Résultats de recherche"); resultsTitle.setFont(new Font("Segoe UI",Font.BOLD,18)); resultsTitle.setForeground(primaryColor);
        DefaultTableModel tableModel = new DefaultTableModel(){@Override public boolean isCellEditable(int r,int c){return false;}};
        tableModel.addColumn("Titre");tableModel.addColumn("Date de sortie");tableModel.addColumn("Genre");tableModel.addColumn("Distributeur");tableModel.addColumn("Budget");
        tableModel.addColumn("Revenus USA");tableModel.addColumn("Revenus mondiaux");tableModel.addColumn("Réalisateurs");tableModel.addColumn("Acteurs");tableModel.addColumn("Producteurs");tableModel.addColumn("Résumé");
        JTable resultTable=new JTable(tableModel);resultTable.setRowHeight(120);resultTable.setFont(smallFont);resultTable.setRowMargin(6);
        resultTable.setIntercellSpacing(new Dimension(10,5));resultTable.setShowGrid(true);resultTable.setGridColor(new Color(220,225,230));
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.setSelectionBackground(new Color(232,242,254));resultTable.setSelectionForeground(textColor);
        JTableHeader header = resultTable.getTableHeader();header.setFont(headerFont);header.setBackground(headerBgColor);header.setForeground(Color.WHITE);
        header.setDefaultRenderer(new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable t,Object v,boolean isSel,boolean hasF,int r,int c){
            JLabel lbl=(JLabel)super.getTableCellRendererComponent(t,v,isSel,hasF,r,c);
            lbl.setFont(headerFont);lbl.setBackground(headerBgColor);lbl.setForeground(Color.WHITE);lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));return lbl;}});
        DefaultTableCellRenderer cellRenderer=new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable t,Object v,boolean isSel,boolean hasF,int r,int c){
            JLabel lbl=(JLabel)super.getTableCellRendererComponent(t,v,isSel,hasF,r,c);lbl.setVerticalAlignment(SwingConstants.TOP);
            lbl.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,1,new Color(220,225,230)),BorderFactory.createEmptyBorder(10,10,10,10)));
            if(!isSel) lbl.setBackground(r%2==0?Color.WHITE:new Color(248,250,252));return lbl;}};
        resultTable.setDefaultRenderer(Object.class,cellRenderer);
        resultTable.addMouseMotionListener(new MouseMotionAdapter(){@Override public void mouseMoved(MouseEvent e){
            int r=resultTable.rowAtPoint(e.getPoint()),c=resultTable.columnAtPoint(e.getPoint());
            if(r>=0&&c>=0){Object val=resultTable.getValueAt(r,c);if(val!=null){String t=val.toString();if(t.startsWith("<html>"))t=t.replaceAll("<[^>]*>","").trim();resultTable.setToolTipText(t);}else resultTable.setToolTipText(null);} }});
        resultTable.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2){int r=resultTable.getSelectedRow(); if(r>=0) showMovieDetails(r,tableModel);}}});
        JScrollPane scrollPane=new JScrollPane(resultTable);scrollPane.getViewport().setBackground(backgroundColor);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(new DropShadowBorder(new Color(0,0,0,100),5,0.2f,10,false,true,true,true),BorderFactory.createEmptyBorder()));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        resultsPanel.add(resultsTitle,BorderLayout.NORTH);resultsPanel.add(scrollPane,BorderLayout.CENTER);

        JPanel contentPanel=new JPanel(new BorderLayout(0,20));contentPanel.setBackground(backgroundColor);contentPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        contentPanel.add(searchPanel,BorderLayout.NORTH);contentPanel.add(resultsPanel,BorderLayout.CENTER);
        mainPanel.add(headerPanel,BorderLayout.NORTH);mainPanel.add(contentPanel,BorderLayout.CENTER);

        TimerActionListener progressController=(TimerActionListener)progressTimer.getActionListeners()[0];
        searchButton.addActionListener(new ActionListener(){@Override public void actionPerformed(ActionEvent e){
            String search=inputField.getText().trim();if(search.isEmpty()){JOptionPane.showMessageDialog(frame,"Veuillez entrer un terme de recherche.","Champ requis",JOptionPane.WARNING_MESSAGE);return;} 
            suggestionsPopup.setVisible(false);tableModel.setRowCount(0);
            resultsTitle.setText("Recherche en cours pour : \""+search+"\"");statusLabel.setText("Recherche en cours...");statusLabel.setForeground(secondaryColor);searchButton.setEnabled(false);
            ((TimerActionListener)progressController).startSearching();
            new SwingWorker<List<Movie>,Void>(){@Override protected List<Movie> doInBackground() throws Exception {boolean cs=false; return searchOptions.getSelectedIndex()==0?mediator.getMoviesByTitle(search,cs):mediator.getMoviesByActorName(search,cs);} 
            @Override protected void done(){try {List<Movie> movies=get();((TimerActionListener)progressController).finishSearching();if(movies.isEmpty()){resultsTitle.setText("Aucun résultat pour : \""+search+"\"");statusLabel.setText("Aucun résultat trouvé.");statusLabel.setForeground(accentColor);JOptionPane.showMessageDialog(frame,"Aucun résultat trouvé pour \""+search+"\".","Recherche terminée",JOptionPane.INFORMATION_MESSAGE);} else{
                for (Movie m : movies) {
                    // Remplacer attributs vides par "non mentionné"
                    String title = m.getStringTitle();
                    String date = m.getStringReleaseDate();
                    String genre = m.getStringGenre();
                    String distributor = m.getStringDistributor();
                    String budget = m.getStringBudget();
                    String usa = m.getStringUsaRevenue();
                    String worldwide = m.getStringWorldwideRevenue();
                    String directors = m.getStringHTMLDirectors();
                    String actors = m.getStringHTMLActors();
                    String producers = m.getStringHTMLProducers();
                    String summary = m.getSummary();
                    // setter défaut
                    String def = "non mentionné";
                    if (title == null || title.trim().isEmpty()) title = def;
                    if (date == null || date.trim().isEmpty()) date = def;
                    if (genre == null || genre.trim().isEmpty()) genre = def;
                    if (distributor == null || distributor.trim().isEmpty()) distributor = def;
                    if (budget == null || budget.trim().isEmpty()) budget = def;
                    if (usa == null || usa.trim().isEmpty()) usa = def;
                    if (worldwide == null || worldwide.trim().isEmpty()) worldwide = def;
                    if (directors == null || directors.trim().isEmpty()) directors = def;
                    if (actors == null || actors.trim().isEmpty()) actors = def;
                    if (producers == null || producers.trim().isEmpty()) producers = def;
                    if (summary == null || summary.trim().isEmpty()) summary = def;
                    tableModel.addRow(new Object[]{title, date, genre, distributor, budget, usa, worldwide, directors, actors, producers, summary});
                }
                resultsTitle.setText(movies.size()+" film(s) trouvé(s) pour : \""+search+"\"");statusLabel.setText("Recherche terminée avec succès. Double-cliquez sur un film pour plus de détails.");statusLabel.setForeground(successColor);} } catch(Exception ex){((TimerActionListener)progressController).finishSearching();resultsTitle.setText("Erreur de recherche");statusLabel.setText("Erreur lors de la recherche: "+ex.getMessage());statusLabel.setForeground(accentColor);ex.printStackTrace();JOptionPane.showMessageDialog(frame,"Une erreur est survenue : "+ex.getMessage(),"Erreur",JOptionPane.ERROR_MESSAGE);}finally{searchButton.setEnabled(true);} }}.execute(); }});
        inputField.addKeyListener(new KeyAdapter(){@Override public void keyPressed(KeyEvent e){if(e.getKeyCode()==KeyEvent.VK_ENTER&&searchButton.isEnabled()){searchButton.doClick();}}});
        frame.setVisible(true);
    }

    private static void updateSuggestions() {
        String text = inputField.getText().trim();
        if (text.length() < 2) { suggestionsPopup.setVisible(false); return; }
        if (suggestionsTimer!=null&&suggestionsTimer.isRunning()) suggestionsTimer.stop();
        suggestionsTimer = new Timer(300, e -> {
            String currentText = inputField.getText().trim(); if (currentText.length()<2) {suggestionsPopup.setVisible(false); return;}
            List<String> suggestions = mediator.getMovieTitleSuggestions(currentText, MAX_SUGGESTIONS);
            if (suggestions.isEmpty()) {suggestionsPopup.setVisible(false); return;}
            suggestionsPopup.removeAll();
            for (String suggestion : suggestions) {
                JMenuItem item = new JMenuItem(suggestion);
                item.setFont(normalFont); item.setBackground(Color.WHITE);
                if (!currentText.isEmpty()) {
                    String safe = Pattern.quote(currentText);
                    item.setText("<html>" + suggestion.replaceAll("(?i)(" + safe + ")","<span style='background-color:#FFFF00;'>$1</span>") + "</html>");
                }
                item.addActionListener(evt -> { inputField.setText(suggestion); suggestionsPopup.setVisible(false); inputField.requestFocusInWindow(); SwingUtilities.invokeLater(() -> searchButton.doClick()); });
                item.addMouseListener(new MouseAdapter(){@Override public void mouseEntered(MouseEvent e){item.setBackground(new Color(240,240,240));} @Override public void mouseExited(MouseEvent e){item.setBackground(Color.WHITE);} });
                suggestionsPopup.add(item);
            }
            if (inputField.isShowing()) {
                Rectangle bounds = inputField.getBounds();
                suggestionsPopup.show(inputField, 0, bounds.height);
                suggestionsPopup.setPopupSize(inputField.getWidth(), Math.min(suggestions.size()*35,350));
                suggestionsPopup.setVisible(true);
            }
        });
        suggestionsTimer.setRepeats(false);
        suggestionsTimer.start();
    }

    private static void showMovieDetails(int row, DefaultTableModel tableModel) {
        JDialog detailsDialog = new JDialog(frame, "Détails du film", true);
        detailsDialog.setSize(900, 650);
        detailsDialog.setLocationRelativeTo(frame);
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(backgroundColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        String title = tableModel.getValueAt(row, 0).toString().replaceAll("<[^>]*>","").trim();
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(primaryColor);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel titlePanel = new JPanel(new BorderLayout(0, 15)); titlePanel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        JSeparator separator = new JSeparator(); separator.setForeground(new Color(200,200,200)); separator.setBackground(backgroundColor);
        titlePanel.add(separator, BorderLayout.SOUTH);
        JPanel infoCard = new RoundedPanel(15, cardColor); infoCard.setLayout(new BorderLayout(15,15)); infoCard.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        JPanel infoGrid = new JPanel(new GridLayout(0,2,15,15)); infoGrid.setOpaque(false);
        addDetailRow(infoGrid,"Date de sortie :",tableModel.getValueAt(row,1));
        addDetailRow(infoGrid,"Genre :",tableModel.getValueAt(row,2));
        addDetailRow(infoGrid,"Distributeur :",tableModel.getValueAt(row,3));
        addDetailRow(infoGrid,"Budget :",tableModel.getValueAt(row,4));
        addDetailRow(infoGrid,"Revenus USA :",tableModel.getValueAt(row,5));
        addDetailRow(infoGrid,"Revenus mondiaux :",tableModel.getValueAt(row,6));
        addDetailRow(infoGrid,"Réalisateurs :",tableModel.getValueAt(row,7));
        addDetailRow(infoGrid,"Acteurs :",tableModel.getValueAt(row,8));
        addDetailRow(infoGrid,"Producteurs :",tableModel.getValueAt(row,9));
        infoCard.add(infoGrid,BorderLayout.CENTER);
        JPanel summaryCard = new RoundedPanel(15,cardColor); summaryCard.setLayout(new BorderLayout(10,10)); summaryCard.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        JLabel summaryTitle = new JLabel("Résumé"); summaryTitle.setFont(new Font("Segoe UI",Font.BOLD,18)); summaryTitle.setForeground(primaryColor); summaryTitle.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
        JTextPane summaryText=new JTextPane(); summaryText.setContentType("text/html"); summaryText.setText(tableModel.getValueAt(row,10).toString()); summaryText.setEditable(false); summaryText.setFont(normalFont); summaryText.setBackground(cardColor); summaryText.setBorder(null);
        JScrollPane summaryScroll=new JScrollPane(summaryText); summaryScroll.setPreferredSize(new Dimension(500,150)); summaryScroll.setBorder(null);
        summaryCard.add(summaryTitle,BorderLayout.NORTH); summaryCard.add(summaryScroll,BorderLayout.CENTER);
        JButton closeButton=new RoundedButton("Fermer",secondaryColor); closeButton.setFont(new Font("Segoe UI",Font.BOLD,14)); closeButton.setForeground(Color.WHITE); closeButton.setPreferredSize(new Dimension(120,40)); closeButton.addActionListener(e->detailsDialog.dispose());
        JPanel centerPanel=new JPanel(new BorderLayout(20,20)); centerPanel.setOpaque(false); centerPanel.add(infoCard,BorderLayout.CENTER); centerPanel.add(summaryCard,BorderLayout.SOUTH);
        JPanel buttonPanel=new JPanel(new FlowLayout(FlowLayout.CENTER)); buttonPanel.setOpaque(false); buttonPanel.add(closeButton);
        mainPanel.add(titlePanel,BorderLayout.NORTH); mainPanel.add(centerPanel,BorderLayout.CENTER); mainPanel.add(buttonPanel,BorderLayout.SOUTH);
        detailsDialog.add(mainPanel); detailsDialog.setVisible(true);
    }

    private static void addDetailRow(JPanel panel, String label, Object value) {
        JLabel lbl=new JLabel(label); lbl.setFont(new Font("Segoe UI",Font.BOLD,14)); lbl.setForeground(primaryColor);
        JTextPane valComp=new JTextPane(); valComp.setContentType("text/html"); valComp.setText(value.toString()); valComp.setEditable(false); valComp.setBackground(cardColor); valComp.setBorder(null);
        panel.add(lbl); panel.add(valComp);
    }

    private static class RoundedPanel extends JPanel {
        private int radius; private Color bgColor;
        public RoundedPanel(int radius, Color bgColor) { super(); this.radius=radius; this.bgColor=bgColor; setOpaque(false); setLayout(new BorderLayout()); setBorder(new ShadowBorder(10,0.2f)); }
        @Override protected void paintComponent(Graphics g) { super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(bgColor); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),radius,radius)); g2.dispose(); }
    }

    private static class RoundedButton extends JButton {
        private Color bgColor, hoverColor; private boolean isHover=false;
        public RoundedButton(String text, Color bg) { super(text); this.bgColor=bg; hoverColor=new Color(Math.max(bg.getRed()*8/10,0),Math.max(bg.getGreen()*8/10,0),Math.max(bg.getBlue()*8/10,0)); setOpaque(false); setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setCursor(new Cursor(Cursor.HAND_CURSOR)); addMouseListener(new MouseAdapter(){@Override public void mouseEntered(MouseEvent e){isHover=true; repaint();} @Override public void mouseExited(MouseEvent e){isHover=false; repaint();}}); }
        @Override protected void paintComponent(Graphics g){ Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(isEnabled()?isHover?hoverColor:bgColor:new Color(180,180,180)); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),10,10)); if(isEnabled()){ GradientPaint gp=new GradientPaint(0,0,new Color(255,255,255,50),0,getHeight()/2,new Color(255,255,255,0)); g2.setPaint(gp); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight()/2,10,10)); } g2.dispose(); super.paintComponent(g); }
    }

    private static class ShadowBorder extends AbstractBorder {
        private final int size; private final float opacity;
        public ShadowBorder(int size, float opacity){ this.size=size; this.opacity=opacity; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h){ Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); for(int i=0;i<size;i++){ float op=opacity*(size-i)/size; g2.setColor(new Color(0,0,0,(int)(op*255))); g2.draw(new RoundRectangle2D.Float(x+i,y+i,w-i*2,h-i*2,20,20)); } g2.dispose(); }
        @Override public Insets getBorderInsets(Component c){ return new Insets(size,size,size,size); }
        @Override public boolean isBorderOpaque(){ return false; }
    }

    private static class DropShadowBorder extends AbstractBorder {
        private final Color color; private final int size; private final float opacity; private final int softness;
        private final boolean top,left,bottom,right;
        public DropShadowBorder(Color color,int size,float opacity,int softness,boolean top,boolean left,boolean bottom,boolean right){
            this.color=color;this.size=size;this.opacity=opacity;this.softness=softness;this.top=top;this.left=left;this.bottom=bottom;this.right=right;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h){ Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int xOff=left?size:0, yOff=top?size:0, sw=w-(left?size:0)-(right?size:0), sh=h-(top?size:0)-(bottom?size:0);
            for(int i=0;i<softness;i++){ float op=opacity/softness*(softness-i); g2.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(), (int)(255*op)));
                if(bottom) g2.fillRect(xOff,y+h-size+i,sw,1);
                if(right) g2.fillRect(x+w-size+i,yOff,1,sh);
                if(left) g2.fillRect(x+i,yOff,1,sh);
                if(top) g2.fillRect(xOff,y+i,sw,1);
            }
            g2.dispose(); }
        @Override public Insets getBorderInsets(Component c){ return new Insets(top?size:0,left?size:0,bottom?size:0,right?size:0); }
        @Override public boolean isBorderOpaque(){ return false; }
    }
}