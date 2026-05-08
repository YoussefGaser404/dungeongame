package dungeon.server;

import dungeon.model.GameConstants;
import dungeon.model.MapData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;

public class ClientHandler implements Runnable {
    private Socket socket;
    private int playerId;
    private PrintWriter out;
    private BufferedReader in;
    private static final String[] KIOSK_QUESTIONS = {
            "Do you trust the shadows?",
            "Would you trade health for power?",
            "Which path feels safer, left or right?",
            "Will you face the boss or hide?",
            "How many coins would you risk for glory?"
    };

    public ClientHandler(Socket socket, int playerId) {
        this.socket = socket;
        this.playerId = playerId;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void send(String msg) { if (out != null) out.println(msg); }

    @Override
    public void run() {
        try {
            out.println("MYID:" + playerId);
            out.println(GameServer.gameState.map.serializeMap());

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("INIT:")) {
                    String charClass = message.split(":")[1];
                    GameServer.gameState.players.put(playerId, new GameState.PlayerInv(2, 2, charClass));

                } else if (message.equals("UP") || message.equals("DOWN") || message.equals("LEFT") || message.equals("RIGHT")) {
                    GameState.PlayerInv player = GameServer.gameState.players.get(playerId);
                    if (player != null) {
                        int dx = 0, dy = 0;
                        if (message.equals("UP")) dy = -1;
                        if (message.equals("DOWN")) dy = 1;
                        if (message.equals("LEFT")) dx = -1;
                        if (message.equals("RIGHT")) dx = 1;

                        int newX = player.x + dx;
                        int newY = player.y + dy;

                        if (newY >= 0 && newY < MapData.ROWS && newX >= 0 && newX < MapData.COLS) {
                            int tile = GameServer.gameState.map.grid[newY][newX];

                            // 0=أرضية, 4=مفتاح, 5=باب, 6=صندوق مفتوح, 7=تفاحة (عشان تمشي عليها وتلمها)
                            if (tile == 0 || tile == 4 || tile == 5 || tile == 6 || tile == 7) {
                                player.x = newX;
                                player.y = newY;
                                if (dx != 0 || dy != 0) {
                                    player.dirX = dx;
                                    player.dirY = dy;
                                }

                                if (tile == 4) { // لم المفتاح
                                    GameServer.gameState.map.grid[newY][newX] = 0;
                                    if (!player.inventory.contains("Key")) player.inventory.add("Key");
                                    broadcastMap();
                                }
                                else if (tile == 7) { // لم التفاحة
                                    GameServer.gameState.map.grid[newY][newX] = 0;
                                    player.applesCount++;
                                    broadcastMap();
                                }
                            }
                        }
                    }
                } else if (message.equals("INTERACT")) {
                    GameState.PlayerInv player = GameServer.gameState.players.get(playerId);
                    if (player != null) {
                        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}, {0,0}};
                        boolean mapChanged = false;

                        for (int[] d : dirs) {
                            int cx = player.x + d[0], cy = player.y + d[1];
                            if (cx >= 0 && cx < MapData.COLS && cy >= 0 && cy < MapData.ROWS) {
                                if (GameServer.gameState.map.grid[cy][cx] == 2 && player.inventory.contains("Key")) {
                                    player.inventory.remove("Key");
                                    GameServer.gameState.map.grid[cy][cx] = 6;
                                    player.coins += 50;
                                    send("EVENT:GOLD_GAIN");
                                    mapChanged = true; break;
                                }
                            }
                        }
                        if (mapChanged) broadcastMap();
                    }
                } else if (message.startsWith("BUY:")) {
                    GameState.PlayerInv player = GameServer.gameState.players.get(playerId);
                    if (player != null) {
                        if (!isNearKiosk(player)) {
                            send("EVENT:SHOP_FAIL:NEED_KIOSK");
                            continue;
                        }
                        String item = message.split(":")[1];
                        if (item.equals("SHIELD")) {
                            handlePurchase(player, GameConstants.SHIELD_COST, "SHIELD", () -> player.defense += 5);
                        } else if (item.equals("ATTACK")) {
                            handlePurchase(player, GameConstants.ATTACK_COST, "ATTACK", () -> player.attack += 5);
                        } else if (item.equals("APPLE")) {
                            handlePurchase(player, GameConstants.APPLE_COST, "APPLE", () -> player.applesCount += 1);
                        } else if (item.equals("QUESTION")) {
                            if (handlePurchase(player, GameConstants.QUESTION_COST, "QUESTION", () -> {})) {
                                send("EVENT:QUESTION:" + randomQuestion());
                            }
                        }
                    }
                } else if (message.equals("ATTACK")) {
                    GameState.PlayerInv player = GameServer.gameState.players.get(playerId);
                    if (player != null) {
                        int tx = player.x + player.dirX;
                        int ty = player.y + player.dirY;
                        if (tx >= 0 && tx < dungeon.model.MapData.COLS && ty >= 0 && ty < dungeon.model.MapData.ROWS) {
                            for (GameState.Enemy e : GameServer.gameState.enemies) {
                                if (e.x == tx && e.y == ty) {
                                    e.hp -= player.attack;
                                    break;
                                }
                            }
                            GameServer.gameState.sendEventToAll("SLASH:" + tx + ":" + ty);
                        }
                    }
                } else if (message.equals("USE_APPLE")) { // 🌟 نظام أكل التفاح
                    GameState.PlayerInv player = GameServer.gameState.players.get(playerId);
                    if (player != null && player.applesCount > 0 && player.hp < 100) {
                        player.applesCount--;
                        player.hp += 20; // التفاحة بتزود 20 HP
                        if (player.hp > 100) player.hp = 100;
                    }
                }
            }
        } catch (IOException e) {
        } finally {
            GameServer.gameState.players.remove(playerId); GameServer.clients.remove(this);
            try { socket.close(); } catch (IOException e) {}
        }
    }
    private void broadcastMap() {
        String m = GameServer.gameState.map.serializeMap();
        for (ClientHandler c : GameServer.clients) c.send(m);
    }

    private boolean isNearKiosk(GameState.PlayerInv player) {
        for (int[] d : GameConstants.ADJACENT_DIRECTIONS) {
            int cx = player.x + d[0], cy = player.y + d[1];
            if (cx >= 0 && cx < MapData.COLS && cy >= 0 && cy < MapData.ROWS) {
                if (GameServer.gameState.map.grid[cy][cx] == 8) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handlePurchase(GameState.PlayerInv player, int cost, String item, Runnable applyItem) {
        if (player.coins < cost) {
            send("EVENT:SHOP_FAIL:COINS");
            return false;
        }
        player.coins -= cost;
        applyItem.run();
        send("EVENT:SHOP_OK:" + item);
        return true;
    }

    private String randomQuestion() {
        int idx = ThreadLocalRandom.current().nextInt(KIOSK_QUESTIONS.length);
        return KIOSK_QUESTIONS[idx];
    }
}
