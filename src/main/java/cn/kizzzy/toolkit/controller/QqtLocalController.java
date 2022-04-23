package cn.kizzzy.toolkit.controller;

import cn.kizzzy.helper.FileHelper;
import cn.kizzzy.helper.LogHelper;
import cn.kizzzy.helper.StringHelper;
import cn.kizzzy.javafx.common.JavafxHelper;
import cn.kizzzy.javafx.common.MenuItemArg;
import cn.kizzzy.javafx.display.DisplayOperator;
import cn.kizzzy.javafx.display.DisplayTabView;
import cn.kizzzy.javafx.setting.ISettingDialogFactory;
import cn.kizzzy.javafx.setting.SettingDialogFactory;
import cn.kizzzy.qqt.GameMode;
import cn.kizzzy.qqt.MapCity;
import cn.kizzzy.qqt.MapElemDataProvider;
import cn.kizzzy.qqt.MapElemProp;
import cn.kizzzy.qqt.QqtAvatar;
import cn.kizzzy.qqt.QqtConfig;
import cn.kizzzy.qqt.QqtImg;
import cn.kizzzy.qqt.QqtImgItem;
import cn.kizzzy.qqt.QqtMap;
import cn.kizzzy.qqt.helper.QqtImgHelper;
import cn.kizzzy.tencent.IdxFile;
import cn.kizzzy.toolkit.view.AbstractView;
import cn.kizzzy.vfs.IPackage;
import cn.kizzzy.vfs.ITree;
import cn.kizzzy.vfs.handler.BufferedImageHandler;
import cn.kizzzy.vfs.handler.IdxFileHandler;
import cn.kizzzy.vfs.handler.JsonFileHandler;
import cn.kizzzy.vfs.handler.MapElemPropHandler;
import cn.kizzzy.vfs.handler.QQtMapHandler;
import cn.kizzzy.vfs.handler.QqtAvatarHandler;
import cn.kizzzy.vfs.handler.QqtImgHandler;
import cn.kizzzy.vfs.handler.StringFileHandler;
import cn.kizzzy.vfs.pack.CombinePackage;
import cn.kizzzy.vfs.pack.FilePackage;
import cn.kizzzy.vfs.pack.QqtPackage;
import cn.kizzzy.vfs.tree.FileTreeBuilder;
import cn.kizzzy.vfs.tree.Forest;
import cn.kizzzy.vfs.tree.IdGenerator;
import cn.kizzzy.vfs.tree.IdxTreeBuilder;
import cn.kizzzy.vfs.tree.Leaf;
import cn.kizzzy.vfs.tree.Node;
import cn.kizzzy.vfs.tree.NodeComparator;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
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
    protected TreeView<Node> tree_view;
    
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
public class QqtLocalController extends QqtViewBase implements Initializable {
    
    protected static final String CONFIG_PATH = "qqt/local.config";
    
    protected static final Comparator<TreeItem<Node>> comparator
        = Comparator.comparing(TreeItem<Node>::getValue, new NodeComparator());
    
    protected IPackage userVfs;
    protected QqtConfig config;
    protected ISettingDialogFactory dialogFactory;
    
    protected IPackage vfs;
    protected ITree tree;
    
    protected DisplayOperator<IPackage> displayer;
    
    protected TreeItem<Node> dummyTreeItem;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userVfs = new FilePackage(System.getProperty("user.home") + "/.user");
        userVfs.getHandlerKvs().put(QqtConfig.class, new JsonFileHandler<>(QqtConfig.class));
        
        config = userVfs.load(CONFIG_PATH, QqtConfig.class);
        config = config != null ? config : new QqtConfig();
        
        JavafxHelper.initContextMenu(tree_view, () -> stage.getScene().getWindow(), new MenuItemArg[]{
            new MenuItemArg(0, "设置", this::openSetting),
            new MenuItemArg(1, "加载/Full", this::loadFull),
            new MenuItemArg(1, "加载/Idx", this::loadIdx),
            new MenuItemArg(1, "加载/目录", this::loadFolder),
            new MenuItemArg(2, "打开/QQ堂", this::openFolderQqtRoot),
            new MenuItemArg(2, "打开/文件路径", this::openFolderExportFile),
            new MenuItemArg(2, "打开/图片路径", this::openFolderExportImage),
            new MenuItemArg(3, "导出/文件", event -> exportFile(false)),
            new MenuItemArg(3, "导出/文件(递归)", event -> exportFile(true)),
            new MenuItemArg(3, "导出/图片", event -> exportImage(false)),
            new MenuItemArg(3, "导出/图片(递归)", event -> exportImage(true)),
            new MenuItemArg(3, "导出/地图", this::exportMapImage),
            new MenuItemArg(4, "复制路径", this::copyPath),
        });
        
        dummyTreeItem = new TreeItem<>();
        tree_view.setRoot(dummyTreeItem);
        tree_view.setShowRoot(false);
        tree_view.getSelectionModel().selectedItemProperty().addListener(this::onSelectItem);
        
        lock_tab.selectedProperty().addListener((observable, oldValue, newValue) -> {
            display_tab.setPin(newValue);
        });
        
        displayer = new DisplayOperator<>("cn.kizzzy.qqt.display", display_tab, IPackage.class);
        displayer.load();
    }
    
    @Override
    public void stop() {
        if (tree != null) {
            tree.stop();
        }
        
        userVfs.save(CONFIG_PATH, config);
        
        super.stop();
    }
    
    protected void onSelectItem(Observable observable, TreeItem<Node> oldValue, TreeItem<Node> newValue) {
        if (newValue != null) {
            Node folder = newValue.getValue();
            Leaf thumbs = null;
            
            if (folder.leaf) {
                thumbs = (Leaf) folder;
            } else {
                newValue.getChildren().clear();
                
                Iterable<Node> list = folder.children.values();
                for (Node temp : list) {
                    TreeItem<Node> child = new TreeItem<>(temp);
                    newValue.getChildren().add(child);
                }
                newValue.getChildren().sort(comparator);
            }
            
            if (thumbs != null) {
                displayer.display(thumbs.path);
            }
        }
    }
    
    private void openSetting(ActionEvent actionEvent) {
        if (dialogFactory == null) {
            dialogFactory = new SettingDialogFactory(stage);
        }
        dialogFactory.show(config);
    }
    
    private void loadFull(ActionEvent event) {
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
                    loadFullImpl(file);
                } catch (Exception e) {
                    LogHelper.error("load folder error", e);
                }
            }).start();
        }
    }
    
    private void loadFullImpl(File file) {
        ITree rootTree = new FileTreeBuilder(file.getAbsolutePath()).build();
        IPackage rootVfs = new FilePackage(file.getAbsolutePath(), rootTree);
        rootVfs.getHandlerKvs().put(IdxFile.class, new IdxFileHandler());
        rootVfs.getHandlerKvs().put(QqtMap.class, new QQtMapHandler());
        rootVfs.getHandlerKvs().put(QqtAvatar.class, new QqtAvatarHandler());
        rootVfs.getHandlerKvs().put(MapElemProp.class, new MapElemPropHandler());
        
        IdxFile idxFile = rootVfs.load("data/object.idx", IdxFile.class);
        if (idxFile == null) {
            return;
        }
        
        ITree idxTree = new IdxTreeBuilder(idxFile, new IdGenerator()).build();
        IPackage idxVfs = new QqtPackage(file.getAbsolutePath(), idxTree);
        idxVfs.getHandlerKvs().put(String.class, new StringFileHandler(Charset.forName("GB2312")));
        idxVfs.getHandlerKvs().put(QqtAvatar.class, new QqtAvatarHandler());
        
        tree = new Forest(Arrays.asList(rootVfs, idxTree));
        vfs = new CombinePackage(rootVfs, idxVfs);
        
        displayer.setContext(vfs);
        
        Platform.runLater(() -> {
            dummyTreeItem.getChildren().clear();
            
            final List<Node> nodes = tree.listNode(0);
            for (Node node : nodes) {
                dummyTreeItem.getChildren().add(new TreeItem<>(node));
            }
        });
    }
    
    
    private void loadIdx(ActionEvent actionEvent) {
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
        iPackage.getHandlerKvs().put(IdxFile.class, new IdxFileHandler());
        iPackage.getHandlerKvs().put(QqtMap.class, new QQtMapHandler());
        iPackage.getHandlerKvs().put(QqtAvatar.class, new QqtAvatarHandler());
        
        IdxFile idxFile = iPackage.load(FileHelper.getName(file.getAbsolutePath()), IdxFile.class);
        tree = new IdxTreeBuilder(idxFile, new IdGenerator()).build();
        
        vfs = new QqtPackage(file.getParent(), tree);
        vfs.getHandlerKvs().put(String.class, new StringFileHandler(Charset.forName("GB2312")));
        vfs.getHandlerKvs().put(QqtAvatar.class, new QqtAvatarHandler());
        
        displayer.setContext(vfs);
        
        Platform.runLater(() -> {
            dummyTreeItem.getChildren().clear();
            
            final List<Node> nodes = tree.listNode(0);
            for (Node node : nodes) {
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
        tree = new FileTreeBuilder(
            file.getAbsolutePath(), new IdGenerator()
        ).build();
        
        vfs = new FilePackage(file.getAbsolutePath());
        vfs.getHandlerKvs().put(String.class, new StringFileHandler(Charset.forName("GB2312")));
        vfs.getHandlerKvs().put(QqtImg.class, new QqtImgHandler());
        vfs.getHandlerKvs().put(QqtMap.class, new QQtMapHandler());
        vfs.getHandlerKvs().put(QqtAvatar.class, new QqtAvatarHandler());
        
        displayer.setContext(vfs);
        
        Platform.runLater(() -> {
            dummyTreeItem.getChildren().clear();
            
            final List<Node> nodes = tree.listNode(0);
            for (Node node : nodes) {
                dummyTreeItem.getChildren().add(new TreeItem<>(node));
            }
        });
    }
    
    private void openFolderQqtRoot(ActionEvent event) {
        openFolderImpl(config.qqt_path);
    }
    
    private void openFolderExportFile(ActionEvent event) {
        openFolderImpl(config.export_file_path);
    }
    
    private void openFolderExportImage(ActionEvent event) {
        openFolderImpl(config.export_image_path);
    }
    
    private void openFolderImpl(String filePath) {
        if (StringHelper.isNotNullAndEmpty(filePath)) {
            new Thread(() -> {
                try {
                    Desktop.getDesktop().open(new File(filePath));
                } catch (Exception e) {
                    LogHelper.error(String.format("open folder error, %s", filePath), e);
                }
            }).start();
        }
    }
    
    private void exportFile(boolean recursively) {
        TreeItem<Node> selected = tree_view.getSelectionModel().getSelectedItem();
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
        
        List<Leaf> list = tree.listLeaf(selected.getValue(), recursively);
        for (Leaf leaf : list) {
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
    
    private void exportImage(boolean recursively) {
        final TreeItem<Node> selected = tree_view.getSelectionModel().getSelectedItem();
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
        Node node = selected.getValue();
        
        List<Leaf> list = tree.listLeaf(selected.getValue(), recursively);
        for (Leaf leaf : list) {
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
    
    private void exportMapImage(ActionEvent event) {
        final TreeItem<Node> selected = tree_view.getSelectionModel().getSelectedItem();
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
        
        Node node = selected.getValue();
        if (node.leaf && node.name.endsWith(".map")) {
            Leaf leaf = (Leaf) node;
            QqtMap map = vfs.load(leaf.path, QqtMap.class);
            if (map == null) {
                return;
            }
            
            MapElemProp prop = vfs.load("object\\mapElem\\mapElem.prop", MapElemProp.class);
            MapElemDataProvider provider = new MapElemDataProvider(prop);
            
            BufferedImage image = new BufferedImage(map.width * 40 + 160, map.height * 40 + 160, BufferedImage.TYPE_INT_ARGB);
            
            for (int i = 2; i >= 0; --i) {
                QqtMap.Layer layer = map.layers[i];
                for (int y = 0; y < map.height; ++y) {
                    for (int x = 0; x < map.width; ++x) {
                        QqtMap.Element element = layer.elements[y][x];
                        processElement(element, x, y, provider, image);
                    }
                }
            }
            
            try {
                GameMode mode = GameMode.valueOf(map.gameMode);
                QqtMap.Points points = map.points[3];
                for (int i = 0; i < mode.getSpecials().length && i < points.points.length; ++i) {
                    QqtMap.Element element = mode.getSpecials()[i];
                    QqtMap.Point point = points.points[i];
                    
                    processElement(element, point.x, point.y, provider, image);
                }
            } catch (Exception e) {
            
            }
            
            IPackage target = new FilePackage(config.export_image_path + "/map");
            target.getHandlerKvs().put(BufferedImage.class, new BufferedImageHandler());
            
            target.save(leaf.path + ".png", image);
            /*
            DisplayTracks tracks = new DisplayTracks();
            DisplayFrame frame = new DisplayFrame();
            frame.x = 0;
            frame.y = 0;
            frame.width = image.getWidth();
            frame.height = image.getHeight();
            frame.image = image;
            frame.extra = "";
            
            DisplayTrack track = new DisplayTrack();
            track.frames.add(frame);
            
            tracks.tracks.add(track);
            
            display_tab.show(new DisplayAAA(DisplayType.SHOW_IMAGE, tracks));*/
        }
    }
    
    private void processElement(QqtMap.Element element, int x, int y, MapElemDataProvider provider, BufferedImage graphics) {
        BufferedImage image = createImage(element);
        if (image == null) {
            return;
        }
        
        MapElemProp.Element elementData = provider.getElementData(element.value);
        if (elementData == null) {
            return;
        }
        
        for (int r = 0; r < image.getHeight(); ++r) {
            for (int c = 0; c < image.getWidth(); ++c) {
                int offsetX = 80 + x * 40 - elementData.x + c;
                int offsetY = 80 + y * 40 - elementData.y + r;
                
                int argb = image.getRGB(c, r);
                if ((argb & 0xFF000000) == 0) {
                    continue;
                }
                graphics.setRGB(offsetX, offsetY, argb);
            }
        }
    }
    
    private BufferedImage createImage(QqtMap.Element element) {
        if (element.city() > 0 && element.id() > 0) {
            MapCity city = MapCity.valueOf(element.city());
            String path = String.format("object/mapelem/%s/elem%d_stand.img", city.getName(), element.id());
            QqtImg img = vfs.load(path, QqtImg.class);
            if (img != null) {
                QqtImgItem item = img.items[0];
                return QqtImgHelper.toImage(item);
            }
        }
        return null;
    }
    
    protected void copyPath(ActionEvent actionEvent) {
        TreeItem<Node> selected = tree_view.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Node node = selected.getValue();
            if (node.leaf) {
                Leaf leaf = (Leaf) node;
                
                String path = leaf.path.replace("\\", "\\\\");
                StringSelection selection = new StringSelection(path);
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(selection, selection);
            }
        }
    }
    
    private TreeItem<Node> filterRoot;
    
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
            filterRoot = new TreeItem<>(new Node(0, "[Filter]"));
            dummyTreeItem.getChildren().add(filterRoot);
        }
        
        filterRoot.getChildren().clear();
        
        List<Node> list = tree.listNodeByRegex(regex);
        for (Node folder : list) {
            filterRoot.getChildren().add(new TreeItem<>(folder));
        }
        
        filterRoot.getChildren().sort(comparator);
    }
}