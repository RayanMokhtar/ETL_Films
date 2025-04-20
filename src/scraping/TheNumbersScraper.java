package scraping;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class realizes a web scraping operation applied to the site "the-numbers.com".
 *
 * */
public class TheNumbersScraper {
	public static void main(String[] args) {
		// Liste des genres
		String[] genres = {"Adventure", "Comedy", "Drama", "Action", "Thriller-or-Suspense", "Romantic-Comedy"};
		try {
			FileWriter writer = new FileWriter("./films.csv"); // Créer un fichier CSV pour tous les films
			writer.append("Genre; Année; Titre; Distributeur\n"); // En-tête du fichier CSV
			// Boucle sur chaque genre
			for (String genre : genres) {
				// Boucle sur les années de 2000 à 2015
				for (int year = 2000; year <= 2015; year++) {
					String url = "http://www.the-numbers.com/market/" + year + "/genre/" + genre;
					Document doc = Jsoup.connect(url).get(); // Récupérer la page HTML
					Elements movies = doc.select("table tr"); // Sélectionner les lignes de la table contenant les films
					ArrayList<String> distributors_unformatted = new ArrayList<String>();
					HashMap<String, String> distributorsEquiv = new HashMap<String, String>();
					for (Element movie : movies) {
						Elements cells = movie.select("td"); // Sélectionner les cellules de la ligne
						if (cells.size() >= 4 && !cells.get(0).text().contains("← ")) {
							String title = cells.get(1).text(); // Titre du film
							if (title.endsWith("…")) {
								// On récupère l'adresse de la page de description du film
								String filmUrl = "http://www.the-numbers.com" + cells.get(1).child(0).child(0).attribute("href").getValue();

								Document docFilm = Jsoup.connect(filmUrl).get();

								Elements h1s = docFilm.select("h1");
								title = h1s.get(0).text().split("\\(")[0].trim();
							}


							String distributor = cells.get(3).text(); // Distributeur du film

							if (distributor.endsWith("…")){
								// On récupère l'adresse de la page de description du film
								String distributorUrl = "http://www.the-numbers.com" + cells.get(3).child(0).attribute("href").getValue();
								if(!distributors_unformatted.contains(distributor)) {
									// Récupération de la page HTML
									distributors_unformatted.add(distributor);
									try {

										Document docDistributor = Jsoup.connect(distributorUrl).get();

										Elements h1s = docDistributor.select("h1");
										distributorsEquiv.putIfAbsent(distributor, h1s.get(0).text());
										distributor = h1s.get(0).text();
									} catch (HttpStatusException e) {
										System.err.println("The link " + distributorUrl + " doesn't exist");
										e.printStackTrace();
									}
								}
								else {
									distributor = distributorsEquiv.get(distributor);
								}
							}
							// Écrire les informations dans le fichier CSV
							writer.append(genre + ";" + year + ";" + title  + ";" + distributor + "\n");
						}
					}
				}
			}
			writer.flush();
			writer.close();
			System.out.println("Fichier films.csv créé avec succès.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}