package cn.kizzzy.qqt.display;

import cn.kizzzy.javafx.display.Display;
import cn.kizzzy.javafx.display.DisplayAAA;
import cn.kizzzy.javafx.display.DisplayAttribute;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.javafx.display.image.DisplayFrame;
import cn.kizzzy.javafx.display.image.DisplayTrack;
import cn.kizzzy.javafx.display.image.DisplayTracks;
import cn.kizzzy.qqt.ImgFile;
import cn.kizzzy.qqt.helper.QqtImgHelper;
import cn.kizzzy.vfs.IPackage;

import java.awt.image.BufferedImage;

@DisplayAttribute(suffix = {
    "img",
})
public class ImgDisplay extends Display<IPackage> {
    
    public ImgDisplay(IPackage vfs, String path) {
        super(vfs, path);
    }
    
    @Override
    public DisplayAAA load() {
        ImgFile img = context.load(path, ImgFile.class);
        if (img == null) {
            return null;
        }
        
        DisplayTrack track = new DisplayTrack();
        int i = 0;
        for (ImgFile.Frame item : img.frames) {
            BufferedImage image = QqtImgHelper.toImage(item);
            if (image != null) {
                float offsetX = -img.maxWidth / 2f - img.offsetX + item.offsetX;
                float offsetY = -img.maxHeight - img.offsetY + item.offsetY + 20;
                
                DisplayFrame frame = new DisplayFrame();
                frame.x = 200 + offsetX;
                frame.y = 200 + offsetY;
                frame.width = item.width;
                frame.height = item.height;
                frame.image = image;
                frame.time = 167 * i;
                frame.extra = String.format("%02d/%02d", i + 1, img.count);
                
                track.frames.add(frame);
            }
            i++;
        }
        
        DisplayTracks tracks = new DisplayTracks();
        tracks.tracks.add(track);
        return new DisplayAAA(DisplayType.SHOW_IMAGE, tracks);
    }
}
