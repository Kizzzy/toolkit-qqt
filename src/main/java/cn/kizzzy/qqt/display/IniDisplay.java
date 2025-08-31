package cn.kizzzy.qqt.display;

import cn.kizzzy.javafx.display.DisplayLoaderAttribute;
import cn.kizzzy.javafx.display.image.ImageArg;
import cn.kizzzy.javafx.display.image.ImageDisplayLoader;
import cn.kizzzy.javafx.display.image.Track;
import cn.kizzzy.qqt.AvatarFile;
import cn.kizzzy.qqt.ImgFile;
import cn.kizzzy.qqt.helper.QqtImgHelper;
import cn.kizzzy.vfs.IPackage;
import cn.kizzzy.vfs.tree.Leaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

@DisplayLoaderAttribute(suffix = {
    "ini",
}, priority = 999)
public class IniDisplay implements ImageDisplayLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(IniDisplay.class);
    
    private static final String[] COLORS = new String[]{
        "#d34a37ff",
        "#1d3bbfff",
        "#ebe35cff",
        "#518c2eff",
        "#cd3985ff",
        "#e09f43ff",
        "#a02bbaff",
        "#595959ff",
    };
    
    private static final String[] DIRS = new String[]{
        "right",
        "up",
        "left",
        "down",
    };
    
    // int j = 0;
    
    @Override
    public ImageArg loadImage(IPackage vfs, Leaf leaf) throws Exception {
        if (!leaf.path.contains("player") || !leaf.path.endsWith(".ini")) {
            return null;
        }
        
        String action = "walk";
        
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
        
        AvatarFile qqtAvatar = vfs.load(leaf.path, AvatarFile.class);
        if (qqtAvatar == null) {
            logger.info("load avatar failed");
            return null;
        }
        
        AvatarFile.Avatar avatar = qqtAvatar.avatarKvs.get(action);
        if (avatar == null) {
            logger.info("avatar of this action is not found");
            return null;
        }
        
        ImageArg arg = new ImageArg();
        arg.colors = COLORS;
        
        int i = 0;
        // j = 0;
        
        for (AvatarFile.Element element : avatar.elementKvs.values()) {
            float time = 167 * (i++);
            processElement(vfs, element, action, time, arg, wIndex, false);
            processElement(vfs, element, action, time, arg, wIndex, true);
        }
        
        return arg;
    }
    
    private void processElement(IPackage vfs, AvatarFile.Element element, String action, float time, ImageArg arg, AvatarFile.Avatar zAvatar, boolean mixed) throws Exception {
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
                
                Track.StaticFrame sf = new Track.StaticFrame();
                sf.x = offsetX;
                sf.y = offsetY;
                sf.width = item.width;
                sf.height = item.height;
                sf.image = image;
                sf.time = time;
                sf.mixed = mixed;
                sf.order = zElement == null ? 0 : Integer.parseInt(zElement.id);
                sf.extra = String.format("%02d/%02d", i, img.count);
                
                track.sfs.add(sf);
                /*
                Track track2 = new Track();
                
                Frame frame2 = new Frame();
                frame2.x = 300 + (j % n) * 80;
                frame2.y = 80 + (j / n) * 80;
                frame2.width = item.width;
                frame2.height = item.height;
                frame2.image = image;
                frame2.time = time;
                frame2.mixed = mixed;
                //sf.extra = String.format("%02d/%02d", i, img.count);
                
                track2.frames.add(frame2);
                
                arg.tracks.add(track2);
                
                j++;*/
            }
        }
        
        arg.tracks.add(track);
    }
}
