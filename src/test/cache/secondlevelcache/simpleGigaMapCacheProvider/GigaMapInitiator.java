package test.cache.secondlevelcache.simpleGigaMapCacheProvider;

import org.openspaces.core.GigaMap;
import org.openspaces.hibernate.cache.SimpleGigaMapCacheProvider;

public class GigaMapInitiator {

    public GigaMapInitiator(GigaMap gigaMap) {
        SimpleGigaMapCacheProvider.setMapContext(gigaMap);
    }
}
