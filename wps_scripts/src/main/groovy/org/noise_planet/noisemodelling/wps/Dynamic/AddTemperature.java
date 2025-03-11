package org.noise_planet.noisemodelling.wps.Dynamic;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;
public class AddTemperature {
    public static void main(String[] args) {
        String file24Hour = "./wps_scripts/24_hour.csv";
        String fileTemperature = "./wps_scripts/temperatureGeneve.csv";
        String outputFile = "./target/24_hour_with_temp.csv";

        DateTimeFormatter formatter24Hour = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DateTimeFormatter formatterTemperature = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            // Lire les températures et stocker dans une map
            Map<LocalDateTime, Double> temperatureMap = Files.lines(Paths.get(fileTemperature))
                    .skip(1) // Skip header
                    .map(line -> line.split(","))
                    .collect(Collectors.toMap(
                            parts -> LocalDateTime.parse(parts[0], formatterTemperature),
                            parts -> Double.parseDouble(parts[1])
                    ));

            // Lire et traiter le fichier 24_hour.csv
            List<String> outputLines = new ArrayList<>();
            outputLines.add("deveui,epoch,Leq,timestamp,The_GEOM,temperature"); // Nouvelle en-tête

            Files.lines(Paths.get(file24Hour)).skip(1).forEach(line -> {
                String[] parts = line.split(",");
                LocalDateTime timestamp = LocalDateTime.parse(parts[3], formatter24Hour);

                // Trouver la température de l'heure la plus proche
                LocalDateTime closestTime = temperatureMap.keySet().stream()
                        .min(Comparator.comparing(t -> Duration.between(t, timestamp).abs()))
                        .orElse(null);

                double temperature = (closestTime != null) ? temperatureMap.get(closestTime) : Double.NaN;
                outputLines.add(line + "," + temperature);
            });

            // Sauvegarder le fichier avec la nouvelle colonne
            Files.write(Paths.get(outputFile), outputLines);
            System.out.println("Fichier généré avec succès : " + outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
