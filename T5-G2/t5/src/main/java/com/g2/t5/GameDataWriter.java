package com.g2.t5;

import org.apache.commons.csv.*;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.*;
import org.json.*;

import java.io.*;
import java.nio.file.*;

import com.g2.Model.Game;

public class GameDataWriter {
    private final HttpClient httpClient = HttpClientBuilder.create().build();

    private static String CSV_FILE_PATH = "AUTName/StudentLogin/";
    private static String CSV_FILE_NAME = "/GameData.csv";

    /*
     * public long getGameId() {
     * long gameId = -1;
     * 
     * try {
     * Reader reader = new FileReader(CSV_FILE_PATH);
     * CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
     * 
     * List<CSVRecord> records = csvParser.getRecords();
     * 
     * if (records.size() > 1) {
     * CSVRecord lastRecord = records.get(records.size() - 1);
     * gameId = Long.parseLong(lastRecord.get(0));
     * }
     * 
     * csvParser.close();
     * reader.close();
     * } catch (IOException e) {
     * System.out.println("Errore durante la lettura del file CSV");
     * e.printStackTrace();
     * }
     * 
     * return gameId;
     * }
     */

    // MODIFICATA A3-T4
    public JSONObject saveGame(Game game) {
        // TODO: modificare createRequest e POST in T4 aggiungendo nuovi campi

        try {
            JSONObject obj = new JSONObject();

            obj.put("class", game.getTestedClass());
            obj.put("difficulty", game.getDifficulty());
            obj.put("name", game.getName());

            JSONArray playersArray = new JSONArray();
            playersArray.put(String.valueOf(game.getPlayerId()));

            obj.put("players", playersArray);

            HttpPost httpPost = new HttpPost("http://t4-g18-app-1:3000/games");
            StringEntity jsonEntity = new StringEntity(obj.toString(), ContentType.APPLICATION_JSON);

            httpPost.setEntity(jsonEntity);

            HttpResponse httpResponse = httpClient.execute(httpPost);
            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode > 299) {
                System.err.println(EntityUtils.toString(httpResponse.getEntity()));
                return null;
            }

            HttpEntity responseEntity = httpResponse.getEntity();
            String responseBody = EntityUtils.toString(responseEntity);
            JSONObject responseObj = new JSONObject(responseBody);

            Long gameID = responseObj.getLong("id");

            JSONObject round = new JSONObject();
            round.put("gameId", gameID);
            round.put("testClassId", game.getTestedClass());

            httpPost = new HttpPost("http://t4-g18-app-1:3000/rounds");
            jsonEntity = new StringEntity(round.toString(), ContentType.APPLICATION_JSON);

            httpPost.setEntity(jsonEntity);

            httpResponse = httpClient.execute(httpPost);
            statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode > 299) {
                System.err.println(EntityUtils.toString(httpResponse.getEntity()));
                return null;
            }

            responseEntity = httpResponse.getEntity();
            responseBody = EntityUtils.toString(responseEntity);
            responseObj = new JSONObject(responseBody);

            // salvo il round id che l'Api mi restituisce
            Integer roundID = responseObj.getInt("id");

            JSONObject turn = new JSONObject();

            turn.put("players", playersArray);
            turn.put("roundId", roundID);
            turn.put("id", 1);

            httpPost = new HttpPost("http://t4-g18-app-1:3000/turns");
            jsonEntity = new StringEntity(turn.toString(), ContentType.APPLICATION_JSON);

            httpPost.setEntity(jsonEntity);

            httpResponse = httpClient.execute(httpPost);
            statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode > 299) {
                System.err.println(EntityUtils.toString(httpResponse.getEntity()));
                return null;
            }

            /*
             * responseEntity = httpResponse.getEntity();
             * responseBody = EntityUtils.toString(responseEntity);
             * 
             * JSONArray responseArrayObj = new JSONArray(responseBody);
             * 
             * Integer turnID = responseArrayObj.getJSONObject(0).getInt("id");
             */

            JSONObject resp = new JSONObject();

            resp.put("game_id", gameID);
            resp.put("round_id", roundID);
            resp.put("turn_id", 1);

            return resp;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean updateGame(Game game, int turnId) {
        // TODO: modificare updateRequest e PUT in T4 aggiungendo nuovi campi

        try {
            JSONObject newTurn = new JSONObject();

            JSONArray playersArray = new JSONArray();

            playersArray.put(String.valueOf(game.getPlayerId()));

            newTurn.put("roundId", game.getRound());
            newTurn.put("id", turnId + 1);
            newTurn.put("players", playersArray);

            HttpPost httpPost = new HttpPost("http://t4-g18-app-1:3000/turns");

            StringEntity jsonEntity = new StringEntity(newTurn.toString(), ContentType.APPLICATION_JSON);

            httpPost.setEntity(jsonEntity);

            HttpResponse httpResponse = httpClient.execute(httpPost);

            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode > 299) {
                System.err.println(EntityUtils.toString(httpResponse.getEntity()));
                return false;
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveGameCSV(Game game, int tid) {
        long pid = game.getPlayerId();
        long gid = game.getId();
        int rid = game.getRound();

        String playerID = "Player" + pid;
        String gameID = "Game" + gid;
        String roundID = "Round" + rid;
        String turnID = "Turn" + tid;

        // Al path bisogna aggiungere PlayerID/GameID/RoundID/TurnID e poi il nome del
        // file
        String fileName = CSV_FILE_PATH + playerID + "/" + gameID + "/" + roundID + "/" + turnID + CSV_FILE_NAME;

        Path path = Paths.get(fileName);

        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                System.out.println("Errore durante la creazione della directory.");
                e.printStackTrace();
            }
        }

        try {
            File file = new File(fileName);

            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter writer = new FileWriter(file);

            CSVFormat csvFormat = CSVFormat.Builder.create().setDelimiter(';').build();
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(
                    "GameID",
                    "Name",
                    "Round",
                    "Class",
                    "Description",
                    "Difficulty",
                    "CreatedAt",
                    "UpdatedAt",
                    "StartedAt",
                    "ClosedAt",
                    "PlayerID",
                    "Robot");

            csvPrinter.printRecord(
                    game.getId(),
                    game.getName(),
                    game.getRound(),
                    game.getTestedClass(),
                    game.getDescription(),
                    game.getDifficulty(),
                    game.getCreatedAt(),
                    game.getUpdatedAt(),
                    game.getStartedAt(),
                    game.getClosedAt(),
                    game.getPlayerId(),
                    game.getRobot());

            csvPrinter.flush();
            csvPrinter.close();
            writer.close();
            System.out.println(gameID + " " + turnID + " salvato correttamente in File System.");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Errore durante la scrittura del file CSV.");
            return false;
        }
    }

    public boolean updateGameCSV(Game game, int tid) {
        long pid = game.getPlayerId();
        long gid = game.getId();
        int rid = game.getRound();

        String playerID = "Player" + pid;
        String gameID = "Game" + gid;
        String roundID = "Round" + rid;
        String turnID = "Turn" + tid;

        String fileName = CSV_FILE_PATH + playerID + "/" + gameID + "/" + roundID + "/" + turnID + CSV_FILE_NAME;

        Path path = Paths.get(fileName);

        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                System.out.println("Errore durante la creazione della directory.");
                e.printStackTrace();
            }
        }

        try {
            File file = new File(fileName);

            FileWriter writer = new FileWriter(file);

            CSVFormat csvFormat = CSVFormat.Builder.create().setDelimiter(';').build();
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(
                    "GameID",
                    "Name",
                    "Round",
                    "Class",
                    "Description",
                    "Difficulty",
                    "CreatedAt",
                    "UpdatedAt",
                    "StartedAt",
                    "ClosedAt",
                    "PlayerID",
                    "Robot");

            csvPrinter.printRecord(
                    game.getId(),
                    game.getName(),
                    game.getRound(),
                    game.getTestedClass(),
                    game.getDescription(),
                    game.getDifficulty(),
                    game.getCreatedAt(),
                    game.getUpdatedAt(),
                    game.getStartedAt(),
                    game.getClosedAt(),
                    game.getPlayerId(),
                    game.getRobot());

            csvPrinter.flush();
            csvPrinter.close();
            writer.close();

            System.out.println(gameID + " " + turnID + " aggiornato correttamente in File System.");
            return true;
        } catch (IOException e) {
            System.out.println("Errore durante la scrittura del file CSV.");
            e.printStackTrace();
            return false;
        }
    }

    public boolean createNextTurnCSV(Game game, int turnId) {
        return saveGameCSV(game, turnId + 1);
    }
    // FINE MODIFICHE A3-T4
}