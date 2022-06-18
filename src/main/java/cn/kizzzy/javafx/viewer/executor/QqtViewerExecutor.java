package cn.kizzzy.javafx.viewer.executor;

import cn.kizzzy.helper.FileHelper;
import cn.kizzzy.helper.LogHelper;
import cn.kizzzy.helper.PrintArgs;
import cn.kizzzy.helper.PrintHelper;
import cn.kizzzy.helper.StringHelper;
import cn.kizzzy.javafx.StageHelper;
import cn.kizzzy.javafx.common.MenuItemArg;
import cn.kizzzy.javafx.display.DisplayOperator;
import cn.kizzzy.javafx.display.DisplayTabView;
import cn.kizzzy.javafx.viewer.ViewerExecutorArgs;
import cn.kizzzy.javafx.viewer.ViewerExecutorAttribute;
import cn.kizzzy.javafx.viewer.ViewerExecutorBinder;
import cn.kizzzy.qqt.AvatarFile;
import cn.kizzzy.qqt.GameMode;
import cn.kizzzy.qqt.ImgFile;
import cn.kizzzy.qqt.MapCity;
import cn.kizzzy.qqt.MapElemDataProvider;
import cn.kizzzy.qqt.MapElemProp;
import cn.kizzzy.qqt.MapFile;
import cn.kizzzy.qqt.QqtConfig;
import cn.kizzzy.qqt.QqtElementXyer;
import cn.kizzzy.qqt.custom.FixedExportView;
import cn.kizzzy.qqt.helper.QqtImgHelper;
import cn.kizzzy.qqt.vfs.handler.AvatarFileHandler;
import cn.kizzzy.qqt.vfs.handler.ImgFileHandler;
import cn.kizzzy.qqt.vfs.handler.MapElemPropHandler;
import cn.kizzzy.qqt.vfs.handler.MapFileHandler;
import cn.kizzzy.qqt.vfs.pack.QqtPackage;
import cn.kizzzy.tencent.IdxFile;
import cn.kizzzy.tencent.vfs.handler.IdxFileHandler;
import cn.kizzzy.tencent.vfs.tree.IdxTreeBuilder;
import cn.kizzzy.vfs.IPackage;
import cn.kizzzy.vfs.ITree;
import cn.kizzzy.vfs.handler.BufferedImageHandler;
import cn.kizzzy.vfs.handler.JsonFileHandler;
import cn.kizzzy.vfs.handler.StringFileHandler;
import cn.kizzzy.vfs.pack.CombinePackage;
import cn.kizzzy.vfs.pack.FilePackage;
import cn.kizzzy.vfs.tree.FileTreeBuilder;
import cn.kizzzy.vfs.tree.IdGenerator;
import cn.kizzzy.vfs.tree.Leaf;
import cn.kizzzy.vfs.tree.Node;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@ViewerExecutorAttribute
public class QqtViewerExecutor extends AbstractViewerExecutor {
    
    private static final String CONFIG_PATH = "qqt/local.config";
    
    private QqtConfig config;
    
    private PrintArgs[] printArgs;
    
    @Override
    public void initialize(ViewerExecutorArgs args) {
        IPackage userVfs = args.getUserVfs();
        userVfs.addHandler(QqtConfig.class, new JsonFileHandler<>(QqtConfig.class));
        
        config = userVfs.load(CONFIG_PATH, QqtConfig.class);
        config = config != null ? config : new QqtConfig();
        
        StageHelper stageHelper = args.getStageHelper();
        stageHelper.addFactory(FixedExportView::new, FixedExportView.class);
        
        printArgs = new PrintArgs[]{
            new PrintArgs(ImgFile.class, null, true),
            new PrintArgs(ImgFile.Frame.class, new PrintArgs.Item[]{
                new PrintArgs.Item("file", true),
                new PrintArgs.Item("offset", true),
                new PrintArgs.Item("size", true),
                new PrintArgs.Item("offset_alpha", true),
                new PrintArgs.Item("size_alpha", true),
            }, false),
        };
    }
    
    @Override
    public void stop(ViewerExecutorArgs args) {
        IPackage userVfs = args.getUserVfs();
        userVfs.save(CONFIG_PATH, config);
    }
    
    @Override
    public void initOperator(DisplayTabView tabView, IPackage vfs) {
        displayer = new DisplayOperator<>("cn.kizzzy.qqt.display", tabView, IPackage.class);
        displayer.load();
        displayer.setContext(vfs);
    }
    
    @Override
    public void displayLeaf(ViewerExecutorArgs args, Leaf leaf) {
        super.displayLeaf(args, leaf);
        
        // print leaf information
        IPackage vfs = args.getVfs();
        if (leaf.name.endsWith(".img")) {
            ImgFile img = vfs.load(leaf.path, ImgFile.class);
            if (img != null) {
                LogHelper.debug(PrintHelper.ToString(img, printArgs));
            }
        }
    }
    
    @Override
    public Iterable<MenuItemArg> showContext(ViewerExecutorArgs args, final Node selected) {
        List<MenuItemArg> list = new ArrayList<>();
        list.add(new MenuItemArg(1, "加载/Full(QQT)", event -> loadFull(args)));
        list.add(new MenuItemArg(1, "加载/IDX(QQT)", event -> loadFile(args)));
        list.add(new MenuItemArg(1, "加载/目录(QQT)", event -> loadFolder(args)));
        if (selected != null) {
            list.add(new MenuItemArg(0, "设置", event -> openSetting(args, config)));
            list.add(new MenuItemArg(2, "打开/根目录", event -> openFolderQqtRoot(args)));
            list.add(new MenuItemArg(2, "打开/文件目录", event -> openFolderExportFile(args)));
            list.add(new MenuItemArg(2, "打开/图片目录", event -> openFolderExportImage(args)));
            list.add(new MenuItemArg(3, "导出/文件", event -> exportFile(args, selected, false)));
            list.add(new MenuItemArg(3, "导出/文件(递归)", event -> exportFile(args, selected, true)));
            list.add(new MenuItemArg(3, "导出/图片", event -> exportImage(args, selected, false, false)));
            list.add(new MenuItemArg(3, "导出/图片(递归)", event -> exportImage(args, selected, true, false)));
            list.add(new MenuItemArg(3, "导出/图片(等尺寸)", event -> exportImage(args, selected, false, true)));
            list.add(new MenuItemArg(3, "导出/图片(等尺寸)(递归)", event -> exportImage(args, selected, true, true)));
            list.add(new MenuItemArg(3, "导出/图片(自定义)", event -> exportByCustom(args, selected)));
            list.add(new MenuItemArg(3, "导出/地图", event -> exportMapImage(args, selected)));
            if (selected.leaf) {
                list.add(new MenuItemArg(9, "复制路径", event -> copyPath(selected)));
            }
        }
        return list;
    }
    
    private void loadFull(ViewerExecutorArgs args) {
        Stage stage = args.getStage();
        
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择QQ堂根目录");
        
        if (StringHelper.isNotNullAndEmpty(config.last_root)) {
            File lastFolder = new File(config.last_root);
            if (lastFolder.exists()) {
                chooser.setInitialDirectory(lastFolder);
            }
        }
        
        File file = chooser.showDialog(stage);
        if (file != null) {
            config.last_root = file.getAbsolutePath();
            
            loadFullImpl(args, file);
        }
    }
    
    private void loadFullImpl(ViewerExecutorArgs args, File file) {
        IdGenerator idGenerator = args.getIdGenerator();
        
        ITree rootTree = new FileTreeBuilder(file.getAbsolutePath(), idGenerator).build();
        IPackage rootVfs = new FilePackage(file.getAbsolutePath(), rootTree);
        rootVfs.addHandler(String.class, new StringFileHandler(Charset.forName("GB2312")));
        rootVfs.addHandler(IdxFile.class, new IdxFileHandler());
        rootVfs.addHandler(ImgFile.class, new ImgFileHandler());
        rootVfs.addHandler(MapFile.class, new MapFileHandler());
        rootVfs.addHandler(AvatarFile.class, new AvatarFileHandler());
        rootVfs.addHandler(MapElemProp.class, new MapElemPropHandler());
        
        IdxFile idxFile = rootVfs.load("data/object.idx", IdxFile.class);
        if (idxFile == null) {
            return;
        }
        
        ITree idxTree = new IdxTreeBuilder(idxFile, idGenerator).build();
        IPackage idxVfs = new QqtPackage(file.getAbsolutePath(), idxTree);
        idxVfs.addHandler(String.class, new StringFileHandler(Charset.forName("GB2312")));
        idxVfs.addHandler(AvatarFile.class, new AvatarFileHandler());
        
        IPackage fullVfs = new CombinePackage(rootVfs, idxVfs);
        
        args.getObservable().setValue(new ViewerExecutorBinder(fullVfs, this));
    }
    
    private void loadFile(ViewerExecutorArgs args) {
        Stage stage = args.getStage();
        
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择idx文件");
        if (StringHelper.isNotNullAndEmpty(config.last_idx)) {
            File lastFolder = new File(config.last_idx);
            if (lastFolder.exists()) {
                chooser.setInitialDirectory(lastFolder);
            }
        }
        
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("IDX", "*.idx"),
            new FileChooser.ExtensionFilter("ALL", "*.*")
        );
        
        File file = chooser.showOpenDialog(stage);
        if (file != null && file.getAbsolutePath().endsWith(".idx")) {
            config.last_idx = file.getParent();
            
            loadIdxImpl(args, file);
        }
    }
    
    private void loadIdxImpl(ViewerExecutorArgs args, File file) {
        IdGenerator idGenerator = args.getIdGenerator();
        
        IPackage dataVfs = new FilePackage(file.getParent());
        dataVfs.addHandler(IdxFile.class, new IdxFileHandler());
        
        String path = FileHelper.getName(file.getAbsolutePath());
        IdxFile idxFile = dataVfs.load(path, IdxFile.class);
        if (idxFile == null) {
            return;
        }
        
        ITree tree = new IdxTreeBuilder(idxFile, idGenerator).build();
        IPackage vfs = new QqtPackage(file.getParent(), tree);
        vfs.addHandler(String.class, new StringFileHandler(Charset.forName("GB2312")));
        
        args.getObservable().setValue(new ViewerExecutorBinder(vfs, this));
    }
    
    private void loadFolder(ViewerExecutorArgs args) {
        Stage stage = args.getStage();
        
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择QQ堂根目录");
        
        if (StringHelper.isNotNullAndEmpty(config.last_root)) {
            File lastFolder = new File(config.last_root);
            if (lastFolder.exists()) {
                chooser.setInitialDirectory(lastFolder);
            }
        }
        
        File file = chooser.showDialog(stage);
        if (file != null) {
            config.last_root = file.getAbsolutePath();
            
            loadFolderImpl(args, file);
        }
    }
    
    private void loadFolderImpl(ViewerExecutorArgs args, File file) {
        IdGenerator idGenerator = args.getIdGenerator();
        
        ITree tree = new FileTreeBuilder(file.getAbsolutePath(), idGenerator).build();
        IPackage rootVfs = new FilePackage(file.getAbsolutePath(), tree);
        rootVfs.addHandler(String.class, new StringFileHandler(Charset.forName("GB2312")));
        rootVfs.addHandler(ImgFile.class, new ImgFileHandler());
        rootVfs.addHandler(MapFile.class, new MapFileHandler());
        rootVfs.addHandler(AvatarFile.class, new AvatarFileHandler());
        rootVfs.addHandler(MapElemProp.class, new MapElemPropHandler());
        
        args.getObservable().setValue(new ViewerExecutorBinder(rootVfs, this));
    }
    
    private void openFolderQqtRoot(ViewerExecutorArgs args) {
        openFolderImpl(config.last_root);
    }
    
    private void openFolderExportFile(ViewerExecutorArgs args) {
        openFolderImpl(config.export_file_path);
    }
    
    private void openFolderExportImage(ViewerExecutorArgs args) {
        openFolderImpl(config.export_image_path);
    }
    
    private void exportFile(ViewerExecutorArgs args, Node selected, boolean recursively) {
        Stage stage = args.getStage();
        IPackage vfs = args.getVfs();
        
        if (StringHelper.isNullOrEmpty(config.export_file_path) || !new File(config.export_file_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存文件的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_file_path = file.getAbsolutePath();
        }
        
        if (selected == null) {
            return;
        }
        
        IPackage saveVfs = null;
        
        List<Leaf> list = vfs.listLeaf(selected, recursively);
        for (Leaf leaf : list) {
            try {
                if (saveVfs == null) {
                    String pkgName = leaf.pack.replace(".idx", "");
                    saveVfs = new FilePackage(config.export_file_path + "/" + pkgName);
                }
                
                byte[] data = vfs.load(leaf.path, byte[].class);
                if (data != null) {
                    saveVfs.save(leaf.path, data);
                }
            } catch (Exception e) {
                LogHelper.info(String.format("export file failed: %s", leaf.path), e);
            }
        }
    }
    
    private void exportImage(ViewerExecutorArgs args, Node selected, boolean recursively, boolean fixed) {
        Stage stage = args.getStage();
        IPackage vfs = args.getVfs();
        
        if (StringHelper.isNullOrEmpty(config.export_image_path) || !new File(config.export_image_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存图片的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_image_path = file.getAbsolutePath();
        }
        
        if (selected == null) {
            return;
        }
        
        IPackage saveVfs = null;
        
        List<Leaf> list = vfs.listLeaf(selected, recursively);
        for (Leaf leaf : list) {
            try {
                if (saveVfs == null) {
                    String pkgName = leaf.pack.replace(".idx", "");
                    saveVfs = new FilePackage(config.export_image_path + "/" + pkgName);
                    saveVfs.addHandler(BufferedImage.class, new BufferedImageHandler());
                }
                
                if (leaf.path.contains(".img")) {
                    ImgFile img = vfs.load(leaf.path, ImgFile.class);
                    if (img != null) {
                        for (ImgFile.Frame item : img.frames) {
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
    
    private void exportByCustom(ViewerExecutorArgs args, Node selected) {
        Stage stage = args.getStage();
        IPackage vfs = args.getVfs();
        StageHelper stageHelper = args.getStageHelper();
        
        if (StringHelper.isNullOrEmpty(config.export_image_path) || !new File(config.export_image_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存图片的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_image_path = file.getAbsolutePath();
        }
        
        if (selected == null) {
            return;
        }
        
        if (selected.leaf && selected.name.endsWith(".img")) {
            Leaf leaf = (Leaf) selected;
            
            String pkgName = leaf.pack.replace(".idx", "");
            IPackage saveVfs = new FilePackage(config.export_image_path + "/" + pkgName);
            saveVfs.addHandler(BufferedImage.class, new BufferedImageHandler());
            
            ImgFile img = vfs.load(leaf.path, ImgFile.class);
            if (img != null) {
                FixedExportView.Args _args = new FixedExportView.Args();
                _args.path = leaf.path;
                _args.img = img;
                _args.loadVfs = vfs;
                _args.saveVfs = saveVfs;
                stageHelper.show(stage, _args, FixedExportView.class);
            }
        }
    }
    
    private void exportMapImage(ViewerExecutorArgs args, Node selected) {
        Stage stage = args.getStage();
        IPackage vfs = args.getVfs();
        
        if (StringHelper.isNullOrEmpty(config.export_image_path) || !new File(config.export_image_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存图片的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_image_path = file.getAbsolutePath();
        }
        
        if (selected == null) {
            return;
        }
        
        if (selected.leaf && selected.name.endsWith(".map")) {
            Leaf leaf = (Leaf) selected;
            MapFile map = vfs.load(leaf.path, MapFile.class);
            if (map == null) {
                return;
            }
            
            MapElemProp prop = vfs.load("object\\mapElem\\mapElem.prop", MapElemProp.class);
            MapElemDataProvider provider = new MapElemDataProvider(prop);
            
            BufferedImage image = new BufferedImage(map.width * 40 + 160, map.height * 40 + 160, BufferedImage.TYPE_INT_ARGB);
            
            for (int i = 2; i >= 0; --i) {
                MapFile.Layer layer = map.layers[i];
                for (int y = 0; y < map.height; ++y) {
                    for (int x = 0; x < map.width; ++x) {
                        MapFile.Element element = layer.elements[y][x];
                        processElement(args.getVfs(), element, x, y, provider, image);
                    }
                }
            }
            
            try {
                GameMode mode = GameMode.valueOf(map.gameMode);
                MapFile.Points points = map.points[3];
                for (int i = 0; i < mode.getSpecials().length && i < points.points.length; ++i) {
                    MapFile.Element element = mode.getSpecials()[i];
                    MapFile.Point point = points.points[i];
                    
                    processElement(args.getVfs(), element, point.x, point.y, provider, image);
                }
            } catch (Exception e) {
            
            }
            
            IPackage saveVfs = new FilePackage(config.export_image_path + "/map");
            saveVfs.addHandler(BufferedImage.class, new BufferedImageHandler());
            
            saveVfs.save(leaf.path + ".png", image);
        }
    }
    
    private void processElement(IPackage vfs, MapFile.Element element, int x, int y, MapElemDataProvider provider, BufferedImage graphics) {
        if (element.city() <= 0 || element.id() <= 0) {
            return;
        }
        
        MapCity city = MapCity.valueOf(element.city());
        String path = String.format("object/mapelem/%s/elem%d_stand.img", city.getName(), element.id());
        ImgFile img = vfs.load(path, ImgFile.class);
        if (img == null) {
            return;
        }
        
        ImgFile.Frame item = img.frames[0];
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
}
