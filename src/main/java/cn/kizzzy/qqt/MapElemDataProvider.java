package cn.kizzzy.qqt;

import java.util.HashMap;
import java.util.Map;

public class MapElemDataProvider {
    
    private final MapElemProp prop;
    
    private final Map<Integer, MapElemProp.Element> kvs = new HashMap<>();
    
    public MapElemDataProvider(MapElemProp prop) {
        this.prop = prop;
        
        for (MapElemProp.Element element : prop.elements) {
            kvs.put(element.id, element);
        }
    }
    
    public MapElemProp.Element getElementData(int id) {
        return kvs.get(id);
    }
}
