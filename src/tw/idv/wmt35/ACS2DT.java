package tw.idv.wmt35;

import org.apache.commons.cli.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.System.exit;

public class ACS2DT {
    private int populationSize;
    private ArrayList<LinkedList<int[]>> frequentItemsets;
    public LinkedList<int[]> sensitive, nonSensitive, preLarge;
    private Apriori apriori;
    private LinkedList<int[]> projectedDatabase;
    public double alpha = 0.0, beta = 0.0, gamma = 0.0, antAlpha = 1.0, antBeta = 2.0, q0 = 0.8, rho = 0.4, minSup;
    private HashMap<String, Integer> itemsetsCount;
    private HashMap<String, Double> remainNodes;
    HashMap<String, Double> outputDeletedTransactions;
    private HashMap<String, Double> pheoromoneTable;
    private LinkedList<int[]> current;
    public LinkedList<int[]> globalBest;
    public double globalBestFitness;
    private LinkedList<int[]> iterationBest;
    private double iterationBestFitness;
    private LinkedList<int[]> nonHideSensitive;
    private LinkedList<int[]> hideFrequent;
    private LinkedList<int[]> fakeFrequent;
    public int fth;
    public int nth;
    public int ntg;
    private double totalWeight, bestWeight;
    private double maxFitness;
    private String bestWeightKey;
    public int maxSelection;
    public double maxSelectionTimes;

    public ACS2DT() {
        populationSize = 2;
        sensitive = new LinkedList<>();
        nonSensitive = new LinkedList<>();
        preLarge = new LinkedList<>();
        projectedDatabase = new LinkedList<>();
        itemsetsCount = new HashMap<>();
        remainNodes = new HashMap<>();
        pheoromoneTable = new HashMap<>();
        current = new LinkedList<>();
        globalBest = new LinkedList<>();
        iterationBest = new LinkedList<>();
        nonHideSensitive = new LinkedList<>();
        fakeFrequent = new LinkedList<>();
        hideFrequent = new LinkedList<>();
        maxSelectionTimes = 1;
    }

    public void setFrequentItemsets(ArrayList<LinkedList<int[]>> frequentItemsets) { this.frequentItemsets = frequentItemsets; }

    public void setPreLargeFrequentItemsets(ArrayList<LinkedList<int[]>> preLargeFrequentItemsets) { preLargeFrequentItemsets.forEach(e -> e.forEach(preLarge::add)); }

    public void readSensitiveInformation(String inputFileName) {
        try (Stream<String> stream = Files.lines(Paths.get(inputFileName))) {
            sensitive.clear();
            stream.forEach(this::readLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
        maxFitness = alpha * (double) sensitive.size();
    }

    private void readLine(String line) {
        String[] stringArray = line.split(",");
        sensitive.add(Arrays.stream(stringArray).mapToInt(Integer::parseInt).toArray());
    }

    public void printSensitiveItemsets() {
        System.out.println("Sensitive Itemsets");
        sensitive.forEach(e -> System.out.println(Arrays.toString(e) + " : " + apriori.getItemsetCount(e)));
    }

    public void printNonSensitiveItemsets() {
        System.out.println("NonSensitive Itemsets");
        nonSensitive.forEach(e -> System.out.println(Arrays.toString(e) + " : " + apriori.getItemsetCount(e)));
    }

    public void printPreLargeItemsets() {
        System.out.println("Pre Large Itemsets");
        preLarge.forEach(e -> System.out.println(Arrays.toString(e) + " : " + apriori.getItemsetCount(e)));
    }

    public void printProjectdDatabase() {
        System.out.println("Projected Database");
        projectedDatabase.forEach(e -> System.out.println(Arrays.toString(e)));
    }

    public int getPopulationSize() { return populationSize; }

    private boolean itemsetIsSensitive(int[] itemset) {
        boolean result = false;
        ListIterator it = sensitive.listIterator();
        while (it.hasNext()) {
            int[] sItemset = (int[]) it.next();
            if (Arrays.equals(sItemset, itemset)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private boolean containArray(int[] a1, int[] a2) {
        int i;
        for (i = 0; i < a2.length; ++i) {
            if (Arrays.binarySearch(a1, a2[i]) < 0)
                break;
        }

        return i == a2.length;
    }

    private void init(Apriori apriori) {
        this.apriori = apriori;
        setFrequentItemsets(apriori.getFrequentItemsets());
        setPreLargeFrequentItemsets(apriori.getPreLargeFrequentItemsets());
        frequentItemsets.forEach(e -> {
            e.forEach(f -> {
                if (!itemsetIsSensitive(f)) {
                    int[] itemset = new int[f.length];
                    System.arraycopy(f, 0, itemset, 0, f.length);
                    nonSensitive.add(itemset);
                }
            });
        });
        ArrayList<int[]> transactions = apriori.getTransactions();
        transactions.forEach(e -> {
            boolean insert = false;
            for (int[] sItemset : sensitive) {
                if (containArray(e, sItemset)) {
                    insert = true;
                    break;
                }
            }
            if (insert) {
                projectedDatabase.add(e);
                pheoromoneTable.put(Apriori.idArrayString(e), 1.0);
            }
        });
        int maxSensitiveCount = 0;
        HashMap<String, Integer> countTable = apriori.getFrequentItemsetsCount();
        ListIterator it = sensitive.listIterator();
        while (it.hasNext()) {
            int count = countTable.get(Apriori.idArrayString((int[]) it.next()));
            if (count > maxSensitiveCount)
                maxSensitiveCount = count;
        }
        maxSelection = (int) ((Math.floor((maxSensitiveCount - apriori.minSupCount) / (1 - minSup)) + 1) * maxSelectionTimes);
        globalBestFitness = Double.MAX_VALUE;
    }

    public void resetHeuristicFunction() {
        nonHideSensitive.clear();
        sensitive.forEach(e -> nonHideSensitive.add(e));
        fakeFrequent.clear();
        hideFrequent.clear();
    }

    public void resetCount() {
        itemsetsCount.clear();
        apriori.getFrequentItemsetsCount().forEach((key, value) -> {
            itemsetsCount.put(key, value);
        });
    }

    public void resetRemainNodes() {
        remainNodes.clear();
        projectedDatabase.forEach(e -> remainNodes.put(Apriori.idArrayString(e), 0.0));
    }

    private int countContain(int[] transaction, LinkedList<int[]> itemsets) {
        Integer count = 0;
        ListIterator it = itemsets.listIterator();
        while (it.hasNext()) {
            if (containArray(transaction, (int[]) it.next()))
                ++count;
        }
        return count;
    }

    private void setWeightsForRemainNodes() {
        totalWeight = 0.0;
        bestWeight = 0.0;
        remainNodes.forEach((key, value) -> {
            int bestCount = 0;
            int[] tran = Arrays.stream(key.split(",")).mapToInt(Integer::parseInt).toArray();
            double phoromone = pheoromoneTable.get(key);
            int countNS = countContain(tran, nonHideSensitive);
            int countFF = countContain(tran, fakeFrequent);
            int countHF = countContain(tran, hideFrequent);
            double heuristic = alpha * (double) countNS + gamma * (double) countFF - beta * (double) countHF;
            double weight = Math.pow(phoromone, antAlpha) + Math.pow(heuristic, antBeta);
            totalWeight += weight;
            remainNodes.put(key, weight);
            if (weight > bestWeight) {
                bestWeight = weight;
                bestWeightKey = key;
                bestCount = 1;
            } else if (weight == bestWeight) {
                if (bestWeightKey.split(",").length > key.split(",").length) {
                    bestWeight = weight;
                    bestWeightKey = key;
                    bestCount = 1;
                } else {
                    ++bestCount;
                    if (Math.random() > (1.0 / (double) bestCount))
                        bestWeightKey = key;
                }
            }
        });
    }

    private double calculateNodeFitnessAndRemoveNode(int[] transaction) {
        nonHideSensitive.clear();
        fakeFrequent.clear();
        hideFrequent.clear();
        current.add(transaction);

        int minSupCount = (int) Math.ceil((apriori.transactionsCount() - current.size()) * minSup);

        for (int[] itemset : sensitive) {
            String itemString = Apriori.idArrayString(itemset);
            int count = itemsetsCount.get(itemString);
            if (containArray(transaction, itemset)) {
                count = count - 1;
                itemsetsCount.put(itemString, count);
            }
            if (count >= minSupCount)
                nonHideSensitive.add(itemset);
        }

        for (int[] itemset : nonSensitive) {
            if (containArray(transaction, itemset)) {
                String itemString = Apriori.idArrayString(itemset);
                int count = itemsetsCount.get(itemString) - 1;
                if (count < minSupCount)
                    hideFrequent.add(itemset);
                itemsetsCount.put(itemString, count);
            }
        }

        for (int[] itemset : preLarge) {
            String itemString = Apriori.idArrayString(itemset);
            int count = itemsetsCount.get(itemString);
            if (containArray(transaction, itemset)) {
                count = count - 1;
                itemsetsCount.put(itemString, count);
            }
            if (count >= minSupCount)
                fakeFrequent.add(itemset);
        }

        remainNodes.remove(Apriori.idArrayString(transaction));

        return alpha * nonHideSensitive.size() + beta * hideFrequent.size() + gamma * fakeFrequent.size();
    }

    public void runIteration(boolean recordProjectedDataset) {
        iterationBestFitness = Double.MAX_VALUE;
        for (int i = 0; i < populationSize; ++i) {
            resetCount();
            resetRemainNodes();
            resetHeuristicFunction();
            current.clear();

            for (int j = 0; j < maxSelection; ++j) {
                String selectKey = new String();
                setWeightsForRemainNodes();
                if (Math.random() <= q0)
                    selectKey = bestWeightKey;
                else {
                    double ball = totalWeight * Math.random();
                    Iterator it = remainNodes.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Double> pair = (Map.Entry) it.next();
                        ball -= pair.getValue();

                        if (ball <= 0.0) {
                            selectKey = pair.getKey();
                            break;
                        }
                    }
                }
                double phoromone = pheoromoneTable.get(selectKey);
                pheoromoneTable.put(selectKey, phoromone * (1 - rho) + rho);
                double fitness = calculateNodeFitnessAndRemoveNode(Arrays.stream(selectKey.split(",")).mapToInt(Integer::parseInt).toArray());

                if ((fitness < iterationBestFitness) || ((fitness == iterationBestFitness) && (current.size() < iterationBest.size()))) {
                    iterationBestFitness = fitness;
                    iterationBest = (LinkedList<int[]>) current.clone();
                }
                if ((fitness < globalBestFitness) || ((fitness == globalBestFitness) && (current.size() < globalBest.size()))) {
                    globalBestFitness = fitness;
                    globalBest = (LinkedList<int[]>) current.clone();
                    fth = nonHideSensitive.size();
                    nth = hideFrequent.size();
                    ntg = fakeFrequent.size();
                    if (recordProjectedDataset)
                        outputDeletedTransactions = (HashMap<String, Double>) remainNodes.clone();
                }
            }
        }
        double addPhoromone = (maxFitness - iterationBestFitness) / maxFitness;
        iterationBest.forEach(e -> {
            String transactionString = Apriori.idArrayString(e);
            double pheoromone = pheoromoneTable.get(transactionString);
            pheoromoneTable.put(transactionString, pheoromone + addPhoromone);
        });
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("h", false, "Lists Short Help");
        options.addOption("t", true, "Transaction Dataset File");
        options.addOption("s", true, "Sensitive Info File");
        options.addOption("m", true, "Minimum Support");
        options.addOption("i", true, "Maximum Iteration");
        options.addOption("p", true, "Population Size");
        options.addOption("a", true, "Alpha Value");
        options.addOption("b", true, "Beta Value");
        options.addOption("g", true, "Gamma Value");
        options.addOption("e", true, "Maximum Selection Times");
        options.addOption("o", true, "Output Transaction Dataset");
        CommandLineParser parser = new DefaultParser();
        String transactionFileName = new String();
        String sensitiveFileName = new String();
        double minSup = 0.0;
        int maxIteration = 0;
        int populationSize = 0;
        double alpha = 0.0, beta = 0.0, gamma = 0.0;
        double maxSelectionTimes = 1.0;
        String outputFileName = new String();
        boolean recordProject = false;
        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(150);
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                hf.printHelp("java -jar ACS2DT.jar", options, true);
                exit(1);
            }

            if (cmd.hasOption("t")) {
                transactionFileName = cmd.getOptionValue("t");
                System.out.println("The transaction file is " + transactionFileName);
            } else {
                hf.printHelp("java -jar ACS2DT.jar", options, true);
                exit(1);
            }

            if (cmd.hasOption("s")) {
                sensitiveFileName = cmd.getOptionValue("s");
                System.out.println("The sensitive info file is " + sensitiveFileName);
            } else {
                hf.printHelp("java -jar ACS2DT.jar", options, true);
                exit(1);
            }

            if (cmd.hasOption("m")) {
                minSup = Double.parseDouble(cmd.getOptionValue("m"));
                System.out.println("The minimum support is " + minSup);
            } else {
                hf.printHelp("java -jar ACS2DT.jar", options, true);
                exit(1);
            }

            if (cmd.hasOption("i")) {
                maxIteration = Integer.parseInt(cmd.getOptionValue("i"));
                System.out.println("The maximum iteration is " + maxIteration);
            } else {
                hf.printHelp("java -jar ACS2DT.jar", options, true);
                exit(1);
            }

            if (cmd.hasOption("p")) {
                populationSize = Integer.parseInt(cmd.getOptionValue("p"));
                System.out.println("The population size is " + populationSize);
            } else {
                hf.printHelp("java -jar ACS2DT.jar", options, true);
                exit(1);
            }

            if (cmd.hasOption("a")) {
                alpha = Double.parseDouble(cmd.getOptionValue("a"));
                System.out.println("The value of alpha is " + alpha);
            } else {
                hf.printHelp("java -jar ACS2DT.jar", options, true);
                exit(1);
            }

            if (cmd.hasOption("b")) {
                beta = Double.parseDouble(cmd.getOptionValue("b"));
                System.out.println("The value of beta is " + beta);
            } else {
                hf.printHelp("java -jar ACS2DT.jar", options, true);
                exit(1);
            }

            if (cmd.hasOption("g")) {
                gamma = Double.parseDouble(cmd.getOptionValue("b"));
                System.out.println("The value of gamma is " + gamma);
            } else {
                hf.printHelp("java -jar ACS2DT.jar", options, true);
                exit(1);
            }

            if (cmd.hasOption("e")) {
                maxSelectionTimes = Double.parseDouble(cmd.getOptionValue("e"));
                System.out.println("The maximum selection times is " + maxSelectionTimes);
            }

            if (cmd.hasOption("o")) {
                outputFileName = cmd.getOptionValue("o");
                System.out.println("The output deleted transactions file is " + outputFileName);
                recordProject = true;
            }

        } catch (MissingArgumentException e) {
            System.out.println("Option: '" + e.getOption().getOpt() + "' requires value.");
            hf.printHelp("java -jar ACS2DT.jar", options, true);
            exit(1);
        } catch (ParseException e) {
            hf.printHelp("java -jar ACS2DT.jar", options, true);
            exit(1);
        }

        ACS2DT ants = new ACS2DT();
        Apriori apriori = new Apriori(transactionFileName);
        ants.populationSize = populationSize;
        ants.minSup = minSup;
        ants.alpha = alpha;
        ants.beta = beta;
        ants.gamma = gamma;
        ants.readSensitiveInformation(sensitiveFileName);
        ants.maxSelectionTimes = maxSelectionTimes;
        apriori.maxSelectionTimes = maxSelectionTimes;
        apriori.setMinSup(minSup);
        apriori.calculateMaxDelete(ants.sensitive);
        apriori.run();
        ants.init(apriori);
        System.out.println("The minimal size is :" + (apriori.getTransactions().size() - ants.maxSelection));
        for (int i = 0; i < maxIteration; ++i) {
            ants.runIteration(recordProject);
        }
        System.out.println("\nfitness\tF-T-H\tN-T-H\tN-T-G\tsize");
        System.out.print(ants.globalBestFitness + "\t");
        System.out.print(ants.fth + "\t");
        System.out.print(ants.nth + "\t");
        System.out.print(ants.ntg + "\t");
        System.out.println(apriori.transactionsCount() - ants.globalBest.size() + "\n");

        if (recordProject) {
            OutputStream fileStream = null;

            try {
                fileStream = new FileOutputStream(outputFileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                OutputStreamWriter writer = new OutputStreamWriter(fileStream, "UTF-8");
                ants.outputDeletedTransactions.entrySet().forEach(e -> {
                    try {
                        writer.write(e.getKey() + "\n");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
    }
}
