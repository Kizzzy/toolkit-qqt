package cn.kizzzy.qqt.display;

import cn.kizzzy.javafx.display.Display;
import cn.kizzzy.javafx.display.DisplayAAA;
import cn.kizzzy.javafx.display.DisplayAttribute;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.javafx.display.image.DisplayFrame;
import cn.kizzzy.javafx.display.image.DisplayTrack;
import cn.kizzzy.javafx.display.image.DisplayTracks;
import cn.kizzzy.qqt.QqtAvatar;
import cn.kizzzy.qqt.QqtAvatarElementComparator;
import cn.kizzzy.qqt.QqtImg;
import cn.kizzzy.qqt.QqtImgItem;
import cn.kizzzy.qqt.helper.QqtImgHelper;
import cn.kizzzy.vfs.IPackage;

import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

@DisplayAttribute(suffix = {
    "ini",
}, priority = 999)
public class IniDisplay extends Display<IPackage> {
    
    private static final Comparator<QqtAvatar.Element> comparator
        = new QqtAvatarElementComparator();
    
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
    
    public IniDisplay(IPackage vfs, String path) {
        super(vfs, path);
    }
    
    //int j = 0;
    
    @Override
    public DisplayAAA load() {
        if (!path.contains("player") || !path.endsWith(".ini")) {
            return null;
        }
        
        QqtAvatar qqtAvatar = context.load(path, QqtAvatar.class);
        if (qqtAvatar == null) {
            return null;
        }
        
        QqtAvatar.Avatar avatar = qqtAvatar.avatarKvs.get("walk");
        if (avatar == null) {
            return null;
        }
        
        DisplayTracks tracks = new DisplayTracks();
        tracks.colors = COLORS;
        
        int i = 0;
        //j = 0;
        
        List<QqtAvatar.Element> elements = new LinkedList<>(avatar.elementKvs.values());
        elements.sort(comparator);
        
        for (QqtAvatar.Element element : elements) {
            float time = 167 * (i++);
            
            processElement(element, time, tracks, false);
            processElement(element, time, tracks, true);
        }
        
        return new DisplayAAA(DisplayType.SHOW_IMAGE, tracks);
    }
    
    private void processElement(QqtAvatar.Element element, float time, DisplayTracks tracks, boolean mixed) {
        String imgPath = String.format("object/%s/%s%s_%s%s.img", element.name, element.name, element.id, "walk", mixed ? "_m" : "");
        QqtImg img = context.load(imgPath, QqtImg.class);
        if (img == null) {
            return;
        }
        
        DisplayTrack track = new DisplayTrack();
        
        for (int i = 0, n = img.count / img.planes; i < n; ++i) {
            QqtImgItem item = img.items[i];
            
            BufferedImage image = QqtImgHelper.toImage(item);
            if (image != null) {
                float diffX = img.maxWidth / 2f + img.offsetX;
                float diffY = img.maxHeight + img.offsetY - 20;
                
                DisplayFrame frame = new DisplayFrame();
                frame.x = 200 + item.offsetX - diffX;
                frame.y = 200 + item.offsetY - diffY;
                frame.width = item.width;
                frame.height = item.height;
                frame.image = image;
                frame.time = time;
                frame.mixed = mixed;
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
