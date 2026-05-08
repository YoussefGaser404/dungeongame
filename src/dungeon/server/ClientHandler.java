package dungeon.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private int playerId;
    private PrintWriter out;
    private BufferedReader in;

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

                        if (newY >= 0 && newY < dungeon.model.MapData.ROWS && newX >= 0 && newX < dungeon.model.MapData.COLS) {
                            int tile = GameServer.gameState.map.grid[newY][newX];

                            // 0=أرضية, 4=مفتاح, 5=باب, 6=صندوق مفتوح, 7=تفاحة (عشان تمشي عليها وتلمها)
                            if (tile == 0 || tile == 4 || tile == 5 || tile == 6 || tile == 7) {
                                player.x = newX;
                                player.y = newY;

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
                            if (cx >= 0 && cx < dungeon.model.MapData.COLS && cy >= 0 && cy < dungeon.model.MapData.ROWS) {
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
}