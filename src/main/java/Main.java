import Core.AppSingletone;
import com.sun.glass.ui.Window;
import com.sun.javafx.css.Style;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class Main extends Application {

    AppSingletone singletone;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        singletone = AppSingletone.getInstance();
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("LoginScene.fxml"));
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("Авторизация");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        singletone.setCurrentStage(primaryStage);
    }
}
