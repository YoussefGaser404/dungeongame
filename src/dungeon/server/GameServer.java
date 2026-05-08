package dungeon.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer {
    // Lazim tkoon Thread-Safe 3ashan ne-avoid el crashes
    public static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public static GameState gameState = new GameState();
    private static int nextId = 1; // Byeddy ID mo5talif l-kol player

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Server Started on port 5000...");

        // 🔥 GAME LOOP (Broadcaster)
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(50); // 20 updates per second
                    String state = gameState.serialize();

                    if (!state.isEmpty()) {
                        for (ClientHandler c : clients) {
                            c.send(state);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Est2bal el players
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Player " + nextId + " Connected!");

            ClientHandler client = new ClientHandler(socket, nextId++);
            clients.add(client);
            new Thread(client).start();
        }
    }
}