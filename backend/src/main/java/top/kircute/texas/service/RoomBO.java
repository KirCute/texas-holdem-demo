package top.kircute.texas.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.socket.WebSocketSession;
import top.kircute.texas.pojo.HandTypeVO;
import top.kircute.texas.pojo.Pair;
import top.kircute.texas.pojo.dto.*;
import top.kircute.texas.utils.GameUtils;

import java.security.SecureRandom;
import java.util.*;

public class RoomBO {
    public static final int PLAYING_STATUS_WAITING = 0;
    public static final int PLAYING_STATUS_PREFLOP = 1;
    public static final int PLAYING_STATUS_FLOP = 2;
    public static final int PLAYING_STATUS_TURN = 3;
    public static final int PLAYING_STATUS_RIVER = 4;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AsyncExecutor asyncExecutor;
    private final ArrayList<GamingPlayerDTO> gamingPlayers;
    private ArrayList<WaitingPlayerDTO> waitingPlayers;
    private final HashMap<String, WebSocketSession> sessions;
    private final ArrayList<Integer> boardCards;
    private final HashSet<String> autoFold;
    private final int initialChip;
    private final int smallBlindBet;
    private int status;
    private int pot;
    private int turn;
    private int lastRaise;
    private boolean preflopFirstCall;

    public RoomBO(AsyncExecutor asyncExecutor, int initialChip, int smallBlindBet) {
        this.asyncExecutor = asyncExecutor;
        gamingPlayers = new ArrayList<>(8);
        waitingPlayers = new ArrayList<>(8);
        sessions = new HashMap<>(8);
        boardCards = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) boardCards.add(0);
        autoFold = new HashSet<>();
        this.initialChip = initialChip;
        this.smallBlindBet = smallBlindBet;
        status = PLAYING_STATUS_WAITING;
        lastRaise = 0;
        pot = 0;
    }

    public RoomStateDTO getState(String playerName) {
        synchronized (this) {
            boolean playing = status != PLAYING_STATUS_WAITING;
            int playerCount = gamingPlayers.size() + waitingPlayers.size();
            String host = (playing ? gamingPlayers.get(0).getPlayer() : waitingPlayers.get(0).getPlayer()).getName();
            return new RoomStateDTO(sessions.containsKey(playerName), playing, playerCount, host);
        }
    }

    public boolean join(String playerName) {
        synchronized (this) {
            if (sessions.containsKey(playerName)) return false;
            autoFold.remove(playerName);
            boolean shouldAddIntoWaitingPlayers = true;
            for (GamingPlayerDTO gamingPlayer : gamingPlayers) {
                if (!gamingPlayer.getPlayer().getName().equals(playerName)) continue;
                shouldAddIntoWaitingPlayers = false;
                break;
            }
            if (shouldAddIntoWaitingPlayers) {
                boolean isHost = gamingPlayers.isEmpty() && waitingPlayers.isEmpty();
                waitingPlayers.add(new WaitingPlayerDTO(new PlayerDTO(playerName, initialChip, 0, isHost), false));
                broadcast();
            }
            sessions.put(playerName, null);
            return true;
        }
    }

    public void connect(String playerName, WebSocketSession session) {
        synchronized (this) {
            sessions.put(playerName, session);
            for (WaitingPlayerDTO player : waitingPlayers) {
                if (!player.getPlayer().getName().equals(playerName)) continue;
                _broadcastTo(player);
                return;
            }
            for (GamingPlayerDTO player : gamingPlayers) {
                if (!player.getPlayer().getName().equals(playerName)) continue;
                _broadcastTo(player);
                return;
            }
        }
    }

    public boolean disconnect(String playerName) {
        synchronized (this) {
            WebSocketSession oldSession = sessions.remove(playerName);
            if (oldSession != null) asyncExecutor.execute(oldSession::close);
            if (sessions.isEmpty()) return false;
            boolean waiting = true;
            if (status != PLAYING_STATUS_WAITING) {
                for (int i = 0; waiting && i < gamingPlayers.size(); i++) {
                    if (!gamingPlayers.get(i).getPlayer().getName().equals(playerName)) continue;
                    waiting = false;
                    if (turn == i) {
                        gamingPlayers.get(i).setLastOperation("Fold");
                        gamingPlayers.get(i).setStatus(GamingPlayerDTO.GAMING_STATUS_FOLD);
                        nextTurn();
                    } else {
                        autoFold.add(playerName);
                    }
                }
            }
            if (!waiting) return true;
            for (int i = 0; i < waitingPlayers.size(); i++) {
                if (!waitingPlayers.get(i).getPlayer().getName().equals(playerName)) continue;
                if (waitingPlayers.get(i).getPlayer().getIsButton()) {
                    waitingPlayers.get((i + 1) % waitingPlayers.size()).getPlayer().setIsButton(true);
                    waitingPlayers.get(i).getPlayer().setIsButton(false);
                }
                waitingPlayers.remove(i);
                broadcast();
                break;
            }
            return true;
        }
    }

    public void fold(String playerName) {
        synchronized (this) {
            if (status == PLAYING_STATUS_WAITING) return;
            if (!gamingPlayers.get(turn).getPlayer().getName().equals(playerName)) return;
            gamingPlayers.get(turn).setLastOperation("Fold");
            gamingPlayers.get(turn).setStatus(GamingPlayerDTO.GAMING_STATUS_FOLD);
            if (preflopFirstCall) {
                lastRaise = turn;
                gamingPlayers.get(turn).setBet(smallBlindBet * 2);
                preflopFirstCall = false;
            }
            nextTurn();
        }
    }

    public void raise(String playerName, int bet) {
        synchronized (this) {
            if (status == PLAYING_STATUS_WAITING) return;
            GamingPlayerDTO player = gamingPlayers.get(turn);
            if (!player.getPlayer().getName().equals(playerName)) return;
            GamingPlayerDTO lastRaisePlayer = gamingPlayers.get(lastRaise);

            int totalBet = bet + player.getBet();
            if (bet >= player.getPlayer().getChips()) {  // ALL IN
                int checkBet = lastRaisePlayer.getBet();
                pot += player.getPlayer().getChips();
                player.getPlayer().setChips(0);
                if (checkBet >= totalBet) {
                    player.setBet(checkBet);
                } else {
                    player.setBet(totalBet);
                    lastRaise = turn;
                }
                player.setLastOperation("All in");
                player.setStatus(GamingPlayerDTO.GAMING_STATUS_ALLIN);
            } else {
                if (totalBet < lastRaisePlayer.getBet()) return;  // INVALID
                // CHECK OR RAISE
                pot += bet;
                player.getPlayer().reduceChips(bet);
                player.setBet(totalBet);
                if (totalBet == lastRaisePlayer.getBet()) {
                    player.setLastOperation(totalBet == 0 ? "Check" : "Call");
                } else {
                    player.setLastOperation((lastRaisePlayer.getBet() == 0 ? "Bet " : "Raise to ") + totalBet);
                    lastRaise = turn;
                }
            }
            if (preflopFirstCall) {
                lastRaise = turn;
                preflopFirstCall = false;
            }
            nextTurn();
        }
    }

    public void call(String playerName) {
        synchronized (this) {
            if (status == PLAYING_STATUS_WAITING) return;
            GamingPlayerDTO player = gamingPlayers.get(turn);
            if (!player.getPlayer().getName().equals(playerName)) return;

            int totalBet = gamingPlayers.get(lastRaise).getBet();
            int bet = totalBet - player.getBet();
            if (bet == 0) {
                player.setLastOperation("Check");
            } else {
                if (bet >= player.getPlayer().getChips()) {  // ALL IN
                    pot += player.getPlayer().getChips();
                    player.getPlayer().setChips(0);
                    player.setBet(totalBet);
                    player.setLastOperation("All in");
                    player.setStatus(GamingPlayerDTO.GAMING_STATUS_ALLIN);
                } else {  // CALL
                    pot += bet;
                    player.getPlayer().reduceChips(bet);
                    player.setBet(totalBet);
                    player.setLastOperation("Call");
                }
            }
            if (preflopFirstCall) {
                lastRaise = turn;
                preflopFirstCall = false;
            }
            nextTurn();
        }
    }

    public void ready(String playerName, boolean value) {
        synchronized (this) {
            if (status != PLAYING_STATUS_WAITING) return;
            for (WaitingPlayerDTO waitingPlayer : waitingPlayers) {
                if (!waitingPlayer.getPlayer().getName().equals(playerName)) continue;
                waitingPlayer.setReady(value);
                broadcast();
                break;
            }
        }
    }

    public void newGame(String playerName) {
        synchronized (this) {
            if (status != PLAYING_STATUS_WAITING) return;
            if (waitingPlayers.size() < 2 || waitingPlayers.size() > 23) return;
            if (!waitingPlayers.get(0).getPlayer().getName().equals(playerName)) return;
            for (WaitingPlayerDTO waitingPlayer : waitingPlayers) {
                if (!waitingPlayer.getReady()) return;
            }

            ArrayList<Integer> card = new ArrayList<>(52);
            for (int i = 0; i < 52; i++) card.add(i);
            Collections.shuffle(card, RANDOM);
            int nextCard = 0;
            for (int i = 0; i < 5; i++) boardCards.set(i, card.get(nextCard++));
            for (WaitingPlayerDTO waitingPlayer : waitingPlayers) {
                int card1 = card.get(nextCard++);
                int card2 = card.get(nextCard++);
                gamingPlayers.add(new GamingPlayerDTO(waitingPlayer.getPlayer(), card1, card2));
            }
            waitingPlayers.clear();

            int btn = 0;
            for (int j = 0; j < gamingPlayers.size(); j++) {
                if (!gamingPlayers.get(j).getPlayer().getIsButton()) continue;
                btn = j;
                break;
            }
            int sb = (btn + 1) % gamingPlayers.size();
            int bb = (btn + 2) % gamingPlayers.size();
            int utg = (btn + 3) % gamingPlayers.size();
            gamingPlayers.get(sb).getPlayer().reduceChips(smallBlindBet);
            gamingPlayers.get(sb).setBet(smallBlindBet);
            gamingPlayers.get(bb).getPlayer().reduceChips(smallBlindBet * 2);
            gamingPlayers.get(bb).setBet(smallBlindBet * 2);
            pot += smallBlindBet * 3;
            lastRaise = bb;
            turn = utg;
            gamingPlayers.get(utg).setLastOperation("Thinking");
            status = PLAYING_STATUS_PREFLOP;
            preflopFirstCall = true;
            broadcast();
        }
    }

    public void chat(String playerName, String content) {
        synchronized (this) {
            JSONObject data = new JSONObject();
            data.put("from", playerName);
            data.put("content", content);
            JSONObject ret = new JSONObject();
            ret.put("type", "chat");
            ret.put("data", data);
            String json = ret.toJSONString();
            for (WebSocketSession session : sessions.values()) chatForwardTo(session, json);
        }
    }

    protected void chatForwardTo(WebSocketSession session, String content) {
        if (session == null) return;
        asyncExecutor.pushMessage(session, content);
    }

    private boolean _switchToNextQuery(int start) {
        boolean nextStatus = true;
        for (int j = (start + 1) % gamingPlayers.size(); j != lastRaise; j = (j + 1) % gamingPlayers.size()) {
            if (gamingPlayers.get(j).getStatus() != GamingPlayerDTO.GAMING_STATUS_NORMAL) continue;
            if (autoFold.contains(gamingPlayers.get(j).getPlayer().getName())) {
                gamingPlayers.get(j).setLastOperation("Fold");
                gamingPlayers.get(j).setStatus(GamingPlayerDTO.GAMING_STATUS_FOLD);
                autoFold.remove(gamingPlayers.get(j).getPlayer().getName());
                continue;
            }
            nextStatus = false;
            turn = j;
            gamingPlayers.get(j).setLastOperation("Thinking");
            break;
        }
        return nextStatus;
    }

    private void nextTurn() {
        int normalCount = 0, allinCount = 0;
        for (GamingPlayerDTO gamingPlayer : gamingPlayers) {
            if (gamingPlayer.getStatus() == GamingPlayerDTO.GAMING_STATUS_NORMAL &&
                    !autoFold.contains(gamingPlayer.getPlayer().getName())) normalCount++;
            else if (gamingPlayer.getStatus() == GamingPlayerDTO.GAMING_STATUS_ALLIN) allinCount++;
        }
        if ((normalCount == 1 && allinCount == 0) || normalCount == 0) {
            summary();
            return;
        }
        if (!_switchToNextQuery(turn)) {
            broadcast();
            return;
        }

        int normalCount_ = 0;
        for (GamingPlayerDTO gamingPlayer : gamingPlayers) {
            if (gamingPlayer.getStatus() == GamingPlayerDTO.GAMING_STATUS_NORMAL &&
                    !autoFold.contains(gamingPlayer.getPlayer().getName())) normalCount_++;
            gamingPlayer.setBet(0);
        }
        if (normalCount_ <= 1 || status == PLAYING_STATUS_RIVER) {
            summary();
            return;
        }
        status++;
        int button = 0;
        for (int j = 0; j < gamingPlayers.size(); j++) {
            if (!gamingPlayers.get(j).getPlayer().getIsButton()) continue;
            button = j;
            break;
        }
        lastRaise = (button + 1) % gamingPlayers.size();
        GamingPlayerDTO lastRaisePlayer = gamingPlayers.get(lastRaise);
        if (autoFold.contains(lastRaisePlayer.getPlayer().getName())) {
            lastRaisePlayer.setLastOperation("Fold");
            lastRaisePlayer.setStatus(GamingPlayerDTO.GAMING_STATUS_FOLD);
            autoFold.remove(lastRaisePlayer.getPlayer().getName());
        }
        if (lastRaisePlayer.getStatus() == GamingPlayerDTO.GAMING_STATUS_NORMAL) {
            turn = lastRaise;
            lastRaisePlayer.setLastOperation("Thinking");
        } else {
            _switchToNextQuery(lastRaise);
        }
        broadcast();
    }

    protected void sendSummaryMsg(String jsonMsg) {
        for (GamingPlayerDTO player : gamingPlayers) {
            if (!sessions.containsKey(player.getPlayer().getName())) continue;
            WebSocketSession session = sessions.get(player.getPlayer().getName());
            asyncExecutor.pushMessage(session, jsonMsg);
        }
        for (WaitingPlayerDTO player : waitingPlayers) {
            if (!sessions.containsKey(player.getPlayer().getName())) continue;
            WebSocketSession session = sessions.get(player.getPlayer().getName());
            asyncExecutor.pushMessage(session, jsonMsg);
        }
    }

    private void summary() {
        ArrayList<Pair<GamingPlayerDTO, HandTypeVO>> summaryPlayers = new ArrayList<>(gamingPlayers.size());
        for (GamingPlayerDTO player : gamingPlayers) {
            if (player.getStatus() != GamingPlayerDTO.GAMING_STATUS_FOLD &&
                    !autoFold.contains(player.getPlayer().getName())) summaryPlayers.add(new Pair<>(player, null));
        }
        ArrayList<SummaryDTO> summaryMsg = new ArrayList<>(summaryPlayers.size());
        if (summaryPlayers.size() == 0) {
            int award = pot / gamingPlayers.size();
            for (GamingPlayerDTO player : gamingPlayers) {
                player.getPlayer().increaseChips(award);
                summaryMsg.add(new SummaryDTO(player.getPlayer().getName(), null, null, null, award));
            }
        } else if (summaryPlayers.size() == 1) {
            summaryPlayers.get(0).getFirst().getPlayer().increaseChips(pot);
            summaryMsg.add(new SummaryDTO(summaryPlayers.get(0).getFirst().getPlayer().getName(), null, null, null, pot));
        } else {
            for (Pair<GamingPlayerDTO, HandTypeVO> summaryPlayer : summaryPlayers) {
                int card1 = summaryPlayer.getFirst().getCard1();
                int card2 = summaryPlayer.getFirst().getCard2();
                summaryPlayer.setSecond(GameUtils.analyseHandType(boardCards, card1, card2));
            }
            summaryPlayers.sort((o1, o2) -> o2.getSecond().compareTo(o1.getSecond()));
            int maxSize = summaryPlayers.get(0).getSecond().getSize();
            int maxSizeCount = 0;
            for (Pair<GamingPlayerDTO, HandTypeVO> pair : summaryPlayers) {
                if (pair.getSecond().getSize() == maxSize) maxSizeCount++;
                String playerName = pair.getFirst().getPlayer().getName();
                String handTypeName = GameUtils.getHandTypeName(pair.getSecond().getSize());
                ArrayList<Integer> holeCards = new ArrayList<>(2);
                holeCards.add(pair.getFirst().getCard1());
                holeCards.add(pair.getFirst().getCard2());
                summaryMsg.add(new SummaryDTO(playerName, holeCards, pair.getSecond().getCards(), handTypeName, 0));
            }
            int award = pot / maxSizeCount;
            for (int i = 0; i < maxSizeCount; i++) {
                summaryPlayers.get(i).getFirst().getPlayer().increaseChips(award);
                summaryMsg.get(i).setAward(award);
            }
        }

        JSONObject ret = new JSONObject();
        ret.put("type", "summary");
        ret.put("data", JSON.toJSON(summaryMsg));
        sendSummaryMsg(ret.toJSONString());

        for (GamingPlayerDTO player : gamingPlayers) {
            if (player.getPlayer().getChips() > smallBlindBet * 2) continue;
            player.getPlayer().increaseBankruptcy(initialChip);
        }

        boolean recreateWaitingPlayerList = !waitingPlayers.isEmpty();
        ArrayList<WaitingPlayerDTO> waitingPlayerList = recreateWaitingPlayerList ?
                new ArrayList<>(waitingPlayers.size() + gamingPlayers.size())
                : waitingPlayers;
        int btnIndex = 0;
        boolean btn = false, btnQuit = false;
        for (GamingPlayerDTO gamingPlayer : gamingPlayers) {
            boolean quit = !sessions.containsKey(gamingPlayer.getPlayer().getName());
            boolean isBtn = gamingPlayer.getPlayer().getIsButton();
            if (isBtn) {
                btn = true;
                btnQuit = quit;
                gamingPlayer.getPlayer().setIsButton(false);
            }
            if (quit) continue;
            if (!isBtn && !btn) btnIndex++;
            waitingPlayerList.add(new WaitingPlayerDTO(gamingPlayer.getPlayer(), false));
        }
        gamingPlayers.clear();
        if (recreateWaitingPlayerList) {
            waitingPlayerList.addAll(waitingPlayers);
            waitingPlayers.clear();
            waitingPlayers = waitingPlayerList;
        }
        int nextBtnIndex = btnQuit ? btnIndex : (btnIndex + 1) % waitingPlayers.size();
        waitingPlayers.get(nextBtnIndex).getPlayer().setIsButton(true);

        status = PLAYING_STATUS_WAITING;
        pot = 0;
        autoFold.clear();
        broadcast();
    }

    private JSONObject _genData() {
        JSONObject data = new JSONObject();
        data.put("playing", status != PLAYING_STATUS_WAITING);
        if (status != PLAYING_STATUS_WAITING) {
            int shownCards = 0;
            switch (status) {
                case PLAYING_STATUS_FLOP: shownCards = 3; break;
                case PLAYING_STATUS_TURN: shownCards = 4; break;
                case PLAYING_STATUS_RIVER: shownCards = 5; break;
            }
            JSONArray broad = (JSONArray) JSON.toJSON(boardCards.subList(0, shownCards));
            data.put("broad", broad);
            data.put("pot", pot);
            data.put("turn", gamingPlayers.get(turn).getPlayer().getName());
            data.put("players", JSON.toJSON(gamingPlayers));
        }
        data.put("lobby", JSON.toJSON(waitingPlayers));
        return data;
    }

    private JSONObject _genBroadcastMsg(JSONObject you) {
        JSONObject data = _genData();
        data.put("you", you);
        JSONObject ret = new JSONObject();
        ret.put("type", "broadcast");
        ret.put("data", data);
        return ret;
    }

    private void broadcast() {
        for (GamingPlayerDTO player : gamingPlayers) _broadcastTo(player);
        for (WaitingPlayerDTO player : waitingPlayers) _broadcastTo(player);
    }

    private void _broadcastTo(GamingPlayerDTO player) {
        if (!sessions.containsKey(player.getPlayer().getName())) return;
        JSONObject you = (JSONObject) JSON.toJSON(player.getPlayer());
        JSONArray hole = new JSONArray();
        hole.add(player.getCard1());
        hole.add(player.getCard2());
        you.put("hole", hole);
        you.put("isHost", gamingPlayers.get(0) == player);
        int minBet = gamingPlayers.get(lastRaise).getBet() - player.getBet();
        you.put("minBet", Math.min(minBet, player.getPlayer().getChips()));
        JSONObject ret = _genBroadcastMsg(you);
        broadcastTo(player.getPlayer().getName(), ret.toJSONString());
    }

    private void _broadcastTo(WaitingPlayerDTO player) {
        if (!sessions.containsKey(player.getPlayer().getName())) return;
        JSONObject you = (JSONObject) JSON.toJSON(player.getPlayer());
        you.put("ready", player.getReady());
        you.put("isHost", gamingPlayers.isEmpty() && waitingPlayers.get(0) == player);
        JSONObject ret = _genBroadcastMsg(you);
        broadcastTo(player.getPlayer().getName(), ret.toJSONString());
    }

    protected void broadcastTo(String player, String msg) {
        WebSocketSession session = sessions.get(player);
        if (session == null) return;
        asyncExecutor.pushMessage(session, msg);
    }
}
