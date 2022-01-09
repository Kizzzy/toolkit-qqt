package cn.kizzzy.display;

import cn.kizzzy.helper.LogHelper;
import cn.kizzzy.javafx.display.DisplayParam;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.qqt.QqtImg;
import cn.kizzzy.qqt.QqtImgItem;
import cn.kizzzy.qqt.helper.QqtImgHelper;

import java.awt.image.BufferedImage;
import java.util.Collections;

@DisplayFlag(suffix = {
    "img",
})
public class ImgDisplay extends Display {
    
    private int index;
    private int total;
    
    private String[] infos;
    private DisplayParam[] params;
    
    public ImgDisplay(DisplayContext context, String path) {
        super(context, path);
    }
    
    @Override
    public void init() {
        QqtImg img = context.load(path, QqtImg.class);
        
        index = 0;
        total = img.items.length - 1;
        
        context.notifyListener(DisplayType.SHOW_TEXT, img.toString());
        
        infos = new String[img.count];
        params = new DisplayParam[img.count];
        for (int i = 0; i < img.count; ++i) {
            QqtImgItem item = img.items[i];
            
            infos[i] = String.format(
                "Show Image(%d/%d) [%d * %d * %s]",
                i + 1,
                item.file.count,
                item.width,
                item.height,
                retrieveImageType(item.type)
            );
            
            BufferedImage image = QqtImgHelper.toImage(item);
            wrapperImage(image);
            
            params[i] = new DisplayParam.Builder()
                .setX(getLayoutX(item))
                .setY(getLayoutY(item))
                .setWidth(item.width)
                .setHeight(item.height)
                .setImage(image)
                .build();
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
            context.notifyListener(DisplayType.TOAST_TIPS, infos[index]);
            context.notifyListener(DisplayType.SHOW_IMAGE, Collections.singletonList(params[index]));
        } catch (Exception e) {
            LogHelper.error(null, e);
        }
    }
}
