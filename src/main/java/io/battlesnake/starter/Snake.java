package io.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.get;

/**
 * Snake server that deals with requests from the snake engine.
 * Just boiler plate code.  See the readme to get started.
 * It follows the spec here: https://github.com/battlesnakeio/docs/tree/master/apis/snake
 */
public class Snake {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Handler HANDLER = new Handler();
    private static final Logger LOG = LoggerFactory.getLogger(Snake.class);

    /**
     * Main entry point.
     *
     * @param args are ignored.
     */
    public static void main(String[] args) {
        String port = System.getProperty("PORT");
        if (port != null) {
            LOG.info("Found system provided port: {}", port);
        } else {
            LOG.info("Using default port: {}", port);
            port = "8080";
        }
        port(Integer.parseInt(port));
        get("/", (req, res) -> "Battlesnake documentation can be found at " + 
            "<a href=\"https://docs.battlesnake.io\">https://docs.battlesnake.io</a>.");
        post("/start", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/ping", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/move", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/end", HANDLER::process, JSON_MAPPER::writeValueAsString);
    }

    /**
     * Handler class for dealing with the routes set up in the main method.
     */
    public static class Handler {

        /**
         * For the ping request
         */
        private static final Map<String, String> EMPTY = new HashMap<>();

        /**
         * Generic processor that prints out the request and response from the methods.
         *
         * @param req
         * @param res
         * @return
         */
        public Map<String, String> process(Request req, Response res) {
            try {
                JsonNode parsedRequest = JSON_MAPPER.readTree(req.body());
                String uri = req.uri();
                LOG.info("{} called with: {}", uri, req.body());
                Map<String, String> snakeResponse;
                if (uri.equals("/start")) {
                    snakeResponse = start(parsedRequest);
                } else if (uri.equals("/ping")) {
                    snakeResponse = ping();
                } else if (uri.equals("/move")) {
                    snakeResponse = move(parsedRequest);
                } else if (uri.equals("/end")) {
                    snakeResponse = end(parsedRequest);
                } else {
                    throw new IllegalAccessError("Strange call made to the snake: " + uri);
                }
                LOG.info("Responding with: {}", JSON_MAPPER.writeValueAsString(snakeResponse));
                return snakeResponse;
            } catch (Exception e) {
                LOG.warn("Something went wrong!", e);
                return null;
            }
        }

        /**
         * /ping is called by the play application during the tournament or on play.battlesnake.io to make sure your
         * snake is still alive.
         *
         * @param pingRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return an empty response.
         */
        public Map<String, String> ping() {
            return EMPTY;
        }

//methid to store Id and board hieght and widthstatic
JsonNode getJsonData(){
    String id = JsonNode.get("id").textValue();
    String boardHieght = JsonNode.get("height").textValue();
    String boardwidth = JsonNode.get("width").textValue();
    
    System.out.println(id);
    System.out.println(boardHieght);
    System.out.println(boardwidth);
    
}


   


















        /**
         * /start is called by the engine when a game is first run.
         *
         * @param startRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing the snake setup values.
         */
        public Map<String, String> start(JsonNode startRequest) {
            Map<String, String> response = new HashMap<>();
            response.put("color", "#ff00ff");
            return response;
        }



        /**
         * /move is called by the engine for each turn the snake has.
         * 
         * @param moveRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing snake movement values.
         */
       
        
        public Map<String, String> move(JsonNode moveRequest) {
            Map<String, String> response = new HashMap<>();
            String id = moveRequest.get("game").get("id").textValue();
            int boardHeight = moveRequest.get("board").get("height").intValue();
            int boardWidth = moveRequest.get("board").get("width").intValue();
            Iterator<JsonNode> bodyIter = moveRequest.get("you").get("body").elements();
            List<int[]> body = new ArrayList<int[]>();

            while (bodyIter.hasNext()) {
                JsonNode coord = bodyIter.next();
                int x = coord.get("x").intValue();
                int y = coord.get("y").intValue();

                body.add(new int[] {x, y});
            }
            
            System.out.println(id);
            System.out.println(boardHeight);
            System.out.println(boardWidth);

            for (int i = 0; i < body.size(); i++) {
                int[] coord = body.get(i);
                System.out.println(coord[0] + "," + coord[1]);
            }

            int xHead = body.get(0)[0];
            int yHead = body.get(0)[1];
        
            boolean right = isInBounds(xHead + 1, yHead, boardWidth, boardHeight);
            boolean down = isInBounds(xHead, yHead + 1, boardWidth, boardHeight);
            boolean left = isInBounds(xHead - 1, yHead, boardWidth, boardHeight);
            boolean up = isInBounds(xHead, yHead - 1, boardWidth, boardHeight);
        
            if (right == true){
                response.put("move","right");
            } else if (down == true){
                response.put("move","down");
            } else if (left == true){
                response.put("move","left");
            } else if (up==true){
                response.put("move","up");
            }

            return response;
        }

        public boolean isInBounds(int x, int y, int boardWidth, int boardHeight) {
            if (x < 0 || x >= boardWidth || y < 0 || y >= boardHeight) {
                return false;
            }
        
            return true;
        }

        /**
         * /end is called by the engine when a game is complete.
         *
         * @param endRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> end(JsonNode endRequest) {
            Map<String, String> response = new HashMap<>();
            return response;
        }
    }
}