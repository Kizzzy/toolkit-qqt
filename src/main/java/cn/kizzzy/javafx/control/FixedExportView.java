package cn.kizzzy.javafx.control;

import cn.kizzzy.helper.FileHelper;
import cn.kizzzy.javafx.JavafxControlParameter;
import cn.kizzzy.javafx.JavafxView;
import cn.kizzzy.javafx.Stageable;
import cn.kizzzy.javafx.display.image.Frame;
import cn.kizzzy.javafx.display.image.ImageArg;
import cn.kizzzy.javafx.display.image.ImageDisplayView;
import cn.kizzzy.javafx.display.image.Track;
import cn.kizzzy.qqt.AvatarFile;
import cn.kizzzy.qqt.ImgFile;
import cn.kizzzy.qqt.helper.QqtImgHelper;
import cn.kizzzy.vfs.IPackage;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class FixedExportViewBase extends JavafxView {
    
    protected static class SliderValueChangedListener implements ChangeListener<Number> {
        
        private final Label label;
        
        private final Runnable callback;
        
        public SliderValueChangedListener(Label label, Runnable callback) {
            this.label = label;
            this.callback = callback;
        }
        
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            label.setText("" + newValue.intValue());
            if (callback != null) {
                callback.run();
            }
        }
    }
    
    @FXML
    protected Slider width_sld;
    
    @FXML
    protected Label width_val;
    
    @FXML
    protected Slider height_sld;
    
    @FXML
    protected Label height_val;
    
    @FXML
    protected Slider pivot_x_sld;
    
    @FXML
    protected Label pivot_x_val;
    
    @FXML
    protected Slider pivot_y_sld;
    
    @FXML
    protected Label pivot_y_val;
    
    @FXML
    protected Button display_btn;
    
    @FXML
    protected Button export_btn;
    
    @FXML
    protected ImageDisplayView image_idv;
}

@JavafxControlParameter(fxml = "/fxml/custom/fixed_export_view.fxml")
public class FixedExportView extends FixedExportViewBase implements Initializable, Stageable<FixedExportView.Args> {
    
    private static final Logger logger = LoggerFactory.getLogger(FixedExportView.class);
    
    public static class Args {
        public String path;
        public ImgFile img;
        public IPackage loadVfs;
        public IPackage saveVfs;
    }
    
    private static class Rect {
        public int x;
        public int y;
        public int width;
        public int height;
        
        public Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
    
    private static class TargetFrame {
        public Frame frame;
        public float originX;
        public float originY;
    }
    
    private static final int zeroX = 200;
    private static final int zeroY = 200;
    
    private static final String[] DIRS = new String[]{
        "right",
        "up",
        "left",
        "down",
    };
    
    private Stage stage;
    private Args args;
    
    private ImageArg arg;
    private List<TargetFrame> targetFrames;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        export_btn.setOnAction(this::onExport);
        display_btn.setOnAction(event -> applyDrawImpl());
        
        width_sld.valueProperty().addListener(new SliderValueChangedListener(width_val, this::applyDrawImpl));
        height_sld.valueProperty().addListener(new SliderValueChangedListener(height_val, this::applyDrawImpl));
        pivot_x_sld.valueProperty().addListener(new SliderValueChangedListener(pivot_x_val, this::applyDrawImpl));
        pivot_y_sld.valueProperty().addListener(new SliderValueChangedListener(pivot_y_val, this::applyDrawImpl));
    }
    
    private void onExport(ActionEvent actionEvent) {
        Rect rect = getInput();
        
        for (ImgFile.Frame item : args.img.frames) {
            try {
                BufferedImage image = QqtImgHelper.toImageByCustom(item, rect.x, rect.y, rect.width, rect.height);
                String fullPath = args.path.replace(".img", String.format("-%02d.png", item.index));
                args.saveVfs.save(fullPath, image);
            } catch (Exception e) {
                logger.error("export error", e);
            }
        }
    }
    
    @Override
    public void show(Stage stage, Args args) {
        this.stage = stage;
        this.args = args;
        
        try {
            String name = FileHelper.getName(args.path);
            Matcher matcher = Pattern.compile("([a-zA-Z]+)(\\d+)_(\\w+).img").matcher(name);
            if (!matcher.matches()) {
                return;
            }
            
            String elementName = matcher.group(1);
            String elementId = matcher.group(2);
            String action = matcher.group(3);
            
            targetFrames = new LinkedList<>();
            
            arg = preloadImpl(args.loadVfs, elementName, elementId, action);
            if (arg != null) {
                applyDrawImpl();
            }
        } catch (Exception e) {
            logger.error("show error", e);
        }
    }
    
    private ImageArg preloadImpl(IPackage vfs, String elementName, String elementId, String action) throws Exception {
        AvatarFile zIndex = vfs.load("object/player/player_z.ini", AvatarFile.class);
        if (zIndex == null) {
            logger.info("load avatar z-index failed");
            return null;
        }
        
        AvatarFile.Avatar wIndex = zIndex.avatarKvs.get(action);
        if (wIndex == null) {
            logger.info("z-index of this action is not found");
            return null;
        }
        
        AvatarFile qqtAvatar = vfs.load("object/player/player2.ini", AvatarFile.class);
        if (qqtAvatar == null) {
            logger.info("load avatar failed");
            return null;
        }
        
        AvatarFile.Avatar avatar = qqtAvatar.avatarKvs.get(action);
        if (avatar == null) {
            logger.info("avatar of this action is not found");
            return null;
        }
        
        int i = 0;
        ImageArg arg = new ImageArg();
        for (AvatarFile.Element element : avatar.elementKvs.values()) {
            boolean target = Objects.equals(element.name, elementName);
            if (target) {
                element.id = elementId;
            }
            float time = 167;
            processElement(element, action, time, arg, wIndex, false, vfs, target);
            processElement(element, action, time, arg, wIndex, true, vfs, target);
        }
        return arg;
    }
    
    private void processElement(AvatarFile.Element element, String action, float time, ImageArg arg, AvatarFile.Avatar zAvatar, boolean mixed, IPackage vfs, boolean target) throws Exception {
        String fullPath = String.format("object/%s/%s%s_%s%s.img", element.name, element.name, element.id, action, mixed ? "_m" : "");
        ImgFile img = vfs.load(fullPath, ImgFile.class);
        if (img == null) {
            return;
        }
        
        Track track = new Track();
        
        for (int i = 0, n = img.count; i < n; ++i) {
            ImgFile.Frame item = img.frames[i];
            
            BufferedImage image = QqtImgHelper.toImage(item);
            if (image != null) {
                int dir = i / (img.count / img.planes);
                String key = String.format("%s_z_%s", element.name, DIRS[dir]);
                AvatarFile.Element zElement = zAvatar.elementKvs.get(key);
                
                float offsetX = -img.maxWidth / 2f - img.offsetX + item.offsetX;
                float offsetY = -img.maxHeight - img.offsetY + item.offsetY + 20;
                
                Frame frame = new Frame();
                frame.x = offsetX;
                frame.y = offsetY;
                frame.width = item.width;
                frame.height = item.height;
                frame.image = image;
                frame.time = time;
                frame.mixed = mixed;
                frame.order = zElement == null ? 0 : Integer.parseInt(zElement.id);
                //frame.extra = String.format("%02d/%02d", i, img.count);
                
                track.frames.add(frame);
                
                if (target) {
                    TargetFrame targetFrame = new TargetFrame();
                    targetFrame.frame = frame;
                    targetFrame.originX = frame.x;
                    targetFrame.originY = frame.y;
                    
                    targetFrames.add(targetFrame);
                }
            }
        }
        
        arg.tracks.add(track);
    }
    
    private Rect getInput() {
        try {
            int width = (int) width_sld.getValue();
            int height = (int) height_sld.getValue();
            int pivotX = (int) pivot_x_sld.getValue();
            int pivotY = (int) pivot_y_sld.getValue();
            
            return new Rect(pivotX, pivotY, width, height);
        } catch (Exception e) {
            e.printStackTrace();
            return new Rect(0, 0, 100, 100);
        }
    }
    
    private void applyDrawImpl() {
        if (arg == null) {
            return;
        }
        
        Rect rect = getInput();
        
        // change display pivot
        arg.pivotX = zeroX;
        arg.pivotY = zeroY;
        
        // change display border
        arg.borderX = (zeroX - rect.width / 2f);
        arg.borderY = (zeroY - rect.height);
        arg.borderW = rect.width;
        arg.borderH = rect.height;
        
        // change display target frame
        for (TargetFrame targetFrame : targetFrames) {
            targetFrame.frame.x = targetFrame.originX + rect.x;
            targetFrame.frame.y = targetFrame.originY + rect.y;
        }
        
        // redraw
        image_idv.show(arg);
    }
}
