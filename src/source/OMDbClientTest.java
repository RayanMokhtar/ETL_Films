package source;

import java.util.ArrayList;

/**
 * Test console pour OMDbClient : affiche les résumés pour différents titres en testant
 * plusieurs variantes (espaces, tirets, suppression de ponctuation).
 */
public class OMDbClientTest {
    public static void main(String[] args) {
        // Titres à tester
        String[] testTitles = { "Inception", "Spider-Man" };
        for (String original : testTitles) {
            System.out.println("\n===== Test OMDb pour '" + original + "' =====");
            // Générer variantes du titre
            ArrayList<String> variants = new ArrayList<>();
            variants.add(original);
            if (original.contains("-")) variants.add(original.replace('-', ' '));
            if (original.contains(" ")) variants.add(original.replace(' ', '-'));
            String stripped = original.replaceAll("[^A-Za-z0-9 ]", " ").trim();
            if (!variants.contains(stripped)) variants.add(stripped);

            String plot = null;
            // Tester chaque variante jusqu'à obtenir un résumé non vide
            for (String var : variants) {
                String fmt = var.replace(' ', '+');
                plot = OMDbClient.getMovieResume(fmt, ""); // année vide pour tester sans filtre
                if (plot != null && !plot.equals("<html>\n<p></p>\n</html>") && !plot.trim().isEmpty()) {
                    System.out.println("Variante utilisée : '" + var + "'");
                    System.out.println(plot);
                    break;
                }
            }
            if (plot == null || plot.equals("<html>\n<p></p>\n</html>") || plot.trim().isEmpty()) {
                System.out.println("Aucun résumé trouvé pour '" + original + "' avec les variantes testées.");
            }
        }
    }
}