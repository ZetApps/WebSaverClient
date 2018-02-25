package Views;

import Core.AppSingletone;
import Core.Synchro;
import com.sun.jndi.toolkit.url.Uri;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToolBar;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import org.eclipse.fx.ui.controls.filesystem.*;
import ru.zetapps.websavermessages.Message;
import ru.zetapps.websavermessages.MessageType;
import ru.zetapps.websavermessages.Messages.CommandMessage;
import ru.zetapps.websavermessages.Messages.FileMessage;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Observable;

public class MainScreen {

    private Double xOffset,yOffset;
    private String settingFile= System.getProperty("user.dir")+"\\src\\main\\resources\\settings.cfg";
    private String userDir;
    private DirItem mainDir;
    private Synchro synchro;
    @FXML
    DirectoryTreeView dirView;
    @FXML
    DirectoryView sDirView;
    @FXML
    ToolBar menuBar;
    @FXML
    ProgressIndicator progress;
    @FXML
    Label statustext;
    @FXML
    HBox statusbar;


    public void initialize(){
        try {
            synchro = new Synchro(statusbar,progress,statustext,AppSingletone.getInstance().getUserLogin());
            statusbar.setVisible(false);
            List<String> lines = Files.readAllLines(Paths.get(settingFile));
            userDir = lines.get(0).split("=")[1] + AppSingletone.getInstance().getUserLogin();
            if (!Files.exists(Paths.get(userDir))){
                Files.createDirectory(Paths.get(userDir));
            }
            System.out.println(userDir);
            mainDir = ResourceItem.createObservedPath(Paths.get(new File(userDir).getPath()));
            dirView.setRootDirectories(FXCollections.observableArrayList(mainDir));
            dirView.setIconSize(IconSize.MEDIUM);
            dirView.getSelectedItems().addListener(new ListChangeListener<DirItem>() {
                @Override
                public void onChanged(Change<? extends DirItem> c) {
                    if (!dirView.getSelectedItems().isEmpty()){
                        sDirView.setDir(dirView.getSelectedItems().get(0));
                        System.out.println("Текущая выбранная папка - " + sDirView.getDir().getUri().toString());
                        synchro.sync_folder(AppSingletone.getInstance().getFw().directories.getCloudPath(sDirView.getDir().getUri().toString().replace("%20"," "),
                                                                                                         AppSingletone.getInstance().getUserLogin()));
                    }else{
                        sDirView.setDir(null);
                    }

                }
            });

            menuBar.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    xOffset = AppSingletone.getInstance().getCurrentStage().getX() - event.getScreenX();
                    yOffset = AppSingletone.getInstance().getCurrentStage().getY() - event.getScreenY();
                }
            });

            menuBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    AppSingletone.getInstance().getCurrentStage().setX(event.getScreenX() + xOffset);
                    AppSingletone.getInstance().getCurrentStage().setY(event.getScreenY() + yOffset);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void quitapp(){
        try {
            AppSingletone.getInstance().getSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Platform.exit();
        System.exit(0);
    }

    public void logout(){
        try {
            AppSingletone.getInstance().openNewWindow("LoginScene.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onAddNewFileBtnClick(){
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Выберите файл для загрузки");
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.showOpenDialog(null);
        fileChooser.setMultiSelectionEnabled(true);
        System.out.println("Файлы выбраны");
        System.out.println(fileChooser.getSelectedFile().toString());
        /*File[] selectedFiles = fileChooser.getSelectedFiles();
        for (File fl:
             selectedFiles) {
            System.out.println("Обработка файла - " + fl.getName());
            AppSingletone.getInstance().getFw().files.saveFile(fl,Paths.get(sDirView.getDir().getUri()).toAbsolutePath().toString(),fl.getName());

        }*/
        File fl = fileChooser.getSelectedFile();
        try {
            AppSingletone.getInstance().getHandler().sendMessage(new FileMessage(MessageType.UPLOAD,fl.getPath(),
                    AppSingletone.getInstance().getFw().files.getCloudPath(sDirView.getSelectedItems().get(0).getUri(),AppSingletone.getInstance().getUserLogin()),
                    fl.getName(),fl.length(),fl.lastModified()));
            AppSingletone.getInstance().waitAnswer(MessageType.UPLOAD,"Файл добавлен успешно!",
                                                                      "Произошла ошибка при добавлении файла, пожалуйста попробуйте снова!",
                                                                      "Добавление нового файла");
            AppSingletone.getInstance().getFw().files.saveFile(fl,AppSingletone.getInstance().getUserLogin(),fl.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onfilerenameclick(){
        String login = AppSingletone.getInstance().getUserLogin();
        System.out.println("Переименование файла");
        String changenamepath = sDirView.getSelectedItems().get(0).getUri();
        System.out.println(changenamepath);

        String oldname = sDirView.getSelectedItems().get(0).getName().split("\\.")[0];
        System.out.println(oldname.split("\\.")[0]);
        String newname = JOptionPane.showInputDialog("Введите новое имя файла:");
        System.out.println(changenamepath.replace(oldname.replace(" ","%20"),newname.replace(" ","%20")));
        try {
            boolean res = false;
            AppSingletone.getInstance().getHandler().sendMessage(new CommandMessage(MessageType.FILE_RENAME,AppSingletone.getInstance().getFw().files.getCloudPath(changenamepath, login)+";"+
                                                                                                        AppSingletone.getInstance().getFw().files.getCloudPath(changenamepath.replace(oldname.replace(" ","%20"),newname.replace(" ","%20")),login)));
            res = AppSingletone.getInstance().waitAnswer(MessageType.FILE_RENAME,"Файл переименован!","Произошла ошибка... попробуйте еще раз!","Переименование файла");
            System.out.println(res);
            if (res) {
                System.out.println("Начинаем переименовывать - "+changenamepath.replace("file:/","").replace("%20"," ") + " в " + newname);
                //File file = new File(changenamepath.replace("file:/","").replace("%20"," "));
                //file.renameTo(new File(changenamepath.replace(oldname.replace(" ","%20"),newname)));
                AppSingletone.getInstance().getFw().files.rename(AppSingletone.getInstance().getFw().files.getCloudPath(changenamepath,login),
                                                                 AppSingletone.getInstance().getFw().files.getCloudPath(changenamepath.replace(oldname.replace(" ","%20"),newname.replace(" ","%20")),login));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void ondeletefolderaction(){
        ondeleteaction(dirView.getSelectedItems().get(0).getUri(), MessageType.FOLDER_DELETE);
    }

    public void ondeletefileaction(){
        ondeleteaction(sDirView.getSelectedItems().get(0).getUri(), MessageType.FILE_DELETE);
    }

    public void ondeleteaction(String path, MessageType msgtype){
        String login = AppSingletone.getInstance().getUserLogin();
        System.out.println("Удаление папки/файла");
        try{
            AppSingletone.getInstance().getHandler().sendMessage(new CommandMessage(msgtype,AppSingletone.getInstance().getFw().files.getCloudPath(path,login)));
            if (AppSingletone.getInstance().waitAnswer(msgtype,"Удаление прошло успешно!","Ошибка удаления","Удаление")){
                AppSingletone.getInstance().getFw().files.delete(AppSingletone.getInstance().getFw().files.getCloudPath(path,login));
            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void onnewfolderaction(){
        String login = AppSingletone.getInstance().getUserLogin();
        System.out.println("Создание папки");
        String newname = JOptionPane.showInputDialog("Введите новое имя файла:");
        String currpath = dirView.getSelectedItems().get(0).getUri();
        try {
            AppSingletone.getInstance().getHandler().sendMessage(new CommandMessage(MessageType.FOLDER_CREATE,AppSingletone.getInstance().getFw().directories.getCloudPath(currpath, login) + "\\" + newname));
            if (AppSingletone.getInstance().waitAnswer(MessageType.FOLDER_CREATE,"Папка создана успешно!","Ошибка при создании новой папки","Новая папка")){
                AppSingletone.getInstance().getFw().directories.makeDir(AppSingletone.getInstance().getFw().directories.getCloudPath(currpath, login) + "\\" + newname);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }


}
