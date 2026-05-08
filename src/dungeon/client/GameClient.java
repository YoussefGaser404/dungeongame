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
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
    private Image shopImg;

    private Label uiLabel;
    private Label goldPopUp;
    private Label msgPopUp;
    private Pane gameWorld;
    private VBox shopUI;

    private final int TILE_SIZE = 40;
    private final int ROOM_SIZE = 15;
    private int myId = -1;
    private PrintWriter out;

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
            cardsBox.getChildren().add(buildCard(stage, info));
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
    // الدخول للعبة والاتصال
    // --------------------------------------------------------
    private void startGame(Stage stage, String selectedClass) {
        try {
            // تحميل الصور كلها
            wallImg = new Image("file:assets/wall.png");
            floorImg = new Image("file:assets/floor.png");
            doorImg = new Image("file:assets/door.png");
            keyImg = new Image("file:assets/key.png");
            appleImg = new Image("file:assets/apple.png");
            chestClosedImg = new Image("file:assets/chest_closed.png");
            chestOpenImg = new Image("file:assets/chest_open.png");
            enemy1Img = new Image("file:assets/enemy1.png");
            enemy2Img = new Image("file:assets/enemy2.png");
            shopImg = new Image("file:assets/shop.png");

            classImages.put("warrior", new Image("file:assets/warrior.png"));
            classImages.put("rogue", new Image("file:assets/rogue.png"));
            classImages.put("archer", new Image("file:assets/archer.png"));

            Socket socket = new Socket("localhost", 5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("INIT:" + selectedClass);

            gameWorld = new Pane();
            Rectangle fog = new Rectangle(600, 600, Color.rgb(0, 0, 0, 0.5));

            // تصميم الـ UI بتاع الدم والفلوس
            uiLabel = new Label("");
            uiLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 8px; -fx-border-radius: 5px;");
            uiLabel.setLayoutX(10);
            uiLabel.setLayoutY(10);

            // تأثير الـ 50 كوين
            goldPopUp = new Label("+50 Coins!");
            goldPopUp.setStyle("-fx-text-fill: #00ff00; -fx-font-size: 26px; -fx-font-weight: bold;");
            goldPopUp.setOpacity(0);
            goldPopUp.setLayoutX(250);
            goldPopUp.setLayoutY(250);

            // رسائل المتجر
            msgPopUp = new Label("");
            msgPopUp.setStyle("-fx-text-fill: #ffcc00; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 10px; -fx-border-radius: 10px;");
            msgPopUp.setOpacity(0);
            msgPopUp.setLayoutX(150);
            msgPopUp.setLayoutY(100);

            // واجهة المتجر (Shop)
            shopUI = new VBox(15);
            shopUI.setAlignment(Pos.CENTER);
            shopUI.setStyle("-fx-background-color: rgba(30,30,40,0.95); -fx-border-color: gold; -fx-border-width: 3; -fx-padding: 20; -fx-border-radius: 10; -fx-background-radius: 10;");
            shopUI.setLayoutX(150);
            shopUI.setLayoutY(150);
            shopUI.setPrefSize(300, 200);
            shopUI.setVisible(false);

            Label shopTitle = new Label("🛒 UPGRADE SHOP");
            shopTitle.setStyle("-fx-text-fill: gold; -fx-font-size: 20px; -fx-font-weight: bold;");

            Button buyShield = new Button("Buy Shield (+20) - 50 Coins");
            buyShield.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

            Button buyAttack = new Button("Buy Attack (+10) - 100 Coins");
            buyAttack.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

            buyShield.setOnAction(e -> out.println("BUY:SHIELD"));
            buyAttack.setOnAction(e -> out.println("BUY:ATTACK"));

            shopUI.getChildren().addAll(shopTitle, buyShield, buyAttack);

            Scene scene = new Scene(new Group(gameWorld, fog, uiLabel, goldPopUp, msgPopUp, shopUI), 600, 600);

            // أزرار التحكم
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.W || e.getCode() == KeyCode.UP) out.println("UP");
                if (e.getCode() == KeyCode.S || e.getCode() == KeyCode.DOWN) out.println("DOWN");
                if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT) out.println("LEFT");
                if (e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT) out.println("RIGHT");
                if (e.getCode() == KeyCode.E) out.println("INTERACT");
                if (e.getCode() == KeyCode.F) out.println("USE_APPLE");
                if (e.getCode() == KeyCode.SPACE) out.println("ATTACK");
                if (e.getCode() == KeyCode.B) shopUI.setVisible(!shopUI.isVisible());
            });

            // استقبال التحديثات من السيرفر
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        String fl = line;
                        Platform.runLater(() -> {
                            if (fl.startsWith("MYID:")) {
                                myId = Integer.parseInt(fl.substring(5));
                            } else if (fl.startsWith("MAP:")) {
                                drawMap(fl.substring(4), gameWorld);
                            } else if (fl.startsWith("STATE:")) {
                                updateUI(fl.substring(6), gameWorld);
                            } else if (fl.startsWith("EVENT:")) {
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
    // التعامل مع الأحداث
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
        else if (evt.equals("SHOP_SUCCESS")) {
            showMsg("Upgrade Bought Successfully!");
        }
        else if (evt.equals("SHOP_FAIL")) {
            showMsg("Not Enough Coins!");
        }
        else if (evt.equals("SHOP_FAR")) {
            showMsg("You are too far from the Shop!");
        }
        else if (evt.startsWith("EXPLOSION:")) {
            String[] p = evt.split(":");
            double exX = Integer.parseInt(p[1]) * TILE_SIZE - TILE_SIZE;
            double exY = Integer.parseInt(p[2]) * TILE_SIZE - TILE_SIZE;

            Rectangle exp = new Rectangle(exX, exY, TILE_SIZE * 3, TILE_SIZE * 3);
            exp.setFill(Color.rgb(255, 50, 0, 0.6));
            gameWorld.getChildren().add(exp);

            FadeTransition ft = new FadeTransition(Duration.millis(400), exp);
            ft.setToValue(0);
            ft.setOnFinished(e -> gameWorld.getChildren().remove(exp));
            ft.play();
        }
        else if (evt.startsWith("SLASH:")) {
            String[] p = evt.split(":");
            double sX = Integer.parseInt(p[1]) * TILE_SIZE;
            double sY = Integer.parseInt(p[2]) * TILE_SIZE;

            Rectangle slash = new Rectangle(sX, sY, TILE_SIZE, TILE_SIZE);
            slash.setFill(Color.WHITE);
            gameWorld.getChildren().add(slash);

            FadeTransition ft = new FadeTransition(Duration.millis(200), slash);
            ft.setToValue(0);
            ft.setOnFinished(e -> gameWorld.getChildren().remove(slash));
            ft.play();
        }
    }

    private void showMsg(String text) {
        msgPopUp.setText(text);
        msgPopUp.setOpacity(1.0);
        msgPopUp.setTranslateY(0);

        TranslateTransition moveUp = new TranslateTransition(Duration.millis(1500), msgPopUp);
        moveUp.setByY(-30);

        FadeTransition fade = new FadeTransition(Duration.millis(1500), msgPopUp);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        moveUp.play();
        fade.play();
    }

    // --------------------------------------------------------
    // رسم الخريطة والأشياء الثابتة
    // --------------------------------------------------------
    private void drawMap(String mapData, Pane gameWorld) {
        gameWorld.getChildren().removeIf(node ->
                node instanceof ImageView &&
                        !playersOnScreen.values().contains(node) &&
                        !enemiesOnScreen.values().contains(node)
        );

        String[] rows = mapData.split(";");
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < rows[i].length(); j++) {
                char tile = rows[i].charAt(j);

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
                    apple.setFitWidth(TILE_SIZE + 4);
                    apple.setFitHeight(TILE_SIZE + 4);
                    apple.setX((j * TILE_SIZE) - 2);
                    apple.setY((i * TILE_SIZE) - 2);
                    gameWorld.getChildren().add(apple);
                }
                else if (tile == '8') {
                    ImageView shop = new ImageView(shopImg);
                    shop.setX(j * TILE_SIZE);
                    shop.setY(i * TILE_SIZE);
                    shop.setFitWidth(TILE_SIZE);
                    shop.setFitHeight(TILE_SIZE);
                    gameWorld.getChildren().add(shop);
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
    }

    // --------------------------------------------------------
    // تحديث الشاشة ورسم اللعيبة والوحوش والأسلحة
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

            if (parts[0].equals("P")) {
                int id = Integer.parseInt(parts[1]);
                int gridX = Integer.parseInt(parts[2]);
                int gridY = Integer.parseInt(parts[3]);
                String cls = parts[4];
                int coins = Integer.parseInt(parts[5]);
                String hasKey = parts[6];
                int hp = Integer.parseInt(parts[7]);
                int apples = Integer.parseInt(parts[8]);
                int shield = Integer.parseInt(parts[9]);
                int attack = Integer.parseInt(parts[10]);

                activeIds.put(id, true);

                if (id == myId) {
                    uiLabel.setText("❤ HP: " + hp + " | 🛡 Shield: " + shield + " | ⚔ Attack: " + attack + "\n💰 Coins: " + coins + " | 🎒 Inv: " + (hasKey.equals("1") ? "Key" : "Empty") + " (🍎x" + apples + ")");
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}