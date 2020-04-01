package cli;

import menu.DefaultFood;
import menu.Destination;
import menu.Meal;
import napsack.Knapsack;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import simulation.Fifo;
import simulation.Results;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class SimController {
    File nameFile; //File that contains a list of names
    ArrayList<String> names; //Stores the list of names
    File ordersFile; //xml file that saves the orders
    private static DefaultFood defaultFood; //The object that stores the default meal combos
    private static Settings settings;
    int MINUTES_IN_SIM = 240; //The number of minutes in the simulation
    ArrayList<Results> aggregatedResults; //The results from all 50 simulations
    int NUMBER_OF_SIMULATIONS = 50;

    ArrayList<PlacedOrder> test;

    //pointer to the single instance of SimController
    private static SimController single_instance = null;


    private SimController() {
        test = new ArrayList<>();
        aggregatedResults = new ArrayList<>();

        names = new ArrayList<>();
        try {
            //Names.txt was generated by Dominic Tarr
            nameFile = new File("Names.txt"); //open the names file

            //Seed the arraylist with names in the file
            Scanner s = new Scanner(nameFile);
            while (s.hasNext()) {
                names.add(s.next());
            }
            s.close();

        } catch (Exception e) {
            System.out.println((e.getMessage()));
        }

        defaultFood = new DefaultFood(); //Get the default food settings
        //settings = new Settings();
        //System.out.println(defaultFood.mapToString());
    }

    //Singleton creator
    public static SimController getInstance() {
        if (single_instance == null) {
            single_instance = new SimController();
        }
        return single_instance;
    }

    /**
     * Generates the orders for the four hour simulation
     * The orders are stored in Orders.xml
     */
    public void generateOrders() {
        Random random = new Random(); //new random generator

        int curMin = 0; //The current minute the simulation is in
        int randName; //random integer for what name to choose the ArrayList
        ArrayList<Destination> map = defaultFood.getMap(); //Map of the given campus
        ArrayList<Integer> ordersPerHour = defaultFood.getOrdersPerHour();
        Meal m; //Current meal being ordered
        Destination d; //The destination to be delivered to



        //open the orders file
        try {
            ordersFile = new File("Orders.xml");
            FileWriter fileWriter = new FileWriter(ordersFile, false); //clear out the orders file
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("<listOfOrders>");
            printWriter.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        //calculate the chance of order per minute
        double chanceOfOrderPerM;
        chanceOfOrderPerM = adjustProbability(ordersPerHour.get(0));


        //For each minute in the simulation
        while (curMin < MINUTES_IN_SIM) {

            if (random.nextDouble() < chanceOfOrderPerM) { //If the probability is low enough, generate an order
                //Get a random name
                randName = random.nextInt(names.size());

                //Get a random meal based on the distribution
                m = randomMeal(random.nextDouble());

                //Get a random destination
                d = map.get(random.nextInt(map.size()));

                if (m != null) {
                    //Create a new order
                    PlacedOrder ord = new PlacedOrder(curMin, d, names.get(randName), m);

                    //Add the order to the xml file
                    ord.addToXML(ordersFile);
                    if (test.size() < 5) {
                        test.add(ord);
                    }
                } else {
                    System.out.println("randomMeal is broken\n");
                }//m!=null

            } else { //If an order is not generated
                curMin++;
                if (curMin %60 == 0 && curMin < MINUTES_IN_SIM) {
                    chanceOfOrderPerM = adjustProbability(ordersPerHour.get(curMin/60));
                }
            }


        }

        //Add the xml closing for the orders file
        try {
            ordersFile = new File("Orders.xml"); //open the orders file
            FileWriter fileWriter = new FileWriter(ordersFile, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("</listOfOrders>");
            printWriter.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        //System.out.println("Least cost distance of the subset of the first five orders is: " + TSP(test));
    }

    /**
     * Run the FIFO and Knapsack algorithms
     */
    public void runAlgorithms() {


        ArrayList<PlacedOrder> allOrders = getXMLOrders(); //All the xml orders placed
        //System.out.println(allOrders.get(0));
        System.out.println("Total number of orders: " + allOrders.size());
        Results results = new Results();

        //Initialize the knapsack and FIFO algorithms
        Knapsack n = new Knapsack(allOrders);
        Fifo f = new Fifo(allOrders);
        int loadMealTime = 0;

        //UNCOMMENT THE CODE WHEN YOU HAVE KNAPSACK/FIFO REFACTORED

        double elapsedTime = 3; //how far into the simulation are we
        boolean ordersStillToProcess = true; //If there are orders still to process
        double droneSpeed = 20 * 5280 / 60; //Flight speed of the drone in ft/minute
        int droneDeliveryNumber = 1;

        try {


            //Knapsack
            while (ordersStillToProcess && droneDeliveryNumber < 100) {
                ArrayList<PlacedOrder> droneRun = n.packDrone(elapsedTime); //Get what is on the current drone
                if (droneRun == null) {
                    ordersStillToProcess = false;
                } else {
                    elapsedTime += n.getTimeSkipped() + loadMealTime;
                    //Find how long the delivery takes
                    elapsedTime += TSP(droneRun) / droneSpeed + .5 * droneRun.size();
                    //System.out.println("Time that delivery " + droneDeliveryNumber+ " arrived with " + droneRun.size() + " deliveries: " + elapsedTime);
                    results.processDelivery(elapsedTime, droneRun);
                    elapsedTime += 3;
                }
                droneDeliveryNumber++;
            }
            results.getFinalResults("Knapsack");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        aggregatedResults.add(results);




        try {

            results = new Results();
            elapsedTime = 3;
            ordersStillToProcess = true;
            droneDeliveryNumber = 1;

            //FIFO
            while (ordersStillToProcess) {
                //System.out.println("Test prior to drone run");
                ArrayList<PlacedOrder> droneRun = f.packDrone(elapsedTime); //Get what is on the current drone
                //System.out.println("Test after FIFO");
                if (droneRun == null) {
                    ordersStillToProcess = false;
                } else {
                    elapsedTime += f.getTimeSkipped() + loadMealTime;
                    //Find how long the delivery takes
                    elapsedTime += TSP(droneRun)/droneSpeed + .5 * droneRun.size();
                    //System.out.println("Time that delivery " + droneDeliveryNumber+ " arrived with " + droneRun.size() + " deliveries: " + elapsedTime);
                    results.processDelivery(elapsedTime, droneRun);
                    elapsedTime += 3;
                }
                	droneDeliveryNumber++;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        results.getFinalResults("FIFO");

        aggregatedResults.add(results);

    }


    /**
     * Get all of the order in the xml file
     *
     * @return ArrayList of orders that were placed
     */
    private ArrayList<PlacedOrder> getXMLOrders() {
        ArrayList<PlacedOrder> placedOrders = new ArrayList<>(); //all the placed orders

        //List of potential meals so that to compare the name in the xml file with
        List<Meal> meals = defaultFood.getMeals();

        try {
            //XML file reading initialization
            File orderFile = new File("Orders.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newDefaultInstance();
            DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(orderFile);
            document.getDocumentElement().normalize();

            //Create a node list of all the elements
            NodeList nodeList = document.getElementsByTagName("order");

            //Variables that go in a placed order
            String cname,dname, mealString, mname;
            int ordTime;
            Destination dest;
            Meal chosenMeal = new Meal("Temp", 0);
            PlacedOrder oneOrder;

            //For each element in the file
            for (int temp = 0; temp < nodeList.getLength(); temp++) {

                Node node = nodeList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) node;

                    //Get the customer's name
                    cname = e.getElementsByTagName("cname").item(0).getTextContent();

                    //Get the time of the order
                    ordTime = Integer.parseInt(e.getElementsByTagName("ordTime").item(0).getTextContent());

                    //Get the destination
                    dname = e.getElementsByTagName("dest").item(0).getTextContent();
                    Scanner destScanner = new Scanner(dname);
                    dest = new Destination(destScanner.next(), destScanner.nextInt(), destScanner.nextInt());
                    destScanner.close();

                    //Get the name of the meal in the file and check it against the potential meals and find
                    //the right meal
                    mealString = e.getElementsByTagName("meal").item(0).getTextContent();
                    Scanner mealScanner = new Scanner(mealString);
                    mealScanner.useDelimiter(":");
                    mname = mealScanner.next();
                    mealScanner.close();
                    for(int m = 0; m < meals.size(); m++) {
                        if (mname.equals(meals.get(m).getName())) {
                            chosenMeal = meals.get(m);
                            break;
                        }
                    }


                    //Create the order and it to the arrayList of all the orders
                    oneOrder = new PlacedOrder(ordTime, dest, cname, chosenMeal);
                    placedOrders.add(oneOrder);
                }
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
        return placedOrders;
    }


    /**
     * Returns a random meal based on the distribution
     *
     * @param rand A random double between 0 and 1
     * @return The meal that was randomly selected
     */
    private Meal randomMeal(double rand) {
        double counter = 0; //Keeps track of the distribution through the loop

        //Store the list of meals
        List<Meal> lm = defaultFood.getMeals();

        //Iterate to where the random number points to in the distribution
        for (int i = 0; i < lm.size(); i++) {
            counter += lm.get(i).getDistribution();
            if (rand < counter) {
                return lm.get(i); //return the meal selected
            }
        }

        //If it returns null the distribution invalid
        return null;
    }

    /**
     * Calculates the least cost distance to complete the delivery cycle
     *
     * @param orders ArrayList of placed orders to travel and deliver food to
     * @return The least cost distance to complete the delivery cycle
     */
    public double TSP(ArrayList<PlacedOrder> orders) {
        ArrayList<Destination> locations = new ArrayList<>(); //The destination of each order

        //The place where the drone leaves and returns to
        Destination home = new Destination("Home", 0, 0, 0);

        //Seed the location ArrayList with each destination from the order list
        for (int i = 0; i < orders.size(); i++) {
            locations.add(orders.get(i).getDest());
        }

        //Call the recursive function which does the brunt work of the algorithm
        return recursiveTSP(locations, home);
    }

    /**
     * Does the brunt work of the TSP. Should run in O(n^2 * 2^n) which is much better than O(n!)
     *
     * @param locations The locations yet to be visited
     * @param lastDest  The last destination the algorithm visited
     * @return The least cost distance to visit all the locations and return
     */
    private double recursiveTSP(ArrayList<Destination> locations, Destination lastDest) {
        if (locations.size() == 0) { //base case
            return lastDest.getDist(); //return to home
        } else {
            double min = Double.MAX_VALUE; //minimum travel distance for given depth in the recursion tree

            //For each possible set of locations
            for (int d = 0; d < locations.size(); d++) {
                Destination newDest = locations.remove(0); //remove the destination

                //recurse: it finds the fastest route in the subset and then adds the distance between the current
                //          point and the subset. It then takes the minimum at the level so to help in the recursion in
                //          the level above it
                min = Math.min(min, recursiveTSP(locations, newDest) + lastDest.distanceBetween(newDest));

                //Add back the location because each iteration of the loop on the same level should have the same
                //number of locations to check
                locations.add(newDest);
            }
            //return the shortest distance found in the given level
            return min;
        }

    }

    /**
     * Adjust the probability so that generateOrders produces the correct number of orders
     * even though multiple orders can come in a single minute
     * @param ordersPerHour The number of orders per this hour
     * @return The adjusted probability
     */
    private double adjustProbability(int ordersPerHour) {
        //The probability that we would be shooting for if we were not allowing multiple orders to
        //come in the same minute
        double idealProbability = ordersPerHour/60.0;

        double newProbability = idealProbability; //The new probability that we will use

        //The sum of the geometric series that is used to find what the input probability will correspond
        //to given that we can have multiple orders per minutes
        double extrapolatedProbability = Double.MAX_VALUE;

        //Keep looping until we lower the newProbability enough so that it will result in the correct
        //number of orders per hours
        while (extrapolatedProbability > idealProbability) {
            newProbability -= .01;
            extrapolatedProbability = newProbability/(1-newProbability); //Sum of a geometric series
        }

        return newProbability;
    }



    public static DefaultFood getDefaultFood() {
        if (single_instance == null) {
            single_instance = new SimController();
        }
        return defaultFood;
    }


    public static Settings getSettings() {
        if (single_instance == null) {
            single_instance = new SimController();
        }
        return settings;
    }

    public int getNUMBER_OF_SIMULATIONS() {
        return NUMBER_OF_SIMULATIONS;
    }

}
