package top.kircute.texas.utils.game;

import org.junit.Test;
import top.kircute.texas.pojo.HandTypeVO;
import top.kircute.texas.utils.GameUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameUtilsTest {
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final char[] suits = {'♠', '♥', '♣', '♦'};
    private static final String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};

    private ArrayList<Integer> shuffle() {
        ArrayList<Integer> ret = new ArrayList<>(52);
        for (int i = 0; i < 52; i++) ret.add(i);
        Collections.shuffle(ret, RANDOM);
        return ret;
    }

    private void print(List<Integer> boardCards, int hole1, int hole2, HandTypeVO result) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < 5; j++) {
            sb.append(suits[GameUtils.getSuit(boardCards.get(j))]);
            sb.append(ranks[GameUtils.getRank(boardCards.get(j))]);
            sb.append(' ');
        }
        sb.append("- ");
        sb.append(suits[GameUtils.getSuit(hole1)]);
        sb.append(ranks[GameUtils.getRank(hole1)]);
        sb.append(' ');
        sb.append(suits[GameUtils.getSuit(hole2)]);
        sb.append(ranks[GameUtils.getRank(hole2)]);
        sb.append(" | ");
        for (int j = 0; j < 5; j++) {
            sb.append(result.getCards().get(j).getHole() ? '+' : '-');
            sb.append(suits[GameUtils.getSuit(result.getCards().get(j).getCard())]);
            sb.append(ranks[GameUtils.getRank(result.getCards().get(j).getCard())]);
            sb.append(' ');
        }
        sb.append(GameUtils.getHandTypeName(result.getSize()));
        System.out.println(sb);
    }

    @Test
    public void randomTest() {
        for (int i = 0; i < 1000; i++) {
            ArrayList<Integer> cards = shuffle();
            List<Integer> boardCards = cards.subList(0, 5);
            int hole1 = cards.get(5), hole2 = cards.get(6);
            HandTypeVO result = GameUtils.analyseHandType(boardCards, hole1, hole2);
            print(boardCards, hole1, hole2, result);
        }
    }

    @Test
    public void specialCasesTest() {
        ArrayList<Integer> boardCards = new ArrayList<>(5);
        boardCards.add(0);
        boardCards.add(1);
        boardCards.add(11);
        boardCards.add(12);
        boardCards.add(4);
        HandTypeVO result0 = GameUtils.analyseHandType(boardCards, 3, 2);
        print(boardCards, 3, 2, result0);
        HandTypeVO result1 = GameUtils.analyseHandType(boardCards, 10, 9);
        print(boardCards, 10, 9, result1);
        HandTypeVO result2 = GameUtils.analyseHandType(boardCards, 13, 2);
        print(boardCards, 13, 2, result2);
        boardCards.set(2, 11 + 13);
        boardCards.set(3, 12 + 13);
        HandTypeVO result3 = GameUtils.analyseHandType(boardCards, 16, 2);
        print(boardCards, 16, 2, result3);
        HandTypeVO result4 = GameUtils.analyseHandType(boardCards, 13, 2);
        print(boardCards, 13, 2, result4);
        HandTypeVO result5 = GameUtils.analyseHandType(boardCards, 23, 9);
        print(boardCards, 23, 9, result5);
        boardCards.set(1, 13);
        HandTypeVO result6 = GameUtils.analyseHandType(boardCards, 13 * 2, 13 * 3);
        print(boardCards, 13 * 2, 13 * 3, result6);
    }

    @Test
    public void testThreeAndThreeFullHouse() {
        ArrayList<Integer> boardCards = new ArrayList<>(5);
        boardCards.add(11);
        boardCards.add(11 + 13);
        boardCards.add(11 + 13 * 2);
        boardCards.add(7 + 13);
        boardCards.add(7 + 13 * 2);
        HandTypeVO result0 = GameUtils.analyseHandType(boardCards, 7 , 5);
        print(boardCards, 7, 5, result0);
        boardCards.set(4, 5 + 13);
        HandTypeVO result1 = GameUtils.analyseHandType(boardCards, 7 , 5);
        print(boardCards, 7, 5, result1);
    }
}
