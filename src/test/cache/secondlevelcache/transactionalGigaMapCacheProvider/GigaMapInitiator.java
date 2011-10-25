package test.cache.secondlevelcache.transactionalGigaMapCacheProvider;

import org.openspaces.core.GigaMap;
import org.openspaces.hibernate.cache.TransactionalGigaMapCacheProvider;

public class GigaMapInitiator {

    public GigaMapInitiator(GigaMap gigaMap) {
        TransactionalGigaMapCacheProvider.setMapContext(gigaMap);
    }
}
