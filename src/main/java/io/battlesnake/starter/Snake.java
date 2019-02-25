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

import org.xguzm.pathfinding.grid.NavigationGrid;
import org.xguzm.pathfinding.grid.GridCell;
import org.xguzm.pathfinding.finders.AStarFinder;
import org.xguzm.pathfinding.grid.finders.GridFinderOptions;

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
                LOG.error("Something went wrong!", e);
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
            GridCell[][] grid = new GridCell[boardWidth][boardHeight];
            List<int[]> ourBody = new ArrayList<int[]>();
            List<List<int[]>> others = new ArrayList<List<int[]>>();
            List<int[]> allBodies = new ArrayList<int[]>();
            List<int[]> food = new ArrayList<int[]>();
            String ourId = moveRequest.get("you").get("id").textValue();

            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    grid[i][j] = new GridCell(i, j, true);
                }
            }

            Iterator<JsonNode> ourBodyIter = moveRequest.get("you").get("body").elements();

            while (ourBodyIter.hasNext()) {
                JsonNode coord = ourBodyIter.next();
                int x = coord.get("x").intValue();
                int y = coord.get("y").intValue();

                ourBody.add(new int[] {x, y});
            }

            Iterator<JsonNode> snakesIter = moveRequest.get("board").get("snakes").elements();
            while (snakesIter.hasNext()){
                List<int[]> snake = new ArrayList<int[]>();
                JsonNode snakeNode = snakesIter.next();
                Iterator<JsonNode> bodyIter = snakeNode.get("body").elements();

                while (bodyIter.hasNext()) {
                    JsonNode coord = bodyIter.next();
                    int x = coord.get("x").intValue();
                    int y = coord.get("y").intValue();

                    snake.add(new int[] {x, y});

                    if (bodyIter.hasNext()) {

        
                        grid[x][y] = new GridCell(x, y, false);
                        allBodies.add(new int[] {x, y});
                    }
                }

                if (ourId != snakeNode.get("id").textValue()) {
                    others.add(snake);
                }
            }

            Iterator<JsonNode> foodIter = moveRequest.get("board").get("food").elements();

            while (foodIter.hasNext()) {
                JsonNode coord = foodIter.next();
                int x = coord.get("x").intValue();
                int y = coord.get("y").intValue();

                food.add(new int[] {x, y});
            }

            NavigationGrid<GridCell> navGrid = new NavigationGrid<GridCell>(grid, false);
            GridFinderOptions opt = new GridFinderOptions();
            opt.allowDiagonal = false;
            AStarFinder<GridCell> finder = new AStarFinder<GridCell>(GridCell.class, opt);

            int xHead = ourBody.get(0)[0];
            int yHead = ourBody.get(0)[1];
            int xTail = ourBody.get(ourBody.size() - 1)[0];
            int yTail = ourBody.get(ourBody.size() - 1)[1];
            int[] directionVector = new int[] { 0, 0 };
            List<GridCell> pathToTail = finder.findPath(navGrid.getCell(xHead, yHead), navGrid.getCell(xTail, yTail), navGrid);

            if (pathToTail != null && pathToTail.size() > 0) {
                System.out.println("Tail path size: " + pathToTail.size());

                GridCell node = pathToTail.get(0);

                directionVector = new int []{ node.getX() - xHead, node.getY() - yHead };   
            }            

            if (food.size() > 0) {
                int[] targetFood = closestFood(ourBody, others, food);

                List<GridCell> pathToFood = finder.findPath(navGrid.getCell(xHead, yHead), navGrid.getCell(targetFood[0], targetFood[1]), navGrid);

                

                if (pathToFood != null && pathToFood.size() > 0) {
                    System.out.println("Food path size: " + pathToFood.size());

                    GridCell node = pathToFood.get(0);

                    List<GridCell> pathToTailFromFoodNode = finder.findPath(navGrid.getCell(node.getX(), node.getY()), navGrid.getCell(xTail, yTail), navGrid);                    

                    if (pathToTailFromFoodNode != null && pathToTailFromFoodNode.size() > 0) {
                        System.out.println("Tail food path size: " + pathToTailFromFoodNode.size());

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

        public int[] closestFood(List<int[]> ourBody, List<List<int[]>> others, List <int[]> food){
            if (food.size() == 0) {
                return null;
            }

            int[] ourHead = ourBody.get(0);
            int[] closestFood = null;
            int closestDistance = Integer.MAX_VALUE;

            for (int i = 0; i< food.size(); i++){
                boolean isValid = true;
                int[] currentFood = food.get(i);
                int distance = Math.abs(ourHead[0]- currentFood[0])+Math.abs(ourHead[1]- currentFood[1]);

                if (distance < closestDistance) {
                    for (int j = 0; j < others.size(); j++) {
                        List<int[]> enemySnake = others.get(j);
                        int[] enemyHead = enemySnake.get(0);

                        if (enemySnake.size() < ourBody.size()) {
                            continue;
                        } else {
                            int enemyDistance = Math.abs(enemyHead[0]- currentFood[0])+Math.abs(enemyHead[1]- currentFood[1]);

                            if (distance >= enemyDistance) {
                                isValid = false;
                                break;
                            }
                        }
                    }
               } else {
                   isValid = false;
               }

               if (isValid) {
                    closestDistance = distance;
                    closestFood = food.get(i);
                }
            }  

            return closestFood;
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

            if (directions.size() == 0) {
                return "right";
            } else {
                Random rand = new Random();

                return directions.get(rand.nextInt(directions.size()));
            }
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