package Core;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import ru.zetapps.websavermessages.Message;
import ru.zetapps.websavermessages.MessageType;
import ru.zetapps.websavermessages.Messages.AnswerMessage;
import ru.zetapps.websavermessages.Messages.CommandMessage;
import ru.zetapps.websavermessages.Messages.FileMessage;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Synchro {
    private HBox statusbar;
    private ProgressIndicator progress;
    private Label status;
    private String login;

    public Synchro(HBox statusbar, ProgressIndicator progress, Label status, String login) {
        this.statusbar = statusbar;
        this.progress = progress;
        this.status = status;
        this.login = login;
    }

    public void sync_folder(String folder){
        Thread synchTread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    statusbar.setVisible(true);
                    status.setText("Получение списка файлов и папок на сервере");
                    System.out.println("ЗАПРОС - " + AppSingletone.getInstance().getFw().directories.getCloudPath(folder,login));
                    AppSingletone.getInstance().getHandler().sendMessage(new CommandMessage(MessageType.SYNC_GET,AppSingletone.getInstance().getFw().directories.getCloudPath(folder,login)));
                    String ans = getSyncMsg();
                    System.out.println("РЕЗУЛЬТАТ ЗАПРОСА - " + ans);
                    makeFolders(ans.substring(0,ans.indexOf("$$$")), status,progress);
                    makeFiles(ans.substring(ans.indexOf("$$$")+3),status,progress);
                    status.setText("Получилось!");
                } catch (IOException e) {
                    System.err.println("Ошибка при отправки сообщения о начале синхронизации папки");
                    e.printStackTrace();
                }
            }
        });
        synchTread.run();
    }

    private String getSyncMsg(){
        try {
            Message msg = AppSingletone.getInstance().getHandler().getMessage();
            if (msg.getClass() == AnswerMessage.class){
                AnswerMessage amsg = (AnswerMessage) msg;
                if (amsg.getType() == MessageType.SYNC_GET){
                    return amsg.getContent();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void makeFolders(String flda, Label status, ProgressIndicator prog){
        System.out.println(flda);
        status.setText("Синхронизация папок");
        if(flda.replace("dir:","").split(";").length>0){
            flda = flda.substring(0,flda.length()-1);
        }
        if (flda.length()>0) {
            String[] fldrList = flda.replace("dir:","").split(";");
            for (int i=0;i<fldrList.length;i++) {
                if (!AppSingletone.getInstance().getFw().directories.makeDir(fldrList[i])) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка синхронизации папки " + fldrList[i]);
                    alert.setContentText("Дождитесь окончания синхронизации и запустите ее самостоятельно");
                    alert.show();
                }else{
                    prog.setProgress(i/fldrList.length);
                }
            }
        }
    }

    private void makeFiles(String fla, Label status, ProgressIndicator prog){
        JOptionPane optionPane = new JOptionPane();
        String pth = "";
        long lastchange, repl;
        status.setText("Синхронизация файлов");
        System.out.println(fla);
        if(fla.replace("files:","").split(";").length>0){
            fla = fla.substring(0,fla.length()-1);
        }
        if (fla.length()>0) {
            String[] fldrList = fla.replace("files:","").split(";");
            for (String fld : fldrList) {
                pth = fld.substring(0,fld.indexOf("###"));
                lastchange = Long.parseLong(fld.substring(fld.indexOf("###")+3));
                if (Files.exists(Paths.get(AppSingletone.getInstance().getFw().files.getAbsPath(pth)))){ //Если файл доступен
                    File fl = new File(AppSingletone.getInstance().getFw().files.getAbsPath(pth));
                    if (fl.lastModified() != lastchange){
                        if (fl.lastModified() > lastchange){
                            repl = optionPane.showConfirmDialog(null,
                                    "Файл на компьютере более новый, обновить имеющийся файл?",
                                    "Обновление файла на сервере",
                                    JOptionPane.YES_NO_OPTION);
                            if (repl == JOptionPane.YES_OPTION){
                                try {
                                    AppSingletone.getInstance().getHandler().sendMessage(new FileMessage(MessageType.UPLOAD,fl.getPath(),
                                            AppSingletone.getInstance().getFw().directories.getCloudPath(fl.getParent(),AppSingletone.getInstance().getUserLogin()),
                                            fl.getName(),fl.length(),fl.lastModified()));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }else{
                            repl = optionPane.showConfirmDialog(null,
                                    "Файл на сервере более новый, обновить имеющийся файл?",
                                    "Обновление локального файла",
                                    JOptionPane.YES_NO_OPTION);
                            if (repl == JOptionPane.YES_OPTION){
                                try {
                                    AppSingletone.getInstance().getHandler().sendMessage(new CommandMessage(MessageType.DOWNLOAD,pth));
                                    FileMessage msg = (FileMessage) AppSingletone.getInstance().getHandler().getMessage();
                                    AppSingletone.getInstance().getFw().files.delete(AppSingletone.getInstance().getFw().files.getCloudPath(fl.getPath(),AppSingletone.getInstance().getUserLogin()));
                                    AppSingletone.getInstance().getFw(). files.saveFile(msg.getContent(),
                                            AppSingletone.getInstance().getFw().directories.getCloudPath(fl.getParent(),AppSingletone.getInstance().getUserLogin()),
                                            fl.getName());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }else{                                                                                      //Если файл не доступен
                    try {
                        String name =pth.substring(pth.lastIndexOf("\\")+1);
                        String savepath = pth.substring(0,pth.lastIndexOf("\\"));
                        System.out.println("Name of downloaded file - " + name);
                        AppSingletone.getInstance().getHandler().sendMessage(new CommandMessage(MessageType.DOWNLOAD,pth));
                        FileMessage msg = (FileMessage) AppSingletone.getInstance().getHandler().getMessage();
                        AppSingletone.getInstance().getFw(). files.saveFile(msg.getContent(),
                                AppSingletone.getInstance().getFw().directories.getCloudPath(savepath,AppSingletone.getInstance().getUserLogin()),
                                name);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
