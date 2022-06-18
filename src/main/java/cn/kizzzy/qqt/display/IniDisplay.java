package cn.kizzzy.qqt.display;

import cn.kizzzy.helper.LogHelper;
import cn.kizzzy.javafx.display.Display;
import cn.kizzzy.javafx.display.DisplayAAA;
import cn.kizzzy.javafx.display.DisplayAttribute;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.javafx.display.image.DisplayFrame;
import cn.kizzzy.javafx.display.image.DisplayTrack;
import cn.kizzzy.javafx.display.image.DisplayTracks;
import cn.kizzzy.qqt.AvatarFile;
import cn.kizzzy.qqt.ImgFile;
import cn.kizzzy.qqt.helper.QqtImgHelper;
import cn.kizzzy.vfs.IPackage;

import java.awt.image.BufferedImage;

@DisplayAttribute(suffix = {
    "ini",
}, priority = 999)
public class IniDisplay extends Display<IPackage> {
    
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
    
    public IniDisplay(IPackage vfs, String path) {
        super(vfs, path);
    }
    
    //int j = 0;
    
    @Override
    public DisplayAAA load() {
        if (!path.contains("player") || !path.endsWith(".ini")) {
            return null;
        }
        
        String action = "walk";
        
        AvatarFile zIndex = context.load("object/player/player_z.ini", AvatarFile.class);
        if (zIndex == null) {
            LogHelper.info("load avatar z-index failed");
            return null;
        }
        
        AvatarFile.Avatar wIndex = zIndex.avatarKvs.get(action);
        if (wIndex == null) {
            LogHelper.info("z-index of this action is not found");
            return null;
        }
        
        AvatarFile qqtAvatar = context.load(path, AvatarFile.class);
        if (qqtAvatar == null) {
            LogHelper.info("load avatar failed");
            return null;
        }
        
        AvatarFile.Avatar avatar = qqtAvatar.avatarKvs.get(action);
        if (avatar == null) {
            LogHelper.info("avatar of this action is not found");
            return null;
        }
        
        DisplayTracks tracks = new DisplayTracks();
        tracks.colors = COLORS;
        
        int i = 0;
        //j = 0;
        
        for (AvatarFile.Element element : avatar.elementKvs.values()) {
            float time = 167 * (i++);
            processElement(element, action, time, tracks, wIndex, false);
            processElement(element, action, time, tracks, wIndex, true);
        }
        
        return new DisplayAAA(DisplayType.SHOW_IMAGE, tracks);
    }
    
    private void processElement(AvatarFile.Element element, String action, float time, DisplayTracks tracks, AvatarFile.Avatar zAvatar, boolean mixed) {
        String fullPath = String.format("object/%s/%s%s_%s%s.img", element.name, element.name, element.id, action, mixed ? "_m" : "");
        ImgFile img = context.load(fullPath, ImgFile.class);
        if (img == null) {
            return;
        }
        
        DisplayTrack track = new DisplayTrack();
        
        for (int i = 0, n = img.count; i < n; ++i) {
            ImgFile.Frame item = img.frames[i];
            
            BufferedImage image = QqtImgHelper.toImage(item);
            if (image != null) {
                int dir = i / (img.count / img.planes);
                String key = String.format("%s_z_%s", element.name, DIRS[dir]);
                AvatarFile.Element zElement = zAvatar.elementKvs.get(key);
                
                float offsetX = -img.maxWidth / 2f - img.offsetX + item.offsetX;
                float offsetY = -img.maxHeight - img.offsetY + item.offsetY + 20;
                
                DisplayFrame frame = new DisplayFrame();
                frame.x = 200 + offsetX;
                frame.y = 200 + offsetY;
                frame.width = item.width;
                frame.height = item.height;
                frame.image = image;
                frame.time = time;
                frame.mixed = mixed;
                frame.order = zElement == null ? 0 : Integer.parseInt(zElement.id);
                //frame.extra = String.format("%02d/%02d", i, img.count);
                
                track.frames.add(frame);
                /*
                DisplayTrack track2 = new DisplayTrack();
                
                DisplayFrame frame2 = new DisplayFrame();
                frame2.x = 300 + (j % n) * 80;
                frame2.y = 80 + (j / n) * 80;
                frame2.width = item.width;
                frame2.height = item.height;
                frame2.image = image;
                frame2.time = time;
                frame2.mixed = mixed;
                //frame.extra = String.format("%02d/%02d", i, img.count);
                
                track2.frames.add(frame2);
                
                tracks.tracks.add(track2);
                
                j++;*/
            }
        }
        
        tracks.tracks.add(track);
    }
}
