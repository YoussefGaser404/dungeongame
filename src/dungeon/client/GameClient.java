package dungeon.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import dungeon.model.GameConstants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.HashMap;

public class GameClient extends Application {

    // --------------------------------------------------------
    // القوائم لتتبع العناصر على الشاشة
    // --------------------------------------------------------
    private final HashMap<Integer, ImageView> playersOnScreen = new HashMap<>();
    private final HashMap<Integer, ImageView> enemiesOnScreen = new HashMap<>();
    private final HashMap<Integer, Node> projectilesOnScreen = new HashMap<>();

    private final HashMap<Integer, SpriteAnimation> playerAnimations = new HashMap<>();
    private final HashMap<Integer, TranslateTransition> playerIdles = new HashMap<>();
    private final HashMap<Integer, SpriteConfig> playerConfigs = new HashMap<>();
    private final HashMap<Integer, Integer> playerFrameW = new HashMap<>();
    private final HashMap<Integer, Integer> playerFrameH = new HashMap<>();

    private final HashMap<String, Image> classImages = new HashMap<>();

    // --------------------------------------------------------
    // الصور
    // --------------------------------------------------------
    private Image wallImg;
    private Image floorImg;
    private Image doorImg;
    private Image chestClosedImg;
    private Image chestOpenImg;
    private Image keyImg;
    private Image appleImg;
    private Image enemy1Img;
    private Image enemy2Img;
    private Image kioskImg;

    private Label uiLabel;
    private Label goldPopUp;
    private Label toastLabel;
    private VBox feedBox;
    private Pane gameWorld;
    private StackPane shopOverlay;
    private Label shopStatus;
    private Group fogLayer;
    private Shape fogMask;
    private GaussianBlur fogBlur;

    private final int TILE_SIZE = 40;
    private final int ROOM_SIZE = 15;
    private int myId = -1;
    private int myGridX = -1;
    private int myGridY = -1;
    private int kioskGridX = -1;
    private int kioskGridY = -1;
    private char[][] mapGrid;
    private PrintWriter out;
    private final ArrayDeque<String> feedMessages = new ArrayDeque<>();

    @Override
    public void start(Stage stage) {
        showCharacterSelect(stage);
    }

    // --------------------------------------------------------
    // شاشة اختيار الشخصيات
    // --------------------------------------------------------
    private void showCharacterSelect(Stage stage) {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #1a1a2e; -fx-font-family: 'Courier New';");

        VBox mainBox = new VBox(30);
        mainBox.setAlignment(Pos.CENTER);
        mainBox.setPadding(new Insets(40));

        Label title = new Label("⚔  CHOOSE YOUR CLASS  ⚔");
        title.setStyle("-fx-text-fill: #e2b96f; -fx-font-size: 24px; -fx-font-weight: bold;");

        HBox cardsBox = new HBox(24);
        cardsBox.setAlignment(Pos.CENTER);

        String[][] classInfo = {
                {"warrior", "WARRIOR",  "file:assets/warrior.png", "4", "3", "Slow & tough.", "#c0c0d0", "#2a2a4a"},
                {"rogue",   "ROGUE",    "file:assets/rogue.png",   "3", "3", "Fast & sneaky.", "#c890d8", "#2a1a3a"},
                {"archer",  "ARCHER",   "file:assets/archer.png",  "3", "3", "Fast & fragile.", "#90c890", "#1a2a1a"},
        };

        for (String[] info : classInfo) {
            VBox card = buildCard(stage, info);
            cardsBox.getChildren().add(card);
        }

        mainBox.getChildren().addAll(title, cardsBox);
        root.getChildren().add(mainBox);

        Scene scene = new Scene(root, 700, 520);
        stage.setScene(scene);
        stage.setTitle("Dungeon Crawler");
        stage.show();
    }

    private VBox buildCard(Stage stage, String[] info) {
        String cls = info[0];
        String labelText = info[1];
        String imgPath = info[2];
        int cols = Integer.parseInt(info[3]);
        int rows = Integer.parseInt(info[4]);
        String descText = info[5];
        String accent = info[6];
        String cardBg = info[7];

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefWidth(185);
        card.setStyle("-fx-background-color: " + cardBg + "; -fx-border-color: " + accent + "; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-cursor: hand;");

        Image img = new Image(imgPath);
        ImageView iv = new ImageView(img);

        int fw = (int)(img.getWidth() / cols);
        int fh = (int)(img.getHeight() / rows);

        iv.setViewport(new Rectangle2D(0, 0, fw, fh));
        iv.setFitWidth(80);
        iv.setFitHeight(80);

        Label nameLabel = new Label(labelText);
        nameLabel.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label descLabel = new Label(descText);
        descLabel.setStyle("-fx-text-fill: #9999bb; -fx-font-size: 11.5px; -fx-text-alignment: center;");

        Button playBtn = new Button("ENTER");
        playBtn.setStyle("-fx-background-color: " + accent + "; -fx-text-fill: #1a1a2e; -fx-font-weight: bold;");
        playBtn.setOnAction(e -> startGame(stage, cls));

        card.getChildren().addAll(iv, nameLabel, descLabel, playBtn);
        return card;
    }

    // --------------------------------------------------------
    // الدخول للعبة
    // --------------------------------------------------------
    private void startGame(Stage stage, String selectedClass) {
        try {
            // تحميل جميع الصور هنا
            wallImg = new Image("file:assets/wall.png");
            floorImg = new Image("file:assets/floor.png");
            doorImg = new Image("file:assets/door.png");
            keyImg = new Image("file:assets/key.png");
            appleImg = new Image("file:assets/apple.png");
            chestClosedImg = new Image("file:assets/chest_closed.png");
            chestOpenImg = new Image("file:assets/chest_open.png");
            enemy1Img = new Image("file:assets/enemy1.png");
            enemy2Img = new Image("file:assets/enemy2.png");
            kioskImg = new Image("file:assets/kiosk.png");

            classImages.put("warrior", new Image("file:assets/warrior.png"));
            classImages.put("rogue", new Image("file:assets/rogue.png"));
            classImages.put("archer", new Image("file:assets/archer.png"));

            Socket socket = new Socket("localhost", 5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("INIT:" + selectedClass);

            gameWorld = new Pane();
            fogLayer = buildFogLayer();

            // واجهة المستخدم (UI)
            uiLabel = new Label("❤ HP: 100  |  💰 Coins: 0  |  ⚔ ATK: " + GameConstants.DEFAULT_ATTACK + "  |  🛡 DEF: " + GameConstants.DEFAULT_DEFENSE + "  |  🎒 Inventory: Empty");
            uiLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 8px; -fx-border-radius: 5px;");
            uiLabel.setLayoutX(10);
            uiLabel.setLayoutY(10);

            goldPopUp = new Label("+50 Coins!");
            goldPopUp.setStyle("-fx-text-fill: #00ff00; -fx-font-size: 26px; -fx-font-weight: bold;");
            goldPopUp.setOpacity(0);
            goldPopUp.setLayoutX(250);
            goldPopUp.setLayoutY(250);

            toastLabel = new Label("");
            toastLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 8px; -fx-border-radius: 5px;");
            toastLabel.setOpacity(0);
            toastLabel.setLayoutX(150);
            toastLabel.setLayoutY(60);

            feedBox = buildFeedBox();
            pushFeedMessage("Kiosk feed online.");

            shopOverlay = buildShopOverlay();

            Group rootGroup = new Group(gameWorld, fogLayer, uiLabel, goldPopUp, toastLabel, feedBox, shopOverlay);
            Scene scene = new Scene(rootGroup, 600, 600);

            // أزرار التحكم
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.W || e.getCode() == KeyCode.UP) out.println("UP");
                if (e.getCode() == KeyCode.S || e.getCode() == KeyCode.DOWN) out.println("DOWN");
                if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT) out.println("LEFT");
                if (e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT) out.println("RIGHT");
                if (e.getCode() == KeyCode.E) out.println("INTERACT");
                if (e.getCode() == KeyCode.P) {
                    tryToggleShop();
                }
                if (e.getCode() == KeyCode.F) out.println("USE_APPLE");
                if (e.getCode() == KeyCode.SPACE) out.println("ATTACK");
            });

            // Thread الاستقبال
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        String fl = line;
                        Platform.runLater(() -> {
                            if (fl.startsWith("MYID:")) {
                                myId = Integer.parseInt(fl.substring(5));
                            }
                            else if (fl.startsWith("MAP:")) {
                                drawMap(fl.substring(4), gameWorld);
                            }
                            else if (fl.startsWith("STATE:")) {
                                updateUI(fl.substring(6), gameWorld);
                            }
                            else if (fl.startsWith("EVENT:")) {
                                handleEvent(fl.substring(6));
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            stage.setScene(scene);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // --------------------------------------------------------
    // التفاعل مع الأحداث (السيف، القنبلة، الفلوس)
    // --------------------------------------------------------
    private void handleEvent(String evt) {
        if (evt.equals("GOLD_GAIN")) {
            goldPopUp.setOpacity(1.0);
            goldPopUp.setTranslateY(0);

            TranslateTransition moveUp = new TranslateTransition(Duration.millis(1500), goldPopUp);
            moveUp.setByY(-60);

            FadeTransition fade = new FadeTransition(Duration.millis(1500), goldPopUp);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);

            moveUp.play();
            fade.play();
        }
        else if (evt.startsWith("SHOP_OK:")) {
            String item = evt.split(":")[1];
            if (item.equals("QUESTION")) {
                showToast("Question unlocked!", "#00ff88");
                updateShopStatus("Question unlocked ✅", "#00ff88");
                pushFeedMessage("🧠 A new question arrives...");
            } else {
                showToast("Purchased " + item + "!", "#00ff88");
                updateShopStatus("Purchased " + item + " ✅", "#00ff88");
                if (item.equals("APPLE")) {
                    pushFeedMessage("🍎 Apple added to your bag.");
                }
            }
        }
        else if (evt.startsWith("SHOP_FAIL:")) {
            String reason = evt.split(":")[1];
            if (reason.equals("COINS")) {
                showToast("Not enough coins!", "#ff5555");
                updateShopStatus("Not enough coins ❌", "#ff5555");
            } else if (reason.equals("NEED_KIOSK")) {
                showToast("Get closer to the kiosk.", "#ffcc00");
                updateShopStatus("Need to be near kiosk ⚠", "#ffcc00");
            } else {
                showToast("Purchase failed.", "#ff5555");
                updateShopStatus("Purchase failed ❌", "#ff5555");
            }
        }
        else if (evt.startsWith("QUESTION:")) {
            String q = evt.substring("QUESTION:".length());
            pushFeedMessage("❓ " + q);
        }
        else if (evt.startsWith("EXPLOSION:")) {
            String[] p = evt.split(":");
            double x = Integer.parseInt(p[1]) * TILE_SIZE;
            double y = Integer.parseInt(p[2]) * TILE_SIZE;

            Rectangle exp = new Rectangle(x - TILE_SIZE, y - TILE_SIZE, TILE_SIZE * 3, TILE_SIZE * 3);
            exp.setFill(Color.rgb(255, 50, 0, 0.6));
            gameWorld.getChildren().add(exp);

            FadeTransition ft = new FadeTransition(Duration.millis(400), exp);
            ft.setToValue(0);
            ft.setOnFinished(e -> gameWorld.getChildren().remove(exp));
            ft.play();
        }
        else if (evt.startsWith("SLASH:")) {
            String[] p = evt.split(":");
            double x = Integer.parseInt(p[1]) * TILE_SIZE;
            double y = Integer.parseInt(p[2]) * TILE_SIZE;

            Rectangle slash = new Rectangle(x, y, TILE_SIZE, TILE_SIZE);
            slash.setFill(Color.WHITE);
            gameWorld.getChildren().add(slash);

            FadeTransition ft = new FadeTransition(Duration.millis(200), slash);
            ft.setToValue(0);
            ft.setOnFinished(e -> gameWorld.getChildren().remove(slash));
            ft.play();
        }
    }

    private StackPane buildShopOverlay() {
        StackPane overlay = new StackPane();
        overlay.setPrefSize(600, 600);

        Rectangle bg = new Rectangle(600, 600, Color.rgb(10, 10, 10, 0.6));

        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: #2b2b2b; -fx-border-color: #d9c57a; -fx-border-width: 4px; -fx-border-radius: 0; -fx-font-family: 'Courier New';");

        Label title = new Label("KIOSK TERMINAL");
        title.setStyle("-fx-text-fill: #d9c57a; -fx-font-size: 18px; -fx-font-weight: bold;");

        Button shieldBtn = new Button("Buy Shield (+5 DEF) - " + GameConstants.SHIELD_COST + " coins");
        shieldBtn.setOnAction(e -> out.println("BUY:SHIELD"));
        shieldBtn.setStyle("-fx-background-color: #5b8def; -fx-text-fill: #111; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

        Button attackBtn = new Button("Buy Attack (+5 ATK) - " + GameConstants.ATTACK_COST + " coins");
        attackBtn.setOnAction(e -> out.println("BUY:ATTACK"));
        attackBtn.setStyle("-fx-background-color: #e76f51; -fx-text-fill: #111; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

        Button appleBtn = new Button("Buy Apple (+1) - " + GameConstants.APPLE_COST + " coins");
        appleBtn.setOnAction(e -> out.println("BUY:APPLE"));
        appleBtn.setStyle("-fx-background-color: #8bc34a; -fx-text-fill: #111; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

        Button questionBtn = new Button("Ask Question - " + GameConstants.QUESTION_COST + " coins");
        questionBtn.setOnAction(e -> out.println("BUY:QUESTION"));
        questionBtn.setStyle("-fx-background-color: #ffd166; -fx-text-fill: #111; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

        shopStatus = new Label("Select an upgrade.");
        shopStatus.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px; -fx-font-family: 'Courier New';");

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> setShopVisible(false));
        closeBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-font-family: 'Courier New';");

        box.getChildren().addAll(title, shieldBtn, attackBtn, appleBtn, questionBtn, shopStatus, closeBtn);
        overlay.getChildren().addAll(bg, box);
        overlay.setVisible(false);
        return overlay;
    }

    private VBox buildFeedBox() {
        VBox box = new VBox(4);
        box.setLayoutX(10);
        box.setLayoutY(520);
        box.setPrefWidth(580);
        box.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 6px; -fx-border-color: #666; -fx-border-width: 2px;");
        Label header = new Label("FEED");
        header.setStyle("-fx-text-fill: #d9c57a; -fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");
        box.getChildren().add(header);
        return box;
    }

    private Group buildFogLayer() {
        fogBlur = new GaussianBlur(24);
        fogMask = new Rectangle(600, 600, Color.rgb(30, 30, 30, 0.55));
        fogMask.setEffect(fogBlur);
        Group group = new Group(fogMask);
        group.setMouseTransparent(true);
        return group;
    }

    private boolean tryToggleShop() {
        if (shopOverlay == null) {
            return false;
        }
        if (shopOverlay.isVisible()) {
            setShopVisible(false);
            return true;
        }
        if (isNearKiosk()) {
            setShopVisible(true);
            return true;
        }
        showToast("No kiosk nearby.", "#ffcc00");
        return false;
    }

    private void setShopVisible(boolean visible) {
        shopOverlay.setVisible(visible);
        if (visible) {
            updateShopStatus("Select an upgrade.", "#cccccc");
        }
    }

    private boolean isNearKiosk() {
        if (mapGrid == null || myGridX < 0 || myGridY < 0) {
            return false;
        }
        for (int[] d : GameConstants.ADJACENT_DIRECTIONS) {
            int nx = myGridX + d[0];
            int ny = myGridY + d[1];
            if (isValidGridPosition(nx, ny)) {
                if (mapGrid[ny][nx] == '8') {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isValidGridPosition(int x, int y) {
        return mapGrid != null && y >= 0 && y < mapGrid.length && x >= 0 && x < mapGrid[0].length;
    }

    private void showToast(String text, String color) {
        toastLabel.setText(text);
        toastLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 8px; -fx-border-radius: 5px;");
        toastLabel.setOpacity(1.0);
        FadeTransition fade = new FadeTransition(Duration.millis(1500), toastLabel);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.play();
    }

    private void updateShopStatus(String text, String color) {
        if (shopStatus != null) {
            shopStatus.setText(text);
            shopStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-family: 'Courier New';");
        }
    }

    private void pushFeedMessage(String message) {
        feedMessages.addFirst(message);
        while (feedMessages.size() > 3) {
            feedMessages.removeLast();
        }
        refreshFeed();
    }

    private void refreshFeed() {
        if (feedBox == null || feedBox.getChildren().isEmpty()) {
            return;
        }
        feedBox.getChildren().remove(1, feedBox.getChildren().size());
        for (String msg : feedMessages) {
            Label line = new Label(msg);
            line.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 11px; -fx-font-family: 'Courier New';");
            feedBox.getChildren().add(line);
        }
    }

    private void updateFogLights() {
        if (fogLayer == null || gameWorld == null) {
            return;
        }
        double offsetX = gameWorld.getTranslateX();
        double offsetY = gameWorld.getTranslateY();

        Shape mask = new Rectangle(600, 600);
        if (myGridX >= 0 && myGridY >= 0) {
            double px = myGridX * TILE_SIZE + TILE_SIZE / 2.0 + offsetX;
            double py = myGridY * TILE_SIZE + TILE_SIZE / 2.0 + offsetY;
            mask = Shape.subtract(mask, new Circle(px, py, 75));
        }
        if (kioskGridX >= 0 && kioskGridY >= 0) {
            double kx = kioskGridX * TILE_SIZE + TILE_SIZE / 2.0 + offsetX;
            double ky = kioskGridY * TILE_SIZE + TILE_SIZE / 2.0 + offsetY;
            mask = Shape.subtract(mask, new Circle(kx, ky, 120));
        }
        mask.setFill(Color.rgb(30, 30, 30, 0.55));
        if (fogBlur == null) {
            fogBlur = new GaussianBlur(24);
        }
        mask.setEffect(fogBlur);
        fogLayer.getChildren().setAll(mask);
        fogMask = mask;
    }

    // --------------------------------------------------------
    // رسم الخريطة (وتكبير الصناديق والمفاتيح)
    // --------------------------------------------------------
    private void drawMap(String mapData, Pane gameWorld) {
        gameWorld.getChildren().removeIf(node ->
                node instanceof ImageView &&
                        !playersOnScreen.values().contains(node) &&
                        !enemiesOnScreen.values().contains(node)
        );

        String[] rows = mapData.split(";");
        mapGrid = new char[rows.length][rows[0].length()];
        kioskGridX = -1;
        kioskGridY = -1;
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < rows[i].length(); j++) {
                char tile = rows[i].charAt(j);
                mapGrid[i][j] = tile;

                ImageView floor = new ImageView(floorImg);
                floor.setX(j * TILE_SIZE);
                floor.setY(i * TILE_SIZE);
                floor.setFitWidth(TILE_SIZE);
                floor.setFitHeight(TILE_SIZE);
                gameWorld.getChildren().add(floor);

                if (tile == '1' || tile == '3') {
                    ImageView wall = new ImageView(wallImg);
                    wall.setX(j * TILE_SIZE);
                    wall.setY(i * TILE_SIZE);
                    wall.setFitWidth(TILE_SIZE);
                    wall.setFitHeight(TILE_SIZE);
                    gameWorld.getChildren().add(wall);
                }
                else if (tile == '2' || tile == '6') {
                    ImageView chest = new ImageView(tile == '2' ? chestClosedImg : chestOpenImg);
                    chest.setFitWidth(TILE_SIZE + 10);
                    chest.setFitHeight(TILE_SIZE + 10);
                    chest.setX((j * TILE_SIZE) - 5);
                    chest.setY((i * TILE_SIZE) - 5);
                    gameWorld.getChildren().add(chest);
                }
                else if (tile == '4') {
                    ImageView key = new ImageView(keyImg);
                    key.setFitWidth(32);
                    key.setFitHeight(32);
                    key.setX((j * TILE_SIZE) + 4);
                    key.setY((i * TILE_SIZE) + 4);
                    gameWorld.getChildren().add(key);
                }
                else if (tile == '5') {
                    ImageView door = new ImageView(doorImg);
                    door.setX(j * TILE_SIZE);
                    door.setY(i * TILE_SIZE);
                    door.setFitWidth(TILE_SIZE);
                    door.setFitHeight(TILE_SIZE);
                    gameWorld.getChildren().add(door);
                }
                else if (tile == '7') {
                    ImageView apple = new ImageView(appleImg);
                    apple.setFitWidth(TILE_SIZE + 10);
                    apple.setFitHeight(TILE_SIZE + 10);
                    apple.setX((j * TILE_SIZE) - 5);
                    apple.setY((i * TILE_SIZE) - 5);
                    gameWorld.getChildren().add(apple);
                }
                else if (tile == '8') {
                    ImageView kiosk = new ImageView(kioskImg);
                    kiosk.setFitWidth(TILE_SIZE);
                    kiosk.setFitHeight(TILE_SIZE);
                    kiosk.setX(j * TILE_SIZE);
                    kiosk.setY(i * TILE_SIZE);
                    gameWorld.getChildren().add(kiosk);
                    kioskGridX = j;
                    kioskGridY = i;
                }
            }
        }

        for (ImageView v : playersOnScreen.values()) {
            v.toFront();
        }
        for (ImageView v : enemiesOnScreen.values()) {
            v.toFront();
        }
        for (Node n : projectilesOnScreen.values()) {
            n.toFront();
        }
        updateFogLights();
    }

    // --------------------------------------------------------
    // تحديث الشاشة
    // --------------------------------------------------------
    private void updateUI(String stateData, Pane gameWorld) {
        if (stateData.trim().isEmpty()) {
            return;
        }

        String[] all = stateData.split(";");
        HashMap<Integer, Boolean> activeIds = new HashMap<>();

        for (String pData : all) {
            if (pData.trim().isEmpty()) {
                continue;
            }

            String[] parts = pData.split(",");

            // ============== بيانات اللاعب ==============
            if (parts[0].equals("P")) {
                int id = Integer.parseInt(parts[1]);
                int gridX = Integer.parseInt(parts[2]);
                int gridY = Integer.parseInt(parts[3]);
                String cls = parts[4];
                int coins = Integer.parseInt(parts[5]);
                String hasKey = parts[6];
                int hp = Integer.parseInt(parts[7]);
                int apples = Integer.parseInt(parts[8]);
                int attack = Integer.parseInt(parts[9]);
                int defense = Integer.parseInt(parts[10]);

                activeIds.put(id, true);

                if (id == myId) {
                    myGridX = gridX;
                    myGridY = gridY;
                    uiLabel.setText("❤ HP: " + hp + "  |  💰 Coins: " + coins + "  |  ⚔ ATK: " + attack + "  |  🛡 DEF: " + defense + "  |  🎒 Inv: " + (hasKey.equals("1") ? "Key" : "Empty") + " (🍎x" + apples + ")");
                    String color = hp > 50 ? "#00ff00" : (hp > 20 ? "orange" : "red");
                    uiLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 8px; -fx-border-radius: 5px;");
                }

                double screenX = gridX * TILE_SIZE;
                double screenY = gridY * TILE_SIZE;

                Image img = classImages.get(cls);
                SpriteConfig cfg = SpriteConfig.forClass(cls);

                int fw = (int)(img.getWidth() / cfg.frameCols);
                int fh = (int)(img.getHeight() / cfg.spriteRows);

                if (!playersOnScreen.containsKey(id)) {
                    ImageView iv = new ImageView(img);
                    iv.setViewport(new Rectangle2D(0, 0, fw, fh));
                    iv.setFitWidth(TILE_SIZE - 8);
                    iv.setFitHeight(TILE_SIZE - 8);
                    iv.setX(screenX + 4);
                    iv.setY(screenY + 4);

                    SpriteAnimation walk = new SpriteAnimation(iv, Duration.millis(cfg.animMs), cfg.frameCols, cfg.frameCols, 0, cfg.rowDown * fh, fw, fh);
                    walk.setCycleCount(Animation.INDEFINITE);

                    TranslateTransition idle = new TranslateTransition(Duration.millis(500), iv);
                    idle.setByY(-3);
                    idle.setCycleCount(Animation.INDEFINITE);
                    idle.setAutoReverse(true);
                    idle.play();

                    playersOnScreen.put(id, iv);
                    playerAnimations.put(id, walk);
                    playerIdles.put(id, idle);
                    playerConfigs.put(id, cfg);
                    playerFrameW.put(id, fw);
                    playerFrameH.put(id, fh);

                    gameWorld.getChildren().add(iv);
                    iv.toFront();
                } else {
                    ImageView iv = playersOnScreen.get(id);
                    SpriteAnimation walk = playerAnimations.get(id);
                    TranslateTransition idle = playerIdles.get(id);
                    SpriteConfig c = playerConfigs.get(id);

                    double oldX = iv.getX();
                    double oldY = iv.getY();
                    double newX = screenX + 4;
                    double newY = screenY + 4;

                    if (Math.abs(newX - oldX) > 0.5 || Math.abs(newY - oldY) > 0.5) {
                        idle.stop();
                        iv.setTranslateY(0);
                        int row;

                        if (newX > oldX) {
                            row = c.rowSide * playerFrameH.get(id);
                            iv.setScaleX(-1);
                        } else if (newX < oldX) {
                            row = c.rowSide * playerFrameH.get(id);
                            iv.setScaleX( 1);
                        } else if (newY > oldY) {
                            row = c.rowDown * playerFrameH.get(id);
                            iv.setScaleX( 1);
                        } else {
                            row = c.rowUp * playerFrameH.get(id);
                            iv.setScaleX( 1);
                        }

                        walk.setOffsetY(row);
                        iv.setX(newX);
                        iv.setY(newY);

                        if (walk.getStatus() != Animation.Status.RUNNING) {
                            walk.play();
                        }
                    } else {
                        walk.stop();
                        iv.setViewport(new Rectangle2D(0, walk.getOffsetY(), playerFrameW.get(id), playerFrameH.get(id)));
                        if (idle.getStatus() != Animation.Status.RUNNING) {
                            idle.play();
                        }
                    }
                }

                if (id == myId) {
                    gameWorld.setTranslateX(-((gridX / ROOM_SIZE) * ROOM_SIZE * TILE_SIZE));
                    gameWorld.setTranslateY(-((gridY / ROOM_SIZE) * ROOM_SIZE * TILE_SIZE));
                }
            }
            // ============== بيانات الوحوش ==============
            else if (parts[0].equals("E")) {
                int id = Integer.parseInt(parts[1]);
                int type = Integer.parseInt(parts[4]);
                int dir = Integer.parseInt(parts[5]);

                activeIds.put(-id, true);

                double screenX = Integer.parseInt(parts[2]) * TILE_SIZE;
                double screenY = Integer.parseInt(parts[3]) * TILE_SIZE;

                if (!enemiesOnScreen.containsKey(id)) {
                    ImageView ev = new ImageView(type == 1 ? enemy1Img : enemy2Img);
                    ev.setFitWidth(TILE_SIZE - 4);
                    ev.setFitHeight(TILE_SIZE - 4);
                    ev.setX(screenX + 2);
                    ev.setY(screenY + 2);
                    ev.setScaleX(dir);

                    TranslateTransition bounce = new TranslateTransition(Duration.millis(500), ev);
                    bounce.setByY(-5);
                    bounce.setAutoReverse(true);
                    bounce.setCycleCount(Animation.INDEFINITE);
                    bounce.play();

                    enemiesOnScreen.put(id, ev);
                    gameWorld.getChildren().add(ev);
                    ev.toFront();
                } else {
                    ImageView ev = enemiesOnScreen.get(id);
                    ev.setScaleX(dir);
                    ev.setX(screenX + 2);
                    ev.setY(screenY + 2);
                }
            }
            // ============== بيانات المقذوفات ==============
            else if (parts[0].equals("PRJ")) {
                int id = Integer.parseInt(parts[1]);
                int type = Integer.parseInt(parts[2]);

                activeIds.put(99000 + id, true);

                double px = Integer.parseInt(parts[3]) * TILE_SIZE + 15;
                double py = Integer.parseInt(parts[4]) * TILE_SIZE + 15;

                if (!projectilesOnScreen.containsKey(id)) {
                    Node prjNode;
                    if (type == 1) {
                        Rectangle rect = new Rectangle(px, py, 15, 4);
                        rect.setFill(Color.YELLOW);
                        int dx = Integer.parseInt(parts[5]);
                        int dy = Integer.parseInt(parts[6]);
                        if (dx != 0) {
                            rect.setRotate(dx == 1 ? 0 : 180);
                        } else {
                            rect.setRotate(dy == 1 ? 90 : -90);
                        }
                        prjNode = rect;
                    } else {
                        Circle circle = new Circle(px, py, 8, Color.BLACK);
                        circle.setStroke(Color.ORANGE);
                        circle.setStrokeWidth(2);
                        prjNode = circle;
                    }

                    projectilesOnScreen.put(id, prjNode);
                    gameWorld.getChildren().add(prjNode);
                    prjNode.toFront();

                } else {
                    Node prjNode = projectilesOnScreen.get(id);
                    if (prjNode instanceof Rectangle) {
                        ((Rectangle)prjNode).setX(px);
                        ((Rectangle)prjNode).setY(py);
                    }
                    else if (prjNode instanceof Circle) {
                        ((Circle)prjNode).setCenterX(px);
                        ((Circle)prjNode).setCenterY(py);
                    }
                }
            }
        }

        // مسح الحاجات اللي اختفت
        playersOnScreen.entrySet().removeIf(e -> {
            if (!activeIds.containsKey(e.getKey())) {
                gameWorld.getChildren().remove(e.getValue());
                return true;
            }
            return false;
        });

        enemiesOnScreen.entrySet().removeIf(e -> {
            if (!activeIds.containsKey(-e.getKey())) {
                gameWorld.getChildren().remove(e.getValue());
                return true;
            }
            return false;
        });

        projectilesOnScreen.entrySet().removeIf(e -> {
            if (!activeIds.containsKey(99000 + e.getKey())) {
                gameWorld.getChildren().remove(e.getValue());
                return true;
            }
            return false;
        });

        updateFogLights();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
