package Views;

import Core.AppSingletone;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import ru.zetapps.filework.FileWorkMain;
import ru.zetapps.websavermessages.MessageType;
import ru.zetapps.websavermessages.Messages.AuthMessage;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class LoginScene {

    private Socket socket;

    private Double xOffset,yOffset;
    @FXML
    ToolBar authtoolbar;
    @FXML
    Button exitButton;
    @FXML
    Button authButton;
    @FXML
    TextField authLoginField;
    @FXML
    PasswordField authPassField;
    @FXML
    TextField regLoginField;
    @FXML
    TextField regPassField;
    @FXML
    TextField regNickField;
    @FXML
    TextField regEmailField;
    @FXML
    TextField regFNameField;
    @FXML
    TextField regSNameField;
    @FXML
    TextField regAgeField;
    @FXML
    TitledPane loginPane;
    @FXML
    TitledPane regPane;


    public void initialize(){
        socket = AppSingletone.getInstance().getSocket();
        System.out.println(System.getProperty("user.dir")+"\\src\\main\\resources\\setting.cfg");
        authtoolbar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = AppSingletone.getInstance().getCurrentStage().getX() - event.getScreenX();
                yOffset = AppSingletone.getInstance().getCurrentStage().getY() - event.getScreenY();
            }
        });

        authtoolbar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                AppSingletone.getInstance().getCurrentStage().setX(event.getScreenX() + xOffset);
                AppSingletone.getInstance().getCurrentStage().setY(event.getScreenY() + yOffset);
            }
        });
    }

    public void exitApplication(){
        Platform.exit();
        System.exit(0);
    }

    public void authentificate(){
        try {
            List<String> conf = Files.readAllLines(Paths.get(System.getProperty("user.dir")+"\\src\\main\\resources\\settings.cfg"));
            System.out.println(conf.get(0).toString());
            AppSingletone.getInstance().initFileWork(conf.get(0).split("=")[1],authLoginField.getText());
            AppSingletone.getInstance().getHandler().sendMessage(new AuthMessage(MessageType.AUTHORISATION,authLoginField.getText()+","+authPassField.getText()));
            AppSingletone.getInstance().setUserLogin(authLoginField.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean res = AppSingletone.getInstance().waitAnswer(MessageType.AUTHORISATION,
                "Авторизация прошла успешно!",
                "Что-то не так...либо пользователя не существует, либо вы ввели неправильные данные!",
                "Авторизация");
        if (res){
            try {
                AppSingletone.getInstance().openNewWindow("MainScreen.fxml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void registration(){
        try {
            AppSingletone.getInstance()
                         .getHandler()
                         .sendMessage(new AuthMessage(MessageType.REGISTRATION,
                                      regLoginField.getText()+"," +
                                              regPassField.getText()+"," +
                                              regNickField.getText()+"," +
                                              regEmailField.getText()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean res =AppSingletone.getInstance().waitAnswer(MessageType.REGISTRATION,
                "Регистрация прошла успешно! Теперь необходимо выбрать папку для хранения файлов, а затем авторизоваться!",
                "Произошла ошибка при регистрации...попробуйте еще раз и все получится!",
                "Регистрация");
        if(res){

            try {
                JFileChooser fChooser = new JFileChooser();
                fChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fChooser.showSaveDialog(null);
                AppSingletone.getInstance().initFileWork(fChooser.getSelectedFile().toString(),regLoginField.getText());
                AppSingletone.getInstance().setUserLogin(regLoginField.getText());
                //Directories directory = new Directories();
                String content ="UserPath=" + fChooser.getSelectedFile().toString();
                Files.write(Paths.get(System.getProperty("user.dir")+"\\src\\main\\resources\\settings.cfg"), content.getBytes(), StandardOpenOption.CREATE);

                AppSingletone.getInstance().getFw().directories.makeUserDir("");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
