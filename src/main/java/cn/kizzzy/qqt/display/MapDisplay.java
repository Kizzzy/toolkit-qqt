package cn.kizzzy.qqt.display;

import cn.kizzzy.helper.LogHelper;
import cn.kizzzy.javafx.display.DisplayParam;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.qqt.QqtImg;
import cn.kizzzy.qqt.QqtImgItem;
import cn.kizzzy.qqt.QqtMap;
import cn.kizzzy.qqt.helper.QqtImgHelper;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@DisplayFlag(suffix = {
    "map",
})
public class MapDisplay extends Display {
    private int index;
    private int total;
    private QqtMap map;
    
    private List<DisplayParam> params;
    
    public MapDisplay(DisplayContext context, String path) {
        super(context, path);
    }
    
    @Override
    public void init() {
        QqtMap map = context.load(path, QqtMap.class);
        
        index = 0;
        total = 1;
        
        params = new LinkedList<>();
        for (int i = 0; i < 3; ++i) {
            params.addAll(createImage(map.layers.get(i), 15, 13, 20, i * 320));
        }
        
        QqtImg img = context.load(path.replace(".map", ".img"), QqtImg.class);
        if (img != null) {
            for (QqtImgItem item : img.items) {
                DisplayParam param = new DisplayParam.Builder()
                    .setX(200)
                    .setY(480)
                    .setWidth(item.width)
                    .setHeight(item.height)
                    .setImage(QqtImgHelper.toImage(item))
                    .build();
                params.add(param);
            }
        }
        
        displayImpl();
    }
    
    @Override
    public void prev() {
        index--;
        if (index < 0) {
            index = total - 1;
        }
        
        displayImpl();
    }
    
    @Override
    public void next() {
        index++;
        if (index >= total) {
            index = 0;
        }
        
        displayImpl();
    }
    
    @Override
    public void play() {
        next();
    }
    
    protected void displayImpl() {
        try {
            context.notifyListener(DisplayType.SHOW_IMAGE, params);
        } catch (Exception e) {
            LogHelper.error(null, e);
        }
    }
    
    public List<DisplayParam> createImage(int[][] data, int w, int h, int unit, int offset) {
        
        BufferedImage image = new BufferedImage(w * unit, h * unit, BufferedImage.TYPE_INT_ARGB);
        
        Map<Long, Integer> countKvs = new TreeMap<>((o1, o2) -> (int) ((o1 & 0xff) - (o2 & 0xff)));
        
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
        
        List<DisplayParam> params = new LinkedList<>();
        DisplayParam param = new DisplayParam.Builder()
            .setX(200 + offset)
            .setY(200)
            .setWidth(w * unit)
            .setHeight(h * unit)
            .setImage(image)
            .build();
        params.add(param);
        return params;
    }
}
