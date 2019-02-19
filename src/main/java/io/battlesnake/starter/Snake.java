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
import java.util.Arrays;
import java.util.Random;
import java.util.LinkedList;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.get;

import com.almasb.astar.AStarGrid;
import com.almasb.astar.AStarNode;
import com.almasb.astar.NodeState;

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
            int boardHeight = moveRequest.get("board").get("height").intValue();
            int boardWidth = moveRequest.get("board").get("width").intValue();
            AStarGrid grid = new AStarGrid(boardWidth, boardHeight);
            List<int[]> ourBody = new ArrayList<int[]>();
            List<int[]> allBodies = new ArrayList<int[]>();
            List<int[]> food = new ArrayList<int[]>();

            Iterator<JsonNode> ourBodyIter = moveRequest.get("you").get("body").elements();

            while (ourBodyIter.hasNext()) {
                JsonNode coord = ourBodyIter.next();
                int x = coord.get("x").intValue();
                int y = coord.get("y").intValue();

                ourBody.add(new int[] {x, y});
            }

            Iterator<JsonNode> snakesIter = moveRequest.get("board").get("snakes").elements();
            while (snakesIter.hasNext()){
                Iterator<JsonNode> bodyIter = snakesIter.next().get("body").elements();

                while (bodyIter.hasNext()) {
                    JsonNode coord = bodyIter.next();

                    if (bodyIter.hasNext()) {
                        int x = coord.get("x").intValue();
                        int y = coord.get("y").intValue();
        
                        grid.setNodeState(x, y, NodeState.NOT_WALKABLE);
                        allBodies.add(new int[] {x, y});
                    }
                }
            }

            Iterator<JsonNode> foodIter = moveRequest.get("board").get("food").elements();

            while (foodIter.hasNext()) {
                JsonNode coord = foodIter.next();
                int x = coord.get("x").intValue();
                int y = coord.get("y").intValue();

                food.add(new int[] {x, y});
            }

            int xHead = ourBody.get(0)[0];
            int yHead = ourBody.get(0)[1];
            int xTail = ourBody.get(ourBody.size() - 1)[0];
            int yTail = ourBody.get(ourBody.size() - 1)[1];
            int[] directionVector = new int[] { 0, 0 };
            List<AStarNode> pathToTail = grid.getPath(xHead, yHead, xTail, yTail);

            if (pathToTail.size() > 0) {
                AStarNode node = pathToTail.get(0);

                directionVector = new int []{ node.getX() - xHead, node.getY() - yHead };   
            }

            System.out.println("Tail path size: " + pathToTail.size());

            if (food.size() > 0) {
                int[] targetFood = food.get(0);

                List<AStarNode> pathToFood = grid.getPath(xHead, yHead, targetFood[0], targetFood[1]);

                System.out.println("Food path size: " + pathToFood.size());

                if (pathToFood.size() > 0) {
                    AStarNode node = pathToFood.get(0);

                    List<AStarNode> pathToTailFromFoodNode = grid.getPath(node.getX(), node.getY(), xTail, yTail);

                    System.out.println("Tail food path size: " + pathToTailFromFoodNode.size());

                    if (pathToTailFromFoodNode.size() > 0) {
                        directionVector = new int []{ node.getX() - xHead, node.getY() - yHead };
                    }
                }
            }

            if (directionVector[0] == 1 && directionVector[1] == 0) {
                response.put("move", "right");
            } else if (directionVector[0] == 0 && directionVector[1] == 1) {
                response.put("move", "down");
            } else if (directionVector[0] == -1 && directionVector[1] == 0) {
                response.put("move", "left");
            } else if (directionVector[0] == 0 && directionVector[1] == -1) {
                response.put("move", "up");
            } else {
                response.put("move", getRandomMove(boardWidth, boardHeight, ourBody, allBodies));
            }

            return response;
        }

        public boolean isInBounds(int[] coord, int boardWidth, int boardHeight) {
            if (coord[0] < 0 || coord[0] >= boardWidth || coord[1] < 0 || coord[1] >= boardHeight) {
                return false;
            }
        
            return true;
        }

        public boolean isColliding(int[] coord, List<int[]> body) {
            for (int i = 0; i < body.size(); i++) {
                int[] bodyCoord = body.get(i);

                if (coord[0] == bodyCoord[0] && coord[1] == bodyCoord[1]) {
                    return true;
                }
            }

            return false;
        }

        public String getRandomMove(int boardWidth, int boardHeight, List<int[]> ourBody, List<int[]> allBodies) {
            int xHead = ourBody.get(0)[0];
            int yHead = ourBody.get(0)[1];

            int[] right = { xHead + 1, yHead };
            int[] down = { xHead, yHead + 1 };
            int[] left = { xHead - 1, yHead };
            int[] up = { xHead, yHead - 1 };
        
            boolean isRightValid = isInBounds(right, boardWidth, boardHeight) && !isColliding(right, allBodies);
            boolean isDownValid = isInBounds(down, boardWidth, boardHeight) && !isColliding(down, allBodies);
            boolean isLeftValid = isInBounds(left, boardWidth, boardHeight) && !isColliding(left, allBodies);
            boolean isUpValid = isInBounds(up, boardWidth, boardHeight) && !isColliding(up, allBodies);

            List<String> directions = new LinkedList<String>(Arrays.asList("right", "down", "left", "up"));

            if (!isRightValid) {
                directions.remove("right");
            }
            
            if (!isDownValid) {
                directions.remove("down");
            }
            
            if (!isLeftValid) {
                directions.remove("left");
            }
            
            if (!isUpValid) {
                directions.remove("up");
            }

            Random rand = new Random();

            return directions.get(rand.nextInt(directions.size()));
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