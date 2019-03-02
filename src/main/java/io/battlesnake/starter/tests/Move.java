package io.battlesnake.starter.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.battlesnake.starter.Snake;

public class Move {
    public static void main(String[] args) {
        try {
            //read json file data to String
            byte[] jsonData = Files.readAllBytes(Paths.get("fixtures/move.json"));
            
            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode moveRequest = objectMapper.readTree(jsonData);
            Snake.Handler handler = new Snake.Handler();

            handler.move(moveRequest);
        } catch (IOException e) {

        }

    }
}