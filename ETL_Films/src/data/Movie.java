package data;

import java.sql.Date;
import java.util.ArrayList;

/**
 * Represents a movie with details such as title, release date, genre,
 * distributor, financial data, involved personnel, and a summary.
 */
public class Movie {

    private String title;

    private Date releaseDate;

    private String genre;

    private String distributor;

    private double budget;

    private double usaRevenue;

    private double worldwideRevenue;

    private ArrayList<String> directors = new ArrayList<String>();

    private ArrayList<String> actors = new ArrayList<String>();

    private ArrayList<String> producers = new ArrayList<String>();

    private String summary;

    public Movie(){
    }

    public String getTitle() {
        return title;
    }

    /**
     * Gets the movie title formatted in HTML.
     * @return HTML formatted title.
     */
    public String getStringTitle(){
        return "<html>\n<p>" + title + "</p>\n</html>";
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    /**
     * Gets the release date of the movie formatted in HTML.
     * @return HTML formatted release date.
     */
    public String getStringReleaseDate(){
        return "<html>\n<p>" + releaseDate + "</p>\n</html>";
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getGenre() {
        return genre;
    }

    /**
     * Gets the genre of the movie formatted in HTML.
     * @return HTML formatted genre.
     */
    public String getStringGenre(){
        return "<html>\n<p>" + genre + "</p>\n</html>";
    }
    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDistributor() {
        return distributor;
    }

    /**
     * Gets the distributor of the movie formatted in HTML.
     * @return HTML formatted distributor.
     */
    public String getStringDistributor(){
        return "<html>\n<p>" + distributor + "</p>\n</html>";
    }

    public void setDistributor(String distributor) {
        this.distributor = distributor;
    }

    public double getBudget() {
        return budget;
    }

    /**
     * Gets the budget of the movie formatted in HTML.
     * @return HTML formatted budget.
     */
    public String getStringBudget(){
        return "<html>\n<p>" + String.format("%.0f", budget) + "$</p>\n</html>";
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public double getUsaRevenue() {
        return usaRevenue;
    }

    /**
     * Gets the USA revenue of the movie formatted in HTML.
     * @return HTML formatted USA revenue.
     */
    public String getStringUsaRevenue(){
        return "<html>\n<p>" + String.format("%.0f", usaRevenue) + "$</p>\n</html>";
    }

    public void setUsaRevenue(double usaRevenue) {
        this.usaRevenue = usaRevenue;
    }
    public double getWorldwideRevenue() {
        return worldwideRevenue;
    }


    /**
     * Gets the worldwide revenue of the movie formatted in HTML.
     * @return HTML formatted worldwide revenue.
     */
    public String getStringWorldwideRevenue(){
        return "<html>\n<p>" + String.format("%.0f", worldwideRevenue) + "$</p>\n</html>";
    }
    public void setWorldwideRevenue(double worldwideRevenue) {
        this.worldwideRevenue = worldwideRevenue;
    }

    public ArrayList<String> getDirectors() {
        return directors;
    }

    /**
     * Gets the directors of the movie formatted in HTML.
     * @return HTML formatted list of directors.
     */
    public String getStringHTMLDirectors(){
        if(!this.directors.isEmpty()){

            // Convertir l'ArrayList en une chaîne de caractères
            StringBuilder sb = new StringBuilder();
            sb.append("<html>\n");
            for (String element : this.directors) {
                sb.append("<p>\n").append(element).append("</p>\n");
            }
            // Supprimer la virgule et l'espace supplémentaires à la fin
            sb.substring(0, sb.length() - 2);
            sb.append("</html>\n");
            String arrayAsString = sb.toString();
            return arrayAsString;
        }
        return "";
    }
    public void setDirectors(ArrayList<String> directors) {
        this.directors = directors;
    }

    public void addDirectors(ArrayList<Object> directors) {
        for (Object director : directors){
            String directorName = ((String)director).split("\\(")[0].trim();
            if (!this.directors.contains(directorName)){
                this.directors.add(directorName);
            }
        }
    }
    public ArrayList<String> getActors() {
        return actors;
    }

    /**
     * Gets the actors of the movie formatted in HTML.
     * @return HTML formatted list of actors.
     */
    public String getStringHTMLActors(){
        if(!this.directors.isEmpty()){

            // Convertir l'ArrayList en une chaîne de caractères
            StringBuilder sb = new StringBuilder();
            sb.append("<html>\n");
            for (String element : this.actors) {
                sb.append("<p>\n").append(element).append("</p>\n");
            }
            // Supprimer la virgule et l'espace supplémentaires à la fin
            sb.substring(0, sb.length() - 2);
            sb.append("</html>\n");
            String arrayAsString = sb.toString();
            return arrayAsString;
        }
        return "";
    }

    public void setActors(ArrayList<String> actors) {
        this.actors = actors;
    }
    public void addActors(ArrayList<Object> actors) {
        for (Object actor : actors){
            String actorName = ((String)actor).split("\\(")[0].trim();
            if (!this.actors.contains(actorName)){
                this.actors.add(actorName);
            }
        }
    }

    public ArrayList<String> getProducers() {
        return producers;
    }

    /**
     * Gets the producers of the movie formatted in HTML.
     * @return HTML formatted list of producers.
     */
    public String getStringHTMLProducers(){
        if(!this.directors.isEmpty()){

            // Convertir l'ArrayList en une chaîne de caractères
            StringBuilder sb = new StringBuilder();
            sb.append("<html>\n");
            for (String element : this.producers) {
                sb.append("<p>\n").append(element).append("</p>\n");
            }


            // Supprimer la virgule et l'espace supplémentaires à la fin
            sb.substring(0, sb.length() - 2);
            sb.append("</html>\n");
            String arrayAsString = sb.toString();
            return arrayAsString;
        }
        return "";
    }
    public void setProducers(ArrayList<String> producers) {
        this.producers = producers;
    }
    public void addProducers(ArrayList<Object> producers) {
        for (Object producer : producers){
            String producerName = ((String)producer).split("\\(")[0].trim();
            if (!this.producers.contains(producerName)){
                this.producers.add(producerName);
            }
        }
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Override
    public String toString() {
        return String.format(
                        "+----------------------------------------+----------------------------------------------------+\n" +
                        "| Title                                  | %50s |\n" +
                        "+----------------------------------------+----------------------------------------------------+\n" +
                        "| Release Date                           | %50s |\n" +
                        "+----------------------------------------+----------------------------------------------------+\n" +
                        "| Genre                                  | %50s |\n" +
                        "+----------------------------------------+----------------------------------------------------+\n" +
                        "| Distributor                            | %50s |\n" +
                        "+----------------------------------------+----------------------------------------------------+\n" +
                        "| Budget                                 | %50.2f |\n" +
                        "+----------------------------------------+----------------------------------------------------+\n" +
                        "| USA Revenue                            | %50.2f |\n" +
                        "+----------------------------------------+----------------------------------------------------+\n" +
                        "| Worldwide Revenue                      | %50.2f |\n" +
                        "+----------------------------------------+----------------------------------------------------+\n" +
                        "| Directors                              | %50s |\n" +
                        "+----------------------------------------+----------------------------------------------------+\n" +
                        "| Actors                                 | %50s |\n" +
                        "+----------------------------------------+----------------------------------------------------+\n" +
                        "| Producers                              | %50s |\n" +
                        "+----------------------------------------+----------------------------------------------------+\n" +
                        "| Summary                                | %50s |\n" +
                        "+----------------------------------------+----------------------------------------------------+",
                title, releaseDate, genre, distributor, budget, usaRevenue, worldwideRevenue,
                String.join(", ", directors), String.join(", ", actors),
                String.join(", ", producers), summary);
    }

}
