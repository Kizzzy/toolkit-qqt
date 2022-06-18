package cn.kizzzy.qqt.display;

import cn.kizzzy.javafx.display.Display;
import cn.kizzzy.javafx.display.DisplayAAA;
import cn.kizzzy.javafx.display.DisplayAttribute;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.javafx.display.image.DisplayFrame;
import cn.kizzzy.javafx.display.image.DisplayTrack;
import cn.kizzzy.javafx.display.image.DisplayTracks;
import cn.kizzzy.qqt.GameMode;
import cn.kizzzy.qqt.ImgFile;
import cn.kizzzy.qqt.MapCity;
import cn.kizzzy.qqt.MapElemDataProvider;
import cn.kizzzy.qqt.MapElemProp;
import cn.kizzzy.qqt.MapFile;
import cn.kizzzy.qqt.QqtElementXyer;
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
        MapFile map = context.load(path, MapFile.class);
        if (map == null) {
            return null;
        }
        
        MapElemProp prop = context.load("object\\mapElem\\mapElem.prop", MapElemProp.class);
        MapElemDataProvider provider = new MapElemDataProvider(prop);
        
        DisplayTracks tracks = new DisplayTracks();
        
        for (int i = 2; i >= 0; --i) {
            MapFile.Layer layer = map.layers[i];
            for (int y = 0; y < map.height; ++y) {
                for (int x = 0; x < map.width; ++x) {
                    MapFile.Element element = layer.elements[y][x];
                    processElement(element, x, y, provider, tracks);
                }
            }
        }
        
        try {
            GameMode mode = GameMode.valueOf(map.gameMode);
            MapFile.Points points = map.points[3];
            for (int i = 0; i < mode.getSpecials().length && i < points.points.length; ++i) {
                MapFile.Element element = mode.getSpecials()[i];
                MapFile.Point point = points.points[i];
                
                processElement(element, point.x, point.y, provider, tracks);
            }
        } catch (Exception e) {
        
        }
        
        return new DisplayAAA(DisplayType.SHOW_IMAGE, tracks);
    }
    
    private void processElement(MapFile.Element element, int x, int y, MapElemDataProvider provider, DisplayTracks tracks) {
        if (element.city() <= 0 || element.id() <= 0) {
            return;
        }
        
        MapCity city = MapCity.valueOf(element.city());
        String path = String.format("object/mapelem/%s/elem%d_stand.img", city.getName(), element.id());
        ImgFile img = context.load(path, ImgFile.class);
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
        
        DisplayFrame frame = new DisplayFrame();
        frame.x = 200 + x * 40 - elementData.x + point.x;
        frame.y = 200 + y * 40 - elementData.y + point.y;
        frame.width = image.getWidth();
        frame.height = image.getHeight();
        frame.image = image;
        frame.extra = "";
        
        DisplayTrack track = new DisplayTrack();
        track.frames.add(frame);
        
        tracks.tracks.add(track);
    }
}