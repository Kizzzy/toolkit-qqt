package cn.kizzzy.qqt.display;

import cn.kizzzy.javafx.display.DisplayLoaderAttribute;
import cn.kizzzy.javafx.display.image.Frame;
import cn.kizzzy.javafx.display.image.ImageArg;
import cn.kizzzy.javafx.display.image.ImageDisplayLoader;
import cn.kizzzy.javafx.display.image.Track;
import cn.kizzzy.qqt.GameMode;
import cn.kizzzy.qqt.ImgFile;
import cn.kizzzy.qqt.MapCity;
import cn.kizzzy.qqt.MapElemDataProvider;
import cn.kizzzy.qqt.MapElemProp;
import cn.kizzzy.qqt.MapFile;
import cn.kizzzy.qqt.QqtElementXyer;
import cn.kizzzy.qqt.helper.QqtImgHelper;
import cn.kizzzy.vfs.IPackage;
import cn.kizzzy.vfs.tree.Leaf;

import java.awt.image.BufferedImage;

@DisplayLoaderAttribute(suffix = {
    "map",
})
public class MapDisplay implements ImageDisplayLoader {
    
    @Override
    public ImageArg loadImage(IPackage vfs, Leaf leaf) throws Exception {
        MapFile map = vfs.load(leaf.path, MapFile.class);
        if (map == null) {
            return null;
        }
        
        MapElemProp prop = vfs.load("object\\mapElem\\mapElem.prop", MapElemProp.class);
        MapElemDataProvider provider = new MapElemDataProvider(prop);
        
        ImageArg arg = new ImageArg();
        
        for (int i = 2; i >= 0; --i) {
            MapFile.Layer layer = map.layers[i];
            for (int y = 0; y < map.height; ++y) {
                for (int x = 0; x < map.width; ++x) {
                    MapFile.Element element = layer.elements[y][x];
                    processElement(vfs, element, x, y, provider, arg);
                }
            }
        }
        
        try {
            GameMode mode = GameMode.valueOf(map.gameMode);
            MapFile.Points points = map.points[3];
            for (int i = 0; i < mode.getSpecials().length && i < points.points.length; ++i) {
                MapFile.Element element = mode.getSpecials()[i];
                MapFile.Point point = points.points[i];
                
                processElement(vfs, element, point.x, point.y, provider, arg);
            }
        } catch (Exception e) {
        
        }
        
        return arg;
    }
    
    private void processElement(IPackage vfs, MapFile.Element element, int x, int y, MapElemDataProvider provider, ImageArg arg) {
        if (element.city() <= 0 || element.id() <= 0) {
            return;
        }
        
        MapCity city = MapCity.valueOf(element.city());
        String path = String.format("object/mapelem/%s/elem%d_stand.img", city.getName(), element.id());
        ImgFile img = vfs.load(path, ImgFile.class);
        if (img == null) {
            return;
        }
        
        Track track = new Track();
        arg.tracks.add(track);
        
        for (ImgFile.Frame item : img.frames) {
            BufferedImage image = QqtImgHelper.toImage(item);
            if (image == null) {
                return;
            }
            
            MapElemProp.Element elementData = provider.getElementData(element.value);
            if (elementData == null) {
                return;
            }
            
            QqtElementXyer.Point point = QqtElementXyer.INS.GetXy(element.value);
            
            Frame frame = new Frame();
            frame.x = x * 40 - elementData.x + point.x;
            frame.y = y * 40 - elementData.y + point.y;
            frame.width = image.getWidth();
            frame.height = image.getHeight();
            frame.image = image;
            frame.time = 167;
            frame.extra = "";
            
            track.frames.add(frame);
        }
    }
}