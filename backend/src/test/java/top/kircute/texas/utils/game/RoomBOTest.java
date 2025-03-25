package top.kircute.texas.utils.game;

import org.springframework.web.socket.WebSocketSession;
import top.kircute.texas.pojo.dto.GameRuleDTO;
import top.kircute.texas.service.RoomBO;

import java.util.Arrays;
import java.util.Scanner;

public class RoomBOTest {
    private static class RoomCli extends RoomBO {
        public RoomCli(int initialChip, int smallBlindBet) {
            super(null, null, new GameRuleDTO(initialChip, smallBlindBet, -1L, 4, 13));
        }

        @Override
        protected void sendSummaryMsg(String jsonMsg) {
            System.out.printf("[Summary] %s\n", jsonMsg);
        }

        @Override
        protected void broadcastTo(String player, String msg) {
            System.out.printf("[Broadcast][%s] %s\n", player, msg);
        }

        @Override
        protected void chatForwardTo(WebSocketSession session, String json) {
            System.out.printf("[Chat] %s\n", json);
        }
    }

    public static void main(String[] args) {
        RoomCli room = new RoomCli(5000, 5);
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String[] cmd = scanner.nextLine().split(" ");
            if (cmd.length == 0) continue;
            boolean failed = false;
            switch (cmd[0]) {
                case "exit":
                    return;
                case "join":
                    if (cmd.length != 2) {
                        failed = true;
                        break;
                    }
                    room.join(cmd[1]);
                    break;
                case "connect":
                    if (cmd.length != 2) {
                        failed = true;
                        break;
                    }
                    room.connect(cmd[1], null);
                    break;
                case "disconnect":
                    if (cmd.length != 2) {
                        failed = true;
                        break;
                    }
                    room.disconnect(cmd[1]);
                    break;
                case "fold":
                    if (cmd.length != 2) {
                        failed = true;
                        break;
                    }
                    room.fold(cmd[1]);
                    break;
                case "raise":
                    if (cmd.length != 3) {
                        failed = true;
                        break;
                    }
                    int i;
                    try {
                        i = Integer.parseInt(cmd[2]);
                    } catch (Exception e) {
                        failed = true;
                        break;
                    }
                    room.raise(cmd[1], i);
                    break;
                case "call":
                    if (cmd.length != 2) {
                        failed = true;
                        break;
                    }
                    room.call(cmd[1]);
                    break;
                case "ready":
                    if (cmd.length != 3) {
                        failed = true;
                        break;
                    }
                    boolean b;
                    try {
                        b = Boolean.parseBoolean(cmd[2]);
                    } catch (Exception e) {
                        failed = true;
                        break;
                    }
                    room.ready(cmd[1], b);
                    break;
                case "new":
                    if (cmd.length != 2) {
                        failed = true;
                        break;
                    }
                    room.newGame(cmd[1]);
                    break;
                case "chat":
                    if (cmd.length < 3) {
                        failed = true;
                        break;
                    }
                    room.chat(cmd[1], String.join(" ", Arrays.copyOfRange(cmd, 2, cmd.length)));
                    break;
                default:
                    failed = true;
                    break;
            }
            if (failed) {
                System.out.println("Invalid command.");
            }
            System.out.println();
        }
    }
}
