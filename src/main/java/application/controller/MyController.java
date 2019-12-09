package application.controller;

import application.async.SearchMusicTask;
import application.music.kuwo.KuwoMusic;
import application.music.kuwo.playingView.PlayingPanel;
import application.music.pojo.kuwo.KuwoPojo;
import application.screenshot.ScreenShot;
import application.translate.baidu.BaiDuTrans;
import application.utils.LyricShowUtil;
import application.utils.MarsException;
import application.utils.MarsLogUtil;
import application.utils.StringUtil;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.Mnemonic;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Setter;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

//@SuppressWarnings("all")
public class MyController implements Initializable {

    @FXML
    private Button myBtn;

    @FXML
    private TextArea inText;
    @FXML
    private TextArea outText;
    @FXML
    private Button playMusic;
    @FXML
    private TextField searchMusicText;
    @FXML
    private ListView<KuwoPojo> musicList;
    @FXML
    private Button screenBtn;
    @FXML
    private TextField lrcText;

    private static KuwoPojo nowMusic;
    private static KuwoPojo selectMusic;
    @Setter
    public Scene scene;
    @Setter
    public Stage mainStage;

    private KuwoMusic kuwoMusic = KuwoMusic.obj;

    /**
     * 作为成员变量，保证了暂停再次播放的时候是同一首歌
     */
    private static MediaPlayer player;

    private ObservableList<KuwoPojo> data;

    private static LyricShowUtil lyricShowUtil;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // 初始化音乐相关信息
        initMusicInfo();

        System.out.println("初始化controller,在加载该类对应的fxml问件时就会被调用");
    }

    /**
     * @Description 音乐相关组件初始化
     * @author LIu Mingyao
     */
    public void initMusicInfo() {
        lyricShowUtil = new LyricShowUtil();
        // 初始化listview的可观察列表(搜索音乐之后的展示列表)
        data = musicList.getItems();

        // 监听音乐选择列表
        musicList.getSelectionModel().selectedItemProperty()
                .addListener((ObservableValue<? extends KuwoPojo> observable, KuwoPojo oldValue,
                              KuwoPojo newValue) -> {
                    System.out.println(newValue + "   newValue");
                    selectMusic = newValue;
                });

    }

    /**
     * @param event
     * @Description 翻译
     * @author LIu Mingyao
     */
    public void trans(ActionEvent event) {
        String in = inText.getText();
        String result;
        try {
            result = BaiDuTrans.getTransResult(in, "auto", "auto");
        } catch (MarsException e) {
            result = "翻译出错";
            MarsLogUtil.debug(getClass(), "翻译出错", e);
        }
        outText.setText(result);

    }

    /**
     * 根据searchMusicText内容搜歌
     */
    public void search() {
        musicList.setCellFactory(null);

        String searchStr = searchMusicText.getText().replaceAll(" ", "+");// 酷我搜索会将空格替换为+
        // searchStr = "2";
        if (StringUtil.isEmpty(searchStr)) {
            MarsLogUtil.debug(getClass(), "*****************请先输入搜索内容*****************");
            return;
        }

        // 搜索之前先清空之前内容
        if (data != null) {
            data.clear();
        }

        MarsLogUtil.info(getClass(), "正在搜索歌曲.......");
        SearchMusicTask searchMusicTask = new SearchMusicTask(searchStr);
        CompletableFuture.runAsync(searchMusicTask);
        searchMusicTask.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue == null) {
                MarsLogUtil.info(getClass(), "搜索歌曲完毕.......");
            } else {
                data.add(newValue);
            }
        });
        /*
         * 将Music对象的部分属性(name)取出来展示在ListView面板
         */
        musicList.setCellFactory(TextFieldListCell.forListView(new StringConverter<KuwoPojo>() {

            @Override
            public String toString(KuwoPojo music) {
                // TODO Auto-generated method stub
                return music != null ? music.getMname() : "";
            }

            @Override
            public KuwoPojo fromString(String string) {
                // TODO Auto-generated method stub
                return null;
            }
        }));
    }

    /**
     * 播放音乐
     */
    public void play(ActionEvent event) {

        if (selectMusic == null) {
            System.out.println("*****************请先选择音乐再播放*****************");
            return;
        }
        // 切歌
        if (player != null && selectMusic != nowMusic) {
            String statu = player.getStatus().toString();
            if (!statu.equals(MediaPlayer.Status.STOPPED.toString())) {
                lyricShowUtil.lyricThread.interrupt();
                player.stop();
            }
        }
        nowMusic = selectMusic;
        player = nowMusic.getPlayer();

        MarsLogUtil.info(getClass(), "====正在播放=======" + nowMusic.getMname());

        // 歌词同步
        lyricShowUtil.showLyricInfo(lrcText, nowMusic, player);

        // 打开专用播放面板
        PlayingPanel.obj.openPlayingState(mainStage, player, lyricShowUtil);

    }

    public void stop(ActionEvent event) {

    }

    public void pause(ActionEvent event) {

    }

    public void nextPageMusic(ActionEvent event) {
        System.out.println("====下一页======");

    }

    /**
     * @Description 主面板快捷键绑定, 在main方法中被调用
     * @author LIu Mingyao
     */
    public void shortcutKeys() {

        // 绑定截图快捷键
        KeyCombination screenKey = KeyCombination.valueOf("ctrl+alt+p");
        Mnemonic mc = new Mnemonic(screenBtn, screenKey);
        scene.addMnemonic(mc);

        // 翻译快捷键
        KeyCombination searchKey = KeyCombination.valueOf("ctrl+alt+i");
        Mnemonic search = new Mnemonic(myBtn, searchKey);
        scene.addMnemonic(search);
    }

    /**
     * @Description 截图按钮点击事件
     * @author LIu Mingyao
     */
    public void screenShot() {
        ScreenShot screenShot = ScreenShot.initScreenShot();
        screenShot.showScreenPanel(mainStage);
    }

}
