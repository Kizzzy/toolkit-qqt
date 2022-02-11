package cn.kizzzy.qqt.display;

import cn.kizzzy.javafx.display.Display;
import cn.kizzzy.javafx.display.DisplayAAA;
import cn.kizzzy.javafx.display.DisplayAttribute;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.javafx.display.image.DisplayFrame;
import cn.kizzzy.javafx.display.image.DisplayTrack;
import cn.kizzzy.javafx.display.image.DisplayTracks;
import cn.kizzzy.qqt.QqtImg;
import cn.kizzzy.qqt.QqtImgItem;
import cn.kizzzy.qqt.QqtMap;
import cn.kizzzy.qqt.helper.QqtImgHelper;
import cn.kizzzy.vfs.IPackage;

import java.awt.image.BufferedImage;

@DisplayAttribute(suffix = {
    "map",
})
public class MapDisplay extends Display<IPackage> {
    
    public MapDisplay(IPackage context, String path) {
        super(context, path);
    }
    
    @Override
    public DisplayAAA load() {
        QqtMap map = context.load(path, QqtMap.class);
        if (map == null) {
            return null;
        }
        
        DisplayTracks tracks = new DisplayTracks();
        
        for (int i = 0; i < 3; ++i) {
            DisplayFrame frame = new DisplayFrame();
            frame.x = 200 + i * 320;
            frame.y = 200;
            frame.width = 15 * 20;
            frame.height = 13 * 20;
            frame.image = createImage(map.layers.get(i), 15, 13, 20);
            frame.extra = "";
            
            DisplayTrack track = new DisplayTrack();
            track.frames.add(frame);
            
            tracks.tracks.add(track);
        }
        
        QqtImg img = context.load(path.replace(".map", ".img"), QqtImg.class);
        if (img != null) {
            DisplayTrack track = new DisplayTrack();
            
            for (QqtImgItem item : img.items) {
                DisplayFrame frame = new DisplayFrame();
                frame.x = 200;
                frame.y = 480;
                frame.width = item.width;
                frame.height = item.height;
                frame.image = QqtImgHelper.toImage(item);
                frame.extra = "";
                
                track.frames.add(frame);
            }
            tracks.tracks.add(track);
        }
        
        return new DisplayAAA(DisplayType.SHOW_IMAGE, tracks);
    }
    
    public BufferedImage createImage(int[][] data, int w, int h, int unit) {
        BufferedImage image = new BufferedImage(w * unit, h * unit, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                int rgb = data[y][x];
                for (int m = 0; m < unit; ++m) {
                    for (int n = 0; n < unit; ++n) {
                        image.setRGB(x * unit + m, y * unit + n, (int) (rgb | 0x4f000000));
                    }
                }
            }
        }
        return image;
    }
}