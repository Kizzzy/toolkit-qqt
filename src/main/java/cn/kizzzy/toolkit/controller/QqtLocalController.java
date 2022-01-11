package cn.kizzzy.toolkit.controller;

import cn.kizzzy.event.EventArgs;
import cn.kizzzy.helper.FileHelper;
import cn.kizzzy.helper.LogHelper;
import cn.kizzzy.helper.StringHelper;
import cn.kizzzy.javafx.TreeItemCell;
import cn.kizzzy.javafx.TreeItemComparator;
import cn.kizzzy.javafx.common.JavafxHelper;
import cn.kizzzy.javafx.common.MenuItemArg;
import cn.kizzzy.javafx.display.DisplayTabView;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.javafx.setting.ISettingDialogFactory;
import cn.kizzzy.javafx.setting.SettingDialogFactory;
import cn.kizzzy.qqt.QqtConfig;
import cn.kizzzy.qqt.QqtFile;
import cn.kizzzy.qqt.QqtIdx;
import cn.kizzzy.qqt.QqtImg;
import cn.kizzzy.qqt.QqtImgItem;
import cn.kizzzy.qqt.QqtMap;
import cn.kizzzy.qqt.display.Display;
import cn.kizzzy.qqt.display.DisplayContext;
import cn.kizzzy.qqt.display.DisplayHelper;
import cn.kizzzy.qqt.helper.QqtImgHelper;
import cn.kizzzy.toolkit.extrator.PlayThisTask;
import cn.kizzzy.toolkit.view.AbstractView;
import cn.kizzzy.vfs.IPackage;
import cn.kizzzy.vfs.ITree;
import cn.kizzzy.vfs.handler.BufferedImageHandler;
import cn.kizzzy.vfs.handler.JsonFileHandler;
import cn.kizzzy.vfs.handler.QQtMapHandler;
import cn.kizzzy.vfs.handler.QqtIdxFileHandler;
import cn.kizzzy.vfs.handler.QqtImgHandler;
import cn.kizzzy.vfs.pack.FilePackage;
import cn.kizzzy.vfs.pack.QqtPackage;
import cn.kizzzy.vfs.tree.FileTreeBuilder;
import cn.kizzzy.vfs.tree.IdGenerator;
import cn.kizzzy.vfs.tree.Leaf;
import cn.kizzzy.vfs.tree.Node;
import cn.kizzzy.vfs.tree.QqtTreeBuilder;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

abstract class QqtViewBase extends AbstractView {
    
    @FXML
    protected ChoiceBox<String> show_choice;
    
    @FXML
    protected TextField filterValue;
    
    @FXML
    protected CheckBox include_leaf;
    
    @FXML
    protected CheckBox lock_tab;
    
    @FXML
    protected TreeView<Node<QqtFile>> tree_view;
    
    @FXML
    protected DisplayTabView display_tab;
    
    @FXML
    protected ProgressBar progress_bar;
    
    @FXML
    protected Label tips;
    
    @Override
    public String getName() {
        return "QqtDisplayer";
    }
}

@MenuParameter(path = "辅助/QQ堂/解包器(本地)")
@PluginParameter(url = "/fxml/toolkit/qqt_local_view.fxml", title = "QQ堂(解包)")
public class QqtLocalController extends QqtViewBase implements DisplayContext, Initializable {
    
    protected static final String CONFIG_PATH = "qqt/local.config";
    
    protected static final TreeItemComparator comparator
        = new TreeItemComparator();
    
    protected IPackage userVfs;
    protected QqtConfig config;
    protected ISettingDialogFactory dialogFactory;
    
    protected IPackage vfs;
    protected ITree<QqtFile> tree;
    
    protected Display display = new Display();
    protected TreeItem<Node<QqtFile>> dummyTreeItem;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userVfs = new FilePackage(System.getProperty("user.home") + "/.user");
        userVfs.getHandlerKvs().put(QqtConfig.class, new JsonFileHandler<>(QqtConfig.class));
        
        config = userVfs.load(CONFIG_PATH, QqtConfig.class);
        config = config != null ? config : new QqtConfig();
        
        JavafxHelper.initContextMenu(tree_view, () -> stage.getScene().getWindow(), new MenuItemArg[]{
            new MenuItemArg(0, "设置", this::openSetting),
            new MenuItemArg(1, "加载Idx", this::loadIdx),
            new MenuItemArg(1, "加载目录", this::loadFolder),
            new MenuItemArg(2, "导出/文件", this::exportFile),
            new MenuItemArg(2, "导出/图片", this::exportImage),
            new MenuItemArg(3, "复制路径", this::copyPath),
        });
        
        addListener(DisplayType.TOAST_TIPS, this::toastTips);
        addListener(DisplayType.SHOW_TEXT, this::onDisplayEvent);
        addListener(DisplayType.SHOW_IMAGE, this::onDisplayEvent);
        addListener(DisplayType.SHOW_TABLE, this::onDisplayEvent);
        
        dummyTreeItem = new TreeItem<>();
        tree_view.setRoot(dummyTreeItem);
        tree_view.setShowRoot(false);
        tree_view.getSelectionModel().selectedItemProperty().addListener(this::onSelectItem);
        tree_view.setCellFactory(callback -> new TreeItemCell());
        
        lock_tab.selectedProperty().addListener((observable, oldValue, newValue) -> {
            display_tab.setPin(newValue);
        });
        
        DisplayHelper.load();
    }
    
    @Override
    public void stop() {
        play = false;
        if (playThisTask != null) {
            playThisTask.stop();
        }
        
        if (tree != null) {
            tree.stop();
        }
        
        userVfs.save(CONFIG_PATH, config);
        
        super.stop();
    }
    
    @Override
    public int provideIndex() {
        return show_choice.getSelectionModel().getSelectedIndex();
    }
    
    @Override
    public boolean isFilterColor() {
        return false;//image_filter.isSelected();
    }
    
    protected void toastTips(EventArgs args) {
        Platform.runLater(() -> tips.setText((String) args.getParams()));
    }
    
    protected void onDisplayEvent(final EventArgs args) {
        Platform.runLater(() -> {
            display_tab.show(args.getType(), args.getParams());
        });
    }
    
    protected void loadIdx(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择idx文件");
        if (StringHelper.isNotNullAndEmpty(config.data_path)) {
            File lastFolder = new File(config.data_path);
            if (lastFolder.exists()) {
                chooser.setInitialDirectory(lastFolder);
            }
        }
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("IDX", "*.idx")
        );
        File file = chooser.showOpenDialog(stage);
        if (file != null && file.getAbsolutePath().endsWith(".idx")) {
            config.data_path = file.getParent();
            
            new Thread(() -> {
                try {
                    loadIdxImpl(file);
                } catch (Exception e) {
                    LogHelper.error("load idx error", e);
                }
            }).start();
        }
    }
    
    private void loadIdxImpl(File file) {
        IPackage iPackage = new FilePackage(file.getParent());
        iPackage.getHandlerKvs().put(QqtIdx.class, new QqtIdxFileHandler());
        iPackage.getHandlerKvs().put(QqtMap.class, new QQtMapHandler());
        
        QqtIdx idx = iPackage.load(FileHelper.getName(file.getAbsolutePath()), QqtIdx.class);
        tree = new QqtTreeBuilder(idx, new IdGenerator()).build();
        
        vfs = new QqtPackage(file.getParent(), tree);
        
        Platform.runLater(() -> {
            dummyTreeItem.getChildren().clear();
            
            final List<Node<QqtFile>> nodes = tree.listNode(0);
            for (Node<QqtFile> node : nodes) {
                dummyTreeItem.getChildren().add(new TreeItem<>(node));
            }
        });
    }
    
    private void loadFolder(ActionEvent actionEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择QQ堂根目录");
        
        if (StringHelper.isNotNullAndEmpty(config.qqt_path)) {
            File lastFolder = new File(config.qqt_path);
            if (lastFolder.exists()) {
                chooser.setInitialDirectory(lastFolder);
            }
        }
        File file = chooser.showDialog(stage);
        if (file != null) {
            config.qqt_path = file.getAbsolutePath();
            
            new Thread(() -> {
                try {
                    loadFolderImpl(file);
                } catch (Exception e) {
                    LogHelper.error("load folder error", e);
                }
            }).start();
        }
    }
    
    private void loadFolderImpl(File file) {
        tree = new FileTreeBuilder<QqtFile>(
            file.getAbsolutePath(), new IdGenerator()
        ).build();
        
        vfs = new FilePackage(file.getAbsolutePath());
        vfs.getHandlerKvs().put(QqtImg.class, new QqtImgHandler());
        vfs.getHandlerKvs().put(QqtMap.class, new QQtMapHandler());
        
        Platform.runLater(() -> {
            dummyTreeItem.getChildren().clear();
            
            final List<Node<QqtFile>> nodes = tree.listNode(0);
            for (Node<QqtFile> node : nodes) {
                dummyTreeItem.getChildren().add(new TreeItem<>(node));
            }
        });
    }
    
    @Override
    public QqtFile retrievePkgSubFile(String path) {
        Leaf<QqtFile> leaf = tree.getLeaf(path);
        return leaf != null ? leaf.item : null;
    }
    
    @Override
    public <T> T load(String path, Class<T> clazz) {
        if (vfs != null) {
            return vfs.load(path, clazz);
        }
        return null;
    }
    
    protected void onSelectItem(Observable observable, TreeItem<Node<QqtFile>> oldValue, TreeItem<Node<QqtFile>> newValue) {
        if (newValue != null) {
            Node<QqtFile> folder = newValue.getValue();
            Leaf<QqtFile> thumbs = null;
            
            if (folder.leaf) {
                thumbs = (Leaf<QqtFile>) folder;
            } else {
                newValue.getChildren().clear();
                
                Iterable<Node<QqtFile>> list = folder.children.values();
                for (Node<QqtFile> temp : list) {
                    TreeItem<Node<QqtFile>> child = new TreeItem<>(temp);
                    newValue.getChildren().add(child);
                }
                newValue.getChildren().sort(comparator);
            }
            
            if (thumbs != null) {
                if (display != null) {
                    display.stop();
                }
                display = DisplayHelper.newDisplay(this, thumbs.path);
                display.init();
            }
        }
    }
    
    protected void onChangeLayer(Observable observable, Number oldValue, Number newValue) {
        display.select(newValue.intValue());
    }
    
    @FXML
    protected void openSetting(ActionEvent actionEvent) {
        if (dialogFactory == null) {
            dialogFactory = new SettingDialogFactory(stage);
        }
        dialogFactory.show(config);
    }
    
    @FXML
    protected void showPrev(ActionEvent actionEvent) {
        display.prev();
    }
    
    @FXML
    protected void showNext(ActionEvent actionEvent) {
        display.next();
    }
    
    private boolean play;
    
    @FXML
    protected void play(ActionEvent actionEvent) {
        if (display != null) {
            play = !play;
            ((Button) actionEvent.getSource()).setText(play ? "暂停" : "播放");
            if (play) {
                new Thread(() -> {
                    while (play) {
                        try {
                            Platform.runLater(() -> display.play());
                            Thread.sleep(125);
                        } catch (InterruptedException e) {
                            LogHelper.error(null, e);
                        }
                    }
                }).start();
            }
        }
    }
    
    private boolean playThis;
    private PlayThisTask playThisTask;
    
    @FXML
    private void playThis(ActionEvent event) {
        playThis = !playThis;
        ((Button) event.getSource()).setText(playThis ? "暂停播放" : "连续播放");
        if (playThis) {
            TreeItem<Node<QqtFile>> selected = tree_view.getSelectionModel().getSelectedItem();
            
            List<Display> displays = new ArrayList<>();
            
            List<Leaf<QqtFile>> fileList = tree.listLeaf(selected.getValue());
            for (Leaf<QqtFile> file : fileList) {
                displays.add(DisplayHelper.newDisplay(this, file.path));
            }
            
            playThisTask = new PlayThisTask(displays);
            
            new Thread(playThisTask).start();
        } else {
            if (playThisTask != null) {
                playThisTask.stop();
            }
        }
    }
    
    @FXML
    protected void exportFile(ActionEvent event) {
        TreeItem<Node<QqtFile>> selected = tree_view.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        
        if (StringHelper.isNullOrEmpty(config.export_file_path) || !new File(config.export_file_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存文件的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_file_path = file.getAbsolutePath();
        }
        
        IPackage target = null;
        
        List<Leaf<QqtFile>> list = tree.listLeaf(selected.getValue());
        for (Leaf<QqtFile> leaf : list) {
            try {
                if (target == null) {
                    String pkgName = leaf.pack.replace(".idx", "");
                    target = new FilePackage(config.export_file_path + "/" + pkgName);
                }
                
                byte[] data = vfs.load(leaf.path, byte[].class);
                target.save(leaf.path, data);
            } catch (Exception e) {
                LogHelper.info(String.format("export file failed: %s", leaf.path), e);
            }
        }
    }
    
    @FXML
    protected void exportImage(ActionEvent event) {
        final TreeItem<Node<QqtFile>> selected = tree_view.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        
        if (StringHelper.isNullOrEmpty(config.export_image_path) || !new File(config.export_image_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存图片的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_image_path = file.getAbsolutePath();
        }
        
        IPackage target = null;
        Node<QqtFile> node = selected.getValue();
        
        List<Leaf<QqtFile>> list = tree.listLeaf(selected.getValue());
        for (Leaf<QqtFile> leaf : list) {
            try {
                if (target == null) {
                    String pkgName = leaf.pack.replace(".idx", "");
                    target = new FilePackage(config.export_image_path + "/" + pkgName);
                    target.getHandlerKvs().put(BufferedImage.class, new BufferedImageHandler());
                }
                
                if (leaf.path.contains(".img")) {
                    QqtImg img = vfs.load(leaf.path, QqtImg.class);
                    if (img != null) {
                        for (QqtImgItem item : img.items) {
                            if (item != null) {
                                String fullPath = leaf.path.replace(".img", String.format("-%02d.png", item.index));
                                target.save(fullPath, QqtImgHelper.toImage(item));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LogHelper.info(String.format("export image failed: %s", leaf.name), e);
            }
        }
    }
    
    protected void copyPath(ActionEvent actionEvent) {
        TreeItem<Node<QqtFile>> selected = tree_view.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Node<QqtFile> node = selected.getValue();
            if (node.leaf) {
                Leaf<QqtFile> leaf = (Leaf<QqtFile>) node;
                
                String path = leaf.path.replace("\\", "\\\\");
                StringSelection selection = new StringSelection(path);
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(selection, selection);
            }
        }
    }
    
    private TreeItem<Node<QqtFile>> filterRoot;
    
    @FXML
    protected void onFilter(ActionEvent event) {
        final String regex = filterValue.getText();
        if (StringHelper.isNullOrEmpty(regex)) {
            return;
        }
        
        try {
            Pattern.compile(regex);
        } catch (Exception e) {
            return;
        }
        
        if (filterRoot == null) {
            filterRoot = new TreeItem<>(new Node<>(0, "[Filter]"));
            dummyTreeItem.getChildren().add(filterRoot);
        }
        
        filterRoot.getChildren().clear();
        
        List<Node<QqtFile>> list = tree.listNodeByRegex(regex);
        for (Node<QqtFile> folder : list) {
            filterRoot.getChildren().add(new TreeItem<>(folder));
        }
        
        filterRoot.getChildren().sort(comparator);
    }
}