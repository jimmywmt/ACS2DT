package tw.idv.wmt35;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by wumingtai on 2016/8/2.
 */
public class Apriori {
    private ArrayList<int[]> transactions;
    private HashMap<Integer, LinkedList<Integer>> relatedTransactions;
    private HashMap<String, Integer> frequentItemsetsCount;
    private ArrayList<LinkedList<int[]>> frequentItemsets, preLargeFrequentItemsets;

    private double minSup;
    public int maxDelete;
    public int minSupCount, preLargeCount;
    public double maxSelectionTimes;

    public Apriori() {
        transactions = new ArrayList<>();
        relatedTransactions = new HashMap<>();
        frequentItemsetsCount = new HashMap<>();
        frequentItemsets = new ArrayList<>();
        preLargeFrequentItemsets = new ArrayList<>();
        minSup = 0.0;
        maxSelectionTimes = 1;
    }

    public Apriori(String inputFileName) {
        this();
        readFile(inputFileName);
    }

    public void readFile(String inputFileName) {
        try (Stream<String> stream = Files.lines(Paths.get(inputFileName))) {
            transactions.clear();
            relatedTransactions.clear();
            stream.forEach(this::readLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readLine(String line) {
        String[] stringArray = line.split(" ");
        int[] intArray = new int[stringArray.length];

        for (int i = 0; i < stringArray.length; ++i) {
            int itemID = Integer.parseInt(stringArray[i]);
            intArray[i] = itemID;
            Integer itemIDInteger = itemID;
            LinkedList<Integer> transactionList = relatedTransactions.get(itemIDInteger);
            transactionList = transactionList != null ? transactionList : new LinkedList<>();
            transactionList.add(transactions.size());
            relatedTransactions.put(itemIDInteger, transactionList);
        }
        transactions.add(intArray);
    }

    public void setMinSup(double minSup) {
        this.minSup = minSup;
        minSupCount = (int) Math.ceil(transactions.size() * minSup);
    }

    public void calculateMaxDelete(LinkedList<int[]> hiddenItemsets) {
        int maxSensitiveCount = 0;
        ListIterator it = hiddenItemsets.listIterator();
        while (it.hasNext()) {
            int[] itemset = (int[]) it.next();
            int count = itemsetCount(itemset);
            if (count > maxSensitiveCount)
                maxSensitiveCount = count;
        }
        maxDelete = (int) ((Math.floor((maxSensitiveCount - minSupCount) / (1 - minSup)) + 1) * maxSelectionTimes);
        preLargeCount = (int) Math.ceil((transactions.size() - maxDelete) * minSup);
    }

    public HashMap<String, Integer> getFrequentItemsetsCount() { return frequentItemsetsCount; }

    public ArrayList<int[]> getTransactions() { return transactions; }

    public void run() {
        if (transactions.size() != 0 && minSup != 0.0) {
            frequentItemsetsCount.clear();
            frequentItemsets.clear();
            preLargeFrequentItemsets.clear();
            LinkedList<int[]> largeItemsets, preLargeItemsets, totalLargeItemsets;

            //find large-1 itemsets
            largeItemsets = new LinkedList<>();
            preLargeItemsets = new LinkedList<>();
            totalLargeItemsets = new LinkedList<>();

            Iterator it = relatedTransactions.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry pair = (HashMap.Entry) it.next();
                if (((LinkedList<Integer>) pair.getValue()).size() >= preLargeCount) {
                    int[] tempArray = new int[]{(Integer) pair.getKey()};
                    totalLargeItemsets.add(tempArray);
                    if (((LinkedList<Integer>) pair.getValue()).size() >= minSupCount)
                        largeItemsets.add(tempArray);
                    else
                        preLargeItemsets.add(tempArray);

                    frequentItemsetsCount.put((pair.getKey()).toString(), ((LinkedList<Integer>) pair.getValue()).size());
                }
            }
            if (largeItemsets.size() != 0)
                frequentItemsets.add(largeItemsets);
            if (preLargeItemsets.size() != 0)
                preLargeFrequentItemsets.add(preLargeItemsets);

            while ((totalLargeItemsets = nextLevelLargeItemsets(totalLargeItemsets)) != null) {
            }
        }
    }

    public void printFrequentItemsets() {
        if (frequentItemsetsCount.size() != 0) {
            System.out.println("===Frequent Itemsets=== (Min Support Count: " + minSupCount + ")");
            frequentItemsets.forEach(e -> e.forEach(f -> System.out.println(Arrays.toString(f) + " : " + getItemsetCount(f))));
        }

        if (preLargeFrequentItemsets.size() != 0) {
            System.out.println("\n===Pre Large Frequent Itemsets=== (Pre Large Count: " + preLargeCount + ")");
            preLargeFrequentItemsets.forEach(e -> e.forEach(f -> System.out.println(Arrays.toString(f) + " : " + getItemsetCount(f))));
        }
    }

    private LinkedList<int[]> nextLevelLargeItemsets(LinkedList<int[]> totalLargeItemsets) {
        LinkedList<int[]> newLargeItemsets, newPreLargeItemsets, newTotalLargeItemsets;
        newLargeItemsets = new LinkedList<>();
        newPreLargeItemsets = new LinkedList<>();
        newTotalLargeItemsets = new LinkedList<>();
        if (totalLargeItemsets.size() > 1) {
            ListIterator i = totalLargeItemsets.listIterator();
            LinkedList<int[]> estimatedItemsets = new LinkedList<>();
            HashMap<String, Integer> candidateItemsets = new HashMap<>();
            while (i.hasNext()) {
                int[] oneItemset = (int[]) i.next();
                ListIterator j = totalLargeItemsets.listIterator(i.nextIndex());
                while (j.hasNext()) {
                    int[] twoItemset = (int[]) j.next();
                    int[] threeItemset = mergeItemsets(oneItemset, twoItemset);

                    if (threeItemset != null) {
                        if (needCheck(estimatedItemsets, threeItemset)) {
                            String itemsetString = idArrayString(threeItemset);
                            Integer countInteger = candidateItemsets.get(itemsetString);
                            if (countInteger != null)
                                countInteger = countInteger + 1;
                            else
                                countInteger = 1;

                            if (countInteger.intValue() == oneItemset.length) {
                                int count = itemsetCount(threeItemset);
                                if (count >= preLargeCount) {
                                    newTotalLargeItemsets.add(threeItemset);
                                    if (count >= minSupCount)
                                        newLargeItemsets.add(threeItemset);
                                    else
                                        newPreLargeItemsets.add(threeItemset);

                                    frequentItemsetsCount.put(itemsetString, count);
                                }
                                continue;
                            }

                            candidateItemsets.put(itemsetString, countInteger);
                        }
                    }
                }
                estimatedItemsets.add(oneItemset);
            }
        }
        if (newPreLargeItemsets.size() != 0)
            preLargeFrequentItemsets.add(newPreLargeItemsets);

        if (newLargeItemsets.size() != 0)
            frequentItemsets.add(newLargeItemsets);

        if (newTotalLargeItemsets.size() == 0)
            return null;
        else
            return newTotalLargeItemsets;
    }

    private boolean needCheck(LinkedList<int[]> estimatedItemsets, int[] checkItemsets) {
        boolean pass;
        for (int[] eItemset : estimatedItemsets) {
            pass = false;
            for (int eItem : eItemset) {
                if (Arrays.binarySearch(checkItemsets, eItem) < 0) {
                    pass = true;
                    break;
                }
            }
            if (pass == false)
                return false;
        }
        return true;
    }

    private int[] mergeItemsets(int[] one, int[] two) {
        int i = 0, j = 0, k = 0, d = 0;
        int length = one.length;
        int[] newItemset = new int[length + 1];
        while (i < length && j < length) {
            if (one[i] == two[j]) {
                newItemset[k] = one[i];
                ++i;
                ++j;
            } else if (one[i] < two[j]) {
                ++d;
                if (d < 3) {
                    newItemset[k] = one[i];
                    ++i;
                } else
                    break;
            } else if (one[i] > two[j]) {
                ++d;
                if (d < 3) {
                    newItemset[k] = two[j];
                    ++j;
                } else
                    break;
            }
            ++k;
        }

        if (d < 3) {
            if (d == 1) {
                if (i < length)
                    newItemset[k] = one[i];
                else
                    newItemset[k] = two[j];

                return newItemset;
            }
            if (i == length && j == length)
                return newItemset;
            else
                return null;
        } else {
            return null;
        }
    }

    public int itemsetCount(int[] itemset) {
        LinkedList<Integer> firstTransactions = relatedTransactions.get(itemset[0]);
        if (itemset.length == 1) {
            return firstTransactions.size();
        }
        LinkedList<Integer> itemsetTransactions = new LinkedList<>();
        for (int i = 1; i < itemset.length; ++i) {
            itemsetTransactions = new LinkedList<>();
            LinkedList<Integer> secondTransactions = relatedTransactions.get(itemset[i]);
            Iterator it1 = firstTransactions.listIterator();
            Iterator it2 = secondTransactions.listIterator();

            if (it1.hasNext() && it2.hasNext()) {
                Integer oneID = (Integer) it1.next();
                Integer twoID = (Integer) it2.next();

                while (true) {
                    int compare = oneID.compareTo(twoID);

                    if (compare == 0) {
                        itemsetTransactions.add(oneID);
                        if (it1.hasNext() && it2.hasNext()) {
                            oneID = (Integer) it1.next();
                            twoID = (Integer) it2.next();
                        } else
                            break;
                    } else if (compare < 0) {
                        if (it1.hasNext())
                            oneID = (Integer) it1.next();
                        else
                            break;
                    } else {
                        if (it2.hasNext())
                            twoID = (Integer) it2.next();
                        else
                            break;
                    }
                }
            }
            firstTransactions = itemsetTransactions;
        }

        return itemsetTransactions.size();
    }

    public static String idArrayString(int[] itemset) { return Arrays.stream(itemset).mapToObj(Integer::toString).collect(Collectors.joining(",")); }

    public int getItemsetCount(int[] itemset) { return frequentItemsetsCount.get(idArrayString(itemset)); }

    public ArrayList<LinkedList<int[]>> getFrequentItemsets() { return frequentItemsets; }

    public ArrayList<LinkedList<int[]>> getPreLargeFrequentItemsets() { return preLargeFrequentItemsets; }

    public int getMinSupCount() { return minSupCount; }

    public int getPreLargeCount() { return preLargeCount; }

    public int transactionsCount() { return transactions.size(); }
}
