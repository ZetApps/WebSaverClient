package Core;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import ru.zetapps.filework.FileWorkMain;
import ru.zetapps.websavermessages.Message;
import ru.zetapps.websavermessages.MessageType;
import ru.zetapps.websavermessages.Messages.AnswerMessage;
import ru.zetapps.websavermessages.Messages.MessageHandler;

import java.io.IOException;
import java.net.Socket;

public class AppSingletone {

    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private Socket socket;
    private MessageHandler handler;
    private Stage currentStage;
    private Scene curentScene;
    private FileWorkMain fw;
    private String userLogin;

    private static AppSingletone ourInstance = new AppSingletone();

    public static AppSingletone getInstance() {
        return ourInstance;
    }

    private AppSingletone() {
        try {
            socket = new Socket(HOST, PORT);
            System.out.println("Подлючение успешно!");
            handler = new MessageHandler(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Socket getSocket() {
        return socket;
    }

    public MessageHandler getHandler() {
        return handler;
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }


    public boolean waitAnswer(MessageType type, String goodAns, String badAns, String title){
        boolean res = false;
        try {
            Message msg = handler.getMessage();
            if (msg.getClass()== AnswerMessage.class){
                AnswerMessage amsg = (AnswerMessage) msg;
                if (amsg.getType() == type){
                    Alert alert;

                    if (amsg.getContent().equals("good")){
                        alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setContentText(goodAns);
                        res = true;
                    }else if(amsg.getContent().equals("bad")){
                        alert = new Alert(Alert.AlertType.ERROR);
                        alert.setContentText(badAns);
                        res = false;
                    }else{
                        alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setContentText(amsg.getContent());
                        res = false;
                    }
                    alert.setTitle(title);
                    alert.setHeaderText(null);
                    alert.showAndWait();
                    return res;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void openNewWindow(String FxmlString) throws IOException{
        Parent newPageParent = FXMLLoader.load(getClass().getClassLoader().getResource(FxmlString));
        Scene newScene = new Scene(newPageParent);
        getCurrentStage().setScene(newScene);
        getCurrentStage().show();
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public FileWorkMain getFw() {
        return fw;
    }

    public void initFileWork(String path, String login){
        if (fw == null){
            //fw = new FileWorkMain(path + "\\" + login);
            fw = new FileWorkMain(path);
        }
    }
}
