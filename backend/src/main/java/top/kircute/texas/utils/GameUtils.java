package top.kircute.texas.utils;

import top.kircute.texas.pojo.HandTypeVO;
import top.kircute.texas.pojo.dto.SummaryDTO;

import java.util.*;

public class GameUtils {
    public static final int HAND_TYPE_FLAG = 0xF00000;
    public static final int TYPE_HIGH_CARD = 0;
    public static final int TYPE_ONE_PAIR = 0x100000;
    public static final int TYPE_TWO_PAIR = 0x200000;
    public static final int TYPE_THREE_KIND = 0x300000;
    public static final int TYPE_STRAIGHT = 0x400000;
    public static final int TYPE_FLUSH = 0x500000;
    public static final int TYPE_FULL_HOUSE = 0x600000;
    public static final int TYPE_FOUR_KIND = 0x700000;
    public static final int TYPE_STRAIGHT_FLUSH = 0x800000;

    public static int getRank(int c) {
        return c % 13;
    }

    public static int getRankSize(int c) {
        int r = getRank(c);
        return r == 0 ? 13 : r;
    }

    public static int getSuit(int c) {
        return c / 13;
    }

    private static int _testFlush(ArrayList<SummaryDTO.Card> cards, ArrayList<SummaryDTO.Card> temp) {
        int[] suitCount = new int[4];
        Arrays.fill(suitCount, 0);
        for (int i = 0; i < 7; i++) suitCount[getSuit(cards.get(i).getCard())]++;
        for (int suit = 0; suit < 4; suit++) {
            if (suitCount[suit] < 5) continue;

            // test straight flush
            int aIdx = -1, straightCount = 0, prevCard = 0;
            for (int i = 0; i < 7; i++) {
                if (getSuit(cards.get(i).getCard()) != suit) continue;
                if (getRank(cards.get(i).getCard()) == 0) aIdx = i;
                if (straightCount == 0 || getRankSize(prevCard) - getRankSize(cards.get(i).getCard()) == 1) {
                    temp.set(straightCount++, cards.get(i));
                } else {
                    temp.set(0, cards.get(i));
                    straightCount = 1;
                }
                prevCard = cards.get(i).getCard();
                if (straightCount == 5) break;
            }
            if (straightCount == 5) {
                return TYPE_STRAIGHT_FLUSH | getRank(prevCard);
            } else if (straightCount == 4 && getRank(prevCard) == 1 && aIdx >= 0) {  // 5 4 3 2 A
                temp.set(4, cards.get(aIdx));
                return TYPE_STRAIGHT_FLUSH;
            }

            int newSize = TYPE_FLUSH;
            int tempIdx = 0;
            for (int i = 0; tempIdx < 5; i++) {
                if (getSuit(cards.get(i).getCard()) != suit) continue;
                temp.set(tempIdx, cards.get(i));
                newSize |= getRankSize(cards.get(i).getCard()) << (16 - tempIdx * 4);
                tempIdx++;
            }
            return newSize;
        }
        return 0;
    }

    private static int _testXOfAKind(ArrayList<SummaryDTO.Card> cards, ArrayList<SummaryDTO.Card> temp) {
        ArrayList<ArrayList<ArrayList<SummaryDTO.Card>>> counter = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) counter.add(new ArrayList<>(7));
        int rank = 0;
        ArrayList<SummaryDTO.Card> cardWithSameRank = new ArrayList<>(4);
        for (int i = 0; i < 7; i++) {
            if (cardWithSameRank.isEmpty()) {
                rank = getRankSize(cards.get(i).getCard());
                cardWithSameRank.add(cards.get(i));
            } else if (rank != getRankSize(cards.get(i).getCard())) {
                counter.get(cardWithSameRank.size() - 1).add(cardWithSameRank);
                rank = getRankSize(cards.get(i).getCard());
                cardWithSameRank = new ArrayList<>(4);
                cardWithSameRank.add(cards.get(i));
            } else cardWithSameRank.add(cards.get(i));
        }
        counter.get(cardWithSameRank.size() - 1).add(cardWithSameRank);

        // test four of a kind
        if (!counter.get(3).isEmpty()) {
            SummaryDTO.Card maxSingle = null;
            for (int i = 3; i >= 0; i--) {
                int j = i == 3 ? 1 : 0;
                if (counter.get(i).size() <= j) continue;
                SummaryDTO.Card c = counter.get(i).get(j).get(0);
                if (maxSingle == null) maxSingle = c;
                else maxSingle = getRankSize(maxSingle.getCard()) < getRankSize(c.getCard()) ? c : maxSingle;
            }
            assert maxSingle != null;
            for (int i = 0; i < 4; i++) temp.set(i, counter.get(3).get(0).get(i));
            temp.set(4, maxSingle);
            int fourKindRank = getRankSize(counter.get(3).get(0).get(0).getCard());
            int singleRank = getRankSize(maxSingle.getCard());
            return TYPE_FOUR_KIND | (fourKindRank << 16) | singleRank;
        }

        // test three of a kind and full house
        if (!counter.get(2).isEmpty()) {
            for (int i = 0; i < 3; i++) temp.set(i, counter.get(2).get(0).get(i));
            int threeKindRank = getRankSize(counter.get(2).get(0).get(0).getCard());
            if (!counter.get(1).isEmpty()) {  // 3 & 2 full house
                int twoKindRank = counter.get(1).isEmpty() ? 0 : getRankSize(counter.get(1).get(0).get(0).getCard());
                for (int i = 0; i < 2; i++) temp.set(i + 3, counter.get(1).get(0).get(i));
                return TYPE_FULL_HOUSE | (threeKindRank << 16) | (twoKindRank << 12);
            } else if (counter.get(2).size() > 1) {  // 3 & 3 full house
                int anotherThreeKindRank = counter.get(2).size() > 1 ? getRankSize(counter.get(2).get(1).get(0).getCard()) : 0;
                for (int i = 0; i < 2; i++) temp.set(i + 3, counter.get(2).get(1).get(i));
                return TYPE_FULL_HOUSE | (threeKindRank << 16) | (anotherThreeKindRank << 12);
            } else {  // three of a kind
                SummaryDTO.Card firstMaxSingle = null;
                SummaryDTO.Card secondMaxSingle = null;
                for (int i = 2; i >= 0; i -= 2) {
                    for (int j = (i == 2 ? 1 : 0); j < counter.get(i).size(); j++) {
                        SummaryDTO.Card c = counter.get(i).get(j).get(0);
                        if (firstMaxSingle == null) firstMaxSingle = c;
                        else if (getRankSize(firstMaxSingle.getCard()) < getRankSize(c.getCard())) {
                            secondMaxSingle = firstMaxSingle;
                            firstMaxSingle = c;
                        }
                        else if (secondMaxSingle == null) secondMaxSingle = c;
                        else if (getRankSize(secondMaxSingle.getCard()) < getRankSize(c.getCard())) secondMaxSingle = c;
                    }
                }
                assert firstMaxSingle != null && secondMaxSingle != null;
                temp.set(3, firstMaxSingle);
                temp.set(4, secondMaxSingle);
                int firstMaxSingleRank = getRankSize(firstMaxSingle.getCard());
                int secondMaxSingleRank = getRankSize(secondMaxSingle.getCard());
                return TYPE_THREE_KIND | (threeKindRank << 16) | (firstMaxSingleRank << 4) | secondMaxSingleRank;
            }
        }

        // test pair
        if (!counter.get(1).isEmpty()) {
            for (int i = 0; i < 2; i++) temp.set(i, counter.get(1).get(0).get(i));
            int firstPairRank = getRankSize(counter.get(1).get(0).get(0).getCard());
            if (counter.get(1).size() > 1) {  // two pair
                for (int i = 0; i < 2; i++) temp.set(i + 2, counter.get(1).get(1).get(i));
                int secondPairRank = getRankSize(counter.get(1).get(1).get(0).getCard());
                SummaryDTO.Card maxSingle = counter.get(0).get(0).get(0);
                int singleRank = getRankSize(maxSingle.getCard());
                if (counter.get(1).size() > 2) {
                    SummaryDTO.Card thirdPair = counter.get(1).get(2).get(0);
                    int thirdPairRank = getRankSize(thirdPair.getCard());
                    if (thirdPairRank > singleRank) {
                        singleRank = thirdPairRank;
                        maxSingle = thirdPair;
                    }
                }
                temp.set(4, maxSingle);
                return TYPE_TWO_PAIR | (firstPairRank << 16) | (secondPairRank << 12) | singleRank;
            } else {  // one pair
                int size = TYPE_ONE_PAIR | (firstPairRank << 16);
                for (int i = 0; i < 3; i++) {
                    SummaryDTO.Card single = counter.get(0).get(i).get(0);
                    size |= getRankSize(single.getCard()) << (8 - i * 4);
                    temp.set(i + 2, single);
                }
                return size;
            }
        }

        return 0;
    }

    private static int _testStraight(ArrayList<SummaryDTO.Card> cards, ArrayList<SummaryDTO.Card> temp) {
        int aIdx = -1, straightCount = 0, prevCardRank = 0;
        for (int i = 0; i < 7; i++) {
            if (aIdx < 0 && getRank(cards.get(i).getCard()) == 0) aIdx = i;
            if (straightCount == 0 || prevCardRank - getRankSize(cards.get(i).getCard()) == 1) {
                temp.set(straightCount++, cards.get(i));
            }
            else if (prevCardRank - getRankSize(cards.get(i).getCard()) == 0) continue;
            else {
                temp.set(0, cards.get(i));
                straightCount = 1;
            }
            prevCardRank = getRankSize(cards.get(i).getCard());
            if (straightCount == 5) break;
        }
        if (straightCount == 5) {
            return TYPE_STRAIGHT | prevCardRank;
        } else if (straightCount == 4 && prevCardRank == 1 && aIdx >= 0) {  // 5 4 3 2 A
            temp.set(4, cards.get(aIdx));
            return TYPE_STRAIGHT;
        }
        return 0;
    }

    public static HandTypeVO analyseHandType(List<Integer> boardCards, int card1, int card2) {
        ArrayList<SummaryDTO.Card> cards = new ArrayList<>(7);
        for (int i = 0; i < 5; i++) cards.add(new SummaryDTO.Card(boardCards.get(i), false));
        cards.add(new SummaryDTO.Card(card1, true));
        cards.add(new SummaryDTO.Card(card2, true));
        cards.sort((o1, o2) -> Integer.compare(getRankSize(o2.getCard()), getRankSize(o1.getCard())));
        int size = TYPE_HIGH_CARD;
        ArrayList<SummaryDTO.Card> ret = new ArrayList<>(5);
        ArrayList<SummaryDTO.Card> temp = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            temp.add(cards.get(i));
            ret.add(cards.get(i));
            size |= getRankSize(cards.get(i).getCard()) << (16 - i * 4);
        }

        // Test flush and straight flush
        int testFlush = _testFlush(cards, temp);
        if (testFlush > size) {
            size = testFlush;
            ArrayList<SummaryDTO.Card> swap = ret;
            ret = temp;
            temp = swap;
        }
        if (size >= TYPE_STRAIGHT_FLUSH) return new HandTypeVO(ret, size);

        // Test x of a kind, full house and two pair
        int xOfAKind = _testXOfAKind(cards, temp);
        if (xOfAKind > size) {
            size = xOfAKind;
            ArrayList<SummaryDTO.Card> swap = ret;
            ret = temp;
            temp = swap;
        }
        if (size >= TYPE_FLUSH) return new HandTypeVO(ret, size);

        // Test straight
        int straight = _testStraight(cards, temp);
        if (straight > size) {
            size = straight;
            ret = temp;
        }
        return new HandTypeVO(ret, size);
    }

    public static String getHandTypeName(int size) {
        switch (size & HAND_TYPE_FLAG) {
            case TYPE_ONE_PAIR: return "ONE PAIR";
            case TYPE_TWO_PAIR: return "TWO PAIR";
            case TYPE_THREE_KIND: return "THREE OF A KIND";
            case TYPE_STRAIGHT: return "STRAIGHT";
            case TYPE_FLUSH: return "FLUSH";
            case TYPE_FULL_HOUSE: return "FULL HOUSE";
            case TYPE_FOUR_KIND: return "FOUR OF A KIND";
            case TYPE_STRAIGHT_FLUSH: return (size & 0xF) == 0x9 ? "ROYAL FLUSH" : "STRAIGHT FLUSH";
            default: return "HIGH CARD";
        }
    }
}
