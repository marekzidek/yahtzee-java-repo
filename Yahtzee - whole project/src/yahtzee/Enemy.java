package yahtzee;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * An enemy object which can play the game and also can decide for keeping dice and taking down combinations
 *
 * @author Marek Zidek
 */
public class Enemy {


    private int rollCount = 0;
    private List<Die> dices = new ArrayList<>();
    private List<Integer> diceVals;
    private ScoreTable scoreTable;
    private DicePad dicepad;
    private final int playerIndex = 1;
    private int savedDie;
    private HashMap<Integer,Boolean> map;
    private static BufferedWriter log;
    public static boolean allRolled;

    public Enemy() {
        dicepad = Main.getmain().getDicepad();
        scoreTable = Main.getmain().getScoreTable();
        diceVals = new ArrayList<>();
        try {
            if(log == null)
                log = new BufferedWriter(new FileWriter("YahtzeeEnemyLog.txt"));
        } catch (IOException e) {
            System.out.println();
        }
    }

    
    /**
     * with this method, enemy can do basic actions for controlling the game
     */
    public void play() {

        allRolled = false;

        scoreTable.table.repaint();

        ToNotKeepAll();
        try {
            log.write("-------------------------------------------------------------");
            log.newLine();
        } catch(IOException e) {
            System.out.println("Nastala IOException");
        }

        for(rollCount = 1; rollCount <= 3; rollCount++) {

            dices = Main.getmain().RollDice();

            try {
                while(!allRolled) {
                    Thread.sleep(200);
                }

                diceVals.clear();
                for (int i = 0; i < dices.size(); i++) {

                    diceVals.add(dices.get(i).getVal());
                }

                allRolled = false;

                Thread.sleep(1000);
                ToNotKeepAll();

                try {
                    log.write("Hod " + rollCount + ". Padlo mu: " + diceVals.get(0) + ", " + diceVals.get(1) + ", " + diceVals.get(2) + ", " + diceVals.get(3) + ", " + diceVals.get(4));
                    log.newLine();

                    EvaluateBest();

                    log.flush();
                } catch (IOException e) {
                    System.out.println("Nastala IOException");
                }
                Thread.sleep(1000);

            } catch (InterruptedException e){
                System.out.println("Interrupt");
            }


        }
    }

    /**
     * Actual decision three. It's divided into two sections for the part, when a player hasn't used all his available rolls and can decide
     * which dice to keep, and a part when the player decides what combination to take down. 
     * 
     * It starts with the best and obvious choices and goes down to low combinations eg. the last choice is it keeps whatever is not taken down yet.
     * Every part of decision is commented in code.
     * 
     */
    public void EvaluateBest() throws IOException{

    	// rollCount < 3 means it can decide which to keep
        if(rollCount < 3){
        	
            //start from the best combinations
            //where it makes sense to take it down and to not roll anymore
        	
        	//yahtzee
            if(!scoreTable.getFilledCells()[playerIndex][11] &&
                    dicepad.getCurrentCombinations().get(11) != 0){
                scoreTable.FillCombination(11+3,dicepad.getCurrentCombinations().get(11),playerIndex);
                log.write("Zapsal si Yahtzee"); log.newLine();
                rollCount = 3;
                return;
            }

            //large straight
            if(!scoreTable.getFilledCells()[playerIndex][10] &&
                    dicepad.getCurrentCombinations().get(10) != 0){
                scoreTable.FillCombination(10+3,dicepad.getCurrentCombinations().get(10),playerIndex);
                log.write("Zapsal si Velkou postupku"); log.newLine();
                rollCount = 3;
                return;
            }

            //full house
            if(!scoreTable.getFilledCells()[playerIndex][8] &&
                    dicepad.getCurrentCombinations().get(8) != 0){
                scoreTable.FillCombination(8+3,dicepad.getCurrentCombinations().get(8),playerIndex);
                log.write("Zapsal si Full house"); log.newLine();
                rollCount = 3;
                return;
            }

            //combinations that are great, but it makes sense to roll some more
            //4 of kind
            if (CombinationsProcedures.OfKind((ArrayList<Integer>)diceVals,4) != 0) {
                for (int i = 0; i < 6; i++) {
                    if(dicepad.getCurrentCombinations().get(i) == 4*(i+1) && (
                            !scoreTable.getFilledCells()[playerIndex][i] ||
                            !scoreTable.getFilledCells()[playerIndex][7] || !scoreTable.getFilledCells()[playerIndex][11]
                    )) {
                        for (int j = 0; j < 5; j++) {
                            if(dices.get(j).getVal() == (i+1)) {
                                dicepad.dieMove(dices.get(j));
                                log.write("Nechal si " + (i+1) + "-ku"); log.newLine();
                            }
                        }
                        return;
                    }
                }
            }

            //small straight
            if (dicepad.getCurrentCombinations().get(9) != 0){
                if(!scoreTable.getFilledCells()[playerIndex][9] && scoreTable.getFilledCells()[playerIndex][10]) {
                    scoreTable.FillCombination(9+3, dicepad.getCurrentCombinations().get(9), playerIndex);
                    rollCount = 3;
                }
                else if(!scoreTable.getFilledCells()[playerIndex][9] || !scoreTable.getFilledCells()[playerIndex][10]) {

                    if(DetermineStraightKeep(1,4)){}
                    else if(DetermineStraightKeep(2,4)){}
                    else DetermineStraightKeep(3,4);
                }
                return;
            }
            
            //3 of kind... it has bigger priority than a possible small straight
            if (CombinationsProcedures.OfKind((ArrayList<Integer>)diceVals,3) != 0) {
                for (int i = 0; i < 6; i++) {
                    if(dicepad.getCurrentCombinations().get(i) == 3*(i+1) && (
                            !scoreTable.getFilledCells()[playerIndex][i] || !scoreTable.getFilledCells()[playerIndex][6] ||
                            !scoreTable.getFilledCells()[playerIndex][7] || !scoreTable.getFilledCells()[playerIndex][11]
                    )) {
                        for (int j = 0; j < 5; j++) {
                            if(dices.get(j).getVal() == (i+1)) {
                                dicepad.dieMove(dices.get(j));
                                log.write("Nechal si " + (i+1) + "-ku"); log.newLine();
                            }
                        }
                        return;
                    }
                }
            }

            //nothing but two of Kind (automatically finds the better one
            if(CombinationsProcedures.OfKind((ArrayList<Integer>)diceVals,2) != 0) {
                //from
                for (int i = 5; i >= 0; i--) {
                    if(dicepad.getCurrentCombinations().get(i) == 2*(i +1) && (
                            !scoreTable.getFilledCells()[playerIndex][i] || !scoreTable.getFilledCells()[playerIndex][6] ||
                                    !scoreTable.getFilledCells()[playerIndex][7] || !scoreTable.getFilledCells()[playerIndex][11]
                    )) {
                        for (int j = 0; j < 5; j++) {
                            if(dices.get(j).getVal() == i+1) {
                                dicepad.dieMove(dices.get(j));
                                log.write("Nechal si " + dices.get(j).getVal() + "-ku"); log.newLine();
                            }
                        }
                        return;
                    }
                }
            }

            //almost small straight
            if (CombinationsProcedures.Straight((ArrayList<Integer>)diceVals,3) != 0 &&
                    (!scoreTable.getFilledCells()[playerIndex][9] || !scoreTable.getFilledCells()[playerIndex][10])) {

                if(DetermineStraightKeep(2,3)) {}
                else if (DetermineStraightKeep(3,3)) {}
                else if(DetermineStraightKeep(1,3)) {}
                else DetermineStraightKeep(4,3);
                
                return;
            }

            //nothing - it keeps whatever is not yet taken down
            
            for (int i = 5; i >= 0; i--) {
                if (dicepad.getCurrentCombinations().get(i) == i + 1 &&
                        (!scoreTable.getFilledCells()[playerIndex][i] || !scoreTable.getFilledCells()[playerIndex][6])) {
                    for (int j = 0; j < dicepad.allDice.size(); j++) {
                        if (dices.get(j).getVal() == i+1) {
                            dicepad.dieMove(dices.get(i));
                            log.write("Nechal si " + (i+1) + "-ku"); log.newLine();
                        }
                    }
                    break;
                }
            }
        }
        //combination named "sance" is kept after all rolls

        //part, where it's decided what do do after it cannot be rolled anymore
        else {
            if(dicepad.getCurrentCombinations().get(11) != 0 && !scoreTable.getFilledCells()[playerIndex][11]) {
                //yahtzee
                scoreTable.FillCombination(11+3,dicepad.getCurrentCombinations().get(11),playerIndex);
                log.write("Zapsal si Yahtzee"); log.newLine();  return;}
            else if(dicepad.getCurrentCombinations().get(10) != 0 && !scoreTable.getFilledCells()[playerIndex][10]) {
                //large straight
                scoreTable.FillCombination(10+3,dicepad.getCurrentCombinations().get(10),playerIndex);
                log.write("Zapsal si Velkou postupku"); log.newLine(); return; }
            else if(dicepad.getCurrentCombinations().get(9) != 0 && !scoreTable.getFilledCells()[playerIndex][9]) {
                //small straight
                scoreTable.FillCombination(9+3,dicepad.getCurrentCombinations().get(9),playerIndex);
                log.write("Zapsal si Malou postupku"); log.newLine(); return; }
            else if(dicepad.getCurrentCombinations().get(8) != 0 && !scoreTable.getFilledCells()[playerIndex][8]) {
                //full house
                scoreTable.FillCombination(8+3,dicepad.getCurrentCombinations().get(8),playerIndex);
                log.write("Zapsal si Full house"); log.newLine(); return; }

            //part when it has 3ofkind or 4ofKind but has to decide wheter to put it into lower part of the table or to "ofKind" combinatios
            //now it priorities 5ts and 6s to 3ofKind or 4ofKind and lower dice to the lower part of table
            else if(dicepad.getCurrentCombinations().get(6) != 0) {
                for (int i = 0; i < 6; i++) {
                    if(dicepad.getCurrentCombinations().get(i) >= (i+1)*3) //so at least three same dice
                        if(i == 4 || i == 5) {
                            if(!scoreTable.getFilledCells()[playerIndex][7] && dicepad.getCurrentCombinations().get(7) != 0) {
                                scoreTable.FillCombination(7 + 3, dicepad.getCurrentCombinations().get(7), playerIndex);
                                log.write("Zapsal si 4 of kind"); log.newLine();
                                return;
                            }
                            else if(!scoreTable.getFilledCells()[playerIndex][6]) {
                                scoreTable.FillCombination(6 + 3, dicepad.getCurrentCombinations().get(6), playerIndex);
                                log.write("Zapsal si 3 of kind"); log.newLine();
                                return;
                            }
                            else if(!scoreTable.getFilledCells()[playerIndex][i]) {
                                scoreTable.FillCombination(i+1,dicepad.getCurrentCombinations().get(i),playerIndex);
                                log.write("Zapsal si " + (i+1) + "-ky"); log.newLine();
                                return;
                            }
                        } else {
                            if(!scoreTable.getFilledCells()[playerIndex][i]) {
                                scoreTable.FillCombination(i + 1, dicepad.getCurrentCombinations().get(i), playerIndex);
                                log.write("Zapsal si " + (i+1) + "-ky"); log.newLine();
                                return;
                            }
                            else if(!scoreTable.getFilledCells()[playerIndex][7] && dicepad.getCurrentCombinations().get(7) != 0) {
                                scoreTable.FillCombination(7 + 3, dicepad.getCurrentCombinations().get(7), playerIndex);
                                log.write("Zapsal si 4 of kind"); log.newLine();
                                return;
                            }
                            else if(!scoreTable.getFilledCells()[playerIndex][6]) {
                                scoreTable.FillCombination(6 + 3, dicepad.getCurrentCombinations().get(6), playerIndex);
                                log.write("Zapsal si 3 of kind"); log.newLine();
                                return;
                            }
                        }
                }

            }

            //then there is "sance", but it is taken down only when beneficial, else it tries to take down lower combinations
            if(dicepad.getCurrentCombinations().get(12) >= 20 && !scoreTable.getFilledCells()[playerIndex][12]) {
                scoreTable.FillCombination(12+3,dicepad.getCurrentCombinations().get(12),playerIndex);
                log.write("Zapsal si sance"); log.newLine(); return;}
            //if only 2ofKind, it's good to take it down
            if(CombinationsProcedures.OfKind((ArrayList<Integer>)diceVals,2) != 0) {
                for (int i = 0; i < 6; i++) {
                    if(!scoreTable.getFilledCells()[playerIndex][i] && dicepad.getCurrentCombinations().get(i) == (i+1)*2) {
                        scoreTable.FillCombination(i+1,dicepad.getCurrentCombinations().get(i),playerIndex);
                        log.write("Zapsal si " + (i+1) + "-ky"); log.newLine();
                        return;
                    }
                }
            }


            boolean tryForChance = false;
            
            //afterall it takes down 0 to the least possible combinations

            for (int i = 11; i >=0 ; i--)
                if(!scoreTable.getFilledCells()[playerIndex][i]) {
                    if( i > 5)
                        scoreTable.FillCombination(i + 3, dicepad.getCurrentCombinations().get(i), playerIndex);
                    else
                        scoreTable.FillCombination(i + 1, dicepad.getCurrentCombinations().get(i), playerIndex);
                    log.write("Nic moc, tak aspon neco nepravdepodobnyho: kombinace s indexem " + (i+1)); log.newLine();
                    tryForChance = true;
                    break;
                }

            if(!tryForChance)
                scoreTable.FillCombination(13, dicepad.getCurrentCombinations().get(12), playerIndex);

        }
    }

    private void ToNotKeepAll() {
        for (int i = 0; i < dicepad.allDice.size(); i++)
            dicepad.allDice.get(i).toKeep = false;
    }

    Boolean[] indexesToKeep = new Boolean[5];

    private Boolean DetermineStraightKeep(int start, int straightCount) throws IOException {
        for (int i = 0; i < 5; i++)
            indexesToKeep[i] = false;

        //this if statement expects the straightCount to be 3 or 4
        assert straightCount == 3 || straightCount == 4;
        if( (straightCount == 3 && diceVals.contains(start) && diceVals.contains(start + 1) && diceVals.contains(start + 2)) ||
            (straightCount == 4 && diceVals.contains(start) && diceVals.contains(start + 1) && diceVals.contains(start + 2) && diceVals.contains(start + 3))){
            for (int i = 0; i < straightCount ; i++) {
                if(diceVals.indexOf(start + i) != -1)
                    indexesToKeep[diceVals.indexOf(start + i)] = true;
            }
            for (int i = 0; i < 5; i++) {
                if(indexesToKeep[i]) {
                    dicepad.dieMove(dices.get(i));
                    log.write("Nechal si " + dices.get(i).getVal() + "-ku"); log.newLine();
                }
            }
            return true;
        }
        return false;
    }
}