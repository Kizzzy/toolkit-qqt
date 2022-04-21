package cn.kizzzy.qqt.display;

import cn.kizzzy.javafx.display.Display;
import cn.kizzzy.javafx.display.DisplayAAA;
import cn.kizzzy.javafx.display.DisplayAttribute;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.javafx.display.image.DisplayFrame;
import cn.kizzzy.javafx.display.image.DisplayTrack;
import cn.kizzzy.javafx.display.image.DisplayTracks;
import cn.kizzzy.qqt.MapCity;
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
        
        for (int i = 2; i >= 0; --i) {
            DisplayFrame frame = new DisplayFrame();
            frame.x = 200;
            frame.y = 200;
            frame.width = 15 * 40;
            frame.height = 13 * 40;
            frame.image = createImage(map.layers[i], 15, 13, 40, 40);
            frame.extra = "";
            
            DisplayTrack track = new DisplayTrack();
            track.frames.add(frame);
            
            tracks.tracks.add(track);
        }
        
        return new DisplayAAA(DisplayType.SHOW_IMAGE, tracks);
    }
    
    public BufferedImage createImage(QqtMap.Layer layer, int w, int h, int unitX, int unitY) {
        BufferedImage image = new BufferedImage(w * unitX, h * unitY, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                QqtMap.Element element = layer.elements[y][x];
                if (element.city > 0 && element.id > 0) {
                    MapCity city = MapCity.valueOf(element.city);
                    
                    String path = String.format("object/mapelem/%s/elem%d_stand.img", city.getName(), element.id);
                    QqtImg img = context.load(path, QqtImg.class);
                    if (img != null) {
                        QqtImgItem item = img.items[0];
                        BufferedImage child = QqtImgHelper.toImage(item);
                        if (child != null) {
                            for (int m = 0; m < unitX && m < item.width; ++m) {
                                for (int n = 0; n < unitY && n < item.height; ++n) {
                                    int rgb = child.getRGB(m, n);
                                    
                                    image.setRGB(x * unitX + m, y * unitY + n, rgb);
                                }
                            }
                        }
                    }
                }
            }
        }
        return image;
    }
}