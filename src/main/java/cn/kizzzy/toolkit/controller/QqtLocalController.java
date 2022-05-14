package cn.kizzzy.toolkit.controller;

import cn.kizzzy.helper.FileHelper;
import cn.kizzzy.helper.LogHelper;
import cn.kizzzy.helper.PrintArgs;
import cn.kizzzy.helper.PrintHelper;
import cn.kizzzy.helper.StringHelper;
import cn.kizzzy.javafx.StageHelper;
import cn.kizzzy.javafx.common.JavafxHelper;
import cn.kizzzy.javafx.common.MenuItemArg;
import cn.kizzzy.javafx.display.DisplayOperator;
import cn.kizzzy.javafx.display.DisplayTabView;
import cn.kizzzy.javafx.setting.SettingDialog;
import cn.kizzzy.qqt.*;
import cn.kizzzy.qqt.custom.FixedExportView;
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
@PluginParameter(url = "/fxml/qqt_local_view.fxml", title = "QQ堂(解包)")
public class QqtLocalController extends QqtViewBase implements Initializable {
    
    private static final String CONFIG_PATH = "qqt/local.config";
    
    private static final Comparator<TreeItem<Node>> comparator
        = Comparator.comparing(TreeItem<Node>::getValue, new NodeComparator());
    
    private IPackage userVfs;
    private QqtConfig config;
    
    private StageHelper stageHelper
        = new StageHelper();
    
    private IPackage vfs;
    
    private DisplayOperator<IPackage> displayer;
    
    private TreeItem<Node> dummyRoot;
    private TreeItem<Node> filterRoot;
    
    private PrintArgs[] printArgs;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userVfs = new FilePackage(System.getProperty("user.home") + "/.user");
        userVfs.getHandlerKvs().put(QqtConfig.class, new JsonFileHandler<>(QqtConfig.class));
        
        config = userVfs.load(CONFIG_PATH, QqtConfig.class);
        config = config != null ? config : new QqtConfig();
        
        stageHelper.addFactory(SettingDialog::new, SettingDialog.class);
        stageHelper.addFactory(FixedExportView::new, FixedExportView.class);
        
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
            new MenuItemArg(3, "导出/图片", event -> exportImage(false, false)),
            new MenuItemArg(3, "导出/图片(递归)", event -> exportImage(true, false)),
            new MenuItemArg(3, "导出/图片(等尺寸)", event -> exportImage(false, true)),
            new MenuItemArg(3, "导出/图片(等尺寸)(递归)", event -> exportImage(true, true)),
            new MenuItemArg(3, "导出/图片(自定义)", this::exportByCustom),
            new MenuItemArg(3, "导出/地图", this::exportMapImage),
            new MenuItemArg(4, "复制路径", this::copyPath),
        });
        
        dummyRoot = new TreeItem<>();
        tree_view.setRoot(dummyRoot);
        tree_view.setShowRoot(false);
        tree_view.getSelectionModel().selectedItemProperty().addListener(this::onSelectItem);
        
        lock_tab.selectedProperty().addListener((observable, oldValue, newValue) -> {
            display_tab.setPin(newValue);
        });
        
        displayer = new DisplayOperator<>("cn.kizzzy.qqt.display", display_tab, IPackage.class);
        displayer.load();
        
        printArgs = new PrintArgs[]{
            new PrintArgs(QqtImg.class, null, true),
            new PrintArgs(QqtImgItem.class, new PrintArgs.Item[]{
                new PrintArgs.Item("file", true),
                new PrintArgs.Item("offset", true),
                new PrintArgs.Item("size", true),
                new PrintArgs.Item("offset_alpha", true),
                new PrintArgs.Item("size_alpha", true),
            }, false),
        };
    }
    
    @Override
    public void stop() {
        if (vfs != null) {
            vfs.stop();
        }
        
        userVfs.save(CONFIG_PATH, config);
        
        super.stop();
    }
    
    protected void onSelectItem(Observable observable, TreeItem<Node> oldValue, TreeItem<Node> newValue) {
        if (newValue != null) {
            Node folder = newValue.getValue();
            if (folder == null) {
                return;
            }
            
            if (folder.leaf) {
                Leaf leaf = (Leaf) folder;
                
                displayer.display(leaf.path);
                
                // print leaf information
                if (leaf.name.endsWith(".img")) {
                    QqtImg img = vfs.load(leaf.path, QqtImg.class);
                    if (img != null) {
                        LogHelper.debug(PrintHelper.ToString(img, printArgs));
                    }
                }
            } else {
                newValue.getChildren().clear();
                
                Iterable<Node> list = folder.children.values();
                for (Node temp : list) {
                    TreeItem<Node> child = new TreeItem<>(temp);
                    newValue.getChildren().add(child);
                }
                newValue.getChildren().sort(comparator);
            }
        }
    }
    
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
            dummyRoot.getChildren().add(filterRoot);
        }
        
        filterRoot.getChildren().clear();
        
        List<Node> list = vfs.listNodeByRegex(regex);
        for (Node folder : list) {
            filterRoot.getChildren().add(new TreeItem<>(folder));
        }
        
        filterRoot.getChildren().sort(comparator);
    }
    
    private void openSetting(ActionEvent actionEvent) {
        SettingDialog.Args args = new SettingDialog.Args();
        args.target = config;
        
        stageHelper.show(stage, args, SettingDialog.class);
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
        IdGenerator idGenerator = new IdGenerator();
        
        ITree rootTree = new FileTreeBuilder(file.getAbsolutePath(), idGenerator).build();
        IPackage rootVfs = new FilePackage(file.getAbsolutePath(), rootTree);
        rootVfs.getHandlerKvs().put(String.class, new StringFileHandler(Charset.forName("GB2312")));
        rootVfs.getHandlerKvs().put(IdxFile.class, new IdxFileHandler());
        rootVfs.getHandlerKvs().put(QqtImg.class, new QqtImgHandler());
        rootVfs.getHandlerKvs().put(QqtMap.class, new QQtMapHandler());
        rootVfs.getHandlerKvs().put(QqtAvatar.class, new QqtAvatarHandler());
        rootVfs.getHandlerKvs().put(MapElemProp.class, new MapElemPropHandler());
        
        IdxFile idxFile = rootVfs.load("data/object.idx", IdxFile.class);
        if (idxFile == null) {
            return;
        }
        
        ITree idxTree = new IdxTreeBuilder(idxFile, idGenerator).build();
        IPackage idxVfs = new QqtPackage(file.getAbsolutePath(), idxTree);
        idxVfs.getHandlerKvs().put(String.class, new StringFileHandler(Charset.forName("GB2312")));
        idxVfs.getHandlerKvs().put(QqtAvatar.class, new QqtAvatarHandler());
        
        //ITree tree = new Forest(Arrays.asList(rootVfs, idxTree));
        vfs = new CombinePackage(rootVfs, idxVfs);
        
        doAfterLoadVfs();
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
        IPackage dataVfs = new FilePackage(file.getParent());
        dataVfs.getHandlerKvs().put(IdxFile.class, new IdxFileHandler());
        
        IdxFile idxFile = dataVfs.load(FileHelper.getName(file.getAbsolutePath()), IdxFile.class);
        if (idxFile == null) {
            return;
        }
        
        ITree tree = new IdxTreeBuilder(idxFile, new IdGenerator()).build();
        vfs = new QqtPackage(file.getParent(), tree);
        vfs.getHandlerKvs().put(String.class, new StringFileHandler(Charset.forName("GB2312")));
        
        doAfterLoadVfs();
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
        ITree tree = new FileTreeBuilder(file.getAbsolutePath()).build();
        vfs = new FilePackage(file.getAbsolutePath(), tree);
        vfs.getHandlerKvs().put(String.class, new StringFileHandler(Charset.forName("GB2312")));
        vfs.getHandlerKvs().put(QqtImg.class, new QqtImgHandler());
        vfs.getHandlerKvs().put(QqtMap.class, new QQtMapHandler());
        vfs.getHandlerKvs().put(QqtAvatar.class, new QqtAvatarHandler());
        vfs.getHandlerKvs().put(MapElemProp.class, new MapElemPropHandler());
        
        doAfterLoadVfs();
    }
    
    private void doAfterLoadVfs() {
        displayer.setContext(vfs);
        
        Platform.runLater(() -> {
            dummyRoot.getChildren().clear();
            
            final List<Node> nodes = vfs.listNode(0);
            for (Node node : nodes) {
                dummyRoot.getChildren().add(new TreeItem<>(node));
            }
            
            if (filterRoot != null) {
                filterRoot.getChildren().clear();
                dummyRoot.getChildren().add(filterRoot);
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
        if (StringHelper.isNullOrEmpty(config.export_file_path) || !new File(config.export_file_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存文件的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_file_path = file.getAbsolutePath();
        }
        
        TreeItem<Node> selected = tree_view.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        
        IPackage saveVfs = null;
        
        List<Leaf> list = vfs.listLeaf(selected.getValue(), recursively);
        for (Leaf leaf : list) {
            try {
                if (saveVfs == null) {
                    String pkgName = leaf.pack.replace(".idx", "");
                    saveVfs = new FilePackage(config.export_file_path + "/" + pkgName);
                }
                
                byte[] data = vfs.load(leaf.path, byte[].class);
                saveVfs.save(leaf.path, data);
            } catch (Exception e) {
                LogHelper.info(String.format("export file failed: %s", leaf.path), e);
            }
        }
    }
    
    private void exportImage(boolean recursively, boolean fixed) {
        if (StringHelper.isNullOrEmpty(config.export_image_path) || !new File(config.export_image_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存图片的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_image_path = file.getAbsolutePath();
        }
        
        final TreeItem<Node> selected = tree_view.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        
        IPackage saveVfs = null;
        
        List<Leaf> list = vfs.listLeaf(selected.getValue(), recursively);
        for (Leaf leaf : list) {
            try {
                if (saveVfs == null) {
                    String pkgName = leaf.pack.replace(".idx", "");
                    saveVfs = new FilePackage(config.export_image_path + "/" + pkgName);
                    saveVfs.getHandlerKvs().put(BufferedImage.class, new BufferedImageHandler());
                }
                
                if (leaf.path.contains(".img")) {
                    QqtImg img = vfs.load(leaf.path, QqtImg.class);
                    if (img != null) {
                        for (QqtImgItem item : img.items) {
                            if (item != null) {
                                BufferedImage image = QqtImgHelper.toImage(item, fixed);
                                if (image != null) {
                                    String fullPath = leaf.path.replace(".img", String.format("-%02d.png", item.index));
                                    saveVfs.save(fullPath, image);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LogHelper.info(String.format("export image failed: %s", leaf.name), e);
            }
        }
    }
    
    private void exportByCustom(ActionEvent actionEvent) {
        if (StringHelper.isNullOrEmpty(config.export_image_path) || !new File(config.export_image_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存图片的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_image_path = file.getAbsolutePath();
        }
        
        final TreeItem<Node> selected = tree_view.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        
        Node node = selected.getValue();
        if (node.leaf && node.name.endsWith(".img")) {
            Leaf leaf = (Leaf) node;
            
            String pkgName = leaf.pack.replace(".idx", "");
            IPackage saveVfs = new FilePackage(config.export_image_path + "/" + pkgName);
            saveVfs.getHandlerKvs().put(BufferedImage.class, new BufferedImageHandler());
            
            QqtImg img = vfs.load(leaf.path, QqtImg.class);
            if (img != null) {
                FixedExportView.Args args = new FixedExportView.Args();
                args.path = leaf.path;
                args.img = img;
                args.loadVfs = vfs;
                args.saveVfs = saveVfs;
                stageHelper.show(stage, args, FixedExportView.class);
            }
        }
    }
    
    private void exportMapImage(ActionEvent event) {
        if (StringHelper.isNullOrEmpty(config.export_image_path) || !new File(config.export_image_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存图片的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_image_path = file.getAbsolutePath();
        }
        
        final TreeItem<Node> selected = tree_view.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
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
            
            IPackage saveVfs = new FilePackage(config.export_image_path + "/map");
            saveVfs.getHandlerKvs().put(BufferedImage.class, new BufferedImageHandler());
            
            saveVfs.save(leaf.path + ".png", image);
        }
    }
    
    private void processElement(QqtMap.Element element, int x, int y, MapElemDataProvider provider, BufferedImage graphics) {
        if (element.city() <= 0 || element.id() <= 0) {
            return;
        }
        
        MapCity city = MapCity.valueOf(element.city());
        String path = String.format("object/mapelem/%s/elem%d_stand.img", city.getName(), element.id());
        QqtImg img = vfs.load(path, QqtImg.class);
        if (img == null) {
            return;
        }
        
        QqtImgItem item = img.items[0];
        BufferedImage image = QqtImgHelper.toImage(item);
        if (image == null) {
            return;
        }
        
        MapElemProp.Element elementData = provider.getElementData(element.value);
        if (elementData == null) {
            return;
        }
        
        QqtElementXyer.Point point = QqtElementXyer.INS.GetXy(element.value);
        
        for (int r = 0; r < image.getHeight(); ++r) {
            for (int c = 0; c < image.getWidth(); ++c) {
                int offsetX = 80 + x * 40 - elementData.x + c + point.x;
                int offsetY = 80 + y * 40 - elementData.y + r + point.y;
                
                int argb = image.getRGB(c, r);
                if ((argb & 0xFF000000) == 0) {
                    continue;
                }
                graphics.setRGB(offsetX, offsetY, argb);
            }
        }
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
}